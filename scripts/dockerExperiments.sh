#!/bin/bash

#docker build . -t shacl:latest
#docker run -it --name testContainer alpine:latest ash

containerName=shacl_wikidata_maxCard
docker ps

# shellcheck disable=SC2046
if [ $( docker ps -a -f name=containerName | wc -l ) -eq 2 ]; then
  echo "${containerName} exists"
else
  echo "${containerName} does not exist"
fi


output=$( docker ps -a -f name=containerName | grep containerName 2> /dev/null )
if [[ ! -z ${output} ]]; then
  echo "A container with a name: ${containerName} exists and has status: $( echo ${output} | awk '{ print $7 }' )"
else
  echo "Container with a name: ${containerName} does not exist"
fi