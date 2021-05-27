package GraphViewer;

import java.util.List;

public interface Vertex {
    int getId();
    void setColor(int newColor);
    int getColor();
    List<Edge> getAdj();
    void addEdge(Vertex v);
}
