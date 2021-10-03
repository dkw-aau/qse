package cs.utils;

import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.query.BindingSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StatsCollector {
    private final GraphDBUtils graphDBUtils;
    
    public StatsCollector() {
        this.graphDBUtils = new GraphDBUtils();
    }
    
    public void doTheJob() {
        List<String[]> vertices = FilesUtil.readCsvAllDataOnceWithPipeSeparator(Constants.TEMP_DATASET_FILE);
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE_2), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            StopWatch watch = new StopWatch();
            watch.start();
            for (String[] v : vertices) {
                
                String query = buildQuery(v[0], v[1], v[2]);
                String count = "";
                for (BindingSet result : graphDBUtils.runSelectQuery(query)) {
                    count = result.getValue("val").stringValue();
                }
                //class, prop, obj, support, classInstanceCount, query, count by SPARQL Endpoint
                String line = v[0] + "|" + v[1] + "|" + v[2] + "|" + v[3] + "|" + v[4] + "|" + "\"" + query + "\"" + "|" + count;
                printWriter.println(line);
            }
            printWriter.close();
            watch.stop();
            System.out.println("Time Elapsed StatsCollector : " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }
    
    public void justTestOneQuery() {
        String query = "SELECT ( COUNT( DISTINCT ?s) AS ?val) WHERE {?s a <http://dbpedia.org/ontology/SoccerClub>.?s <http://www.w3.org/2002/07/owl#differentFrom> ?obj .?obj a <http://dbpedia.org/ontology/SoccerClub> ;}";
        String count = "";
        for (BindingSet result : graphDBUtils.runSelectQuery(query)) {
            count = result.getValue("val").stringValue();
        }
        System.out.println("A: " + count);
        query = "PREFIX onto: <http://www.ontotext.com/> SELECT ( COUNT( DISTINCT ?s) AS ?val) FROM onto:explicit WHERE {?s a <http://dbpedia.org/ontology/SoccerClub>.?s <http://www.w3.org/2002/07/owl#differentFrom> ?obj .?obj a <http://dbpedia.org/ontology/SoccerClub> ;}";
        count = "";
        for (BindingSet result : graphDBUtils.runSelectQuery(query)) {
            count = result.getValue("val").stringValue();
        }
        System.out.println("B: " + count);
    }
    
    
    private String buildQuery(String classVal, String propVal, String objTypeVal) {
        String query = "";
        if (objTypeVal.contains("<")) {
            query = "PREFIX onto: <http://www.ontotext.com/>" +
                    "SELECT ( COUNT( DISTINCT ?s) AS ?val) FROM onto:explicit WHERE {" +
                    "?s a   <" + classVal + "> . " +
                    "?s <" + propVal + "> ?obj . " +
                    "FILTER(dataType(?obj) = <" + objTypeVal + "> ) " +
                    "}";
        } else {
            query =
                    "PREFIX onto: <http://www.ontotext.com/>" +
                            "SELECT ( COUNT( DISTINCT ?s) AS ?val) FROM onto:explicit WHERE {" +
                            "?s a <" + classVal + ">." +
                            "?s <" + propVal + "> ?obj ." +
                            "?obj a <" + objTypeVal + "> ;" +
                            "}" +
                            "";
        }
        return query;
        
    }
}
