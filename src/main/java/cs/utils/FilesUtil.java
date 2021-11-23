package cs.utils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.lang3.time.StopWatch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    
    public static void removeLinesFromFile(String fileAddress) {
        Set<Integer> set = new HashSet<>(Arrays.asList(49055920, 102321347, 139173933, 139173935, 227083600, 239236340, 253238795, 270566784, 375514677, 391800519, 479917244, 506049772, 512935894, 513422558, 580153897, 617594836, 632984001, 633018801, 661973189, 663507348, 717022541, 731157190, 735792513, 735906944, 745675025, 747865608, 754041234, 758082081, 761476635, 765720595, 775474763, 922339846, 968821511, 976562620, 977498125, 978793230, 993852366, 993989735, 997989429, 1017097140, 1065683869, 1094380798, 1096017490, 1097087428, 1115266547, 1115297310, 1145763802, 1219073744, 1230584279, 1356510123, 1437557453, 1449421627, 1465534436, 1490345910, 1597627142, 1674187287, 1685480354, 1693296493, 1693683198, 1698177019, 1705151282, 1724152104, 1725564921, 1819595969, 1838609688, 1915126583, 1929122671, 1929511564));
        //Set<Integer> set = new HashSet<>(Arrays.asList(1, 3, 10));
        StopWatch watch = new StopWatch();
        watch.start();
        AtomicInteger lineNo = new AtomicInteger(1);
        try {
            FileWriter fileWriter = new FileWriter(Constants.CLEAN_DATASET_FILE, true);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            
            FileWriter fileWriter2 = new FileWriter(Constants.DIRTY_DATASET_FILE, true);
            PrintWriter printWriter2 = new PrintWriter(fileWriter2);
            Files.lines(Path.of(fileAddress))
                    .forEach(line -> {
                        if (set.contains(lineNo.get())) {
                            printWriter2.println(line);
                        } else {
                            printWriter.println(line);
                        }
                        lineNo.getAndIncrement();
                    });
            printWriter.close();
            printWriter2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed removeLinesFromFile: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()) + " : " + TimeUnit.MILLISECONDS.toMinutes(watch.getTime()));
    }
}
