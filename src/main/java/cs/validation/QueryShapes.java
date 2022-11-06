package cs.validation;

import cs.utils.ConfigManager;
import cs.utils.FilesUtil;
import cs.utils.graphdb.GraphDBUtils;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryShapes {
    private final GraphDBUtils graphDBUtils;
    
    public QueryShapes() {
        this.graphDBUtils = new GraphDBUtils();
        runQueries();
    }
    
    public void runQueries() {
        //read classes from file
        String support = "100";
        String confidence = "0.10";
        String fileAddress = ConfigManager.getProperty("config_dir_path") + "pruning/classes.txt";
        List<String> classes = FilesUtil.readAllLinesFromFile(fileAddress);
        AtomicInteger i = new AtomicInteger(1);
        classes.stream().parallel().forEach(classIri -> {
            System.out.println(i + ". " + classIri);
            String query = FilesUtil.readShaclQuery("query")
                    .replace("CLASS_IRI", classIri)
                    .replace("SUPPORT_VAL", support)
                    .replace("CONFIDENCE_VAL", confidence);
            List<BindingSet> result = graphDBUtils.runSelectQuery(query);
            if (result.size() > 1) {
                System.out.println("------------------------------> " + classIri + ": " + result.size());
            }
            i.getAndIncrement();
        });
//        classes.forEach(classIri -> {
//            System.out.println(i + ". " + classIri);
//            String query = FilesUtil.readShaclQuery("query")
//                    .replace("CLASS_IRI", classIri)
//                    .replace("SUPPORT_VAL", support)
//                    .replace("CONFIDENCE_VAL", confidence);
//            List<BindingSet> result = graphDBUtils.runSelectQuery(query);
//            if (result.size() > 1) {
//                System.out.println("------------------------------> " + classIri + ": " + result.size());
//            }
//            i.getAndIncrement();
//        });
    }
    
    
}
