package cs.utils.graphdb;

import cs.utils.FilesUtil;
import cs.utils.Utils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Literal;

import java.util.concurrent.TimeUnit;

public class ExampleQueryExecutor {
    private final GraphDBUtils graphDBUtils;
    String sparqlQuery;
    
    public ExampleQueryExecutor() {
        this.graphDBUtils = new GraphDBUtils();
        sparqlQuery = "select * where { \n" +
                "\t?s ?p ?o .\n" +
                "} ";
    }
    
    public void runQuery() {
        StopWatch watch = new StopWatch();
        watch.start();
        //This query will return a table having two columns class: IRI of the class, classCount: number of instances of class
        System.out.println("About to run query: " + sparqlQuery);
        System.out.println("Iterating over results:");
        //graphDBUtils.streamQuery(sparqlQuery);
        /*graphDBUtils.runSelectQuery(sparqlQuery).forEach(row -> {
          //do nothing - just iterate
        });*/
        watch.stop();
        System.out.println("Time Elapsed runQuery: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}
