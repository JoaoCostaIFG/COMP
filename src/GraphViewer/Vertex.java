package GraphViewer;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
    protected boolean isEnabled;
    protected String info;
    protected int color;
    protected final List<Edge> adj;

    public Vertex(String info) {
        this.isEnabled = true;
        this.info = info;
        this.color = -1;
        this.adj = new ArrayList<>();
    }

    public String getInfo() {
        return this.info;
    }

    public void setColor(int newColor) {
        this.color = newColor;
    }

    public int getColor() {
        return this.color;
    }

    public List<Edge> getAdj() {
        return this.adj;
    }

    public int getDegree() {
        return this.adj.size();
    }

    public void addEdge(Vertex v) {
        for (Edge edge : this.adj) {
            if (edge.getDest().getInfo().equals(v.getInfo()))
                return;
        }

        Edge e = new Edge(this, v);
        this.adj.add(e);
        v.addEdge(this);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void toggleState() {
        this.isEnabled = !isEnabled;
    }

    @Override
    public String toString() {
        return this.info + "[" + this.color + "]";
    }
}
