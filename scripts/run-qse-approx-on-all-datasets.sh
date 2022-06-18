#!/bin/bash
cd ..

##############################
############## QSE Approximate Sampling with dynamic Reservoir Size (250-1500) having fixed Sampling Percentage:25 ############
##############################


### Build Docker Image
image=shacl:QSE-Approximate
docker build . -t $image


echo "------------------ LUBM ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_LUBM_1000_100

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/lubmConfig.properties

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



echo "------------------ DBpedia ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_DBpedia_1000_100

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/dbpediaConfig.properties

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


echo "------------------ YAGO-4 ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_YAGO_1000_100

echo "About to run docker container: ${container}"

docker run -m 32GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx16g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/yagoConfig.properties

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



echo "------------------ WIKIDATA - SHEXER------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_WikiData_SHEXER_1000_100

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx32g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wikiDataSheXerConfig.properties

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




echo "------------------ WIKIDATA ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_WikiData_1000_100

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx32g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wikiDataConfig.properties

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

