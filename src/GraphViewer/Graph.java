package GraphViewer;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private List<Vertex> vertexSet;

    public Graph() {
        this.vertexSet = new ArrayList<>();
    }

    public void setVertexSet(List<Vertex> vertexSet) {
        this.vertexSet = vertexSet;
    }

    public Vertex getVertexIdx(int idx) {
        return this.vertexSet.get(idx);
    }

    public Vertex getVertexById(int id) {
        for (Vertex v : this.vertexSet) {
            if (v.getId() == id)
                return v;
        }
        return null;
    }

    public void addEdge(Vertex orig, Vertex dest) {
        orig.addEdge(dest);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Vertex v : this.vertexSet) {
            for (Edge e : v.getAdj()) {
                builder.append(v.getId()).append(" --- ").append(e.dest.getId()).append("\n");
            }
        }

        return builder.toString();
    }
}
