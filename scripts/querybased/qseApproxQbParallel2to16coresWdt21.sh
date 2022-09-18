#!/bin/bash
cd ../../

### Build Docker Image
image=shacl:QSE-ApproxQbParallel
docker build . -t $image

echo "------------------  wikiDataConfig2 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wikiDataConfig2

echo "About to run docker container: ${container}"

docker run -m 128GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx64g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt21-qse-endpoint/wikiDataConfig2.properties

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



echo "------------------  wikiDataConfig4 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wikiDataConfig4

echo "About to run docker container: ${container}"

docker run -m 128GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx64g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt21-qse-endpoint/wikiDataConfig4.properties

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



echo "------------------  wikiDataConfig8 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wikiDataConfig8

echo "About to run docker container: ${container}"

docker run -m 128GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx64g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt21-qse-endpoint/wikiDataConfig8.properties

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



echo "------------------  wikiDataConfig16 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wikiDataConfig16

echo "About to run docker container: ${container}"

docker run -m 128GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx64g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt21-qse-endpoint/wikiDataConfig16.properties

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

echo "------------------  wikiDataConfig1 ------------------"

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QseApproxQbPar_1k_100_wikiDataConfig1

echo "About to run docker container: ${container}"

docker run -m 128GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx64g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git3/shacl/,target=/app/local $image /app/local/config/wo-max-card/wdt21-qse-endpoint/wikiDataConfig1.properties

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