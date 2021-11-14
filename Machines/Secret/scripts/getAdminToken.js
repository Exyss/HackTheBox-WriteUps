const jwt = require('jsonwebtoken')

SECRET = "gXr67TtoQL8TShUc8XYsK2HvsBYfyQSFCFZe4MQp7gRpFuMkKjcM72CNQN4fMfbZEKx4i7YiWuNAkmuTcdEriCMm9vPAYkhpwPTiuVwVhvwE"

data = {
    "_id": "6114654d77f9a54e00fe5777",
    "name": "theadmin",
    "email": "root@dasith.works"
}
const token = jwt.sign(data, SECRET )
console.log(token)