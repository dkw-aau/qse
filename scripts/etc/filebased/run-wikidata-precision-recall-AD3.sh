#!/bin/bash
cd ..

##############################
############## A3 ############
##############################


#Copy the files
cp ../files_directory/A3/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/A3/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-A3
docker build . -t $image

echo "------------------ A3: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_A3

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-A3.properties

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




##############################
############## B3 ############
##############################


#Copy the files
cp ../files_directory/B3/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/B3/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-B3
docker build . -t $image

echo "------------------ B3: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_B3

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-B3.properties

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




##############################
############## C3 ############
##############################


#Copy the files
cp ../files_directory/C3/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/C3/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-C3
docker build . -t $image

echo "------------------ C3: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_C3

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-C3.properties

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




##############################
############## D3 ############
##############################


#Copy the files
cp ../files_directory/D3/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/D3/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-D3
docker build . -t $image

echo "------------------ D3: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_D3

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-D3.properties

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


