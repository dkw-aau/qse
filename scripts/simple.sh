#!/bin/bash

### Build Docker Image
cd ..
docker build . -t shacl:QSE-MemoryMeasuringTrie-WIKIDATA

echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS Measuring Memory 1 - 30GB/25gb ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_MemoryMeasuringTrie-1_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 30GB -d --name QSE_WIKIDATA_MemoryMeasuringTrie-1_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx25g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local shacl:QSE-MemoryMeasuringTrie-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

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



echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS Measuring Memory 2 - 40GB/35gb ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_MemoryMeasuringTrie-2_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 40GB -d --name QSE_WIKIDATA_MemoryMeasuringTrie-2_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx35g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local shacl:QSE-MemoryMeasuringTrie-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

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






echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS Measuring Memory 3 - 50GB/45gb ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_MemoryMeasuringTrie-3_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 50GB -d --name QSE_WIKIDATA_MemoryMeasuringTrie-3_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx45g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local shacl:QSE-MemoryMeasuringTrie-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

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






echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS Measuring Memory 4 - 60GB/55gb ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_MemoryMeasuringTrie-4_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 60GB -d --name QSE_WIKIDATA_MemoryMeasuringTrie-4_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx55g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local shacl:QSE-MemoryMeasuringTrie-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

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






echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS Measuring Memory 5 - 70GB/65gb ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_MemoryMeasuringTrie-5_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 70GB -d --name QSE_WIKIDATA_MemoryMeasuringTrie-5_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx65g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local shacl:QSE-MemoryMeasuringTrie-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

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






echo "------------------ WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS Measuring Memory 6 - 80GB/75gb ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_MemoryMeasuringTrie-6_wo_maxCard

echo "About to run docker container: ${container}"

docker run -m 80GB -d --name QSE_WIKIDATA_MemoryMeasuringTrie-6_wo_maxCard -e "JAVA_TOOL_OPTIONS=-Xmx75g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local shacl:QSE-MemoryMeasuringTrie-WIKIDATA /app/local/config/wo-max-card/wikiDataConfig.properties

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




