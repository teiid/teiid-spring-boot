This sample requires an external PostgreSQL server. We can use Docker to easily set up the environment:

Running a psql in Docker:

> docker run -d  --name some-postgres -p 5432:5432 -e POSTGRES_PASSWORD=mm -e POSTGRES_USER=rareddy postgres:9.5

We set the default username/password when we start up the postgres database. We also map the default postgres port on `5432` to our localhost on port `5432`. If you're running in a VM (ie, if you're running on Windows or Mac), you'll need to figure out a way to port forward your localhost to the VM that's running your docker daemon (ie, the application assumes the postgres is running on localhost). 
 
We can init the database with a simple script (note, we're mounting in the script so adjust the mount location to your location -- in this project it's in the `./scripts` folder)
 
> docker run -it --rm -v /Users/ceposta/dev/idea-workspace/teiid/teiid-spring-boot/samples/rdbms/scripts:/scripts --link some-postgres:postgres postgres:9.5 psql -h postgres -U rareddy -f /scripts/example.sql


