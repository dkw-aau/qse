#!/bin/bash

### Build Docker Image
cd ..
docker build . -t shacl:QSE-LATEST-WIKIDATA

echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"

container=QSE_WIKIDATA_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 750GB -d --name QSE_WIKIDATA_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx700g" --mount type=bind,source=/user/cs.aau.dk/iq26og/data/,target=/app/data --mount type=bind,source=/user/cs.aau.dk/iq26og/git/shacl/,target=/app/local shacl:QSE-LATEST-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

docker ps

# Get the status of the current docker container
status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}"

### Keep it in sleep for 15 minutes while this container is running
while :
do
  status=$(docker container inspect -f '{{.State.Status}}' $container)
  if [ $status == "exited" ]; then
    break
  fi
  echo "Sleeping for 15 minutes : $(date +%T)"
  sleep 15m
done

status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}" ### Container exited


echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"

container=QSE_WIKIDATA_wd_maxCard

echo "About to run docker container: ${container}"

docker run -m 750GB -d --name QSE_WIKIDATA_wd_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx700g" --mount type=bind,source=/user/cs.aau.dk/iq26og/data/,target=/app/data --mount type=bind,source=/user/cs.aau.dk/iq26og/git/shacl/,target=/app/local shacl:QSE-LATEST-WIKIDATA /app/local/config/wd-max-card/wikiDataConfig.properties

docker ps

# Get the status of the current docker container
status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}"

### Keep it in sleep for 15 minutes while this container is running
while :
do
  status=$(docker container inspect -f '{{.State.Status}}' $container)
  if [ $status == "exited" ]; then
    break
  fi
  echo "Sleeping for 15 minutes : $(date +%T)"
  sleep 15m
done

status=$(docker container inspect -f '{{.State.Status}}' $container)

echo "Status of the ${container} is ${status}" ### Container exited

