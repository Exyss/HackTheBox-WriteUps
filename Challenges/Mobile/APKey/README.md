# APKEY

## Overview

* Difficulty: Easy
* Category: Mobile

## Description
> This app contains some unique keys. Can you get one?

## Approach

1. The given file is an Android Backup archive. We can extract it's content using `binwalk`, getting a tar archive, which can also be extracted using `tar -xf`

```bash

```

## Flag

<details>
<summary>Click to view the flag</summary>

__HTB{ThisBackupIsUnprotected}__

</details>