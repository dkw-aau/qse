package cs.utils;

import cs.Main;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class is used to configure the input params provided in the config file
 */
public class ConfigManager {
    
    public static String getProperty(String property) {
        java.util.Properties prop = new java.util.Properties();
        try {
            if (Main.configPath != null) {
                FileInputStream configFile = new FileInputStream(Main.configPath);
                prop.load(configFile);
                configFile.close();
            } else {
                System.out.println("Config Path is not specified in Main Arg");
            }
            return prop.getProperty(property);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
}