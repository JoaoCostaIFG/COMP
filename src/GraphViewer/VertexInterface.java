package GraphViewer;

import java.util.List;

public interface VertexInterface {
    String getInfo();
    void setColor(int newColor);
    int getColor();
    List<Edge> getAdj();
    int getDegree();
    void addEdge(VertexInterface v);
}
