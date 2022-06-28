#!/bin/bash
cd ..

##############################
############## QSE Approximate Sampling with dynamic Reservoir Size (500,1000,1500) having Sampling Percentage:25,50,75,100
##############################


### Build Docker Image
image=shacl:QSE-Approximate
docker build . -t $image


echo "------------------ wikiData-sp-10-rs-20.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------ xmx 32"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_29_june-sp-10-rs-20_xmx_32g

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx32g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-10-rs-20.properties

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
docker logs $container &> "docker_logs_${container}.txt"




echo "------------------ wikiData-sp-10-rs-20.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------ xmx 16"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_29_june-sp-10-rs-20_xmx_16g

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-10-rs-20.properties

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
docker logs $container &> "docker_logs_${container}.txt"



echo "------------------ wikiData-sp-10-rs-20.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------ xmx 24"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_29_june-sp-10-rs-20_xmx_24g

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx24g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-10-rs-20.properties

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
docker logs $container &> "docker_logs_${container}.txt"


#### sp-10-rs-200


echo "------------------ wikiData-sp-10-rs-200.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------ xmx 32"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_29_june-sp-10-rs-200_xmx_32g

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx32g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-10-rs-200.properties

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
docker logs $container &> "docker_logs_${container}.txt"




echo "------------------ wikiData-sp-10-rs-200.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------ xmx 16"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_29_june-sp-10-rs-200_xmx_16g

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-10-rs-200.properties

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
docker logs $container &> "docker_logs_${container}.txt"



echo "------------------ wikiData-sp-10-rs-200.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------ xmx 24"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_29_june-sp-10-rs-200_xmx_24g

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx24g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-10-rs-200.properties

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
docker logs $container &> "docker_logs_${container}.txt"
