package cs.utils;

import com.google.common.collect.Sets;
import cs.qse.experiments.ExperimentsUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PrecisionRecallComputer {
    Set<Value> psA;
    Set<Value> psB;
    Map<Value, Set<Value>> npsA;
    Map<Value, Set<Value>> npsB;
    List<String> logsPrecisionRecall = new ArrayList<>();
    List<String> logsWronglyPrunedShapes = new ArrayList<>();
    
    public PrecisionRecallComputer() {
        String file1 = "/Users/kashifrabbani/Documents/GitHub/shacl/Output/WWW/dbpedia/models/dbpedia_ml_DEFAULT_SHACL.ttl";
        String file2 = "/Users/kashifrabbani/Documents/GitHub/shacl/Output/WWW/dbpedia/models/dbpedia_ml_CUSTOM_0.5_50_SHACL.ttl";
        
        //readFiles();
        processFiles(file1, file2);
        computePrecisionRecall();
        //computeStatisticsOfWronglyPrunedShapes();
        System.out.println();
        
    }
    
    private void readFiles() {
        //compare default models
        String fileA = ConfigManager.getProperty("output_file_path") + "/sampled/" + ConfigManager.getProperty("dataset_name") + "_DEFAULT_SHACL.ttl";
        String fileB = ConfigManager.getProperty("output_file_path") + "/default/" + ConfigManager.getProperty("dataset_name") + "_DEFAULT_SHACL.ttl";
        logsPrecisionRecall.add(fileA);
        logsPrecisionRecall.add(fileB);
        logsPrecisionRecall.add("STANDARD_CONF");
        logsPrecisionRecall.add("STANDARD_SUPP");
        processFiles(fileA, fileB);
        
        //compare pruned models
        for (Map.Entry<Double, List<Integer>> entry : ExperimentsUtil.getSupportConfRange().entrySet()) {
            Double conf = entry.getKey();
            List<Integer> supportRange = entry.getValue();
            for (Integer supp : supportRange) {//se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
                fileA = ConfigManager.getProperty("output_file_path") + "/sampled/" + ConfigManager.getProperty("dataset_name") + "_CUSTOM_" + conf + "_" + supp + "_SHACL.ttl";
                fileB = ConfigManager.getProperty("output_file_path") + "/default/" + ConfigManager.getProperty("dataset_name") + "_CUSTOM_" + conf + "_" + supp + "_SHACL.ttl";
                logsPrecisionRecall.add(fileA);
                logsPrecisionRecall.add(fileB);
                logsPrecisionRecall.add(String.valueOf(conf));
                logsPrecisionRecall.add(String.valueOf(supp));
                processFiles(fileA, fileB);
            }
        }
    }
    
    private void processFiles(String fileA, String fileB) {
        Model modelA = createModel(fileA);
        Model modelB = createModel(fileB);
        
        npsA = getNodePropShapes(modelA);
        npsB = getNodePropShapes(modelB);
        
        psA = new HashSet<>();
        psB = new HashSet<>();
        
        npsA.values().forEach(psA::addAll);
        npsB.values().forEach(psB::addAll);
        
        logsPrecisionRecall.add(String.valueOf(npsA.keySet().size()));
        logsPrecisionRecall.add(String.valueOf(psA.size()));
        
        logsPrecisionRecall.add(String.valueOf(npsB.keySet().size()));
        logsPrecisionRecall.add(String.valueOf(psB.size()));
    }
    
    private void computePrecisionRecall() {
        Set<Value> commonNs = Sets.intersection(npsA.keySet(), npsB.keySet());
        Set<Value> commonPs = Sets.intersection(psA, psB);
        
        double precision_ns = compute(commonNs.size(), npsB.keySet().size());
        double recall_ns = compute(commonNs.size(), npsA.keySet().size());
        
        double precision_ps = compute(commonPs.size(), psB.size());
        double recall_ps = compute(commonPs.size(), npsA.keySet().size());
        
        logsPrecisionRecall.add(String.valueOf(precision_ns));
        logsPrecisionRecall.add(String.valueOf(recall_ns));
        logsPrecisionRecall.add(String.valueOf(precision_ps));
        logsPrecisionRecall.add(String.valueOf(recall_ps));
    }
    
    private void computeStatisticsOfWronglyPrunedShapes() {
        Set<Value> diffNs = Sets.difference(npsA.keySet(), npsB.keySet());
        Set<Value> diffPs = Sets.difference(psA, psB);
    }
    
    
    private Model createModel(String file) {
        Model model = null;
        try {
            model = Rio.parse(new FileInputStream(file), "", RDFFormat.TURTLE);
            // To check that we have correctly read the file, let's print out the model to the screen again
            //for (Statement statement : model) {System.out.println(statement);}
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }
    
    private Map<Value, Set<Value>> getNodePropShapes(Model model) {
        Map<Value, Set<Value>> nodeToPropertyShapes = new HashMap<>();
        Repository db = new SailRepository(new MemoryStore());
        db.init();
        try (RepositoryConnection conn = db.getConnection()) {
            conn.add(model);
            conn.setNamespace("sh", SHACL.NAMESPACE);
            conn.setNamespace("shape", Constants.SHAPES_NAMESPACE);
            TupleQuery queryNs = conn.prepareTupleQuery(readQuery("query_node_shapes"));
            List<Value> queryNsOutput = executeQuery(queryNs, "nodeShape");
            queryNsOutput.forEach(value -> {
                nodeToPropertyShapes.putIfAbsent(value, new HashSet<>());
                TupleQuery queryPs = conn.prepareTupleQuery(readQuery("query_property_shapes").replace("NODE_SHAPE", value.stringValue()));
                List<Value> queryPsOutput = executeQuery(queryPs, "propertyShape");
                nodeToPropertyShapes.get(value).addAll(queryPsOutput);
            });
            return nodeToPropertyShapes;
        } finally {
            db.shutDown();
        }
    }
    
    private List<Value> executeQuery(TupleQuery query, String bindingName) {
        List<Value> output = new ArrayList<>();
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet solution = result.next();
                output.add(solution.getValue(bindingName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
    
    private String readQuery(String query) {
        String q = null;
        try {
            String queriesDirectory = ConfigManager.getProperty("resources_path") + "/stats/";
            q = new String(Files.readAllBytes(Paths.get(queriesDirectory + query + ".txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q;
    }
    
    private double compute(int nominator, int denominator) {
        return (double) nominator / (double) denominator;
    }
    
}
