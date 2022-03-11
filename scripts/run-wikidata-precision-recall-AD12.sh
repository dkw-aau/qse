#!/bin/bash
cd ..

##############################
############## A1 ############
##############################


#Copy the files
cp ../files_directory/A1/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/A1/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-A1
docker build . -t $image

echo "------------------ A1: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_A1

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-A1.properties

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

# Remove Docker
docker rm $container

##############################
############## A2 ############
##############################


#Copy the files
cp ../files_directory/A2/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/A2/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-A2
docker build . -t $image

echo "------------------ A2: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_A2

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-A2.properties

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

# Remove Docker
docker rm $container

##############################
############## B1 ############
##############################


#Copy the files
cp ../files_directory/B1/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/B1/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-B1
docker build . -t $image

echo "------------------ B1: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_B1

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-B1.properties

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

# Remove Docker
docker rm $container

##############################
############## B2 ############
##############################


#Copy the files
cp ../files_directory/B2/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/B2/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-B2
docker build . -t $image

echo "------------------ B2: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_B2

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-B2.properties

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

# Remove Docker
docker rm $container

##############################
############## C1 ############
##############################


#Copy the files
cp ../files_directory/C1/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/C1/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-C1
docker build . -t $image

echo "------------------ C1: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_C1

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-C1.properties

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

# Remove Docker
docker rm $container

##############################
############## C2 ############
##############################


#Copy the files
cp ../files_directory/C2/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/C2/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-C2
docker build . -t $image

echo "------------------ C2: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_C2

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-C2.properties

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

# Remove Docker
docker rm $container

##############################
############## D1 ############
##############################


#Copy the files
cp ../files_directory/D1/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/D1/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-D1
docker build . -t $image

echo "------------------ D1: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_D1

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-D1.properties

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

# Remove Docker
docker rm $container

##############################
############## D2 ############
##############################


#Copy the files
cp ../files_directory/D2/DynamicBullyReservoirSampling.java src/main/java/cs/qse/sampling/
cp ../files_directory/D2/ShapesExtractor.java src/main/java/cs/qse/

### Build Docker Image
image=shacl:QSE-DynamicBullyReservoirSampling-WIKIDATA-D2
docker build . -t $image

echo "------------------ D2: WIKIDATA WITHOUT MAX CARDINALITY CONSTRAINTS ------------------"
### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=QSE_WIKIDATA_DynamicBullyResSampling_WoMax_1_500_D2

echo "About to run docker container: ${container}"

docker run -m 100GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx80g" --mount type=bind,source=/srv/data/iq26og/data/,target=/app/data --mount type=bind,source=/srv/data/iq26og/git/shacl/,target=/app/local $image /app/local/config/wo-max-card/wiki/wikiData-D2.properties

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

# Remove Docker
docker rm $container

