import smtplib

email = "kyle@writer.htb"

message = "Subject: Hello john, gimme access"

try:
    smtp = smtplib.SMTP("127.0.0.1", 25)
    smtp.ehlo()
    smtp.sendmail(email, email, message)
    smtp.quit()
except Exception as e:
    smtp.quit()
