#!/bin/bash

### Build Docker Image
cd ..
docker build . -t shacl:QSE-WWW

echo "------------------ LUBM ------------------"

### Run LUBM Docker Container
container=QSE_lubm_WWW
docker run -m 32GB -d --name QSE_lubm_WWW -e "JAVA_TOOL_OPTIONS=-Xmx17g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/www/shacl/,target=/app/local shacl:QSE-WWW /app/local/lubmConfig.properties

### Check the status of docker containers
docker ps

### Get the status of the current docker container
status=$(docker container inspect -f '{{.State.Status}}' $container)
echo "Status of the ${container} is ${status}"

### Keep it in sleep for 2 minutes while this container is running
while :
do
  status=$(docker container inspect -f '{{.State.Status}}' $container)
  if [ $status == "exited" ]; then
    break
  fi
  echo "Sleeping for 2 minutes : $(date +%T)"
  sleep 2m
done

status=$(docker container inspect -f '{{.State.Status}}' $container)
echo "Status of the ${container} is ${status}" ### Container exited

echo "------------------ DBpedia ------------------"

### Run DBpedia Docker Container
container=QSE_dbpedia_WWW
docker run -m 32GB -d --name QSE_dbpedia_WWW -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/www/shacl/,target=/app/local shacl:QSE-WWW /app/local/dbpediaConfig.properties

docker ps
status=$(docker container inspect -f '{{.State.Status}}' $container)
echo "Status of the ${container} is ${status}"

### Keep it in sleep for 2 minutes while this container is running
while :
do
  status=$(docker container inspect -f '{{.State.Status}}' $container)
  if [ $status == "exited" ]; then
    break
  fi
  echo "Sleeping for 2 minutes : $(date +%T)"
  sleep 2m
done

status=$(docker container inspect -f '{{.State.Status}}' $container)


echo "Status of the ${container} is ${status}" ### Container exited

echo "------------------ YAGO ------------------"

### Run YAGO Docker Container
container=QSE_yago_WWW
docker run -m 32GB -d --name QSE_yago_WWW -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/www/shacl/,target=/app/local shacl:QSE-WWW /app/local/yagoConfig.properties

docker ps

status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}"

### Keep it in sleep for 2 minutes while this container is running
while :
do
  status=$(docker container inspect -f '{{.State.Status}}' $container)
  if [ $status == "exited" ]; then
    break
  fi
  echo "Sleeping for 2 minutes : $(date +%T)"
  sleep 2m
done

status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}" ### Container exited


