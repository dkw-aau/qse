#!/bin/bash

memorySize="150 100 75 50 25 15 10"

for val in ${memorySize}; do
  echo ${val}
  java -jar -Xmx${val}g build/libs/shacl-1.0-SNAPSHOT-all.jar configServer.properties
  if [ $? -eq 0 ]; then
    echo "it executed without any errors, or ${val} memory max limit"
  else
    echo "it crashed out for ${val} memory max limit"
  fi
done

#java -jar -Xms50g -Xmx150g  build/libs/shacl-1.0-SNAPSHOT-all.jar configServer.properties

