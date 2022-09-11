# Quality Shapes Extraction (QSE)

This repository contains the source code, resources, and instructions to reproduce the experiments performed for the following research paper under submission in VLDB - July 2022:
> **Extraction of Validating Shapes from very large Knowledge Graphs [Scalable Data Science]**

**The extended version of the paper is available [here](https://github.com/Kashif-Rabbani/shacl/blob/main/qse-extended.pdf).**

-------

Please follow these steps to get the code and data to reproduce the results:

### 1. Getting the code
Clone the GitHub repository using the following url.
```
git clone https://github.com/Kashif-Rabbani/shacl.git
```

### 2. Getting the data
We have used WikiData, DBpedia, YAGO-4, and LUBM datasets. Details on how to download these datasets are given below:

1. **DBPedia:** We used our [dbpedia script](https://github.com/Kashif-Rabbani/shacl/blob/main/scripts/dbpedia/download-dbpedia.sh) to download the dbpedia files listed [here](https://github.com/dkw/qse/blob/main/scripts/dbpedia/dbpedia-files.txt).
2. **YAGO-4:** We downloaded YAGO-4 English version from [https://yago-knowledge.org/data/yago4/en/](https://yago-knowledge.org/data/yago4/en/).
3. **LUBM:** We used [LUBM-Generator](https://github.com/rvesse/lubm-uba) to generate LUBM-500.
4. **WikiData (Wdt15):** We downloaded a WikiData dump from 2015 form [this](https://archive.org/details/wikidata-json-20150518) link.
5. **WikiData (Wdt21):** We downloaded the [truthy dump](https://dumps.wikimedia.org/wikidatawiki/entities/) of WikiData (2021) and then used our [wikidata python script](https://github.com/Kashif-Rabbani/shacl/blob/main/scripts/wikidata/filter_WikiData.py) to remove labels, descriptions, and non-English strings.

We provide a copy of some of these  datasets in a [single archive](http://130.226.98.152/www_datasets/).
You should download these datasets in `data` directory. You can check the size and number of lines (triples) with the commands:
``` cd data; du -sh yago.n3; wc -l yago.n3 ```, etc.



### 3. Running the experiments (with Docker)
We used Docker and shell scripts to build and run the code on different datasets. We allow users to specify the configuration parameters in the config files depending on the dataset and user's requirement.


####  3.1. Requirements
The experiments run on a _single machine_. To reproduce the experiments the software used are *a GNU/Linux distribution (with git, bash, make, and wget)*, Docker,  and Java  *version 15.0.2.fx-zulu*
having a machine with 256 GB (minimum required 16GB) and CPU with 16 cores (minimum required 1 core).

We have prepared shell scripts and configuration files for each dataset to make the process of running experiments as much easy as possible.

#### 3.2. Configuration Parameters
Please update the configuration file for each dataset available in the [config](https://github.com/dkw/qse/tree/main/config) directory, i.e., `dbpediaConfig`, `yagoConfig`, `lubmConfig`, `wdt15Config`, and `wdt21Config` to set the correct paths for your machine.
You have to choose from one of these options to either extract shapes using QSE-Exact (file or query-based) or QSE-Approximate.

| Parameter             | Description                                                                       | Options           |
|-----------------------|-----------------------------------------------------------------------------------|-------------------|
| qse_exact_file        | set the value to extract shapes from a file using QSE-Exact                       | `true` or `false` |
| qse_exact_query_based | set the value to extract shapes from an endpoint using QSE-Exact                  | `true` or `false` |
| qse_approximate_file  | set the value to extract shapes from a file using QSE-Approximate                 | `true` or `false` |


Depending on the approach you have chosen from one of the above, you have to set the value for the following parameters:

| Parameter                         | Description                                                                                                                      | Options                       |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| dataset_path                      | set the path of the datasets directory                                                                                           | `qse/data/`                   |
| resources_path                    | set the path of the resources directory                                                                                          | `qse/src/main/resources`      |
| output_file_path                  | set the path of the output directory                                                                                             | `qse/output/`                 |
| max_cardinality                   | set the value to enable extraction of sh:maxCount constraints for property shapes                                                | `true` or `false`             |
| graphdb_url                       | set the path of graphdb's endpoint url (when qse_exact_query_based is set as `true`)                                             | `http://172.0.0.0:7200`       |
| graphdb_repository                | set the name of the graphdb's repository                                                                                         | `repository`                  |
| entity_sampling_threshold         | set the entity sampling threshold (maximum size of reservoir) for qse_approximate                                                | default `500`                 |
| entity_sampling_target_percentage | set the entity sampling percentage for qse_approximate                                                                           | default `75`                  |
| default_directory                 | set the directory where output of qse_exact is stored to compute precision and recall in comparison to output of qse_approximate | `qse/output/dbpedia/default/` |
| is_wikidata                       | set if the dataset is WikiData or any of its version                                                                             | `true` or  `false`            |





#### 3.3. Shell Scripts
Assuming that you are in the project's directory, you have updated the configuration file(s), and docker is installed on your machine, move into [scripts](https://github.com/Kashif-Rabbani/shacl/tree/main/scripts) directory using the command ``` cd scripts ``` and then execute one of the following shell scripts files:
``` ./dbpedia.sh ``` ,
``` ./yago.sh ``` ,
``` ./lubm.sh ``` ,
``` ./wdt15.sh ``` , or
``` ./wdt21.sh ```

You will see logs and the output will be stored in the path of the output directory specified in the config file.

*Note: You may have to execute ```chmod +rwx ``` for each script to solve the permissions issue. In case you want to run the experiments without script, please follow the instructions on [this](https://github.com/Kashif-Rabbani/shacl/blob/main/README-withoutScript.md) page.*



---------

### 4. Running the experiments (without Docker)

1. Install Java:  Please follow [these](https://sdkman.io/install) steps to install sdkman and execute the following commands to install the specified version of Java.

        sdk list java
        sdk install java 15.0.2.fx-zulu
        sdk use java 15.0.2.fx-zulu

2. Install gradle

        sdk install gradle 6.8.3

3. Build project

        gradle clean
        gradle build
        gradle shadowJar


4. Install GraphDB by following the instructions listed [here](https://graphdb.ontotext.com/).

As stated above, that you have to set the configuration parameters for each file individually based on your machine and requirements.

```
java -jar -Xmx10g  build/libs/qse.jar config/dbpediaConfig.properties &> dbpedia.logs
java -jar -Xmx10g  build/libs/qse.jar config/yagoConfig.properties &> yago.logs
java -jar -Xmx10g  build/libs/qse.jar config/lubmConfig.properties &> lubm.logs
java -jar -Xmx16g  build/libs/qse.jar config/wdt15Config.properties &> wdt15.logs
java -jar -Xmx32g  build/libs/qse.jar config/wdt21Config.properties &> wdt21.logs
```
