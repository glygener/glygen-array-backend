--------------------------------------------------------------------------------------------
To execute SQL queries on  using shell.

1. Open docker container into shell using following command

	docker exec -it postgres_postgres_1 bash

2. connect to psql database using following command

	psql -h 127.0.0.1 -p 5432 -U postgres -w postgres

-h, --host=HOSTNAME      database server host or socket directory (default: "local socket")
-p, --port=PORT          database server port (default: "5432")
-U, --username=USERNAME  database user name (default: "root")
-w, --no-password        never prompt for password
-W, --password           force password prompt (should happen automatically)

3. type and excute your query.

4. "\q" to come out of scripting shell
5. "exit" to return to your termnial from bash
--------------------------------------------------------------------------------------------