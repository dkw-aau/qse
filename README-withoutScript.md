### Without Script:
In case you want to build and run docker for each dataset individually, then follow these guidelines:

#### Build Docker

Go inside the project directory and execute the following command to build the docker

```
docker build . -t qse:v1
```

#### Run the container
Running the build image as a container to run QSE approach for LUBM dataset using `lubmConfig.properties`.
You can use other config files such as `yagoConfig.properties` or `dbpediaConfig.properties` for YAGO-4 and DBpedia datasets.
```
docker run -d --name qse \
    -m 16GB \
    -e "JAVA_TOOL_OPTIONS=-Xmx10g" \
    --mount type=bind,source=/srv/data/home/data/,target=/app/data \ 
    --mount type=bind,source=/srv/data/home/git/qse/,target=/app/local \ 
    qse:v1 /app/local/lubmConfig.properties
```

`-m` limits the container memory.  <br />
`JAVA_TOOL_OPTIONS` specifies the min (`Xms`) and max (`Xmx`) memory values for JVM memory allocation pool. <br />
`-e` sets environment variables. <br />
`-d` runs container in background and prints container ID. <br />
`--name`  assigns a name to the container. <br />
`--mount` attaches a filesystem mount to the container. <br />

#### Get inside the container
```
sudo docker exec -it qse /bin/sh
```

#### Log Output
```
docker logs --follow qse
```

#### See Memory Utilization by Docker Container
```
docker stats
```