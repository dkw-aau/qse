package cs.utils.tries;

public class Trie {
    int counter;
    private final TrieNode root;
    private TrieNode current;
    
    public Trie() {
        root = new TrieNode();
        counter = 0;
    }
    
    public int encode(String word) {
        int encodedValue;
        if (find(word)) {//Check if it already exists
            encodedValue = current.getKey();
            return encodedValue;
        } else {
            TrieNode current = root;
            for (char l : word.toCharArray()) {
                current = current.getChildren().computeIfAbsent(l, c -> new TrieNode());
            }
            current.setEndOfWord(true);
            current.setKey(this.counter);
            encodedValue = counter;
            this.counter++;
        }
        return encodedValue;
    }
    
    public int getEncodedInteger(String word) {
        int encodedValue = -99999;
        if (find(word)) {//Check if it already exists
            encodedValue = current.getKey();
            return encodedValue;
        }
        return encodedValue;
    }
    
    boolean delete(String word) {
        return delete(root, word, 0);
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
        this.current = root;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode node = this.current.getChildren().get(ch);
            if (node == null) {
                return false;
            }
            this.current = node;
        }
        return this.current.isEndOfWord();
    }
    
    void insert(String word) {
        TrieNode current = root;
        
        for (char l : word.toCharArray()) {
            current = current.getChildren().computeIfAbsent(l, c -> new TrieNode());
        }
        current.setEndOfWord(true);
    }
}