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
                String line = v[0] + "|" + v[1] + "|" + v[2] + "|" + v[3] + "|" + "\"" + query + "\"" + "|" + count;
                printWriter.println(line);
            }
            printWriter.close();
            watch.stop();
            System.out.println("Time Elapsed StatsCollector : " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }
    
    
    private String buildQuery(String classVal, String propVal, String objTypeVal) {
        String query = "";
        if (objTypeVal.equals("<http://www.w3.org/2001/XMLSchema#string>") || objTypeVal.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")) {
            query =
                    "SELECT ( COUNT( DISTINCT ?s) AS ?val) WHERE {" +
                            "?s a <" + classVal + ">." +
                            "?s <" + propVal + "> ?obj ." +
                            "FILTER(isLiteral(?obj))" +
                            "}" +
                            "";
        } else if (objTypeVal.contains("<")) {
            query =
                    "SELECT ( COUNT( DISTINCT ?s) AS ?val) WHERE {" +
                            "?s a <" + classVal + ">." +
                            "?s <" + propVal + "> ?obj ." +
                            "?obj a " + objTypeVal + " ;" +
                            "}" +
                            "";
        } else {
            query =
                    "SELECT ( COUNT( DISTINCT ?s) AS ?val) WHERE {" +
                            "?s a <" + classVal + ">." +
                            "?s <" + propVal + "> ?obj ." +
                            "?obj a <" + objTypeVal + "> ;" +
                            "}" +
                            "";
        }
        return query;
        
    }
}
