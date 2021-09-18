package cs;

import cs.parsers.BaselineParserWithBloomFilterCache;
import cs.parsers.BaselineParserWithBloomFilters;
import cs.parsers.bl.BaselineParserEncoded;
import cs.parsers.bl.WikiDataBaselineParserEncoded;
import cs.parsers.mg.MgSchemaExtractor;
import cs.parsers.mg.MgSchemaExtractorCache;
import cs.parsers.mg.MgSchemaExtractorCacheRbm;
import cs.parsers.mg.WikiDataMgSchemaExtractorCache;
import cs.utils.ConfigManager;
import cs.utils.Utils;


public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        benchmark();
    }
    
    private static void benchmark() {
        System.out.println("Benchmark Initiated");
        Utils.getCurrentTimeStamp();
        try {
            if (isOn("BlSchemaExtractor")) {
                System.out.println("BlSchemaExtractor - Encoded Strings");
                new BaselineParserEncoded(datasetPath, numberOfClasses).run();
            }
            
            if (isOn("WikiDataSchemaExtractor")) {
                System.out.println("WikiDataSchemaExtractor - Encoded Strings");
                new WikiDataBaselineParserEncoded(datasetPath, numberOfClasses).run();
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
                new MgSchemaExtractorCacheRbm(datasetPath, numberOfClasses).run();
            }
    
            if (isOn("WikiDataMgSchemaExtractor")) {
                System.out.println("WikiDataMgSchemaExtractor");
                new WikiDataMgSchemaExtractorCache(datasetPath, numberOfClasses).run();
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
