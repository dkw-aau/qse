## Anytime Algorithm to Generate  Validating Schemas for RDF Datasets

### Setup

Follow [these](https://sdkman.io/install) steps to install sdkman.
1. List available java versions using
    
        sdk list java
   
2. Install Java

        sdk install java 11.0.10.fx-zulu 
   
         Use this version because of integrating ART library 
         -> sdk install java 15.0.2.fx-zulu
         -> sdk use java 15.0.2.fx-zulu 

3. Install gradle 

        sdk install gradle 6.8.3

4. Build project
       
        gradle clean
    
        gradle build
    
        gradle shadowJar




### Datasets
1. #### LUBM
    We use [uba](https://github.com/rvesse/lubm-uba) lubm data generator.

2. #### YAGO-4
    We use [English](https://yago-knowledge.org/downloads/yago-4) version of latest YAGO-4 dataset.
