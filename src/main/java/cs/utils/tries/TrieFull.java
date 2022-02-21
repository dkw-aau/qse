package cs.utils.tries;

public class TrieFull {
    private final TrieNode root;
    
    public TrieFull() {
        root = new TrieNode();
    }
    
    void insert(String word) {
        TrieNode current = root;
        
        for (char l : word.toCharArray()) {
            current = current.getChildren().computeIfAbsent(l, c -> new TrieNode());
        }
        current.setEndOfWord(true);
    }
 
    boolean delete(String word) {
        return delete(root, word, 0);
    }
    
    boolean containsNode(String word) {
        TrieNode current = root;
        
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) {
                return false;
            }
            current = node;
        }
        return current.isEndOfWord();
    }
    
    boolean isEmpty() {
        return root == null;
    }
    
    private boolean delete(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEndOfWord()) {
                return false;
            }
            current.setEndOfWord(false);
            return current.getChildren().isEmpty();
        }
        char ch = word.charAt(index);
        TrieNode node = current.getChildren().get(ch);
        if (node == null) {
            return false;
        }
        boolean shouldDeleteCurrentNode = delete(node, word, index + 1) && !node.isEndOfWord();
        
        if (shouldDeleteCurrentNode) {
            current.getChildren().remove(ch);
            return current.getChildren().isEmpty();
        }
        return false;
    }
    
    public boolean find(String word) {
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) {
                return false;
            }
            current = node;
        }
        return current.isEndOfWord();
    }
    
    
    public static TrieFull createExampleTrie() {
        TrieFull trie = new TrieFull();
        trie.insert("Programming");
        trie.insert("is");
        trie.insert("a");
        trie.insert("way");
        trie.insert("of");
        trie.insert("life");
        trie.insert("self");
        trie.insert("selfish");
        trie.insert("selfsatisfactory");
        return trie;
    }
    
   /* public static void main(String[] args) throws Exception {
        TrieFull trie = createExampleTrie();
        System.out.println(trie);
    }*/
}