package GraphViewer;

public class Edge {
    protected final VertexInterface orig;
    protected final VertexInterface dest;

    public Edge(VertexInterface orig, VertexInterface dest) {
        this.orig = orig;
        this.dest = dest;
    }

    public VertexInterface getOrig() {
        return orig;
    }

    public VertexInterface getDest() {
        return dest;
    }
}
