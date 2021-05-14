/* Generated By:JJTree: Do not edit this line. SimpleNode.java Version 6.1 */
/* JavaCCOptions:MULTI=false,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */

import pt.up.fe.comp.jmm.JmmNode;

import java.lang.RuntimeException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public
class SimpleNode implements Node, JmmNode {
    protected Node parent;
    protected Node[] children;
    protected int id;
    protected Object value;
    protected Jmm parser;

    // added
    private final Map<String, String> attributes;

    public SimpleNode(int i) {
        this.attributes = new HashMap<>();
        id = i;
    }

    public SimpleNode(Jmm p, int i) {
        this(i);
        parser = p;
    }

    /**
     * @return the kind of this node (e.g. MethodDeclaration, ClassDeclaration, etc.)
     */
    public String getKind() {
        return toString();
    }

    /**
     * // TODO implement this
     *
     * @return the names of the attributes supported by this Node kind
     */
    public List<String> getAttributes() {
        return new ArrayList<>(this.attributes.keySet());
    }

    /**
     * Sets the value of an attribute.
     *
     * @param attribute
     * @param value
     */
    public void put(String attribute, String value) {
        this.attributes.put(attribute, value);
    }

    /**
     * @param attribute
     * @returns the value of an attribute. To see all the attributes iterate the list provided by
     * {@link JmmNode#getAttributes()}
     */
    public String get(String attribute) {
        if (!this.attributes.containsKey(attribute))
            return null;
        return this.attributes.get(attribute);
    }

    /**
     * @return the children of the node or an empty list if there are no children
     */
    public List<JmmNode> getChildren() {
        return JmmNode.convertChildren(children);
    }

    /**
     * @return the number of children of the node
     */
    public int getNumChildren() {
        return jjtGetNumChildren();
    }

    /**
     * Inserts a node at the given position
     *
     * @param child
     * @param index
     */
    public void add(JmmNode child, int index) {
        if (!(child instanceof Node)) {
            throw new RuntimeException("Node not supported: " + child.getClass());
        }

        jjtAddChild((Node) child, index);
    }


    public void jjtOpen() {
    }

    public void jjtClose() {
    }

    public void jjtSetParent(Node n) {
        parent = n;
    }

    public Node jjtGetParent() {
        return parent;
    }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new Node[i + 1];
        } else if (i >= children.length) {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    public void jjtSetValue(Object value) {
        this.value = value;
    }

    public Object jjtGetValue() {
        return value;
    }

  /* You can override these two methods in subclasses of SimpleNode to
     customize the way the node appears when the tree is dumped.  If
     your output uses more than one line you should override
     toString(String), otherwise overriding toString() is probably all
     you need to do. */

    public String toString() {
        return JmmTreeConstants.jjtNodeName[id];
    }

    public String toString(String prefix) {
        return prefix + toString();
    }

  /* Override this method if you want to customize how the node dumps
     out its children. */

    public void dump(String prefix) {
        System.out.print(toString(prefix));

        for (Map.Entry<String, String> entry : this.attributes.entrySet())
            System.out.print(" - " + entry.getKey() + ": " + entry.getValue());
        System.out.println("");

        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                SimpleNode n = (SimpleNode) children[i];
                if (n != null) {
                    n.dump(prefix + " ");
                }
            }
        }
    }

    public int getId() {
        return id;
    }

    /**
     * Removes the child at the specified position.
     *
     * @param index
     * @return the node that has been removed
     */
    public JmmNode removeChild(int index) {
        JmmNode ret = null;
        Node[] copy = new Node[this.children.length - 1];
        for (int i = 0, j = 0; i < this.children.length; i++) {
            if (i != index)
                copy[j++] = this.children[i];
            else
                ret = (JmmNode) this.children[i];
        }
        this.children = copy;
        return ret;
    }

    /**
     * Removes the given child.
     *
     * @param node
     * @return the node that has been removed, which is the same as the given node
     */
    public int removeChild(JmmNode node) {
        int ret = -1;
        Node[] copy = new Node[this.children.length - 1];
        for (int i = 0, j = 0; i < this.children.length; i++) {
            if ((JmmNode) this.children[i] != node)
                copy[j++] = this.children[i];
            else
                ret = i;
        }
        this.children = copy;
        return ret;
    }

    /**
     * Removes this node from the tree.
     */
    public void delete() {
        JmmNode parent = (JmmNode) this.parent;
        parent.removeChild(this);
    }
}

/* JavaCC - OriginalChecksum=d33fdb2b8063d5de3474649324d5d160 (do not edit this line) */
