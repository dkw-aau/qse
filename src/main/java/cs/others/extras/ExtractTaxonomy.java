package cs.others.extras;

import cs.qse.endpoint.SHACLER;
import cs.utils.ConfigManager;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class ExtractTaxonomy {
    String rdfFile = "";
    SHACLER shacler = new SHACLER();
    
    // Constants
    final String RDFType = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
    int expectedNumberOfClasses = 10000;
    
    // Classes, instances, properties
    HashMap<String, Integer> classInstanceCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1)); //0.75 is the load factor
    HashMap<Node, HashMap<Node, HashSet<String>>> classToPropWithObjTypes = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    HashMap<Node, HashMap<Node, Integer>> classToPropWithCount = new HashMap<>((int) ((expectedNumberOfClasses) / 0.75 + 1));
    //HashMap<Node, List<Node>> instanceToClass = new HashMap<>((int) (1000000 / 0.75 + 1));
    HashMap<Node, List<Node>> instanceToClass = new HashMap<>();
    HashSet<Node> properties = new HashSet<>();
    
    String classesArray[] = {
            "<http://bioschemas.org/BioChemEntity>",
            "<http://bioschemas.org/Gene>",
            "<http://bioschemas.org/MolecularEntity>",
            "<http://bioschemas.org/Taxon>",
            "<http://schema.org/Brand>",
            "<http://schema.org/BroadcastChannel>",
            "<http://schema.org/CreativeWork>",
            "<http://schema.org/Article>",
            "<http://schema.org/Book>",
            "<http://schema.org/ComicStory>",
            "<http://schema.org/CreativeWorkSeason>",
            "<http://schema.org/TVSeason>",
            "<http://schema.org/CreativeWorkSeries>",
            "<http://schema.org/MovieSeries>",
            "<http://schema.org/RadioSeries>",
            "<http://schema.org/TVSeries>",
            "<http://schema.org/VideoGameSeries>",
            "<http://schema.org/Episode>",
            "<http://schema.org/TVEpisode>",
            "<http://schema.org/Movie>",
            "<http://schema.org/MusicComposition>",
            "<http://schema.org/PublicationIssue>",
            "<http://schema.org/PublicationVolume>",
            "<http://schema.org/SoftwareApplication>",
            "<http://schema.org/VideoGame>",
            "<http://schema.org/TVSeason>",
            "<http://schema.org/TVSeries>",
            "<http://schema.org/VisualArtwork>",
            "<http://schema.org/Event>",
            "<http://schema.org/SportsEvent>",
            "<http://schema.org/MedicalEntity>",
            "<http://schema.org/AnatomicalStructure>",
            "<http://schema.org/MedicalCondition>",
            "<http://schema.org/MedicalSignOrSymptom>",
            "<http://schema.org/Organization>",
            "<http://schema.org/Airline>",
            "<http://schema.org/SportsOrganization>",
            "<http://schema.org/Person>",
            "<http://schema.org/Place>",
            "<http://schema.org/Product>",
            "<http://schema.org/Vehicle>"};
    List<String> classesList = Arrays.asList(classesArray);
    
    // Constructor
    ExtractTaxonomy(String filePath) {
        this.rdfFile = filePath;
    }
    
    public ExtractTaxonomy(String filePath, int expSizeOfClasses) {
        this.rdfFile = filePath;
        this.expectedNumberOfClasses = expSizeOfClasses;
    }
    
    private void firstPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    .filter(line -> line.contains(RDFType))
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
                 /*           if( classesList.contains(nodes[2].toString())){
                                classInstanceCount.put(nodes[2].toString(), (classInstanceCount.getOrDefault(nodes[2].toString(), 0)) + 1);
    
                                if (instanceToClass.containsKey(nodes[0])) {
                                    instanceToClass.get(nodes[0]).add(nodes[2]);
                                } else {
                                    List<Node> list = new ArrayList<>();
                                    list.add(nodes[2]);
                                    instanceToClass.put(nodes[0], list);
                                }
                            }*/
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed firstPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void secondPass() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            FileWriter fileWriter = new FileWriter(ConfigManager.getProperty("output_file_path") + "dump.n3", false);
            Files.lines(Path.of(rdfFile))                           // - Stream of lines ~ Stream <String>
                    //.filter(line -> !line.contains(RDFType))        // - Exclude RDF type triples
                    .forEach(line -> {                              // - A terminal operation
                        try {
                            Node[] nodes = NxParser.parseNodes(line);
                            if (instanceToClass.containsKey(nodes[0])) {
                                try {
                                    System.out.println(line);
                                    fileWriter.write(line);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            properties.add(nodes[1]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        watch.stop();
        System.out.println("Time Elapsed secondPass: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private void populateShapes() {
        StopWatch watch = new StopWatch();
        watch.start();
        classToPropWithObjTypes.forEach((c, p) -> {
            shacler.setParams(c, p);
            shacler.constructShape();
        });
        watch.stop();
        System.out.println("Time Elapsed populateShapes: " + TimeUnit.MILLISECONDS.toSeconds(watch.getTime()));
    }
    
    private String getType(String value) {
        String theType = "<http://www.w3.org/2001/XMLSchema#string>"; //default type is XSD:string
        
        if (value.contains("\"^^")) {
            //value.split("\\^\\^")[0] is the literal value, and value.split("\\^\\^")[1]  the type of the value ;
            if (value.split("\\^\\^").length > 1) {
                theType = value.split("\\^\\^")[1];
            }
        } else if (value.contains("\"@")) {
            //value.split("\"@")[0] is the literal value and value.split("\"@")[1] is the language tag
            theType = RDF.LANGSTRING.toString();  //rdf:langString
        }
        return theType;
    }
    
    private void runParser() {
        firstPass();
        System.out.println(classInstanceCount);
        //secondPass();
        System.out.println("STATS: \n\t" + "No. of Classes: " + classInstanceCount.size() + "\n\t" + "No. of distinct Properties: " + properties.size());
        //populateShapes();
        //shacler.writeModelToFile();
    }
    
    
    public void run() {
        runParser();
    }
}