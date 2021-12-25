#!/bin/bash

### Build Docker Image
cd ..
docker build . -t shacl:WWW-Wikidata


echo "------------------ WIKIDATA  ------------------"

container=QSE_WIKIDATA_WWW

echo "About to run docker container: ${container}"

docker run -m 750GB -d --name QSE_WIKIDATA_WWW -e "JAVA_TOOL_OPTIONS=-Xmx700g" --mount type=bind,source=/user/cs.aau.dk/iq26og/data/,target=/app/data --mount type=bind,source=/user/cs.aau.dk/iq26og/www/shacl/,target=/app/local shacl:WWW-Wikidata /app/local/wikiDataConfig.properties

docker ps

# Get the status of the current docker container
status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}"

### Keep it in sleep for 1 minutes while this container is running
while :
do
  status=$(docker container inspect -f '{{.State.Status}}' $container)
  if [ $status == "exited" ]; then
    break
  fi
  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
  echo "Sleeping for 1 minutes : $(date +%T)"
  sleep 1m
done

status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}" ### Container exited

