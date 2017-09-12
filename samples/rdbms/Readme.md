This sample requires an external PostgreSQL server. We can use Docker to easily set up the environment:

Running a psql in Docker:

> docker run -d  --name some-postgres -p 5432:5432 -e POSTGRES_PASSWORD=mm -e POSTGRES_USER=rareddy postgres:9.5

We set the default username/password when we start up the postgres database. We also map the default postgres port on `5432` to our localhost on port `5432`. If you're running in a VM (ie, if you're running on Windows or Mac), you'll need to figure out a way to port forward your localhost to the VM that's running your docker daemon (ie, the application assumes the postgres is running on localhost). 
 
We can init the database with a simple script (note, we're mounting in the script so adjust the mount location to your location -- in this project it's in the `./scripts` folder)

To do that, we can have two options: 

1. Build the Docker image for the `psql` client that includes the sample setup scripts
2. Use an already built image to do the same as `1`

#### Build the docker image yourself

From the `./scripts` folder, run the following to build the docker image:

> docker build -t postgres-cli:latest .

#### Use an existing psql image with the scripts built in:

> docker pull ceposta/postgres-cli:latest

Then you can run it and init the database like this (note the docker image name -- should be ceposta/postgres-cli:latest if pulling it down instead of building it):

> docker run -it --rm -e PGPASSWORD=mm --link some-postgres:postgres postgres-cli -h postgres -U rareddy -f /scripts/example.sql

Now we can log into the `psql` terminal and verify our tables were set up correctly:

> docker run -it --rm -e PGPASSWORD=mm --link some-postgres:postgres postgres-cli -h postgres -U rareddy 

```
psql (9.5.9)
Type "help" for help.

rareddy=# \l
                                 List of databases
   Name    |  Owner   | Encoding |  Collate   |   Ctype    |   Access privileges   
-----------+----------+----------+------------+------------+-----------------------
 customer  | rareddy  | UTF8     | en_US.utf8 | en_US.utf8 | 
 postgres  | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
 rareddy   | postgres | UTF8     | en_US.utf8 | en_US.utf8 | 
 template0 | postgres | UTF8     | en_US.utf8 | en_US.utf8 | =c/postgres          +
           |          |          |            |            | postgres=CTc/postgres
 template1 | postgres | UTF8     | en_US.utf8 | en_US.utf8 | =c/postgres          +
           |          |          |            |            | postgres=CTc/postgres
 test      | rareddy  | UTF8     | en_US.utf8 | en_US.utf8 | 
(6 rows)

rareddy=# 
```

You can see our `test` and `customer` tables are there!


