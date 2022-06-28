#!/bin/bash
cd ..

##############################
############## QSE Approximate Sampling with dynamic Reservoir Size (500,1000,1500) having Sampling Percentage:25,50,75,100
##############################


### Build Docker Image
image=shacl:QSE-Approximate
docker build . -t $image



# 75


echo "------------------ wikiData-sp-75-rs-500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_28_June-sp-75-rs-500

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-75-rs-500.properties

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


echo "------------------ wikiData-sp-75-rs-1000.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_28_June-sp-75-rs-1000

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-75-rs-1000.properties

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


#
#echo "------------------ wikiData-sp-75-rs-1500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
#### Clear Cache
#echo "Clearing cache"
#sync; echo 1 > /proc/sys/vm/drop_caches
#
#container=QSE_Approximate_wikiData_28_June-sp-75-rs-1500
#
#echo "About to run docker container: ${container}"
#
#docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-75-rs-1500.properties
#
#docker ps
#
## Get the status of the current docker container
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}"
#
#### Keep it in sleep for 1 minutes while this container is running
#while :
#do
#  status=$(docker container inspect -f '{{.State.Status}}' $container)
#  if [ $status == "exited" ]; then
#    break
#  fi
#  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
#  echo "Sleeping for 1 minutes : $(date +%T)"
#  sleep 1m
#done
#
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}" ### Container exited
#
#docker logs $container &> "docker_logs_${container}.txt"
#
#

# ----- 50


echo "------------------ wikiData-sp-50-rs-500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_28_June-sp-50-rs-500

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-50-rs-500.properties

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


echo "------------------ wikiData-sp-50-rs-1000.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_28_June-sp-50-rs-1000

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-50-rs-1000.properties

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


#
#echo "------------------ wikiData-sp-50-rs-1500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
#### Clear Cache
#echo "Clearing cache"
#sync; echo 1 > /proc/sys/vm/drop_caches
#
#container=QSE_Approximate_wikiData_28_June-sp-50-rs-1500
#
#echo "About to run docker container: ${container}"
#
#docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-50-rs-1500.properties
#
#docker ps
#
## Get the status of the current docker container
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}"
#
#### Keep it in sleep for 1 minutes while this container is running
#while :
#do
#  status=$(docker container inspect -f '{{.State.Status}}' $container)
#  if [ $status == "exited" ]; then
#    break
#  fi
#  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
#  echo "Sleeping for 1 minutes : $(date +%T)"
#  sleep 1m
#done
#
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}" ### Container exited
#
#docker logs $container &> "docker_logs_${container}.txt"



# ---- 25

echo "------------------ wikiData-sp-25-rs-500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_28_June-sp-25-rs-500

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-25-rs-500.properties

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


echo "------------------ wikiData-sp-25-rs-1000.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_Approximate_wikiData_28_June-sp-25-rs-1000

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-25-rs-1000.properties

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


#
#echo "------------------ wikiData-sp-25-rs-1500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
#### Clear Cache
#echo "Clearing cache"
#sync; echo 1 > /proc/sys/vm/drop_caches
#
#container=QSE_Approximate_wikiData_28_June-sp-25-rs-1500
#
#echo "About to run docker container: ${container}"
#
#docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-25-rs-1500.properties
#
#docker ps
#
## Get the status of the current docker container
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}"
#
#### Keep it in sleep for 1 minutes while this container is running
#while :
#do
#  status=$(docker container inspect -f '{{.State.Status}}' $container)
#  if [ $status == "exited" ]; then
#    break
#  fi
#  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
#  echo "Sleeping for 1 minutes : $(date +%T)"
#  sleep 1m
#done
#
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}" ### Container exited
#
#docker logs $container &> "docker_logs_${container}.txt"
#





## ----- 100
#
#echo "------------------ wikiData-sp-100-rs-500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
#### Clear Cache
#echo "Clearing cache"
#sync; echo 1 > /proc/sys/vm/drop_caches
#
#container=QSE_Approximate_wikiData_28_June-sp-100-rs-500
#
#echo "About to run docker container: ${container}"
#
#docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-100-rs-500.properties
#
#docker ps
#
## Get the status of the current docker container
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}"
#
#### Keep it in sleep for 1 minutes while this container is running
#while :
#do
#  status=$(docker container inspect -f '{{.State.Status}}' $container)
#  if [ $status == "exited" ]; then
#    break
#  fi
#  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
#  echo "Sleeping for 1 minutes : $(date +%T)"
#  sleep 1m
#done
#
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}" ### Container exited
#docker logs $container &> "docker_logs_${container}.txt"
#
#
#echo "------------------ wikiData-sp-100-rs-1000.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
#### Clear Cache
#echo "Clearing cache"
#sync; echo 1 > /proc/sys/vm/drop_caches
#
#container=QSE_Approximate_wikiData_28_June-sp-100-rs-1000
#
#echo "About to run docker container: ${container}"
#
#docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-100-rs-1000.properties
#
#docker ps
#
## Get the status of the current docker container
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}"
#
#### Keep it in sleep for 1 minutes while this container is running
#while :
#do
#  status=$(docker container inspect -f '{{.State.Status}}' $container)
#  if [ $status == "exited" ]; then
#    break
#  fi
#  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
#  echo "Sleeping for 1 minutes : $(date +%T)"
#  sleep 1m
#done
#
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}" ### Container exited
#docker logs $container &> "docker_logs_${container}.txt"
#
#
#
#echo "------------------ wikiData-sp-100-rs-1500.properties: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
#### Clear Cache
#echo "Clearing cache"
#sync; echo 1 > /proc/sys/vm/drop_caches
#
#container=QSE_Approximate_wikiData_28_June-sp-100-rs-1500
#
#echo "About to run docker container: ${container}"
#
#docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx50g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-sp-100-rs-1500.properties
#
#docker ps
#
## Get the status of the current docker container
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}"
#
#### Keep it in sleep for 1 minutes while this container is running
#while :
#do
#  status=$(docker container inspect -f '{{.State.Status}}' $container)
#  if [ $status == "exited" ]; then
#    break
#  fi
#  docker stats --no-stream | cat >>   "${container}-Docker-Stats.csv"
#  echo "Sleeping for 1 minutes : $(date +%T)"
#  sleep 1m
#done
#
#status=$(docker container inspect -f '{{.State.Status}}' $container)
#
#echo "Status of the ${container} is ${status}" ### Container exited
#
#docker logs $container &> "docker_logs_${container}.txt"
#
#
#
