package cs.others.parsers.mg;


import cs.utils.*;
import cs.utils.encoders.NodeEncoder;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.roaringbitmap.RoaringBitmap;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import java.util.*;
import java.util.stream.Collectors;

public class MembershipGraphRbm {
    DefaultDirectedGraph<Integer, DefaultEdge> membershipGraph;
    NodeEncoder encoder;
    Map<Integer, List<List<Integer>>> membershipSets;
    Integer membershipGraphRootNode;
    HashMap<Integer, RoaringBitmap> ctiRbm;
    HashMap<String, Integer> classInstanceCount;
    
    public MembershipGraphRbm(NodeEncoder e, HashMap<Integer, RoaringBitmap> ctiBf, HashMap<String, Integer> classInstanceCount) {
        this.membershipGraph = new DefaultDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
        this.encoder = e;
        this.ctiRbm = ctiBf;
        this.classInstanceCount = classInstanceCount;
    }
    
    public void createMembershipSets(HashMap<Node, List<Integer>> instanceToClass) {
        this.membershipSets = instanceToClass.values().stream().distinct()
                .collect(Collectors.groupingBy(List::size)).entrySet().stream().sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                    throw new AssertionError();
                }, LinkedHashMap::new));
    }
    
    public void createMembershipGraph() {
        //AtomicBoolean flag = new AtomicBoolean(false);
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
                            memberFrequency.put(element, this.classInstanceCount.get(encoder.decode(element).getLabel()));
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
                boolean flag = false;
                if (subGraphVertices.size() > 1) {
                    for (Integer vertex : subGraphVertices) {
                        if (membershipGraph.inDegreeOf(vertex) == 0) {
                            rootNodesOfSubGraphs.add(vertex);
                            flag = true;
                        }
                    }
                    
                    //Handle the graph having no node with inDegree 0
                    if (!flag) {
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
    
    public void membershipGraphCompression(Integer threshold) {
        int node = this.membershipGraphRootNode;
        System.out.println("Membership Graph Vertices Before Normalization: " + this.membershipGraph.vertexSet().size());
        //int node = 8902; // ROOT NODE OF MEMBERSHIP GRAPH; int focusedSubGraphSize = getGraphSizeViaBFS(focusNode);
        getFocusNodesViaBFS(node, threshold).forEach(focusNode -> {
            //System.out.println(focusNode + " : " + encoder.decode(focusNode).getLabel());
            performCompression(threshold, focusNode);
        });
        System.out.println("Membership Graph Vertices After Normalization: " + this.membershipGraph.vertexSet().size());
        //VISUALIZING
        //new MembershipGraphVisualizer().createBfsTraversedEncodedShortenIRIsNodesGraph(this.membershipGraph, encoder, node);
    }
    
    private void performCompression(int threshold, int focusNode) {
        //RETRIEVING DIRECT CHILDREN OF FOCUS NODE
        List<Integer> directChildrenOfNode = getDirectChildrenOfNode(focusNode);
        int numberOfGroups = (directChildrenOfNode.size() / threshold) + 1;
        
        List<MetaNodeChild> metaNodeChildList = new ArrayList<>();
        
        directChildrenOfNode.forEach(node -> {
            metaNodeChildList.add(new MetaNodeChild(node, classInstanceCount.get(encoder.decode(node).getLabel()), getNumberOfChildrenOfNode(node)));
        });
        metaNodeChildList.sort(Comparator.comparing(MetaNodeChild::getNode).thenComparing(MetaNodeChild::getNoc).thenComparing(MetaNodeChild::getFrequency));
        
        Queue<MetaNodeChild> metaNodeChildQueue = new LinkedList<>(metaNodeChildList);
        
        //Round Robin Policy, distributing the load
        HashMap<Integer, List<Integer>> metaNodeBalancedChildren = new HashMap<>();
        for (int i = 0; i < numberOfGroups; i++) {
            metaNodeBalancedChildren.put(i, new ArrayList<>());
        }
        
        while (!metaNodeChildQueue.isEmpty()) {
            metaNodeBalancedChildren.forEach((metaNodeIndex, metaNodeChild) -> {
                if (!metaNodeChildQueue.isEmpty()) {
                    metaNodeBalancedChildren.get(metaNodeIndex).add(Objects.requireNonNull(metaNodeChildQueue.poll()).getNode());
                }
            });
        }
        
        List<MetaNode> metaNodeInstances = new ArrayList<>();
        for (int i = 0, groupsSize = metaNodeBalancedChildren.size(); i < groupsSize; i++) {
            metaNodeInstances.add(new MetaNode(i, metaNodeBalancedChildren.get(i), focusNode, encoder, ctiRbm, true));
        }
        
        //Using group nodes to create and remove edges
        metaNodeInstances.forEach(metaNode -> {
            this.membershipGraph.addVertex(metaNode.getMetaNodeId());
            metaNode.getNodes().forEach(n -> {
                this.membershipGraph.removeEdge(focusNode, n);
                this.membershipGraph.addEdge(metaNode.getMetaNodeId(), n);
            });
            this.membershipGraph.addEdge(focusNode, metaNode.getMetaNodeId());
            this.ctiRbm.put(metaNode.metaNodeId, metaNode.getGroupRoaringBitMaps());
        });
    }
    
    private List<Integer> getDirectChildrenOfNode(Integer node) {
        List<Integer> directChildren = new ArrayList<>();
        this.membershipGraph.outgoingEdgesOf(node).forEach(edge -> {
            directChildren.add(this.membershipGraph.getEdgeTarget(edge));
        });
        return directChildren;
    }
    
    private Integer getNumberOfChildrenOfNode(Integer node) {
        List<Integer> directChildren = new ArrayList<>();
        this.membershipGraph.outgoingEdgesOf(node).forEach(edge -> {
            directChildren.add(this.membershipGraph.getEdgeTarget(edge));
        });
        return directChildren.size();
    }
    
    private Integer getGraphSizeViaBFS(Integer startNode) {
        BreadthFirstIterator<Integer, DefaultEdge> bfsIterator = new BreadthFirstIterator<>(membershipGraph, startNode);
        int size = 0;
        while (bfsIterator.hasNext()) {
            bfsIterator.next();
            size++;
        }
        return size;
    }
    
    private List<Integer> getFocusNodesViaBFS(Integer startNode, Integer threshold) {
        BreadthFirstIterator<Integer, DefaultEdge> bfsIterator = new BreadthFirstIterator<>(membershipGraph, startNode);
        //int focusNode = -1;
        //int focusNodeCount = 0;
        List<Integer> focusNodes = new ArrayList<>();
        while (bfsIterator.hasNext()) {
            int child = bfsIterator.next();
            if (membershipGraph.outDegreeOf(child) > threshold) {
                //focusNode = child;
                //focusNodeCount = membershipGraph.outDegreeOf(child);
                focusNodes.add(child);
                //break;
            }
        }
        return focusNodes;
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
    
    
    //Getters
    public Integer getMembershipGraphRootNode() {
        return membershipGraphRootNode;
    }
    
    public HashMap<Integer, RoaringBitmap> getCtiRbm() {return ctiRbm;}
    
    public Map<Integer, List<List<Integer>>> getMembershipSets() {return membershipSets;}
    
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
    
    // Setters
    
    public void setMembershipGraphRootNode(Integer membershipGraphRootNode) {
        this.membershipGraphRootNode = membershipGraphRootNode;
    }
}
