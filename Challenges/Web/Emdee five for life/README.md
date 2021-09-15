# Emdee five for life

## Overview

* Difficulty: Easy
* Category: Web

## Description
> Can you encrypt fast enough?

## Approach

## Flag

1. Write a simple python script that makes a request to the server, parses the string and sends the hash in a post request.

```py
import requests as req
from hashlib import md5

url = "http://188.166.173.208:31790/"

s = req.Session()
r = s.get(url)

to_be_hashed = r.text.split("<h3 align='center'>")[1].split("</h3>")[0]
hashed = md5(to_be_hashed.encode("utf-8")).hexdigest()

r = s.post(url, data = {"hash": hashed})
flag = r.text.split("<p align='center'>")[1].split("</p><center>")[0]

print(f"String\t| {to_be_hashed}")
print(f"Hash  \t| {hashed}")
print(f"Flag  \t| {flag}")
```

<details>
<summary>Click to view the flag</summary>

__HTB{N1c3_ScrIpt1nG_B0i!}__

</details>