import requests as req
from hashlib import md5

url = "http://188.166.173.208:31790/"

s = req.Session()
r = s.get(url)

to_be_hashed = r.text.split("<h3 align='center'>")[1].split("</h3>")[0]
hashed = md5(to_be_hashed.encode("utf-8")).hexdigest()

r = s.post(url, data = {"hash": hashed})
flag = r.text.split("<p align='center'>")[1].split("</p><center>")[0]

print("String\t| "+to_be_hashed)
print("Hash\t| "+hashed)
print("Flag\t| "+flag)