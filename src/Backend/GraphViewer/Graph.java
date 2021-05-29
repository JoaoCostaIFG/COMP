package Backend.GraphViewer;

import java.util.*;

public class Graph {
    private List<Vertex> vertexSet;
    private Stack<Vertex> vertexStack;
    private int colorsUsed;

    public Graph() {
        this.vertexSet = new ArrayList<>();
        this.vertexStack = new Stack<>();
        this.colorsUsed = 0;
    }

    public int getColorsUsed() {
        return colorsUsed;
    }

    public List<Vertex> getVertexSet() {
        return vertexSet;
    }

    public void setVertexSet(List<Vertex> vertexSet) {
        this.vertexSet = vertexSet;
    }

    public Vertex getVertexIdx(int idx) {
        return this.vertexSet.get(idx);
    }

    public Vertex getVertexByInfo(String info) {
        for (Vertex v : this.vertexSet) {
            if (v.getInfo().equals(info))
                return v;
        }
        return null;
    }

    public void addVertex(Vertex v) {
        for (Vertex vertex : this.vertexSet) {
            if (vertex.getInfo().equals(v.getInfo()))
                return;
        }
        this.vertexSet.add(v);
    }

    public void addVertex(String info) {
        this.addVertex(new Vertex(info));
    }

    public void addEdge(String origInfo, String destInfo) {
        if (origInfo.equals(destInfo)) return;
        Vertex orig = this.getVertexByInfo(origInfo);
        Vertex dest = this.getVertexByInfo(destInfo);
        orig.addEdge(dest);
    }

    public boolean graphColoring(int k) {
        // Step 1: find a vertex with degree < k
        boolean foundOne;
        do {
            foundOne = false;
            for (Vertex v : this.vertexSet) {
                if (v.isEnabled && v.getDegree() < k) {
                    foundOne = true;
                    v.toggleState();
                    this.vertexStack.add(v);
                    break;
                }
            }
        } while (foundOne);
        // Step 1 check: all nodes need to be on the stack
        if (this.vertexStack.size() != this.vertexSet.size())
            return false;

        // Step 2: repeatedly color
        boolean[] colors = new boolean[k];
        while (!this.vertexStack.empty()) {
            // reset available colors
            for (int i = 0; i < k; ++i) colors[i] = false;

            Vertex v = this.vertexStack.pop();
            v.toggleState();
            for (Edge e : v.getAdj()) {
                if (!e.dest.isEnabled) continue;
                colors[e.dest.color] = true;
            }

            boolean haveColorAvailable = false;
            for (int i = 0; i < k; ++i) {
                if (!colors[i]) {
                    haveColorAvailable = true;
                    v.setColor(i);
                    break;
                }
            }
            if (!haveColorAvailable)
                return false;
        }

        // Step 3: save colors used
        Set<Integer> colorsUsedSet = new HashSet<>();
        for (Vertex v : this.vertexSet)
            colorsUsedSet.add(v.color);
        this.colorsUsed = colorsUsedSet.size();

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Set<Vertex> alreadyDrawn = new HashSet<>();
        for (Vertex v : this.vertexSet) {
            if (v.getAdj().size() == 0) {
                builder.append(v).append("\n");
            } else {
                for (Edge e : v.getAdj()) {
                    if (v.isEnabled && e.dest.isEnabled && !alreadyDrawn.contains(e.dest))
                        builder.append(v).append(" --- ").append(e.dest).append("\n");
                }
            }
            alreadyDrawn.add(v);
        }

        return builder.toString();
    }
}
