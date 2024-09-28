### introduction
This project is an example of Redis lock implementation with Spring Boot.

The objective is to create a resource lock on some database entities to prevent them being updated simultaneously by multiple processes.

### run the project
## Redis
You will need to launch a Redis instance on your computer before running the project.

You can either install Redis directly on your machine or run it through Docker :
`docker run -p 6379:6379 redis`

## Database
Install postgres locally or run it through docker with :
`docker run -p 5432:5432 -e POSTGRES_DB=redis_lock_db -e POSTGRES_USER=redis_lock -e POSTGRES_PASSWORD=redis_lock postgres`

## Application
Once Redis and the postgres database are launched, you can start the Spring Boot project and try it out.

Get all jobs
```
curl --location 'http://localhost:8080/redis-lock-example/jobs'
```

Create a job
```
curl --location --request POST 'http://localhost:8080/redis-lock-example/jobs?name=SOME_JOB'
```

Start a job
```
curl --location --request POST 'http://localhost:8080/redis-lock-example/jobs/3/start'
```