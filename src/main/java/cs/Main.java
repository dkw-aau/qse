package cs;

import com.google.common.hash.Funnels;
import cs.parsers.*;
import cs.parsers.mg.MgSchemaExtractor;
import cs.parsers.mg.MgSchemaExtractorCache;
import cs.utils.ConfigManager;
import cs.utils.Utils;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;

import java.nio.charset.StandardCharsets;


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
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean isOn(String option) {return Boolean.parseBoolean(ConfigManager.getProperty(option));}
    
 /*   private static void test() {
        for (int i = 0; i < Integer.parseInt(args[2]); i++) {
            BloomFilter<String> bf = new FilterBuilder(Integer.parseInt(args[0]), Double.parseDouble(args[1])).buildBloomFilter();
        }
    }*/
}
