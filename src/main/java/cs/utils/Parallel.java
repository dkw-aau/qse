//package cs.qse.endpoint;
//
//import com.google.common.collect.Lists;
//import org.apache.commons.collections.map.HashedMap;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//
//public class Parallel {
//    Map<Integer, String> sampledEntities = new HashMap<Integer, String>();
//
//    public Parallel() {
//        for (int i = 0; i < 1001; i++) {
//            sampledEntities.put(i, "entity_" + i);
//        }
//    }
//
//    private void parallelize() {
//        int numberOfThreads = 2;
//        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
//        try {
//            List<Callable<HashMap<Integer, String>>> jobs = new ArrayList<>();
//            List<Integer> entities = new ArrayList<>(sampledEntities.keySet());
//            List<List<Integer>> partition = Lists.partition(entities, entities.size()/numberOfThreads);
//            System.out.println(partition);
//
//            jobs.add(() -> work(entities.subList(0, entities.size() / 2)));
//            jobs.add(() -> work(entities.subList((entities.size() / 2) + 1, entities.size())));
//
//
//            //execute tasks list using invokeAll() method
//            try {
//                List<Future<HashMap<Integer, String>>> results = executor.invokeAll(jobs);
//
//                for (Future<HashMap<Integer, String>> result : results) {
//                    System.out.println(result.get());
//                }
//            } catch (InterruptedException e1) {
//                e1.printStackTrace();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        executor.shutdownNow();
//    }
//
//    private HashMap<Integer, String> work(List<Integer> list) {
//        System.out.println("Running Thread Name: " + Thread.currentThread().getName() + " -> " + list);
//        HashMap<Integer, String> tempHashMap = new HashMap<Integer, String>();
//        list.forEach(val -> {
//            tempHashMap.put(val + 100, "entity_" + val + " + 100");
//        });
//        return tempHashMap;
//    }
//
//
//    public static void main(String[] args) throws ExecutionException {
//        Parallel parallel = new Parallel();
//        parallel.parallelize();
//    }
//}
///*
//ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
//try {
//    List<Callable<Void>> jobs = new ArrayList<>();
//    for (Map.Entry<Integer, EntityData> entry : entityDataMapContainer.entrySet()) {
//        Integer subjID = entry.getKey();
//        EntityData entityData = entry.getValue();
//        jobs.add(() -> {
//            collectMetaData(subjID, entityData);
//            return null;
//        });
//    }
//    executor.invokeAll(jobs);
//    executor.shutdown();
//} catch (Exception e) {
//    e.printStackTrace();
//}
//https://howtodoinjava.com/java/multi-threading/executor-service-example/
//https://graphdb.ontotext.com/
//*/