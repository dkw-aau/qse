#!/bin/bash
cd ..

### Build Docker Image
image=qse:dockerImage
docker build . -t $image

### Clear Cache
echo "Clearing cache"
sync; echo 1 > /proc/sys/vm/drop_caches

container=qse_container_yago

echo "About to run docker container: ${container}"

docker run -m 16GB -d --name $container -e "JAVA_TOOL_OPTIONS=-Xmx10g" --mount type=bind,source=/data/,target=/app/data --mount type=bind,source=/qse/,target=/app/local $image /app/local/config/yagoConfig.properties

### Logging memory consumption stats by docker container

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
