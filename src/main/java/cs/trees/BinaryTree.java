package cs.trees;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import com.google.common.hash.BloomFilter;
import org.semanticweb.yars.nx.Node;

public class BinaryTree {
    
    BtNode root;
    
    public void add(int value, Node node, BloomFilter<CharSequence> bf) {
        root = addRecursive(root, value, node, bf);
    }
    
    private BtNode addRecursive(BtNode current, int value, Node node, BloomFilter<CharSequence> bf) {
        
        if (current == null) {
            return new BtNode(value, node, bf);
        }
        
        if (value < current.value) {
            current.left = addRecursive(current.left, value, node, bf);
        } else if (value > current.value) {
            current.right = addRecursive(current.right, value, node, bf);
        } else {
            current.right = addRecursive(current.right, value, node, bf);
        }
        
        return current;
    }
    
    public boolean isEmpty() {
        return root == null;
    }
    
    public int getSize() {
        return getSizeRecursive(root);
    }
    
    private int getSizeRecursive(BtNode current) {
        return current == null ? 0 : getSizeRecursive(current.left) + 1 + getSizeRecursive(current.right);
    }
    
    public boolean containsNode(int value, String valToSearch) {
        return containsNodeRecursive(root, value, valToSearch);
    }
    
    private boolean containsNodeRecursive(BtNode current, int value, String valToSearch) {
        if (current == null) {
            return false;
        }
        
        if (current.bloomFilter.mightContain(valToSearch))
            return true;
        
        
        return value < current.value
                ? containsNodeRecursive(current.left, value, valToSearch)
                : containsNodeRecursive(current.right, value, valToSearch);
    }
    
    public void delete(int value) {
        root = deleteRecursive(root, value);
    }
    
    private BtNode deleteRecursive(BtNode current, int value) {
        if (current == null) {
            return null;
        }
        
        if (value == current.value) {
            // Case 1: no children
            if (current.left == null && current.right == null) {
                return null;
            }
            
            // Case 2: only 1 child
            if (current.right == null) {
                return current.left;
            }
            
            if (current.left == null) {
                return current.right;
            }
            
            // Case 3: 2 children
            int smallestValue = findSmallestValue(current.right);
            current.value = smallestValue;
            current.right = deleteRecursive(current.right, smallestValue);
            return current;
        }
        if (value < current.value) {
            current.left = deleteRecursive(current.left, value);
            return current;
        }
        
        current.right = deleteRecursive(current.right, value);
        return current;
    }
    
    private int findSmallestValue(BtNode root) {
        return root.left == null ? root.value : findSmallestValue(root.left);
    }
    
    public void traverseInOrder(BtNode btNode) {
        if (btNode != null) {
            traverseInOrder(btNode.left);
            visit(btNode.value);
            traverseInOrder(btNode.right);
        }
    }
    
    public void traversePreOrder(BtNode btNode) {
        if (btNode != null) {
            visit(btNode.value);
            System.out.print(btNode.classNode.getLabel());
            traversePreOrder(btNode.left);
            traversePreOrder(btNode.right);
        }
    }
    
    public void traversePostOrder(BtNode btNode) {
        if (btNode != null) {
            traversePostOrder(btNode.left);
            traversePostOrder(btNode.right);
            visit(btNode.value);
        }
    }
    
    public void traverseLevelOrder() {
        if (root == null) {
            return;
        }
        
        Queue<BtNode> btNodes = new LinkedList<>();
        btNodes.add(root);
        
        while (!btNodes.isEmpty()) {
            
            BtNode btNode = btNodes.remove();
            
            System.out.print(" " + btNode.value);
            
            if (btNode.left != null) {
                btNodes.add(btNode.left);
            }
            
            if (btNode.right != null) {
                btNodes.add(btNode.right);
            }
        }
    }
    
    public void traverseInOrderWithoutRecursion() {
        Stack<BtNode> stack = new Stack<>();
        BtNode current = root;
        
        while (current != null || !stack.isEmpty()) {
            while (current != null) {
                stack.push(current);
                current = current.left;
            }
            
            BtNode top = stack.pop();
            visit(top.value);
            current = top.right;
        }
    }
    
    
    public void traversePreOrderWithoutRecursion() {
        Stack<BtNode> stack = new Stack<>();
        BtNode current = root;
        stack.push(root);
        
        while (current != null && !stack.isEmpty()) {
            current = stack.pop();
            visit(current.value);
            
            if (current.right != null)
                stack.push(current.right);
            
            if (current.left != null)
                stack.push(current.left);
        }
    }
    
    public Node traversePreOrderWithoutRecursion(BtNode btNode, String valueToSearch) {
        Node node = null;
        Stack<BtNode> stack = new Stack<>();
        BtNode current = btNode;
        stack.push(root);
        
        while (current != null && !stack.isEmpty()) {
            current = stack.pop();
            //visit(current.value);
            if (current.bloomFilter.mightContain(valueToSearch)) {
                node = current.classNode;
                break;
            }
          
            if (current.left != null)
                stack.push(current.left);
    
            if (current.right != null)
                stack.push(current.right);
    
        }
        return node;
    }
    
    public void traversePostOrderWithoutRecursion() {
        Stack<BtNode> stack = new Stack<>();
        BtNode prev = root;
        BtNode current = root;
        stack.push(root);
        
        while (current != null && !stack.isEmpty()) {
            current = stack.peek();
            boolean hasChild = (current.left != null || current.right != null);
            boolean isPrevLastChild = (prev == current.right || (prev == current.left && current.right == null));
            
            if (!hasChild || isPrevLastChild) {
                current = stack.pop();
                visit(current.value);
                prev = current;
            } else {
                if (current.right != null) {
                    stack.push(current.right);
                }
                if (current.left != null) {
                    stack.push(current.left);
                }
            }
        }
    }
    
    private void visit(int value) {
        System.out.println(" " + value);
    }
    
    class BtNode {
        int value;
        Node classNode;
        BloomFilter<CharSequence> bloomFilter;
        BtNode left;
        BtNode right;
        
        BtNode(int value, Node c, BloomFilter<CharSequence> bf) {
            this.value = value;
            this.classNode = c;
            this.bloomFilter = bf;
            right = null;
            left = null;
        }
    }
}