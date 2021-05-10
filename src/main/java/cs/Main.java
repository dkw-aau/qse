package cs;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import cs.extras.RDFVault;
import cs.extras.VariousFileReadingApproaches;
import cs.parsers.*;
import cs.utils.ConfigManager;

import java.util.NavigableMap;

public class Main {
    public static String configPath;
    public static String datasetPath;
    public static int numberOfClasses;
    
    public static void main(String[] args) throws Exception {
        configPath = args[0];
        datasetPath = ConfigManager.getProperty("dataset_path");
        numberOfClasses = Integer.parseInt(ConfigManager.getProperty("expected_number_classes"));
        
       //BaselineParser baselineParser = new BaselineParser(datasetPath, numberOfClasses);
       //baselineParser.run();
    
        //WikidataParser wikidataParser = new WikidataParser(datasetPath, numberOfClasses);
        //wikidataParser.run();
        
        //OnDiskMapParser onDiskMapParser = new OnDiskMapParser(datasetPath, numberOfClasses);
        //onDiskMapParser.run();
    
        //MemOptimalParser memOptimalParser = new MemOptimalParser(datasetPath, numberOfClasses);
        //memOptimalParser.run();
    
        //VariousFileReadingApproaches parser = new VariousFileReadingApproaches(datasetPath);
        //parser.singlePassNioStreamFileReader();
    
        //BaselineWithRdfVault baselineWithRdfVault = new BaselineWithRdfVault(datasetPath, numberOfClasses);
        //baselineWithRdfVault.run();
        
        //BaselineWithART baselineWithART = new BaselineWithART(datasetPath, numberOfClasses);
        //baselineWithART.run();
        
        //BaselineParserWithBloomFilters baselineParserWithBloomFilters = new BaselineParserWithBloomFilters(datasetPath, numberOfClasses);
        //baselineParserWithBloomFilters.run();
        
        //BaselineParserWithBloomFilterCache baselineParserWithBloomFilterCache = new BaselineParserWithBloomFilterCache(datasetPath, numberOfClasses);
        //baselineParserWithBloomFilterCache.run();
        
        
        BaselineParserEncoded baselineParserEncoded = new BaselineParserEncoded(datasetPath, numberOfClasses);
        baselineParserEncoded.run();
    }
}
