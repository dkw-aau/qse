package cs.utils.tries;

import java.util.HashMap;
import java.util.Map;

class TrieNode {
    private final Map<Character, TrieNode> children = new HashMap<>();
    private boolean endOfWord;
    private Integer key;
    
    Map<Character, TrieNode> getChildren() {
        return children;
    }
    
    boolean isEndOfWord() {
        return endOfWord;
    }
    
    void setEndOfWord(boolean endOfWord) {
        this.endOfWord = endOfWord;
    }
    
    void setKey(Integer keyValue) {
        this.key = keyValue;
    }
    
    Integer getKey() {
        return this.key;
    }
}