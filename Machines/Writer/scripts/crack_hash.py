import py_django_crack.jake as jake
from hashlib import sha256

hash = "pbkdf2_sha256$260000$wJO3ztk0fOlcbssnS1wJPD$bbTyCB8dYWMGYlz4dSArozTY7wcZCS7DV6l5dpuXM4A="

# {algorithm}${iteration times}${salt}${hashed password}
parts = hash.split("$")

it_times = int(parts[1])
salt = parts[2]
hashed_psw = parts[3]

with open("/home/exyss/Desktop/rockyou.txt", "r") as f:
    for line in f:
        psw = line[:-1]

        test_hash = jake.get_base64_hashed(psw, salt, it_times, sha256)
        print(f"[*] Testing with {psw} - Hash: {test_hash}")

        if hashed_psw == test_hash:
            print("--- Found!!! ---")
            break