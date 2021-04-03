###Setup
Using gradle 6.8.3 & Java "11.0.10"

    gradle clean

    gradle build

    gradle shadowJar

### LUBM Data Generation

https://github.com/rvesse/lubm-uba

### Results per method

|           	|        method       	| parallel 	| No. of Triples 	| No. of Classes 	| Time in MS 	| Time in Seconds 	| Time in Minutes 	|
|:---------:	|:-------------------:	|:--------:	|:--------------:	|:--------------:	|:----------:	|:---------------:	|:---------------:	|
|  LUBM-500 	| Stream Over Triples 	|    no    	|   50 Million   	|       15       	|    35420   	|                 	|                 	|
|  LUBM-500 	| Stream Over Triples 	|    no    	|   91 Million   	|       23       	|   156214   	|                 	|                 	|
| LUBM-1000 	| Stream Over Triples 	|    no    	|   138 Million  	|       15       	|   113090   	|                 	|                 	|