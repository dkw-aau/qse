#!/bin/bash
cd ../../

### Build Docker Image
image=shacl:QSE-ApproxQbParallel
docker build . -t $image

echo "------------------  dbpediaConfig2 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_dbpediaConfig2

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/dbpedia-qse-endpoint/dbpediaConfig2.properties

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

docker logs ${container} &> ${container}.logs



echo "------------------  dbpediaConfig4 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_dbpediaConfig4

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/dbpedia-qse-endpoint/dbpediaConfig4.properties

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

docker logs ${container} &> ${container}.logs



echo "------------------  dbpediaConfig8 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_dbpediaConfig8

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/dbpedia-qse-endpoint/dbpediaConfig8.properties

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

docker logs ${container} &> ${container}.logs



echo "------------------  dbpediaConfig16 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_dbpediaConfig16

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/dbpedia-qse-endpoint/dbpediaConfig16.properties

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

docker logs ${container} &> ${container}.logs

echo "------------------  dbpediaConfig1 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_dbpediaConfig1

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/dbpedia-qse-endpoint/dbpediaConfig1.properties

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

docker logs ${container} &> ${container}.logs