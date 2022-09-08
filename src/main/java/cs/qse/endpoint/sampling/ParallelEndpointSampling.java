package cs.qse.endpoint.sampling;

import com.google.common.collect.Lists;
import cs.Main;
import cs.qse.EntityData;
import cs.qse.ShapesExtractor;
import cs.qse.SupportConfidence;
import cs.qse.experiments.ExperimentsUtil;
import cs.utils.*;
import cs.utils.encoders.ConcurrentEncoder;
import cs.utils.encoders.Encoder;
import cs.utils.encoders.NodeEncoder;
import cs.utils.graphdb.GraphDBUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParallelEndpointSampling {
    private final GraphDBUtils graphDBUtils;
    Integer expectedNumberOfClasses;
    Integer expNoOfInstances;
    ConcurrentEncoder encoder;
    String typePredicate;
    NodeEncoder nodeEncoder;
    Integer maxEntityThreshold;
    
    // In the following the size of each data structure
    // N = number of distinct nodes in the graph
    // T = number of distinct types
    // P = number of distinct predicates
    
    Map<Integer, EntityData> entityDataMapContainer; // Size == N For every entity (encoded as integer) we save a number of summary information
    Map<Integer, Integer> classEntityCount; // Size == T
    Map<Integer, List<Integer>> sampledEntitiesPerClass; // Size == O(T*entityThreshold)
    Map<Integer, Integer> reservoirCapacityPerClass; // Size == T
    
    
    Map<Integer, Map<Integer, Set<Integer>>> classToPropWithObjTypes; // Size O(T*P*T)
    Map<Tuple3<Integer, Integer, Integer>, SupportConfidence> shapeTripletSupport; // Size O(T*P*T) For every unique <class,property,objectType> tuples, we save their support and confidence
    Map<Integer, Integer> propCount; // real count of *all (entire graph)* triples having predicate P   // |P| =  |< _, P , _ >| in G
    Map<Integer, Integer> sampledPropCount; // count of triples having predicate P across all entities in all reservoirs  |< _ , P , _ >| (the sampled entities)
    
    public ParallelEndpointSampling(int expNoOfClasses, int expNoOfInstances, String typePredicate, Integer entitySamplingThreshold) {
        this.graphDBUtils = new GraphDBUtils();
        this.expectedNumberOfClasses = expNoOfClasses;
        this.expNoOfInstances = expNoOfInstances;
        this.typePredicate = typePredicate;
        this.classEntityCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.sampledEntitiesPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        this.entityDataMapContainer = new HashMap<>((int) ((expNoOfInstances) / 0.75 + 1));
        
        this.encoder = new ConcurrentEncoder();
        this.nodeEncoder = new NodeEncoder();
        this.maxEntityThreshold = entitySamplingThreshold;
        
        //this.classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        //this.propCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        //this.sampledPropCount = new HashMap<>((int) ((10000) / 0.75 + 1));
        //this.shapeTripletSupport = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    }
    
    
    public void run() {
        System.out.println("Started ParallelEndpointSampling ...");
        getNumberOfInstancesOfEachClass();
        dynamicBullyReservoirSampling();  // send a query to the endpoint and get all entities, parse the entities and sample using reservoir sampling
        collectEntityPropData(4); //run query for each sampled entity to get the property metadata ...
        writeSupportToFile();
        extractSHACLShapes(false);
    }
    
    private void getNumberOfInstancesOfEachClass() {
        StopWatch watch = new StopWatch();
        watch.start();
        String query = FilesUtil.readQuery("query2").replace(":instantiationProperty", typePredicate);
        //This query will return a table having two columns class: IRI of the class, classCount: number of instances of class
        graphDBUtils.runSelectQuery(query).forEach(result -> {
            String c = result.getValue("class").stringValue();
            int classCount = 0;
            if (result.getBinding("classCount").getValue().isLiteral()) {
                Literal literalClassCount = (Literal) result.getBinding("classCount").getValue();
                classCount = literalClassCount.intValue();
            }
            classEntityCount.put(encoder.encode(c), classCount);
        });
        watch.stop();
        System.out.println("Time Elapsed getNumberOfInstancesOfEachClass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logTime("getNumberOfInstancesOfEachClass ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void dynamicBullyReservoirSampling() {
        System.out.println("invoked:dynamicBullyReservoirSampling()");
        StopWatch watch = new StopWatch();
        watch.start();
        String queryToGetWikiDataEntities = "PREFIX onto: <http://www.ontotext.com/>  CONSTRUCT from onto:explicit WHERE { ?s " + typePredicate + " ?o .} ";
        
        Random random = new Random(100);
        AtomicInteger lineCounter = new AtomicInteger();
        this.reservoirCapacityPerClass = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
        int minEntityThreshold = 1;
        int samplingPercentage = Main.entitySamplingTargetPercentage;
        DynamicBullyReservoirSamplingEndpoint drs = new DynamicBullyReservoirSamplingEndpoint(entityDataMapContainer, sampledEntitiesPerClass, reservoirCapacityPerClass, nodeEncoder, encoder);
        try {
            graphDBUtils.runConstructQuery(queryToGetWikiDataEntities).forEach(line -> {
                try {
                    String triple = "<" + line.getSubject() + "> <" + line.getPredicate() + "> <" + line.getObject() + "> ."; // prepare triple in N3 format to avoid changing many methods using nodes of type Node
                    Node[] nodes = NxParser.parseNodes(triple); // Get [S,P,O] as Node from triple
                    
                    int objID = encoder.encode(nodes[2].getLabel());
                    sampledEntitiesPerClass.putIfAbsent(objID, new ArrayList<>(maxEntityThreshold));
                    reservoirCapacityPerClass.putIfAbsent(objID, minEntityThreshold);
                    
                    if (sampledEntitiesPerClass.get(objID).size() < reservoirCapacityPerClass.get(objID)) {
                        drs.sample(nodes);
                    } else {
                        drs.replace(random.nextInt(lineCounter.get()), nodes);
                    }
                    classEntityCount.merge(objID, 1, Integer::sum); // Get the real entity count for current class
                    drs.resizeReservoir(classEntityCount.get(objID), sampledEntitiesPerClass.get(objID).size(), maxEntityThreshold, samplingPercentage, objID);
                    
                    lineCounter.getAndIncrement(); // increment the line counter
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("firstPass:dynamicBullyReservoirSampling", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
        Utils.logSamplingStats("dynamicBullyReservoirSampling", samplingPercentage, minEntityThreshold, maxEntityThreshold, entityDataMapContainer.size());
    }
    
    private void collectEntityPropData(Integer numberOfThreads) {
        StopWatch watch = new StopWatch();
        watch.start();
        System.out.println("Started collectEntityPropData(" + numberOfThreads + ")");
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        try {
            List<Integer> entitiesList = new ArrayList<>(entityDataMapContainer.keySet());
            List<List<Integer>> entitiesPart = Lists.partition(entitiesList, entitiesList.size() / numberOfThreads);
            
            //declare jobs
            List<Callable<SubEntityPropDataCollector>> jobs = new ArrayList<>();
            
            // create jobs
            int jobIndex = 1;
            for (List<Integer> part : entitiesPart) {
                int finalJobIndex = jobIndex;
                jobs.add(() -> {
                    SubEntityPropDataCollector subEntityPdc = new SubEntityPropDataCollector(expectedNumberOfClasses);
                    subEntityPdc.setFeatures(typePredicate, encoder, nodeEncoder);
                    subEntityPdc.job(finalJobIndex, part, entityDataMapContainer);
                    return subEntityPdc;
                });
                jobIndex++;
            }
            
            //execute jobs using invokeAll() method and collect results
            try {
                List<Future<SubEntityPropDataCollector>> subEntityPropDataCollectors = executor.invokeAll(jobs);
                
                StopWatch sw = new StopWatch();
                sw.start();
                int i = 0;
                for (Future<SubEntityPropDataCollector> subEntityPdc : subEntityPropDataCollectors) {
                    SubEntityPropDataCollector sEpDc = subEntityPdc.get();
                    if (i == 0) {
                        this.classToPropWithObjTypes = sEpDc.classToPropWithObjTypes;
                        this.shapeTripletSupport = sEpDc.shapeTripletSupport;
                        this.sampledPropCount = sEpDc.sampledPropCount;
                    } else {
                        mergeJobsOutput(sEpDc);
                    }
                    i++;
                }
                sw.stop();
                Utils.logTime("cs.qse.endpoint.collectEntityPropData:MergingJobs", TimeUnit.MILLISECONDS.toSeconds(sw.getTime()), TimeUnit.MILLISECONDS.toMinutes(sw.getTime()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdownNow();
        watch.stop();
        Utils.logTime("cs.qse.endpoint.collectEntityPropData with threads: " + numberOfThreads + " ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    private void mergeJobsOutput(SubEntityPropDataCollector sEpDc) {
        // 1. Merging class to property with object type information
        sEpDc.classToPropWithObjTypes.forEach((c, localPwOt) -> {
            Map<Integer, Set<Integer>> globalPwOt = this.classToPropWithObjTypes.get(c);
            if (globalPwOt == null) {
                this.classToPropWithObjTypes.put(c, localPwOt);
            } else {
                localPwOt.forEach((p, localOt) -> {
                    Set<Integer> globalOt = globalPwOt.get(p);
                    if (globalOt == null) {
                        globalPwOt.put(p, localOt);
                    } else {
                        globalPwOt.get(p).addAll(localOt);
                    }
                });
                this.classToPropWithObjTypes.put(c, globalPwOt);
            }
        });
        
        // 2. Merging shape triplet support
        sEpDc.shapeTripletSupport.forEach((tuple3, localSupp) -> {
            SupportConfidence globalSupp = this.shapeTripletSupport.get(tuple3);
            if (globalSupp == null) {
                this.shapeTripletSupport.put(tuple3, localSupp);
            } else {
                this.shapeTripletSupport.put(tuple3, new SupportConfidence(globalSupp.getSupport() + localSupp.getSupport()));
            }
        });
        
        // 3. Merging count of sampled properties
        sEpDc.sampledPropCount.forEach((p, localCount) -> {
            Integer globalCount = this.sampledPropCount.get(p);
            if (globalCount == null) {
                this.sampledPropCount.put(p, localCount);
            } else { //equivalent to this.sampledPropCount.containsKey(p)
                this.sampledPropCount.put(p, globalCount + localCount);
            }
        });
    }
    
    
    public void writeSupportToFile() {
        System.out.println("Started writeSupportToFile()");
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileWriter fileWriter = new FileWriter(new File(Constants.TEMP_DATASET_FILE), false);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            for (Map.Entry<Tuple3<Integer, Integer, Integer>, SupportConfidence> entry : this.shapeTripletSupport.entrySet()) {
                Tuple3<Integer, Integer, Integer> tupl3 = entry.getKey();
                Integer count = entry.getValue().getSupport();
                String log = encoder.decode(tupl3._1) + "|" + encoder.decode(tupl3._2) + "|" +
                        encoder.decode(tupl3._3) + "|" + count + "|" + sampledEntitiesPerClass.get(tupl3._1).size();
                printWriter.println(log);
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        watch.stop();
        Utils.logTime("writeSupportToFile(): ", TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
    protected void extractSHACLShapes(Boolean performPruning) {
        System.out.println("Started extractSHACLShapes()");
        StopWatch watch = new StopWatch();
        watch.start();
        String methodName = "extractSHACLShapes:No Pruning";
        ShapesExtractorEndpoint se = new ShapesExtractorEndpoint(encoder, shapeTripletSupport, classEntityCount);
        //se.setPropWithClassesHavingMaxCountOne(statsComputer.getPropWithClassesHavingMaxCountOne());
        se.constructDefaultShapes(classToPropWithObjTypes); // SHAPES without performing pruning based on confidence and support thresholds
        if (performPruning) {
            StopWatch watchForPruning = new StopWatch();
            watchForPruning.start();
            ExperimentsUtil.getSupportConfRange().forEach((conf, supportRange) -> {
                supportRange.forEach(supp -> {
                    StopWatch innerWatch = new StopWatch();
                    innerWatch.start();
                    se.constructPrunedShapes(classToPropWithObjTypes, conf, supp);
                    innerWatch.stop();
                    Utils.logTime(conf + "_" + supp + "", TimeUnit.MILLISECONDS.toSeconds(innerWatch.getTime()), TimeUnit.MILLISECONDS.toMinutes(innerWatch.getTime()));
                });
            });
            methodName = "extractSHACLShapes";
            watchForPruning.stop();
            Utils.logTime(methodName + "-Time.For.Pruning.Only", TimeUnit.MILLISECONDS.toSeconds(watchForPruning.getTime()), TimeUnit.MILLISECONDS.toMinutes(watchForPruning.getTime()));
        }
        
        ExperimentsUtil.prepareCsvForGroupedStackedBarChart(Constants.EXPERIMENTS_RESULT, Constants.EXPERIMENTS_RESULT_CUSTOM, true);
        watch.stop();
        Utils.logTime(methodName, TimeUnit.MILLISECONDS.toSeconds(watch.getTime()), TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
    
}
