#!/bin/bash

#docker build . -t shacl:latest
#docker run -it --name testContainer alpine:latest ash

container=shacl_wikidata_maxCard
docker ps



if [ $( docker ps -a -f name=${container} | wc -l ) -eq 2 ]; then
  echo "${container} exists"
else
  echo "${container} does not exist"
fi


output=$( docker ps -a -f name=${container} | grep ${container} 2> /dev/null )
if [[ ! -z ${output} ]]; then
  echo "A container with a name: ${container} exists and has status: $( echo ${output} | awk '{ print $7 }' )"
else
  echo "Container with a name: ${container} does not exist"
fi