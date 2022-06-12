#!/bin/bash

### Build Docker Image
cd ..
docker build . -t shacl:QSE-Full-WIKIDATA-Default-wd-max

echo "------------------ WIKIDATA WITH MAX CARDINALITY FULL ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_full_wd_maxCard_default

echo "About to run docker container: ${container}"

docker run -m 750GB -d --name QSE_WIKIDATA_full_wd_maxCard_default -e "JAVA_TOOL_OPTIONS=-Xmx550g" --mount type=bind,source=/user/cs.aau.dk/iq26og/data/,target=/app/data --mount type=bind,source=/user/cs.aau.dk/iq26og/git/shacl/,target=/app/local shacl:QSE-Full-WIKIDATA-Default-wd-max /app/local/config/wd-max-card/wikiDataConfig.properties

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
