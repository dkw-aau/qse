package cs.utils.graphdb;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.ntriples.NTriplesWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class helps in querying GraphDB
 */
public class GraphDBUtils {
    KBManagement kbManager;
    Repository repository;
    RepositoryConnection repositoryConnection;
    
    public GraphDBUtils() {
        this.kbManager = new KBManagement();
        this.repository = kbManager.initGraphDBRepository();
        this.repositoryConnection = repository.getConnection();
    }
    
    public ValueFactory getValueFactory() {
        return repositoryConnection.getValueFactory();
    }
    
    public List<BindingSet> runSelectQuery(String query) {
        List<BindingSet> result = new ArrayList<>();
        try {
            TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            TupleQueryResult classesQueryResult = tupleQuery.evaluate();
            classesQueryResult.forEach(result::add);
            classesQueryResult.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
        return result;
    }
    
    public TupleQueryResult evaluateSelectQuery(String query) {
        TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, query);
        return tupleQuery.evaluate();
    
        //try (TupleQueryResult result = tupleQuery.evaluate()) {
//            for (BindingSet bindingSet : result) {
//                //Value firstValue = bindingSet.getValue(bindingNames.get(0));
//                //Value secondValue = bindingSet.getValue(bindingNames.get(1));
//                // do something interesting with the values here...
//                count++;
//            }
        //}
    }
    
    public Boolean runAskQuery(String query) {
        Boolean result = null;
        try {
            BooleanQuery queryResult = repositoryConnection.prepareBooleanQuery(query);
            result = queryResult.evaluate();
        } catch (Exception e) {
            e.printStackTrace();
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
        return result;
    }
    
    public GraphQueryResult runConstructQuery(String query) {
        GraphQueryResult resultantTriples = null;
        try {
            GraphQuery queryResult = repositoryConnection.prepareGraphQuery(query);
            resultantTriples = queryResult.evaluate();
            
        } catch (Exception e) {
            e.printStackTrace();
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
        return resultantTriples;
    }
    
    public void runConstructQuery(String query, String address) {
        try {
            GraphQuery queryResult = repositoryConnection.prepareGraphQuery(query);
            GraphQueryResult resultantTriples = queryResult.evaluate();
            
            FileWriter fileWriter = new FileWriter(address, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            resultantTriples.forEach(statement -> {
                printWriter.println("<" + statement.getSubject() + "> <" + statement.getPredicate() + "> <" + statement.getObject() + "> .");
            });
            
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
    }
    
    public void runGraphQuery(String query, String address) {
        try {
            GraphQuery graphQuery = repositoryConnection.prepareGraphQuery(QueryLanguage.SPARQL, query);
            
            GraphQueryResult graphQueryResult = graphQuery.evaluate();
            
            OutputStream out = new FileOutputStream(address, true);
            RDFWriter writer = new NTriplesWriter(out);
            graphQuery.evaluate(writer);
            
            graphQueryResult.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
    }
    
    public List<BindingSet> runSelectQueryWithTimeOut(String query) {
        List<BindingSet> result = new ArrayList<>();
        try {
            TupleQuery tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            tupleQuery.setMaxExecutionTime(300);
            TupleQueryResult classesQueryResult = tupleQuery.evaluate();
            classesQueryResult.forEach(result::add);
            classesQueryResult.close();
        } catch (Exception e) {
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
        return result;
    }
    
    public void updateQueryExecutor(String query) {
        try {
            repositoryConnection.begin();
            Update updateOperation = repositoryConnection.prepareUpdate(QueryLanguage.SPARQL, query);
            updateOperation.execute();
            repositoryConnection.commit();
            //repositoryConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (repositoryConnection.isActive())
                repositoryConnection.rollback();
        }
    }
}