# Quality Shapes Extraction (QSE)

QSE is a scalable shapes extraction tool which helps you extract validating shapes (SHACL) from large knowledge graphs.
Please read our [research paper](https://relweb.cs.aau.dk/qse/extended.pdf) for details.


### Instructions to extract SHACL shapes from your Knowledge Graph

We provide a JAR file to help user easily extract SHACL 
shapes given: 

1. the input knowledge graph is in `.nt` format and 
2. the config file contains correct parameters. 

#### Set Config Params: 
We already provide default parameters in the [config.properties](https://github.com/dkw-aau/qse/blob/main/config.properties) file, 
you only need to update the following parameters to get started:

      dataset_path=/dir_path/knowledge_graph.nt
      resources_path=/dir_path/qse/src/main/resources
      config_dir_path=/dir_path/qse/config/
      output_file_path=/dir_path/qse/Output/

Please replace the `dir_path` with respect to your directory. 

By default, QSE uses confidence 0.1 and support 100 to prune the extracted shapes. You can specify more values of support and range in [this](https://github.com/dkw-aau/qse/blob/main/config/pruning/pruning_thresholds.csv) file.


#### Run Jar file:
The jar file is located in [jar](https://github.com/dkw-aau/qse/tree/shactor/jar) directory. Please execute the following command to run the jar: 

```
java -jar -Xmx16g  build/libs/qse.jar config.properties &> output.logs
```
You can change the value for Xmx16g according to your machine's specification. It specifies the maximum memory usage by the JVM machine to run this jar.


**Note:** QSE requires Java to be installed on your system to run its Jar. You can install it by following [these](https://sdkman.io/install) steps to install sdkman and execute the following commands to install the specified version of Java and Gradle.

        sdk list java
        sdk install java 17.0.2-open
        sdk use java 17.0.2-open
        sdk install gradle 7.4-rc-1


#### Output:
QSE will output SHACL shapes in the `output_file_path` directory along with `classFrequency.csv` file containing number of instances (nodes) of each class in the dataset and some other logs.
- The file with suffix `_QSE_FULL_SHACL.ttl` contains the full set of SHACL shapes using the configuration provided in config.properties file. 
- The file with suffix `_QSE_0.1_100_SHACL.ttl` contains the set of SHACL shapes pruned using confidence 0.1 and support 100 using the configuration provided in config.properties file.

---
### Reproducibility 
If you want to reproduce the results of the paper, please read  [VLDB_Reproducibility_README](https://github.com/dkw-aau/qse/blob/main/VLDB_Reproducibility_README.md) file.

### Citation

If you use this tool please reference:
>  Rabbani, Kashif; Lissandrini, Matteo; and Hose, Katja. **Extraction of Validating Shapes from very large Knowledge Graphs**. In Proceedings of the Very Large Databases 2023 (Volume 16), August 28 - Sept 02, 2023, Vancouver, Canada.
