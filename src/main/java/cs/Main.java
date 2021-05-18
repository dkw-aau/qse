package cs;

import com.github.rohansuri.art.AdaptiveRadixTree;
import com.github.rohansuri.art.BinaryComparables;
import cs.extras.ExtractTaxonomy;
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
        
        new BaselineParserWithBloomFilterCache(datasetPath, numberOfClasses).run();
        new BaselineParserWithBloomFilters(datasetPath, numberOfClasses).run();
        
        //new ExtractTaxonomy(datasetPath, numberOfClasses).run();
        
        //new BaselineParser(datasetPath, numberOfClasses).run();
        
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
        /*System.out.println("\n\nRunning: BaselineParserWithBloomFilterCache");
        BaselineParserWithBloomFilterCache baselineParserWithBloomFilterCache = new BaselineParserWithBloomFilterCache(datasetPath, numberOfClasses);
        baselineParserWithBloomFilterCache.run();
        
        System.out.println("\n\nRunning: BaselineParserWithBloomFilters");
        BaselineParserWithBloomFilters baselineParserWithBloomFilters = new BaselineParserWithBloomFilters(datasetPath, numberOfClasses);
        baselineParserWithBloomFilters.run();
        */
        
        
        //BaselineParserEncoded baselineParserEncoded = new BaselineParserEncoded(datasetPath, numberOfClasses);
        //baselineParserEncoded.run();
    }
}
