# Shield

- Target Machine: 10.10.10.29

# Enumeration

## Nmap scan

```bash
[exyss@exyss Shield]$ nmap -p- -sC -sV 10.10.10.29 -oN nmap/all_ports.log
Starting Nmap 7.92 ( https://nmap.org ) at 2021-09-07 11:54 CEST
Nmap scan report for 10.10.10.29
Host is up (0.045s latency).
Not shown: 65533 filtered tcp ports (no-response)
PORT     STATE SERVICE VERSION
80/tcp   open  http    Microsoft IIS httpd 10.0
|_http-title: IIS Windows Server
|_http-server-header: Microsoft-IIS/10.0
| http-methods: 
|_  Potentially risky methods: TRACE
3306/tcp open  mysql   MySQL (unauthorized)
Service Info: OS: Windows; CPE: cpe:/o:microsoft:windows
```

### Results

- Windows server on port 80
- MySQL server on port 3306

## Gobuster dir discovery

```bash
[exyss@exyss Shield]$ gobuster dir -u 10.10.10.29 -w /usr/share/SecLists/Discovery/Web-Content/common.txt -o gobuster/dir_discovery.log
===============================================================
Gobuster v3.1.0
by OJ Reeves (@TheColonial) & Christian Mehlmauer (@firefart)
===============================================================
[+] Url:                     http://10.10.10.29
[+] Method:                  GET
[+] Threads:                 10
[+] Wordlist:                /usr/share/SecLists/Discovery/Web-Content/common.txt
[+] Negative Status codes:   404
[+] User Agent:              gobuster/3.1.0
[+] Timeout:                 10s
===============================================================
2021/09/07 11:59:38 Starting gobuster in directory enumeration mode
===============================================================
/wordpress            (Status: 301) [Size: 152] [--> http://10.10.10.29/wordpress/]
                                                                                   
===============================================================
2021/09/07 12:00:00 Finished
===============================================================

[exyss@exyss Shield]$ gobuster dir -u 10.10.10.29/wordpress -w /usr/share/SecLists/Discovery/Web-Content/common.txt -o gobuster/dir_discovery2.log
===============================================================
Gobuster v3.1.0
by OJ Reeves (@TheColonial) & Christian Mehlmauer (@firefart)
===============================================================
[+] Url:                     http://10.10.10.29/wordpress
[+] Method:                  GET
[+] Threads:                 10
[+] Wordlist:                /usr/share/SecLists/Discovery/Web-Content/common.txt
[+] Negative Status codes:   404
[+] User Agent:              gobuster/3.1.0
[+] Timeout:                 10s
===============================================================
2021/09/07 12:02:57 Starting gobuster in directory enumeration mode
===============================================================
/index.php            (Status: 301) [Size: 0] [--> http://10.10.10.29/wordpress/]
/wp-admin             (Status: 301) [Size: 161] [--> http://10.10.10.29/wordpress/wp-admin/]
/wp-content           (Status: 301) [Size: 163] [--> http://10.10.10.29/wordpress/wp-content/]
/wp-includes          (Status: 301) [Size: 164] [--> http://10.10.10.29/wordpress/wp-includes/]
/xmlrpc.php           (Status: 405) [Size: 42]                                                 
                                                                                               
===============================================================
2021/09/07 12:03:24 Finished
===============================================================

```

### Results

- /wordpress
    - /index.php
    - /wp-admin
    - /wp-content
    - /wp-includes
    - /xmlrpc.php

If we access the `/wp-admin` page, we get redirected to a login form. By testing with common credentials, we see that there is an user named `admin`. The password is the one found in the last challenge, `P@s5w0rd!`.

## Getting a reverse shell

Since we know that the website is run by an old version of WordPress, we can search for know exploits on the Internet, finding this Metasploit module that logs into the admin panel and generates a shell (for more info, check this [link](https://www.hackingarticles.in/wordpress-reverse-shell/))

NOT COMPLETED