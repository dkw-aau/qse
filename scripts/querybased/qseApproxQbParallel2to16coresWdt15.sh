#!/bin/bash
cd ../../

### Build Docker Image
image=shacl:QSE-ApproxQbParallel
docker build . -t $image

echo "------------------  wdt15Config2 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wdt15Config2

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt15-qse-endpoint/wdt15Config2.properties

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



echo "------------------  wdt15Config4 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wdt15Config4

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt15-qse-endpoint/wdt15Config4.properties

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



echo "------------------  wdt15Config8 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wdt15Config8

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt15-qse-endpoint/wdt15Config8.properties

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



echo "------------------  wdt15Config16 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wdt15Config16

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt15-qse-endpoint/wdt15Config16.properties

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

echo "------------------  wdt15Config1 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wdt15Config1

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt15-qse-endpoint/wdt15Config1.properties

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