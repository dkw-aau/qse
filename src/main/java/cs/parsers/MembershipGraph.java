package cs.parsers;

import cs.utils.Constants;
import cs.utils.FilesUtil;
import cs.utils.NodeEncoder;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MembershipGraph {
    DefaultDirectedGraph<Integer, DefaultEdge> membershipGraph;
    NodeEncoder encoder;
    Map<Integer, List<List<Integer>>> membershipSets;
    Integer membershipGraphRootNode;
    
    public MembershipGraph(NodeEncoder e) {
        this.membershipGraph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        this.encoder = e;
    }
    
    public MembershipGraph(boolean devMode) {
        this.importGraphRelatedData();
        HashMap<Integer, BloomFilter<String>> ctiBf = new HashMap<>();
        this.membershipGraph.vertexSet().forEach(v -> {
            ctiBf.put(v, new FilterBuilder(100, 0.01).buildBloomFilter());
        });
        this.membershipGraphOutlierNormalization(this.membershipGraph.vertexSet().size(), ctiBf);
    }
    
    public void createMembershipSets(HashMap<Node, List<Integer>> instanceToClass) {
        this.membershipSets = instanceToClass.values().stream().distinct()
                .collect(Collectors.groupingBy(List::size)).entrySet().stream().sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
    }
    
    public void createMembershipGraph(HashMap<String, Integer> classInstanceCount) {
        AtomicBoolean flag = new AtomicBoolean(false);
        ArrayList<Integer> rootNodesOfSubGraphs = new ArrayList<>();
        try {
            this.membershipSets.forEach((mSize, mSets) -> {
                //System.out.println(mSize + " - " + mSets.size());
                if (mSize == 1) {
                    mSets.forEach(set -> {
                        set.forEach(membershipGraph::addVertex);
                    });
                } else if (mSize > 1) {
                    mSets.forEach(set -> {
                        HashMap<Integer, Integer> memberFrequency = new HashMap<>();
                        set.forEach(element -> {
                            memberFrequency.put(element, classInstanceCount.get(encoder.decode(element).getLabel()));
                            if (!membershipGraph.containsVertex(element)) membershipGraph.addVertex(element);
                        });
                        Integer[] sortedElementsOfMemberSet = getKeysOfMapSortedByValues(memberFrequency).keySet().toArray(new Integer[0]);
                        //System.out.println(Arrays.toString(sortedGroupMembers));
                        
                        for (int i = 1; i < sortedElementsOfMemberSet.length; i++) {
                            if (memberFrequency.get(sortedElementsOfMemberSet[i - 1]).equals(memberFrequency.get(sortedElementsOfMemberSet[i]))) {
                                //System.out.println("SAME " + sortedElementsOfMemberSet[i - 1] + " -- " + sortedElementsOfMemberSet[i]);
                                membershipGraph.addEdge(sortedElementsOfMemberSet[i - 1], sortedElementsOfMemberSet[i]);
                                membershipGraph.addEdge(sortedElementsOfMemberSet[i], sortedElementsOfMemberSet[i - 1]);
                            } else {
                                membershipGraph.addEdge(sortedElementsOfMemberSet[i - 1], sortedElementsOfMemberSet[i]);
                            }
                        }
                    });
                }
            });
            
            
            ConnectivityInspector<Integer, DefaultEdge> connectivityInspector = new ConnectivityInspector<>(membershipGraph);
            connectivityInspector.connectedSets().stream().sorted(Comparator.comparingInt(Set::size)).forEach(subGraphVertices -> {
                if (subGraphVertices.size() > 1) {
                    subGraphVertices.forEach(vertex -> {
                        if (membershipGraph.inDegreeOf(vertex) == 0) {
                            rootNodesOfSubGraphs.add(vertex);
                            flag.set(true);
                        }
                    });
                    
                    //Handle the graph having no node with inDegree 0
                    if (!flag.get()) {
                        for (Integer v : subGraphVertices) {
                            if (membershipGraph.inDegreeOf(v) == 1) {
                                rootNodesOfSubGraphs.add(v);
                                break;
                            }
                        }
                    }
                } else if (subGraphVertices.size() == 1) {
                    rootNodesOfSubGraphs.addAll(subGraphVertices);
                }
            });
            setMembershipGraphRootNode(encoder.encode(NxParser.parseNodes(Constants.MEMBERSHIP_GRAPH_ROOT_NODE)[2]));
            //Add a main root to connect all the sub-graphs
            membershipGraph.addVertex(membershipGraphRootNode);
            for (Integer node : rootNodesOfSubGraphs) {
                membershipGraph.addEdge(membershipGraphRootNode, node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void membershipGraphOutlierNormalization(Integer enoc, HashMap<Integer, BloomFilter<String>> ctiBf) {
        int threshold = 50;
        int focusNodeCount = 0;
        int node = this.membershipGraphRootNode;
        int focusNode;
        
        HashSet<Integer> visited = new HashSet<>(enoc);
        LinkedList<Integer> queue = new LinkedList<Integer>();
        queue.add(node);
        visited.add(node);
        
        while (queue.size() != 0) {
            node = queue.poll();
            for (DefaultEdge edge : membershipGraph.outgoingEdgesOf(node)) {
                Integer child = membershipGraph.getEdgeTarget(edge);
                if (!visited.contains(child)) {
                    if (membershipGraph.outDegreeOf(child) > threshold) {
                        focusNode = child;
                        focusNodeCount = membershipGraph.outDegreeOf(child);
                        break;
                    }
                    queue.add(child);
                    visited.add(child);
                }
            }
        }
    }
    
    public void exportGraphRelatedData() {
        FilesUtil.deleteFile(Constants.MG_VERTICES_FILE);
        FilesUtil.deleteFile(Constants.MG_VERTICES_FILE);
        FilesUtil.deleteFile(Constants.MG_ENCODED_TABLE_FILE);
        FilesUtil.deleteFile(Constants.MG_ENCODED_R_TABLE_FILE);
        
        this.membershipGraph.vertexSet().forEach(vertex -> {
            FilesUtil.writeToFileInAppendMode(String.valueOf(vertex), Constants.MG_VERTICES_FILE);
        });
        
        this.membershipGraph.edgeSet().forEach(defaultEdge -> {
            String v = membershipGraph.getEdgeSource(defaultEdge) + "|" + membershipGraph.getEdgeTarget(defaultEdge);
            FilesUtil.writeToFileInAppendMode(v, Constants.MG_EDGES_FILE);
        });
        
        encoder.getTable().forEach((k, v) -> {
            FilesUtil.writeToFileInAppendMode(String.valueOf(k) + "|" + "<http://www.schema.hng.root> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + v.toString() + " . ", Constants.MG_ENCODED_TABLE_FILE);
        });
        
        
        encoder.getReverseTable().forEach((k, v) -> {
            FilesUtil.writeToFileInAppendMode("<http://www.schema.hng.root> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + k.toString() + " . " + "|" + v.toString(), Constants.MG_ENCODED_R_TABLE_FILE);
        });
        
    }
    
    public void importGraphRelatedData() {
        List<String[]> vertices = FilesUtil.readCsvAllDataOnceWithPipeSeparator(Constants.MG_VERTICES_FILE);
        List<String[]> edges = FilesUtil.readCsvAllDataOnceWithPipeSeparator(Constants.MG_EDGES_FILE);
        
        DefaultDirectedGraph<Integer, DefaultEdge> graph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        vertices.forEach(v -> {
            graph.addVertex(Integer.valueOf(v[0]));
        });
        edges.forEach(edge -> {
            graph.addEdge(Integer.valueOf(edge[0]), Integer.valueOf(edge[1]));
        });
        
        List<String[]> encodedTable = FilesUtil.readCsvAllDataOnceWithPipeSeparator(Constants.MG_ENCODED_TABLE_FILE);
        List<String[]> encodedReversedTable = FilesUtil.readCsvAllDataOnceWithPipeSeparator(Constants.MG_ENCODED_R_TABLE_FILE);
        
        HashMap<Integer, Node> table = new HashMap<>();
        HashMap<Node, Integer> reverseTable = new HashMap<>();
        
        encodedTable.forEach(line -> {
            try {
                String x = line[1];
                Node[] nodes = NxParser.parseNodes(String.valueOf(line[1]));
                table.put(Integer.valueOf(line[0]), nodes[2]);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        
        encodedReversedTable.forEach(line -> {
            try {
                Node[] nodes = NxParser.parseNodes(line[0]);
                reverseTable.put(nodes[2], Integer.valueOf(line[1]));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        this.membershipGraph = graph;
        this.encoder = new NodeEncoder(table.size(), table, reverseTable);
    }
    
    public void setMembershipGraphRootNode(Integer membershipGraphRootNode) {
        this.membershipGraphRootNode = membershipGraphRootNode;
    }
    
    public Integer getMembershipGraphRootNode() {
        return membershipGraphRootNode;
    }
    
    public DefaultDirectedGraph<Integer, DefaultEdge> getMembershipGraph() {
        return membershipGraph;
    }
    
    private Map<Integer, Integer> getKeysOfMapSortedByValues(HashMap<Integer, Integer> map) {
        //https://www.baeldung.com/java-sorting
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort(new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(
                    Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        Map<Integer, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
}
