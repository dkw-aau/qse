### QSE Configuration Parameters

Please update the configuration file for each dataset available in the [config](https://github.com/dkw-aau/qse/tree/main/config) directory, i.e., `dbpediaConfig`, `yagoConfig`, `lubmConfig`, `wdt15Config`, and `wdt21Config` to set the correct paths for your machine.
You have to choose from one of these options to either extract shapes using QSE-Exact (file or query-based) or QSE-Approximate.

| Parameter                   | Description                                                                       | Options           |
|-----------------------------|-----------------------------------------------------------------------------------|-------------------|
| qse_exact_file              | set the value to extract shapes from a file using QSE-Exact                       | `true` or `false` |
| qse_exact_query_based       | set the value to extract shapes from an endpoint using QSE-Exact                  | `true` or `false` |
| qse_approximate_file        | set the value to extract shapes from a file using QSE-Approximate                 | `true` or `false` |
| qse_approximate_query_based | set the value to extract shapes from an endpoint using QSE-Approximate                  | `true` or `false` |


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
| config_dir_path                   | set the path of config directory where configuration for pruning and other configs like list of specific classes is available    | `qse/config/`                 |
| qse_validation                    | set if you want to use this dataset for validation using shapes available in `validation_input_dir`                              | `true` or  `false`            |
| validation_input_dir              | set the path of directory where SHACL shapes are available to be used for validation                                             | `qse/validation`              |
| qse_validation_with_shNot         | set if you want to use `sh:Not` constraint for finding errors during validation                                                  | `true` or  `false`            |

There are config files available in [config](https://github.com/dkw-aau/qse/tree/main/config) directory for each of our dataset used in experiments.