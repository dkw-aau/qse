package cs.utils.graphdb;

import cs.utils.ConfigManager;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.http.HTTPUpdateExecutionException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;

import java.io.File;
import java.io.IOException;

//import static dk.uni.cs.utils.RdfUtils.logger;

/**
 * Management of the Knowledge Base (GraphDB)
 */
public class KBManagement {
    
    //Variables
    private Repository repository;
    /*private RepositoryManager repositoryManager;
    private RepositoryConnection respositoryConnection;*/
    
    /**
     * Initialisation of the GraphDB repository
     */
    public Repository initGraphDBRepository() {
//        logger.info("Init GraphDB connection.");
        //Instantiate a local repository manager and initialize it
        //repositoryManager = RepositoryProvider.getRepositoryManager(Constants.GraphDB_URL);
        try {
            RepositoryManager repositoryManager = new RemoteRepositoryManager(ConfigManager.getProperty("graphDB_URL"));
            repository = repositoryManager.getRepository(ConfigManager.getProperty("graphDB_REPOSITORY"));
            repository.init();
            //repositoryManager.initialize();
            //Get the repository from repository manager
            //repository = repositoryManager.getRepository(Constants.GraphDB_REPOSITORY);
//            logger.info("Current initialization status of the repository:");
            
        } catch (Exception e) {
//            logger.error(e.getMessage());
        }
        return repository;
    }
    
    
    /**
     * Shutdown repository and manager
     */
    public void shutDownGraphDB() {
        repository.shutDown();
        //repositoryManager.shutDown();
    }
    
    /*    *//**
     * Execution of the SPARQL query send in the input file
     *//*
	public void readAndExecuteSPARQL(File inputFile) {
		//Open connection
		initSPARQLEndpoint();

		String query = "";

		try {
			//Transforms the content of the file into String
			query = FileUtils.readFileToString(inputFile, "UTF-8");

			//Executes the query
		    respositoryConnection.prepareUpdate(QueryLanguage.SPARQL, query).execute();
		} catch (MalformedQueryException | IOException | HTTPUpdateExecutionException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	    //Close connection
	    closeSPARQLEndpoint();
	}

	*//**
     * Get connection from the repository
     *//*
	private void initSPARQLEndpoint() {
		respositoryConnection = repository.getConnection();
	}

	*//**
     * Close connection of the repository
     *//*
	private void closeSPARQLEndpoint() {
	    respositoryConnection.close();
	}*/
 
}