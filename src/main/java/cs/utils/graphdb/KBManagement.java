package cs.utils.graphdb;

import cs.utils.ConfigManager;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * This class is used for the management of the Knowledge Base (GraphDB)
 */
public class KBManagement {
    
    private Repository repository;
    
    /**
     * Initialisation of the GraphDB repository
     */
    public Repository initGraphDBRepository() {
        try {
            RepositoryManager repositoryManager = new RemoteRepositoryManager(ConfigManager.getProperty("graphdb_url"));
            repository = repositoryManager.getRepository(ConfigManager.getProperty("graphdb_repository"));
            repository.init();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return repository;
    }

    public Repository initGraphDBRepository(String graphdbUrl, String graphdbRepository) {
        try {
            RepositoryManager repositoryManager = new RemoteRepositoryManager(graphdbUrl);
            repository = repositoryManager.getRepository(graphdbRepository);
            repository.init();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return repository;
    }
    
    /**
     * Shutdown repository and manager
     */
    public void shutDownGraphDB() {
        repository.shutDown();
    }
}