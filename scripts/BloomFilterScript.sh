#!/bin/bash


for i in 10 15 25 50 100
do
    for j in 1000000 5000000 10000000
    do
        for l in 25 100 500 5000 10000
        do

        for k in 0.001 0.0001 0.00001 0.000001
                do
                  #echo "$i $j $k"
                  #echo "Memory: ${i}  ... BloomFilterSize: ${j} ... FPP: ${k} ... NoC: ${l}"
                  java -jar -Xmx${i}g build/libs/shacl-1.0-SNAPSHOT-all.jar  ${j} ${k} ${l}
                   if [ $? -eq 0 ]; then
                      echo "PASS,${i},${j},${k},${l}"
                    else
                       echo "FAIL,${i},${j},${k},${l}"
                    fi

                done

        done
    done
done