# Quality Shapes Extractor

**This repository contains the code for the following paper under submission in WWW-2022:**
> **Extracting Validating Shapes from very large Knowledge Graphs with Quality Guarantees**


In this paper, we propose a **Quality Shapes Extraction Approach (QSE)** to extract validating shapes from very large
RDF graphs with quality guarantees. QSE is a support-based shapes-extraction approach which supports shapes extraction
from knowledge graphs available as datafiles or in a triplestore as an endpoint denoted as **QSE-File** and **QSE-Endpoint**.


## Reproducibility Instructions

The following instructions are for reproducing the experiments we presented in our paper. To reproduce and extend the experiments you should clone the branch `master` on the Github repository as follows:

```
git clone https://github.com/Kashif-Rabbani/shacl.git
```
The repository contains all code, and instructions. Dataset should be downloaded separately as explained below.


## Requirements
The experiments run on a _single machine_. To reproduce the experiments the suggested setup is:
- **Software:**
   - A GNU/Linux distribution (with git, bash, make, and wget)
   - Java version 15.0.2.fx-zulu

- **Hardware used in the experiments (and minimum required):**
   - RAM: 256 GB (minimum 16GB)
   - CPU: 16 cores (minimum 1)
   - Disk space: ~100 GB free


## Getting the data
We have used DBpedia, YAGO-4, and LUBM datasets. Details on how we downloaded are given below:

1. **DBPedia:** We used our [dbpedia script](https://github.com/Kashif-Rabbani/qse/blob/master/download-dbpedia.sh) to download the dbpedia files listed [here](https://github.com/Kashif-Rabbani/qse/blob/master/dbpedia-files.txt).
2. **YAGO-4:** We downloaded YAGO-4 English version from [https://yago-knowledge.org/data/yago4/en/](https://yago-knowledge.org/data/yago4/en/).
3. **LUBM:** We used [LUBM-Generator](https://github.com/rvesse/lubm-uba) to generate LUBM-500.


We provide a copy of all our datasets in a [single archive](http://130.226.98.152/www_datasets/). You can download these datasets in `data` folder, and check the size and number of lines (triples) with the following commands:

```
 cd data 
 du -sh yago.n3 or dbpedia.n3 or yago.n3
 wc -l yago.n3 or dbpedia.n3 or yago.n3
```
## Software Setup (with Docker)

### Build Docker 

Go inside the project directory and execute the following command to build the docker

```
docker build . -t shacl:v1
```

### Run the container
Running the build image as a container to run QSE approach for LUBM dataset using `lubmConfig.properties`. 
You can use other config files such as `yagoConfig.properties` or `dbpediaConfig.properties` for YAGO-4 and DBpedia datasets.
```
docker run -d --name shacl \
    -m 20GB \
    -e "JAVA_TOOL_OPTIONS=-Xms16g -Xmx16g" \
    --mount type=bind,source=/srv/data/home/data/,target=/app/data \ 
    --mount type=bind,source=/srv/data/home/git/shacl/,target=/app/local \ 
    shacl:v1 /app/local/lubmConfig.properties
```

`-m` limits the container memory.  <br /> 
`JAVA_TOOL_OPTIONS` specifies the min (`Xms`) and max (`Xmx`) memory values for JVM memory allocation pool. <br />
`-e` sets environment variables. <br />
`-d` runs container in background and prints container ID. <br />
`--name`  assigns a name to the container. <br />
`--mount` attaches a filesystem mount to the container. <br />

#### Get inside the container
```
sudo docker exec -it shacl /bin/sh
```

#### Log Output
```
docker logs --follow shacl
```

#### See Memory Utilization by Docker Container
```
docker stats
```



## Software Setup (without Docker)

1. Install Java
   Follow [these](https://sdkman.io/install) steps to install sdkman and execute the following commands to install the specified version of Java.

        sdk list java
        sdk install java 11.0.10.fx-zulu 
        sdk use java 11.0.10.fx-zulu 

2. Install gradle

        sdk install gradle 6.8.3

3. Build project

        gradle clean
        gradle build
        gradle shadowJar


4. Install GraphDB by following the instructions listed [here](https://graphdb.ontotext.com/).


## Experimentation
Here we explain how to repeat experiments and the output (numbers) presented in the evaluation section of our paper.


#### How to repeat experiments?

Update the following parameters in the [config](https://github.com/Kashif-Rabbani/qse/blob/master/config.properties) file to setup configuration for **QSE-File** approach:

```
 QSE_File=true, dataset_path, expected_number_classes, expected_number_of_lines, dataset_name,  QSE_Endpoint=false
```
To run **QSE-Endpoint** approach, you will have to set the following paramteres as well:

```
QSE_Endpoint=true, graphDB_URL and graphDB_REPOSITORY, QSE_File=false
```
We have already prepared 3 config files for each of our dataset, you can use these files to run the experiments using the following commands:
```
java -jar -Xmx16g  build/libs/qse.jar dbpediaConfig.properties &> dbpedia.logs
java -jar -Xmx16g  build/libs/qse.jar yagoConfig.properties &> yago.logs
java -jar -Xmx16g  build/libs/qse.jar lubmConfig.properties &> lubm.logs
```