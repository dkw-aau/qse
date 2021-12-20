#!/bin/bash

for i in 10 15 25 50 100
do
    for j in 1000000 5000000 10000000
    do
        for l in 25 100 500 5000 10000 20000 30000 40000 50000
        do
          java -jar -Xmx${i}g build/libs/shacl-1.0-SNAPSHOT-all.jar  ${l} ${j}
           if [ $? -eq 0 ]; then
              echo "RBM,PASS,${i},${j},${l}"
            else
               echo "RBM,FAIL,${i},${j},${l}"
            fi
        done
    done
done