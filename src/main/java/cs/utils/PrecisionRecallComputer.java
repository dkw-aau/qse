package cs.utils;

import com.google.common.collect.Sets;
import cs.Main;
import cs.qse.common.ExperimentsUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

public class PrecisionRecallComputer {
    Set<Value> psA;
    Set<Value> psB;
    Map<Value, Set<Value>> npsA;
    Map<Value, Set<Value>> npsB;
    String baseAddressA;
    String baseAddressB;
    String outputFilePath;
    
    List<String> columnsCSV;
    List<String> precisionRecallCSV = new ArrayList<>();
    List<String> logsWronglyPrunedShapes = new ArrayList<>();
    
    public PrecisionRecallComputer() {
        getBaseAddress();
        prepareCsvHeader();
        computePrecisionRecallForDefaultModels();
        computePrecisionRecallForPrunedModels();
        
        precisionRecallCSV.forEach(line -> {
            String fileAddress = outputFilePath + ConfigManager.getProperty("dataset_name") + "_PrecisionRecall.csv";
            Utils.writeLineToFile(line, fileAddress);
        });
    }
    
    private void getBaseAddress() {
        Path path = Paths.get(Main.datasetPath);
        outputFilePath = ConfigManager.getProperty("output_file_path");
        baseAddressA = ConfigManager.getProperty("default_directory") + FilenameUtils.removeExtension(path.getFileName().toString());
        baseAddressB = outputFilePath + FilenameUtils.removeExtension(path.getFileName().toString());
    }
    
    private void prepareCsvHeader() {
        String header = "File_A, File_B, Confidence, Support, NS, PS, NS_Samp, PS_Samp, Precision_NS, Recall_NS, Precision_PS, Recall_PS, MaxReservoirSize, TargetPercentage";
        precisionRecallCSV.add(header);
    }
    
    private void computePrecisionRecallForDefaultModels() {
        String fileA = baseAddressA + "_DEFAULT_SHACL.ttl";
        String fileB = baseAddressB + "_DEFAULT_SHACL.ttl";
        columnsCSV = new ArrayList<>();
        columnsCSV.add(fileA);
        columnsCSV.add(fileB);
        columnsCSV.add("-");
        columnsCSV.add("-");
        processNsAndPs(fileA, fileB);
        computePrecisionRecall();
        columnsCSV.add(String.valueOf(Main.entitySamplingThreshold));
        columnsCSV.add(String.valueOf(Main.entitySamplingTargetPercentage));
        precisionRecallCSV.add(StringUtils.join(columnsCSV, ","));
    }
    
    private void computePrecisionRecallForPrunedModels() {
        for (Map.Entry<Double, List<Integer>> entry : ExperimentsUtil.getSupportConfRange().entrySet()) {
            Double conf = entry.getKey();
            List<Integer> supportRange = entry.getValue();
            for (Integer supp : supportRange) {
                String fileA = baseAddressA + "_CUSTOM_" + conf + "_" + supp + "_SHACL.ttl";
                String fileB = baseAddressB + "_CUSTOM_" + conf + "_" + supp + "_SHACL.ttl";
                
                columnsCSV = new ArrayList<>();
                columnsCSV.add(fileA);
                columnsCSV.add(fileB);
                columnsCSV.add(String.valueOf(conf));
                columnsCSV.add(String.valueOf(supp));
                
                processNsAndPs(fileA, fileB);
                computePrecisionRecall();
                columnsCSV.add(String.valueOf(Main.entitySamplingThreshold));
                columnsCSV.add(String.valueOf(Main.entitySamplingTargetPercentage));
                precisionRecallCSV.add(StringUtils.join(columnsCSV, ","));
            }
        }
    }
    
    
    private void processNsAndPs(String fileA, String fileB) {
        Model modelA = createModel(fileA);
        Model modelB = createModel(fileB);
        
        npsA = getNodePropShapes(modelA);
        npsB = getNodePropShapes(modelB);
        
        psA = new HashSet<>();
        psB = new HashSet<>();
        
        npsA.values().forEach(psA::addAll);
        npsB.values().forEach(psB::addAll);
        
        columnsCSV.add(String.valueOf(npsA.keySet().size()));
        columnsCSV.add(String.valueOf(psA.size()));
        
        columnsCSV.add(String.valueOf(npsB.keySet().size()));
        columnsCSV.add(String.valueOf(psB.size()));
    }
    
    private void computePrecisionRecall() {
        Set<Value> commonNs = Sets.intersection(npsA.keySet(), npsB.keySet());
        Set<Value> commonPs = Sets.intersection(psA, psB);
        
        double precision_ns = divide(commonNs.size(), npsB.keySet().size());
        double recall_ns = divide(commonNs.size(), npsA.keySet().size());
        
        double precision_ps = divide(commonPs.size(), psB.size());
        double recall_ps = divide(commonPs.size(), psA.size());
        
        DecimalFormat df = new DecimalFormat("0.00");
        
        columnsCSV.add(df.format(precision_ns));
        columnsCSV.add(df.format(recall_ns));
        columnsCSV.add(df.format(precision_ps));
        columnsCSV.add(df.format(recall_ps));
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
    
    private double divide(int nominator, int denominator) {
        return (double) nominator / (double) denominator;
    }
}