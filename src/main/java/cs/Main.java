package cs;

import cs.others.parsers.BaselineParserWithBloomFilterCache;
import cs.others.parsers.BaselineParserWithBloomFilters;
import cs.others.parsers.mg.MgSchemaExtractor;
import cs.others.parsers.mg.MgSchemaExtractorCache;
import cs.others.parsers.mg.WikiDataMgSeCacheBf;
import cs.qse.endpoint.EndpointParser;
import cs.qse.Parser;
import cs.utils.ConfigManager;
import cs.utils.Constants;
import cs.utils.Utils;


public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    public static int numberOfInstances;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes")); // expected or estimated numberOfClasses
        numberOfInstances = Integer.parseInt(ConfigManager.getProperty("expected_number_of_lines")); // expected or estimated numberOfInstances
        benchmark();
    }
    
    private static void benchmark() {
        System.out.println("Benchmark Initiated for " + ConfigManager.getProperty("dataset_path"));
        Utils.getCurrentTimeStamp();
        try {
            if (isOn("BlSchemaExtractor")) {
                System.out.println("Parser");
                new Parser(datasetPath, numberOfClasses, numberOfInstances, Constants.RDF_TYPE).run();
            }
    
            if (isOn("EndpointSchemaExtractor")) {
                System.out.println("EndpointSchemaExtractor - Using SPARQL Queries");
                new EndpointParser().run();
            }
            
            if (isOn("WikiDataSchemaExtractor")) {
                System.out.println("WikiParser");
                new Parser(datasetPath, numberOfClasses, numberOfInstances, Constants.INSTANCE_OF).run();
                //new WikiParser(datasetPath, numberOfClasses).run();
            }
            
            if (isOn("BfSchemaExtractor")) {
                System.out.println("BfSchemaExtractor - Bloom Filters");
                new BaselineParserWithBloomFilters(datasetPath, numberOfClasses).run();
            }
            
            if (isOn("BfSchemaExtractorCache")) {
                System.out.println("BfSchemaExtractorCache - Bloom Filters With Cache");
                new BaselineParserWithBloomFilterCache(datasetPath, numberOfClasses).run();
            }
            
            if (isOn("MgSchemaExtractor")) {
                System.out.println("MgSchemaExtractor");
                new MgSchemaExtractor(datasetPath, numberOfClasses).run();
            }
            
            if (isOn("MgSchemaExtractorCache")) {
                System.out.println("MgSchemaExtractorCache");
                new MgSchemaExtractorCache(datasetPath, numberOfClasses).run();
            }
    
            if (isOn("WikiDataMgSchemaExtractor")) {
                System.out.println("WikiDataMgSchemaExtractor");
                new WikiDataMgSeCacheBf(datasetPath, numberOfClasses).run();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean isOn(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
    
    private static void custom() {
        /*
        ArrayList<RoaringBitmap> roaringBitmapArrayList = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(args[0]); i++) {
            int[] intArray = new int[Integer.parseInt(args[1])]; // allocating memory
            roaringBitmapArrayList.add(RoaringBitmap.bitmapOf(intArray));
        }
        
        
        ArrayList<BloomFilter> bloomFilterArrayList = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(args[2]); i++) {
            bloomFilterArrayList.add(new FilterBuilder(Integer.parseInt(args[0]), Double.parseDouble(args[1])).buildBloomFilter());
        }*/
    }
}
