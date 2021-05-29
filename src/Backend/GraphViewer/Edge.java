package Backend.GraphViewer;

public class Edge {
    protected final Vertex orig;
    protected final Vertex dest;

    public Edge(Vertex orig, Vertex dest) {
        this.orig = orig;
        this.dest = dest;
    }

    public Vertex getOrig() {
        return orig;
    }

    public Vertex getDest() {
        return dest;
    }
}
