package cs.qse.common;

import cs.utils.FilesUtil;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * This class is to annotate SHACL shapes with some constraints that require querying shacl shapes, e.g., sh:node.
 * provided that QSE has already extracted shapes which are then being used as input by this class to process and annotate shapes constrains.
 * Currently, this class only annotates sh:node to shapes, but it can be extended to support more constraints.
 */
public class PostConstraintsAnnotator {
    public RepositoryConnection conn;
    
    //For testing of constraints annotation by reading SHACL shapes from a file
    public PostConstraintsAnnotator() {
        String fileAddress = "";
        this.conn = Utility.readFileAsRdf4JModel(fileAddress);
        addShNodeConstraint();
        new ShapesExtractor().writeModelToFile("dbpedia_sh_node_", this.conn);
    }
    
    // to be used by ShapesExtractor
    public PostConstraintsAnnotator(RepositoryConnection conn) {
        System.out.println("Invoked:: PostConstraintsAnnotator");
        this.conn = conn;
    }
    
    public void addShNodeConstraint() {
        getNodeShapesAndIterativelyProcessPropShapes();
    }
    
    private void getNodeShapesAndIterativelyProcessPropShapes() {
        TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclQuery("node_shapes"));
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet solution = result.next();
                //binding : nodeShape
                getPropShapesWithDirectShClassAttribute(solution.getValue("nodeShape").stringValue());
                getPropShapesWithEncapsulatedShClassAttribute(solution.getValue("nodeShape").stringValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Property Shapes having direct sh:class constraint
     */
    private void getPropShapesWithDirectShClassAttribute(String nodeShape) {
        TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclQuery("ps_of_ns_direct_sh_class").replace("NODE_SHAPE", nodeShape));
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet solution = result.next();
                insertShNodeConstraint(solution);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Property Shapes having sh:class constraint encapsulated in sh:or RDF list
     */
    private void getPropShapesWithEncapsulatedShClassAttribute(String nodeShape) {
        TupleQuery queryA = conn.prepareTupleQuery(FilesUtil.readShaclQuery("ps_of_ns_indirect_sh_class").replace("NODE_SHAPE", nodeShape));
        try (TupleQueryResult resultA = queryA.evaluate()) {
            while (resultA.hasNext()) {
                BindingSet bindingsA = resultA.next(); //bindings : ?propertyShape
                TupleQuery queryB = conn.prepareTupleQuery(FilesUtil.readShaclQuery("sh_class_indirect_ps")
                        .replace("PROPERTY_SHAPE", bindingsA.getValue("propertyShape").stringValue())
                        .replace("NODE_SHAPE", nodeShape));
                try (TupleQueryResult resultB = queryB.evaluate()) {
                    while (resultB.hasNext()) {
                        BindingSet bindingsB = resultB.next(); //bindings : ?propertyShape ?path ?class
                        insertShNodeConstraint(bindingsB);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     *
     */
    private void insertShNodeConstraint(BindingSet bindings) {
        String classVal = bindings.getValue("class").stringValue();
        if (classVal.contains("<"))
            classVal = classVal.substring(1, classVal.length() - 1);
        
        if (isNodeShape(classVal)) {
            // Annotate with sh:node
            String insertStatement = "INSERT { <" + bindings.getValue("propertyShape").stringValue() + "> <" + SHACL.NODE + "> <" + getNodeShape(classVal) + "> } " +
                    " WHERE { <" + bindings.getValue("propertyShape") + "> a <http://www.w3.org/ns/shacl#PropertyShape> . " + " }";
            //System.out.println(insertStatement);
            Update updateQuery = conn.prepareUpdate(insertStatement);
            updateQuery.execute();
        }
    }
    
    private boolean isNodeShape(String shaclClassValue) {
        String query = FilesUtil.readShaclQuery("ns_existence").replace("SHACL_CLASS", shaclClassValue);
        return conn.prepareBooleanQuery(query).evaluate();
    }
    
    
    private String getNodeShape(String shaclClassValue) {
        String nodeShapeIRI = "";
        TupleQuery query = conn.prepareTupleQuery(FilesUtil.readShaclQuery("ns").replace("SHACL_CLASS", shaclClassValue));
        
        try (TupleQueryResult resultB = query.evaluate()) {
            while (resultB.hasNext()) {
                BindingSet solution = resultB.next(); //bindings : ?nodeShape
                nodeShapeIRI = solution.getValue("nodeShape").stringValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodeShapeIRI;
    }
}
