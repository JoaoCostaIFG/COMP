package GraphViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Graph {
    private List<VertexInterface> vertexSet;
    private Stack<VertexInterface> vertexStack;

    public Graph() {
        this.vertexSet = new ArrayList<>();
    }

    public void setVertexSet(List<VertexInterface> vertexSet) {
        this.vertexSet = vertexSet;
    }

    public VertexInterface getVertexIdx(int idx) {
        return this.vertexSet.get(idx);
    }

    public VertexInterface getVertexByInfo(String info) {
        for (VertexInterface v : this.vertexSet) {
            if (v.getInfo().equals(info))
                return v;
        }
        return null;
    }

    public void addVertex(VertexInterface v) {
        for (VertexInterface vertex : this.vertexSet) {
            if (vertex.getInfo().equals(v.getInfo()))
                return;
        }
        this.vertexSet.add(v);
    }

    public void addVertex(String info) {
        this.vertexSet.add(new Vertex(info));
    }

    public void addEdge(String origInfo, String destInfo) {
        if (origInfo.equals(destInfo)) return;
        VertexInterface orig = this.getVertexByInfo(origInfo);
        VertexInterface dest = this.getVertexByInfo(destInfo);
        orig.addEdge(dest);
    }

    public boolean graphColoring(int k) {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (VertexInterface v : this.vertexSet) {
            for (Edge e : v.getAdj()) {
                builder.append(v.getInfo()).append(" --- ").append(e.dest.getInfo()).append("\n");
            }
        }

        return builder.toString();
    }
}
