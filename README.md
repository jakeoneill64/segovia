# Segovia Test Task

to get started, adjust the urls in `application.yml` to match your network setup (this is not tested with `host.docker.internal`)

in the project root dir run
```
bash gradlew bootJar
docker build . -t segovia:payment-service
docker run -it -p 8080:8080 --mount type=bind,source=<payments.csv directory local>,target=/var/opt/segovia segovia:payment-service
```

an example of the last command may be 


`docker run -it -p 8080:8080 --mount type=bind,source=/home/myuser/segovia/,target=/var/opt/segovia segovia:payment-service`

where payments.csv is in `/home/myuser/segovia/`


The `output.csv` will be produced in the same directory.