package extras;

import java.util.Arrays;

/**
 * extras.RDFVault is a trie-based compact in-memory dictionary encoder especially tuned for RDF data.
 * It is based on a enhanced version of List-Trie which both fast and memory efficient, especially
 * for strings that have long common prefixes
 * <p>
 * extras.RDFVault does not map each string to a calculated numerical ID, but instead, it maps each string
 * to a memory location from where the string can be reconstructed again. Thus, the dictionary coder
 * which often requires two data structures to be constructed is confined into a single data structure,
 * saving memory and time.
 *
 * @author Steven de Rooij
 * @author Hamid R. Bazoobandi
 */

public class RDFVault {
    
    // The root of the tree is always present and always an internal node.
    private final int REDEEM_BUF_SIZE = 4 * (1024 * 1024);
    private InternalNode _root = new InternalNode(null, null, null, null);
    private int _size = 0; // The number of items in the tree.
    private char[] redeemBuf = new char[REDEEM_BUF_SIZE];
    private static final int ITEM_MARKER = 65536; // n.find_child(ITEM_MARKER) returns a dummy child if present.
    
    /**
     * Returns the number of unique items stored in the vault.
     *
     * @return The number of items.
     */
    public int size() { return _size; }
    
    public void trash(Object entry) {
        Leaf e = (Leaf) entry;
        e.verify();
        InternalNode n = e._parent; // we need the parent so we can repair its neighborhood later
        e.remove();                // unlink the leaf and mark it as invalid
        e.invalidate();
        _size--;
        
        // see if I am now a node with 0 or 1 children, if so remove me too
        if (n._parent == null) return;                   // never remove the root
        
        Node ch = n._children;
        if (ch == null) {
            n.remove();
            return;
        } // no more children? just remove node
        if (ch._siblings != null) return; // more than one child? Then we're done
        
        // There is exactly one child. Remove n and join the child with its parent's parent.
        int l_n = n._label.length;
        char[] new_label;
        if (ch._label == null) {
            new_label = n._label;
        } else {
            new_label = Arrays.copyOf(n._label, l_n + ch._label.length);
            System.arraycopy(ch._label, 0, new_label, l_n, ch._label.length);
        }
        n.remove();
        ch._label = new_label;
        ch._parent = n._parent;
        ch._siblings = n._parent._children;
        n._parent._children = ch;
    }
    
    public Object encode(String item) {
        // Find the parent of the leaf for this item. If the parent does not exist yet, create it.
        int len = item.length();
        InternalNode n = _root;
        int depth = 0;
        search:
        while (depth < len) {
            Node ch = n.find_child(item.charAt(depth));
            if (ch == null) break search; // we know a leaf can be added
            int ext;
            for (ext = 1; ext < ch._label.length; ext++) {
                if (depth + ext == len || item.charAt(depth + ext) != ch._label[ext]) {
                    n = ch.split_edge(ext);
                    depth += ext;
                    break search;
                }
            }
            // we have traversed the edge label and now we've reached ch
            depth += ext;
            if (ch instanceof Leaf) {
                if (depth == len) return ch;
                // the leaf represents a prefix of this item. Insert an internal node here
                n = ch.split_edge(ext);
                break search;
            }
            n = (InternalNode) ch;
        }
        
        // Add a new leaf to the tree, starting at internal node n.
        
        char[] new_label;
        if (len == depth) {
            Node ch = n.find_child(ITEM_MARKER);
            if (ch != null) return ch;
            new_label = null;
        } else {
            new_label = item.substring(depth).toCharArray();
        }
        
        Leaf res = new Leaf(n, n._children, new_label);
        n._children = res;
        _size++;
        return res;
    }
    
    public Object search(String item) {
        Node n;
        int depth;
        for (n = _root, depth = 0; depth < item.length(); depth += n._label.length) {
            if (n instanceof Leaf) return null;
            Node ch = ((InternalNode) n).find_child(item.charAt(depth));
            if (ch == null) return null;
            for (int i = 1; i < ch._label.length; i++) {
                if (depth + i == item.length() || item.charAt(depth + i) != ch._label[i]) return null;
            }
            n = ch;
        }
        return n instanceof Leaf ? n : ((InternalNode) n).find_child(ITEM_MARKER);
    }
    
    public String decode(Object ticket) {
        if (!(ticket instanceof Leaf)) return null;
        Leaf leaf = (Leaf) ticket;
        leaf.verify();
        int i;
        
        int idx = REDEEM_BUF_SIZE - 1;
        if (leaf._label != null) {
            for (i = leaf._label.length - 1; i >= 0; idx--, i--) {
                redeemBuf[idx] = leaf._label[i];
            }
        }
        
        Node n = leaf._parent;
        for (n = leaf._parent; n != null; n = n._parent) {
            if (n._label != null) {
                for (i = n._label.length - 1; i >= 0; idx--, i--) {
                    redeemBuf[idx] = n._label[i];
                }
            }
        }
        
        return new String(redeemBuf, idx, REDEEM_BUF_SIZE - idx);
    }
    
    ///////////////////////////////////////// Tree nodes ///////////////////////////////////////////////
    
    private static abstract class Node {
        protected InternalNode _parent;
        protected Node _siblings;
        protected char[] _label;
        
        // Returns the first character of the edge label into this node, or ITEM_MARKER if the label is empty
        private int label_char() {
            return _label == null ? ITEM_MARKER : _label[0];
        }
        
        // Elminates this node from the tree. Do not remove the root node.
        protected void remove() {
            InternalNode n = _parent;
            if (n._children == this) {
                n._children = _siblings;
            } else {
                Node m = n._children;
                while (m._siblings != this) { m = m._siblings; }
                m._siblings = m._siblings._siblings;
            }
        }
        
        // inserts an internal node into this edge with a label of length ext
        private InternalNode split_edge(int ext) {
            char[] label_chunk1, label_chunk2;
            if (ext == _label.length) {
                label_chunk1 = _label;
                label_chunk2 = null;
            } else {
                label_chunk1 = Arrays.copyOfRange(_label, 0, ext);
                label_chunk2 = Arrays.copyOfRange(_label, ext, _label.length);
            }
            InternalNode n = new InternalNode(_parent, this, _siblings, label_chunk1);
            _parent._children = n;
            _parent = n;
            _siblings = null;
            _label = label_chunk2;
            return n;
        }
        
    }
    
    private static class InternalNode extends Node {
        private Node _children;
        
        private InternalNode(InternalNode parent, Node children, Node siblings, char[] label) {
            _parent = parent;
            _children = children;
            _siblings = siblings;
            _label = label;
        }
        
        /* Return the child whose edge label starts with c, or null if it is not there.
         * The requested child, if it exists, is moved to the front of the linked list.
         * This may speed up future lookups, and makes it easier to relink the child.
         */
        private Node find_child(int c) {
            Node ch = _children;
            if (ch == null || ch.label_char() == c) return ch;
            for (Node cur = ch, nxt = ch._siblings; nxt != null; cur = nxt, nxt = cur._siblings) {
                if (nxt.label_char() == c) {
                    cur._siblings = nxt._siblings;
                    nxt._siblings = ch;
                    _children = nxt;
                    return nxt;
                }
            }
            return null;
        }
    }
    
    private static class Leaf extends Node {
        
        private Leaf(InternalNode parent, Node siblings, char[] label) {
            _parent = parent;
            _siblings = siblings;
            _label = label;
        }
        
        private void invalidate() {
            _parent = null;
            _siblings = null;
            _label = null;
        }
        
        private void verify() {
            if (_parent == null) {
                System.out.println("Exception - Verification failed");
            }
        }
    }
}