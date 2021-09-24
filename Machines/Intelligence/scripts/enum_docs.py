import requests as req
from os import mkdir


try:
    mkdir("Found_Docs")
except:
    pass

found_doc_lens = set()

for year in range(2019, 2022):
    for month in range(1, 13):
        for day in range(1, 32):
            
            test_doc = f"{year}-{str(month).zfill(2)}-{str(day).zfill(2)}-upload.pdf"

            print(f"[*] Looking for /documents/{test_doc}...\r", end="")

            r = req.get(f"http://10.10.10.248/documents/{test_doc}")
            
            if r.status_code == 200:
                doc_len = r.headers['Content-Length']
                print(f"[+] Found document /documents/{test_doc} - Content-Length: {doc_len}")

                with open(f"Found_Docs/{test_doc}", "wb") as f:
                    f.write(r.content)
