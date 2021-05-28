package GraphViewer;

import java.util.ArrayList;
import java.util.List;

public class Vertex implements VertexInterface {
    protected String info;
    protected int color;
    protected final List<Edge> adj;

    public Vertex(String info) {
        this.info = info;
        this.color = 0;
        this.adj = new ArrayList<>();
    }

    @Override
    public String getInfo() {
        return this.info;
    }

    @Override
    public void setColor(int newColor) {
        this.color = newColor;
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public List<Edge> getAdj() {
        return this.adj;
    }

    @Override
    public int getDegree() {
        return this.adj.size();
    }

    @Override
    public void addEdge(VertexInterface v) {
        for (Edge edge : this.adj) {
            if (edge.getDest().getInfo().equals(v.getInfo()))
                return;
        }

        Edge e = new Edge(this, v);
        this.adj.add(e);
        v.addEdge(this);
    }
}
