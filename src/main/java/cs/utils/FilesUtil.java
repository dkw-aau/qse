package cs.utils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains various methods used as a utility in the project to interact with files
 */
public class FilesUtil {
    public static void writeToFile(String str, String fileNameAndPath) {
        try {
            FileWriter fileWriter = new FileWriter(new File(fileNameAndPath));
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(str);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void writeToFileInAppendMode(String str, String fileNameAndPath) {
        try {
            FileWriter fileWriter = new FileWriter(new File(fileNameAndPath), true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println(str);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static List<String[]> readCsvAllDataOnceWithPipeSeparator(String fileAddress) {
        List<String[]> allData = null;
        try {
            FileReader filereader = new FileReader(fileAddress);
            // create csvParser object with
            // custom separator pipe
            CSVParser parser = new CSVParserBuilder().withSeparator('|').build();
            
            // create csvReader object with
            // parameter file reader and parser
            CSVReader csvReader = new CSVReaderBuilder(filereader).withCSVParser(parser).build();
            
            // Read all data at once
            allData = csvReader.readAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allData;
    }
    
    public static List<String[]> readCsvAllDataOnceWithCustomSeparator(String fileAddress, char separator) {
        List<String[]> allData = null;
        try {
            FileReader filereader = new FileReader(fileAddress);
            // create csvParser object with
            // custom separator pipe
            CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
            
            // create csvReader object with
            // parameter file reader and parser
            CSVReader csvReader = new CSVReaderBuilder(filereader).withCSVParser(parser).build();
            
            // Read all data at once
            allData = csvReader.readAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allData;
    }
    
    public static boolean deleteFile(String fileAddress) {
        File file = new File(fileAddress);
        return file.delete();
    }
    
    public static String readQuery(String query) {
        String q = null;
        try {
            String queriesDirectory = ConfigManager.getProperty("resources_path") + "/queries/";
            q = new String(Files.readAllBytes(Paths.get(queriesDirectory + query + ".txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q;
    }
    
    public static String readShaclQuery(String query) {
        String q = null;
        try {
            String queriesDirectory = ConfigManager.getProperty("resources_path") + "/shacl_queries/";
            q = new String(Files.readAllBytes(Paths.get(queriesDirectory + query + ".txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q;
    }
    
    public static String readShaclStatsQuery(String query, String type) {
        String q = null;
        try {
            String queriesDirectory = ConfigManager.getProperty("resources_path") + "/shacl_stats_queries/" + type + "/";
            q = new String(Files.readAllBytes(Paths.get(queriesDirectory + query + ".txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return q;
    }
    
    public static List<String> readAllLinesFromFile(String fileAddress) {
        List<String> allLines = new ArrayList<>();
        try {
            allLines = Files.readAllLines(Paths.get(fileAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allLines;
    }
    
}
