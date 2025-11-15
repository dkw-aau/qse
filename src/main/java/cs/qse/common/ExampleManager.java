package cs.qse.common;

import cs.Main;
import cs.qse.common.encoders.Encoder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.semanticweb.yars.nx.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ExampleManager {

    /** Maximum number of examples per class **/
    public static final int EXAMPLES_FOR_CLASS = 5;
    /** Maximum number of examples per property (of a specific class) **/
    public static final int EXAMPLES_FOR_PROPERTY = 5;
    /** Default min-value of a property cardinality **/
    public static final int DEFAULT_MIN = 0;
    /** Default max-value of a property cardinality **/
    public static final int DEFAULT_MAX = Integer.MAX_VALUE;
    /** Default encoded property type **/
    public static final int DEFAULT_ENCODED_PROP_TYPE = -1;
    /** Default label of a property **/
    public static final String DEFAULT_LABEL = " ";
    /** IRI used as predicate to denote examples in Shapes and PropertyShapes **/
    public static IRI EXAMPLE_IRI = Values.getValueFactory().createIRI("http://example.org/example");


    /** For each class we save a bunch of data **/
    Map<Integer,ClassExampleData> classToExData;

    /**
     * List of ALL example-nodes of ALL classes with their data.<br>
     * N.B. Each couple (key,value) stored in this object is the SAME one stored in his corresponding map in [classToExData].
     *      So, all objects stored in this map ARE ONLY REFERENCES to real objects stored in the maps [classToExData/exNodeToExData]
     */
    Map<Node,ExampleNodeData> exNodeToExData;

    /**
     * Set of ALL properties, independently of the class.<br>
     * N.B. All objects stored in this set ARE ONLY REFERENCES to real objects stored in the maps [classToExData/propToExData]
     */
    Set<Integer> encodedProperties;

    /**
     * Set of ALL values of ALL properties (even property-values of example-nodes).<br>
     * N.B. All objects stored in this set ARE ONLY REFERENCES to real objects stored in the sets [classToExData/propToExData/encodedValues]
     */
    Set<PropertyValue> encodedPropertiesValues;

    /** For each class, property, example-node and property-value we save his label (if there is one)**/
    Map<Integer, Integer> IRItoLabel;

    /**
     * Encoder used to encode/decode strings.<br>
     * N.B. This has to be the same encoder used in the parser!
     */
    Encoder stringEncoder;


    public ExampleManager(Encoder stringEncoder){
        classToExData = new HashMap<>();
        exNodeToExData = new HashMap<>();
        encodedProperties = new HashSet<>();
        encodedPropertiesValues = new HashSet<>();
        IRItoLabel = new HashMap<>();
        this.stringEncoder = stringEncoder;
        // Use the exampleIRI of the config-file if there is one
        if (Main.exampleIRI != null) {
            EXAMPLE_IRI = Values.getValueFactory().createIRI(Main.exampleIRI);
        }
    }



    /*=============================Metodi per la gestione dei nodi di esempio=============================*/
    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @return The number of example-nodes stored for the class {@code encodedClassIRI}.
     */
    public int exampleNodeCount(Integer encodedClassIRI){
        if (classToExData.get(encodedClassIRI) == null)
            return 0;

        return classToExData.get(encodedClassIRI).exNodeToExData.size();
    }

    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @return The total number of nodes (of class {@code encodedClassIRI}) seen so far
     */
    public int totalNodeCount(Integer encodedClassIRI){
        if (classToExData.get(encodedClassIRI) == null)
            return 0;

        return classToExData.get(encodedClassIRI).getTotalNodeCount();
    }

    /**
     * Add {@code exNode} to the example-nodes of the class {@code encodedClassIRI}.
     * @param encodedClassIRI Encoded IRI of the class.
     * @param exNode The example-node we want to add for {@code encodedClassIRI}
     */
    public void addExampleNode(Integer encodedClassIRI, Node exNode){
        // If no example-node has been added to the class yet, initialize the example-node list of the class
        if (!classToExData.containsKey(encodedClassIRI)){
            classToExData.put(encodedClassIRI, new ClassExampleData());
        }

        ExampleNodeData nodeData = new ExampleNodeData();
        // Adding the example-node to the class example-nodes
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        classData.addExampleNode(exNode, nodeData);
    }

    /**
     * IMPLEMENTS RESERVOIR SAMPLING ON EXAMPLE-NODES.<br>
     * If {@code candidateIndex} < {@code EXAMPLES_FOR_CLASS}, then replace node of index {@code candidateIndex} with the new example-node {@code exNode}
     * @param candidateIndex Index of the example-node that could be replaced
     * @param encodedClassIRI Encoded IRI of the class
     * @param exNode New exampleNode
     **/
    public void replaceExampleNode(int candidateIndex, Integer encodedClassIRI, Node exNode){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        Set<Node> classNodes = classData.exNodeToExData.keySet();

        // With a probability of EXAMPLES_FOR_CLASS/totalNodeCount, we add the new node removing an old one
        if (candidateIndex < EXAMPLES_FOR_CLASS){
            /* Adding the new node ONLY IF it's not already in the set.
             If we don't check this, we would delete the candidate-node without adding the new node. This would happen because
             if the new node is already in the set, it won't be added(since a set can't contain the same node twice).
             So we would have one less example-node!*/
            if (!classNodes.contains(exNode)) {
                // Removing the candidate node
                classData.removeExampleNode(candidateIndex);
                // Adding the new example-node
                addExampleNode(encodedClassIRI, exNode);
            }
        }
        // If the "dice roll" is not good, just increment the number of nodes seen so far
        else{
            classData.incrementTotalNodeCount();
        }
    }

    /**
     * Copy the references of ALL example-nodes of ALL classes into {@code exNodeToExData}
     */
    public void listExampleNodes(){
        classToExData.forEach((encodedClassIRI, classData) -> {
            /* N.B. In [exNodeToExData] we copy ONLY the references of the objects [classData.exNodeToExData] (no new objects are created).
             In this way, updating the data in one map automatically updates the data in the other.*/
            exNodeToExData.putAll(classData.exNodeToExData);
        });
    }

    /**
     * @param exNode Node to check for presence among the example-nodes.
     * @return {@code true} if {@code exNode} is an example-node of any class. <br>
     *         {@code false} otherwise.
     */
    public boolean isExampleNode(Node exNode){
        return exNodeToExData.containsKey(exNode);
    }

    /**
     * Add {@code encodedProperty} to {@code exNode} properties and store {@code encodedValue} as its example-value. <br>
     * If {@code encodedProperty} is already a {@code exNode} property, store {@code encodedValue} as another example-value
     * @param exNode Example node
     * @param encodedProperty Encoded property of {@code exNode}
     * @param encodedActualValue Encoded value of the property
     * @param encodedRawValue Encoded raw-value of the property
     */
    public void addNodeData(Node exNode, Integer encodedProperty, Integer encodedActualValue, Integer encodedRawValue){
        ExampleNodeData nodeData = exNodeToExData.get(exNode);
        nodeData.addPropertyValue(encodedProperty, encodedActualValue, encodedRawValue);
    }
    /*=====================================================================================================*/





    /*===============================Metodi per la gestione delle proprietà===============================*/
    /**
     * Store {@code encodedProperty} in the list of all properties({@code encodedProperties})
     * @param encodedProperty Encoded property to store
     */
    public void storeProperty(Integer encodedProperty){
        encodedProperties.add(encodedProperty);
    }

    /**
     * @param encodedProperty Encoded property to check for
     * @return {@code true} if {@code encodedProperty} is stored(it appears in {@code encodedProperties}). <br>
     *          {@code false} otherwise
     */
    public boolean isProperty(Integer encodedProperty){
        return encodedProperties.contains(encodedProperty);
    }

    /**
     * Copy the references of ALL example-values of ALL properties of ALL classes into {@code encodedPropertiesValues}
     */
    public void listPropertiesValues(){
        classToExData.forEach((encodedClassIRI, classData) -> {
            classData.propToExData.forEach((encodedProperty, propertyData) -> {
                /* N.B. Into [encodedPropertiesValues] we copy ONLY THE REFERENCES to the [encodedValues]. So no new objects are created */
                encodedPropertiesValues.addAll(propertyData.encodedValues);
            });
        });

    }

    /**
     * @param encodedActualValue Encoded value
     * @param encodedRawValue Encoded raw-value
     * @return {@code true} if the couple ({@code encodedActualValue},{@code encodedRawValue}) is stored(it appears in {@code encodedPropertiesValues}). <br>
     *          {@code false} otherwise.
     */
    public boolean isPropertyValue(Integer encodedActualValue, Integer encodedRawValue){
        PropertyValue totalValue = new PropertyValue(encodedActualValue, encodedRawValue);
        return encodedPropertiesValues.contains(totalValue);
    }

    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @return The current number of example-values stored for the property {@code encodedProperty} in class {@code encodedClassIRI}
     */
    public int propertyValuesCount(Integer encodedClassIRI, Integer encodedProperty){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        return classData.propertyValuesCount(encodedProperty);
    }

    /**
     * Add the couple ({@code encodedActualValue}, {@code encodedRawValue}) to the example-values of the property {@code encodedProperty} in class {@code encodedClassIRI}
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @param encodedActualValue Encoded value.
     * @param encodedRawValue Encoded raw-value.
     */
    public void addPropertyExample(Integer encodedClassIRI, Integer encodedProperty, Integer encodedActualValue, Integer encodedRawValue){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        classData.addPropertyExample(encodedProperty, encodedActualValue, encodedRawValue);
        // Keeping track of all properties
        storeProperty(encodedProperty);
    }

    /**
     * IMPLEMENTS RESERVOIR SAMPLING FOR EXAMPLE-VALUES OF A PROPERTY. <br>
     * If {@code candidateIndex} < {@code EXAMPLES_FOR_PROPERTY}, then replace value of index {@code candidateIndex} with the new example-value ({@code encodedActualValue}, {@code encodedRawValue}).
     * @param candidateIndex Index of the example-value that could be replaced.
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @param encodedActualValue Encoded value.
     * @param encodedRawValue Encoded raw-value.
     **/
    public void replacePropertyExample(int candidateIndex, Integer encodedClassIRI, Integer encodedProperty, Integer encodedActualValue, Integer encodedRawValue){
        ClassExampleData classData = classToExData.get(encodedClassIRI);

        // With a probability of EXAMPLES_FOR_PROPERTY/totalValueCount, we add the new value removing an old one
        if (candidateIndex < EXAMPLES_FOR_PROPERTY){
            /* Adding the new value ONLY IF it's not already in the set.
             If we don't check this, we would delete the candidate-value without adding the new one. This would happen because
             if the new value is already in the set, it won't be added(since a set can't contain the same value twice).*/
            if (!classData.propertyContainsValue(encodedProperty, encodedActualValue, encodedRawValue)){
                // Removing the candidate value
                classData.removePropertyValue(candidateIndex, encodedProperty);
                // Adding the new example-value
                addPropertyExample(encodedClassIRI, encodedProperty, encodedActualValue, encodedRawValue);
            }
        }
        // If the "dice roll" is not good, just increment the number of value seen so far for the property
        else{
            classData.incrementPropertyTotaleValueCount(encodedProperty);
        }
    }

    /**
     * Set the minCount of {@code encodedProperty} in class {@code encodedClassIRI} to the given value.
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @param minCount Given value.
     */
    public void setPropertyMinCount(Integer encodedClassIRI, Integer encodedProperty, int minCount){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        classData.setPropertyMinCount(encodedProperty, minCount);
    }

    /**
     * Set the maxCount of {@code encodedProperty} in class {@code encodedClassIRI} to the given value.
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @param maxCount Given value.
     */
    public void setPropertyMaxCount(Integer encodedClassIRI, Integer encodedProperty, int maxCount){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        classData.setPropertyMaxCount(encodedProperty, maxCount);
    }

    /**
     * Set the propertyType of {@code encodedProperty} in class {@code encodedClassIRI} to the given value.
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @param encodedPropertyType Encoded given value.
     */
    public void setPropertyType(Integer encodedClassIRI, Integer encodedProperty, Integer encodedPropertyType){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        classData.setPropertyType(encodedProperty, encodedPropertyType);
    }

    /**
     * Set the isLiteralType of {@code encodedProperty} in class {@code encodedClassIRI} to the given value.
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @param isLiteral Given value.
     */
    public void setPropertyIsLiteral(Integer encodedClassIRI, Integer encodedProperty, boolean isLiteral){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        classData.setPropertyIsLiteral(encodedProperty, isLiteral);
    }

    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @return {@code true} if property {@code encodedProperty} in class {@code encodedClassIRI} is a literal-type property.<br>
     *            {@code false} otherwise.
     */
    public boolean isPropertyLiteralType(Integer encodedClassIRI, Integer encodedProperty){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        return classData.isPropertyLiteralType(encodedProperty);
    }

    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property.
     * @return The total number of values seen so far for the property {@code encodedProperty} in class {@code encodedClassIRI}.
     */
    public int totalPropertyValueCount(Integer encodedClassIRI, Integer encodedProperty){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        return classData.getPropertyTotalValueCount(encodedProperty);
    }
    /*================================================================================================*/



    /*===============================Metodi per la gestione delle label===============================*/

    /**
     * Associate the label {@code encodedLabel} to the subject {@code encodedSubject}.
     * @param encodedSubject Encoded subject of the label.
     * @param encodedLabel Encoded label.
     */
    public void storeLabel(Integer encodedSubject, Integer encodedLabel){
        IRItoLabel.put(encodedSubject, encodedLabel);
    }

    /**
     * @param encodedSubject Encoded subject to analyze.
     * @return {@code true} if {@code encodedSubject} represents a class.
     */
    public boolean isClass(Integer encodedSubject){
        return classToExData.containsKey(encodedSubject);
    }
    /*================================================================================================*/




    /*================================Metodi per la creazione degli esempi================================*/
    /**
     * @param encodedClassIRI Encoded IRI of the class to build the example of.
     * @return Set of example strings for class {@code encodedClassIRI}.
     */
    public Set<String> buildExampleForClass(Integer encodedClassIRI){
        // Apply no filter to properties
        Set<Integer> allClassProperties = classToExData.get(encodedClassIRI).propToExData.keySet();
        return buildExampleForClassWithFilter(encodedClassIRI, allClassProperties);
    }

    /**
     * @param encodedClassIRI Encoded IRI of the class to build the example of.
     * @param filteredProperties Set of (encoded)properties that could be used to build example strings.
     * @return Set of example strings for class {@code encodedClassIRI}. Strings will contain only properties in {@code filteredProperties}
     */
    public Set<String> buildExampleForClassWithFilter(Integer encodedClassIRI, Set<Integer> filteredProperties){
        // Final set of example strings for the given class
        Set<String> completeExample = new HashSet<>();

        // Iterate over all the class example-nodes and, for each one, build an example string
        classToExData.get(encodedClassIRI).exNodeToExData.forEach((exNode, nodeData)->{
            String nodeName = exNode.getLabel();
            String className = stringEncoder.decode(encodedClassIRI);
            Integer encodedNodeIRI = stringEncoder.encode(nodeName);
            // Adding labels of example-node and class (if there are associated labels)
            if (IRItoLabel.containsKey(encodedNodeIRI))
                nodeName += "("+stringEncoder.decode(IRItoLabel.get(encodedNodeIRI))+")";
            else
                nodeName += "("+DEFAULT_LABEL+")";
            if (IRItoLabel.containsKey(encodedClassIRI))
                className += "("+stringEncoder.decode(IRItoLabel.get(encodedClassIRI))+")";
            else
                className += "("+DEFAULT_LABEL+")";


            StringBuilder exampleString = new StringBuilder();
            exampleString.append(nodeName).append(" is a ").append(className);

            // Iterate over all the example-node data (properties and property-values)
            // For each property add the example of that property
            nodeData.propToExData.forEach((encodedProperty, propertyData) -> {
                // Adding only properties that belong to the filtered property set
                if (filteredProperties.contains(encodedProperty)) {
                    exampleString.append("; has ");

                    // Adding min and max constraints (only if different from min:0, max:INF)
                    Integer minCount = propertyData.minCount;
                    Integer maxCount = propertyData.maxCount;
                    if (!minCount.equals(DEFAULT_MIN))
                        exampleString.append("at least ").append(minCount).append(" ");
                    if (!maxCount.equals(DEFAULT_MAX))
                        exampleString.append("at most ").append(maxCount).append(" ");

                    // Adding property name
                    exampleString.append( stringEncoder.decode(encodedProperty) );
                    // Adding property label
                    if (IRItoLabel.containsKey(encodedProperty)) {
                        exampleString.append("(")
                                .append( stringEncoder.decode(IRItoLabel.get(encodedProperty)) )
                                .append(")");
                    }
                    else {
                        exampleString.append("(").append(DEFAULT_LABEL).append(")");
                    }
                }
            });

            // Adding current example string to the complete set
            completeExample.add( exampleString.toString() );
        });

        return completeExample;
    }


    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property to build the example of.
     * @return Set of example-values for property {@code encodedProperty} of class {@code encodedClassIRI}. Values are given as Literals.
     */
    public Set<Literal> buildExamplesForLiteralTypeProperty(Integer encodedClassIRI, Integer encodedProperty){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        PropertyExampleData propertyData = classData.propToExData.get(encodedProperty);
        Set<PropertyValue> encodedValues = null;
        Integer propertyType = null;

        // Check if there property exists in the class
        if (propertyData != null){
           encodedValues = propertyData.encodedValues;
           propertyType = propertyData.getPropertyType();
        }

        Set<Literal> examples = new HashSet<>();

        // Check if there is any example-value for the property
        if (encodedValues != null) {
            for (PropertyValue enVal : encodedValues) {
                String rawValue = stringEncoder.decode(enVal.rawValue);
                Literal literal;

                /* Trying to create the literal for the example-value
                 N.B. It's important to use the raw-value because it contains the value type (i.e. "30"^^<http://www.w3.org/2001/XMLSchema#int>)
                      Otherwise the parser wouldn't know which type use to create the Literal*/
                try {
                    literal = NTriplesUtil.parseLiteral(rawValue, Values.getValueFactory());
                }
                // If the literal creation fails, just create a Literal of type string
                catch (IllegalArgumentException e) {
                    System.err.println("Formato N-Triples non valido per il valore: " + rawValue);
                    literal = Values.literal(rawValue);
                }

                examples.add(literal);
            }
        }



        return examples;
    }


    /**
     * @param encodedClassIRI Encoded IRI of the class.
     * @param encodedProperty Encoded property to build the example of.
     * @return  Set of example-values for property {@code encodedProperty} of class {@code encodedClassIRI}.
     *          Values are given as Strings and also contains labels(if there is any).
     */
    public Set<String> buildExamplesForNonLiteralTypeProperty(Integer encodedClassIRI, Integer encodedProperty){
        ClassExampleData classData = classToExData.get(encodedClassIRI);
        PropertyExampleData propertyData = classData.propToExData.get(encodedProperty);
        Set<PropertyValue> encodedValues = null;

        // Check if there property exists in the class
        if (propertyData != null){
            encodedValues = propertyData.encodedValues;
        }

        Set<String> examples = new HashSet<>();

        // Check if there is any example-value for the property
        if (encodedValues != null) {
            for (PropertyValue enVal : encodedValues) {
                // Adding example-value
                StringBuilder ex = new StringBuilder(stringEncoder.decode(enVal.rawValue));
                // Adding label
                String label;
                if (IRItoLabel.containsKey(enVal.actualValue)) {
                    label = "(" + stringEncoder.decode(IRItoLabel.get(enVal.actualValue)) + ")";
                } else {
                    label = "(" + DEFAULT_LABEL + ")";
                }
                ex.append(label);

                examples.add(ex.toString());
            }
        }

        return examples;
    }
    /*====================================================================================================*/









    /*===================================Classi di supporto====================================*/
    /**
     * Store all the data of a specific class.
     */
    private class ClassExampleData{
        // For each example-node (of this class) we store a bunch of data
        Map<Node,ExampleNodeData> exNodeToExData;
        // For each property (of this class) we store a bunch of data
        Map<Integer,PropertyExampleData> propToExData;
        // Total number of node (of this class) seen so far. Useful for reservoir sampling
        int totalNodeCount;

        protected ClassExampleData(){
            exNodeToExData = new HashMap<>(EXAMPLES_FOR_CLASS);
            propToExData = new HashMap<>();
            totalNodeCount = 0;
        }

        /**
         * Add {@code exNode} to the example-nodes and associate to it data {@code nodeData}
         * @param exNode Example-node
         * @param nodeData Data of {@code exNode}
         */
        protected void addExampleNode(Node exNode, ExampleNodeData nodeData) {
            exNodeToExData.put(exNode, nodeData);
            // Keeping track of total number of nodes seen so far
            incrementTotalNodeCount();
        }

        /**
         * (useful for reservoir sampling)<br>
         * Delete node of index {@code candidateIndex} from example-nodes set
         * @param candidateIndex Index of the example-node to delete
         */
        public void removeExampleNode(int candidateIndex) {
            Node candidateNode = (Node) exNodeToExData.keySet().toArray()[candidateIndex];
            exNodeToExData.remove(candidateNode);
        }

        /**
         * Add the example-value ({@code encodedActualValue}, {@code encodedRawValue}) to the property {@code encodedProperty} example-values
         * @param encodedProperty Encoded property
         * @param encodedActualValue Encoded non-raw value
         * @param encodedRawValue Encoded raw value
         */
        protected void addPropertyExample(Integer encodedProperty, Integer encodedActualValue, Integer encodedRawValue){
            // If it's the first time we see this property in this class, initialize property data
            if (propToExData.get(encodedProperty) == null){
                propToExData.put(encodedProperty, new PropertyExampleData());
            }

            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.addValue(encodedActualValue, encodedRawValue);
        }

        /**
         * (useful for reservoir sampling)<br>
         * Delete value of index {@code candidateIndex} from {@code encodedProperty} example-values set.
         *
         * @param candidateIndex  Index of the example-value to delete
         * @param encodedProperty Encoded property
         */
        public void removePropertyValue(int candidateIndex, Integer encodedProperty) {
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.removeValue(candidateIndex);
        }

        /**
         * @param encodedProperty Encoded property
         * @return The total number of values seen so far for the property {@code encodedProperty} in this class
         */
        protected int propertyValuesCount(Integer encodedProperty){
            PropertyExampleData propertyData = propToExData.get(encodedProperty);

            if (propertyData == null)
                return 0;

            return propertyData.valuesCount();
        }

        /**
         * @param encodedProperty Encoded property
         * @param encodedActualValue Encoded non-raw value
         * @param encodedRawValue Encoded raw value
         * @return {@code true} if the value ({@code encodedActualValue}, {@code encodedRawValue}) is stored as example-value for property {@code encodedProperty}<br>
         *          {@code false} otherwise
         */
        protected boolean propertyContainsValue(Integer encodedProperty, Integer encodedActualValue, Integer encodedRawValue){
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            return propertyData.containsValue(encodedActualValue, encodedRawValue);
        }

        /**
         * Set the property {@code encodedProperty} min count to the given value
         * @param encodedProperty Encoded property
         * @param minCount Given value
         */
        protected void setPropertyMinCount(Integer encodedProperty, int minCount){
            // Set the property minCount in the class
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setMinCount(minCount);
            // Set the property minCount in all example-nodes of the class
            exNodeToExData.forEach((node, nodeData) ->{
                // An example-node may not have all properties
                if (nodeData.hasProperty(encodedProperty))
                    nodeData.setPropertyMinCount(encodedProperty, minCount);
            });
        }

        /**
         * Set the property {@code encodedProperty} max count to the given value
         * @param encodedProperty Encoded property
         * @param maxCount Given value
         */
        protected void setPropertyMaxCount(Integer encodedProperty, int maxCount){
            // Set the property maxCount in the class
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setMaxCount(maxCount);
            // Set the property maxCount in all example-nodes of the class
            exNodeToExData.forEach((node, nodeData) ->{
                // An example-node may not have all properties
                if (nodeData.hasProperty(encodedProperty))
                    nodeData.setPropertyMaxCount(encodedProperty, maxCount);
            });
        }

        /**
         * Set the property type of property {@code encodedProperty} to the given type
         * @param encodedProperty Encoded property
         * @param encodedPropertyType Given property type
         */
        protected void setPropertyType(Integer encodedProperty, Integer encodedPropertyType) {
            if (propToExData.get(encodedProperty) == null)
                propToExData.put(encodedProperty, new PropertyExampleData());

            // Set the propertyType in the class
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setPropertyType(encodedPropertyType);
            // Set the property type in all example-nodes of the class
            exNodeToExData.forEach((node, nodeData) ->{
                // An example-node may not have all properties
                if (nodeData.hasProperty(encodedProperty))
                    nodeData.setPropertyType(encodedProperty, encodedPropertyType);
            });
        }

        /**
         * Set the isLiteral value of property {@code encodedProperty} to the given value
         * @param encodedProperty Encoded property
         * @param isLiteral Given value
         */
        protected void setPropertyIsLiteral(Integer encodedProperty, boolean isLiteral) {
            // Set the isLiteralType of property in the class
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setIsLiteral(isLiteral);
            // Set the isLiteralType in all example-nodes of the class
            exNodeToExData.forEach((node, nodeData) ->{
                // An example-node may not have all properties
                if (nodeData.hasProperty(encodedProperty))
                    nodeData.setPropertyIsLiteral(encodedProperty, isLiteral);
            });
        }

        /**
         * @param encodedProperty Encoded property
         * @return {@code true} if {@code encodedProperty} is a literal type.<br>
         *          {@code false} otherwise
         */
        protected boolean isPropertyLiteralType(Integer encodedProperty) {
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            return propertyData.isLiteralType();
        }

        /**
         * @return Total number of nodes(of this class) seen so far
         */
        protected int getTotalNodeCount(){
            return totalNodeCount;
        }

        /**
         * Increment the total number of nodes(of this class) seen so far
         */
        protected void incrementTotalNodeCount(){
            totalNodeCount++;
        }

        /**
         * @param encodedProperty Encoded property
         * @return Total number of nodes(of this class) seen so far
         */
        protected int getPropertyTotalValueCount(Integer encodedProperty){
            return propToExData.get(encodedProperty).getTotalValueCount();
        }

        /**
         * Increment the total number of values seen so far for property {@code encodedProperty}
         * @param encodedProperty Encoded property
         */
        protected void incrementPropertyTotaleValueCount(Integer encodedProperty){
            propToExData.get(encodedProperty).incrementTotalValueCount();
        }
    }


    /**
     * Store all data of a specific example-node
     */
    private class ExampleNodeData{
        // For each property of the example-node we save a bunch of data
        Map<Integer,PropertyExampleData> propToExData;

        protected ExampleNodeData(){
            propToExData = new HashMap<>();
        }

        /**
         * Add value ({@code encodedActualValue}, {@code encodedRawValue}) to property {@code encodedProperty} example-values
         * @param encodedProperty Encoded property
         * @param encodedActualValue Encoded non-raw value to add
         * @param encodedRawValue Encoded raw value to add
         */
        protected void addPropertyValue(Integer encodedProperty, Integer encodedActualValue, Integer encodedRawValue){
            // If it's the first time we see the property for this node, add the property to the node
            if (!propToExData.containsKey(encodedProperty)){
                propToExData.put(encodedProperty, new PropertyExampleData());
            }

            PropertyExampleData propData = propToExData.get(encodedProperty);
            propData.addValue(encodedActualValue, encodedRawValue);
        }

        /**
         * @param encodedProperty Encoded property
         * @return {@code true} if this node has the property {@code encodedProperty}
         */
        protected boolean hasProperty(Integer encodedProperty){
            return propToExData.containsKey(encodedProperty);
        }

        /**
         * Set the property {@code encodedProperty} min count to the given value
         * @param encodedProperty Encoded property
         * @param minCount Given value
         */
        protected void setPropertyMinCount(Integer encodedProperty, int minCount){
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setMinCount(minCount);
        }

        /**
         * Set the property {@code encodedProperty} max count to the given value
         * @param encodedProperty Encoded property
         * @param maxCount Given value
         */
        protected void setPropertyMaxCount(Integer encodedProperty, int maxCount){
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setMaxCount(maxCount);
        }

        /**
         * Set the property type of property {@code encodedProperty} to the given type
         * @param encodedProperty Encoded property
         * @param encodedPropertyType Given type
         */
        public void setPropertyType(Integer encodedProperty, Integer encodedPropertyType) {
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setPropertyType(encodedPropertyType);
        }

        /**
         * Set the isLiteral value of property {@code encodedProperty} to the given value
         * @param encodedProperty Encoded property
         * @param isLiteral Given value
         */
        public void setPropertyIsLiteral(Integer encodedProperty, boolean isLiteral) {
            PropertyExampleData propertyData = propToExData.get(encodedProperty);
            propertyData.setIsLiteral(isLiteral);
        }
    }


    /**
     * Store all data of a property OF A SPECIFIC CLASS. So, all data stored in a object of this type refers to a specific couple (class, property)
     */
    private class PropertyExampleData{
        // Set of example-values(encoded) of the property
        Set<PropertyValue> encodedValues;
        // Min and Max cardinal constraints
        int maxCount;
        int minCount;
        // Property type(encoded)
        Integer encodedPropertyType;
        // TRUE: the property contains literal values. FALSE: the property contains IRIs
        boolean isLiteralType;
        // Total number of values seen so far (useful for reservoir sampling)
        int totalValueCount;

        protected PropertyExampleData(){
            encodedValues = new HashSet<>(EXAMPLES_FOR_PROPERTY);
            maxCount = DEFAULT_MAX;
            minCount = DEFAULT_MIN;
            encodedPropertyType = DEFAULT_ENCODED_PROP_TYPE;
            isLiteralType = true;
            totalValueCount = 0;
        }

        /**
         * Add the value ({@code encodedActualValue}, {@code encodedRawValue}) to the example-values
         * @param encodedActualValue Encoded non-raw value
         * @param encodedRawValue Encoded raw value
         */
        protected void addValue(Integer encodedActualValue, Integer encodedRawValue){
            PropertyValue completeValue = new PropertyValue(encodedActualValue, encodedRawValue);
            encodedValues.add(completeValue);
            // Keeping track of total number of values seen so far
            incrementTotalValueCount();
        }

        /**
         * (useful for reservoir sampling)
         * Delete the value of index {@code candidateIndex} from the example-values set
         *
         * @param candidateIndex Index of the example-value to delete
         */
        public void removeValue(int candidateIndex) {
            PropertyValue candidateEncodedValue = (PropertyValue) encodedValues.toArray()[candidateIndex];
            encodedValues.remove(candidateEncodedValue);
        }

        /**
         * @return The number of example-values stored for this property
         */
        protected int valuesCount(){
            return encodedValues.size();
        }

        /**
         * @param encodedActualValue Encoded non-raw value
         * @param encodedRawValue Encoded raw value<
         * @return {@code true} if value ({@code encodedActualValue}, {@code encodedRawValue}) is in example-values set.<br>
         *          {@code false} otherwise
         */
        protected boolean containsValue(Integer encodedActualValue, Integer encodedRawValue){
            PropertyValue targetValue = new PropertyValue(encodedActualValue,encodedRawValue);
            return encodedValues.contains(targetValue);
        }

        /**
         * Set the property min count to the given value
         * @param minCount Given value
         */
        protected void setMinCount(int minCount){
            this.minCount=minCount;
        }

        /**
         * Set the property max count to the given value
         * @param maxCount Given value
         */
        protected void setMaxCount(int maxCount){
            this.maxCount=maxCount;
        }

        /**
         * Set the property type to the given type
         * @param encodedPropertyType Given type
         */
        protected void setPropertyType(Integer encodedPropertyType){
            this.encodedPropertyType = encodedPropertyType;
        }

        /**
         * @return The (encoded)property type of this property
         */
        protected Integer getPropertyType(){
            return encodedPropertyType;
        }

        /**
         * Set the property isLiteral value to the given value
         * @param isLiteral Given value
         */
        protected void setIsLiteral(boolean isLiteral) {
            this.isLiteralType = isLiteral;
        }

        /**
         * @return {@code true} if property is literal type.<br>
         *          {@code false} otherwise
         */
        protected boolean isLiteralType() {
            return isLiteralType;
        }

        /**
         * Increment the total number of values seen so far for this property
         */
        protected void incrementTotalValueCount(){
            totalValueCount++;
        }

        /**
         * @return The total number of values seen so far for this property
         */
        protected int getTotalValueCount(){
            return totalValueCount;
        }
    }

    /**
     * Contains one value of a property. The value is stored both in raw and non-raw form. <br>
     * Store raw-values is useful for creating literals of the correct type. <br>
     * EXAMPLES: <br>
     * Valore                                               ||   actualValue                                         ||   rawValue
     * <http://www.Department0.University0.edu/Course20>    ||   'http://www.Department0.University0.edu/Course20'   ||   '<http://www.Department0.University0.edu/Course20>'
     * "30"^^<http://www.w3.org/2001/XMLSchema#int>         ||   '30'                                                ||   '"30"^^<http://www.w3.org/2001/XMLSchema#int>'
     */
    private class PropertyValue{
        /** Non-raw property value **/
        Integer actualValue;
        /** Raw property value**/
        Integer rawValue;

        PropertyValue(Integer actualValue, Integer rawValue){
            this.actualValue = actualValue;
            this.rawValue = rawValue;
        }


        @Override
        // Important to store object of this type in HashSets
        public boolean equals(Object obj){
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            PropertyValue that = (PropertyValue) obj;
            return actualValue.equals(that.actualValue) && rawValue.equals(that.rawValue);
        }

        @Override
        // Important to store object of this type in HashSets
        public int hashCode() {
            return actualValue.hashCode() ^ rawValue.hashCode();
        }

    }
    /*=====================================================================================================*/
}
