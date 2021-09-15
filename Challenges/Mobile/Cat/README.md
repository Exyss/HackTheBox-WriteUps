# Cat

## Overview

* Difficulty: Easy
* Category: Mobile

## Description
> Easy leaks

## Approach

1. The given file is an Android Backup archive. We can extract it's content using `binwalk`, getting a tar archive, which can also be extracted using `tar -xf`

```bash
[exyss@exyss Cat]$ binwalk -e cat.ab 

DECIMAL       HEXADECIMAL     DESCRIPTION
--------------------------------------------------------------------------------
0             0x0             Android Backup, compressed, unencrypted
24            0x18            Zlib compressed data, best compression

[exyss@exyss Cat]$ cd _cat.ab.extracted/
[exyss@exyss _cat.ab.extracted]$ ls
18  18.zlib
[exyss@exyss _cat.ab.extracted]$ tar -xf 18
[exyss@exyss _cat.ab.extracted]$ ls
18  18.zlib  apps  shared
[exyss@exyss _cat.ab.extracted]$ cd shared/
[exyss@exyss shared]$ find .
.
./0
./0/Ringtones
./0/Movies
./0/Podcasts
./0/Alarms
./0/Pictures
./0/Pictures/IMAG0002.jpg
./0/Pictures/IMAG0004.jpg
./0/Pictures/IMAG0001.jpg
./0/Pictures/IMAG0006.jpg
./0/Pictures/IMAG0003.jpg
./0/Pictures/IMAG0005.jpg
./0/Download
./0/Notifications
./0/Music
./0/DCIM
```

2. Open the picture `IMAG0004.jpg` with an image viewer. The flag is under the man's arm

## Flag

<details>
<summary>Click to view the flag</summary>

__HTB{ThisBackupIsUnprotected}__

</details>