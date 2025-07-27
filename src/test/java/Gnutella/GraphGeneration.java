package Gnutella;

import be.FileUtils;
import be.GraphUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.Set;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GraphGeneration {
    private static DirectedMultigraph<String, DefaultEdge> base = GraphUtils.loadGraph("src/main/resources/p2p-Gnutella31.txt");

    @Test
    @Order(1)
    public void testRemoveConnectedSubgraph() throws IOException {
        String dir = "output/similarity/removeConnectedSubgraph";
        FileUtils.ensureDirectoryExists(dir);
        Set<String> comp = GraphUtils.getStronglyConnectedComponent(base, 10);
        DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.removeVertices(base, comp);
        GraphUtils.saveGraph(modif, String.format("%s/removeConnectedSubgraph.txt",dir));
    }

    @Test
    @Order(2)
    public void testRemoveRandomVertices() throws IOException {
        double[] fractions = {0.05, 0.10, 0.20};
        String dir = "output/similarity/removeRandomVertices";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomVertexRemoval(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/removeRandomVertices_%02dpct.txt", dir, (int)(frac*100)));
        }
    }

    @Test
    @Order(3)
    public void testRemoveHubNodes() throws IOException {
        int[] kVals = {1, 5, 10};
        String dir = "output/similarity/removeHubs";
        FileUtils.ensureDirectoryExists(dir);
        for (int k : kVals) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.removeTopDegreeVertices(base, k);
            GraphUtils.saveGraph(modif, String.format("%s/removeHubs_top%d.txt", dir, k));
        }
    }

    @Test
    @Order(4)
    public void testRemoveRandomEdges() throws IOException {
        double[] fractions = {0.05, 0.15, 0.20};
        String dir = "output/similarity/removeRandomEdges";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomEdgeRemoval(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/removeRandomEdges_%02dpct.txt", dir, (int)(frac*100)));
        }
    }


    @Test
    @Order(5)
    public void testAddRandomEdges() throws IOException {
        double[] fractions = {0.05, 0.10, 0.20};
        String dir = "output/similarity/addRandomEdges";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomEdgeAddition(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/addRandomEdges_%02dpct.txt",dir, (int)(frac*100)));
        }
    }

    @Test
    @Order(6)
    public void testInvertEdgeDirection() throws IOException {
        double[] fractions = {0.05, 0.10, 0.20};
        String dir = "output/similarity/invertEdges";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.invertRandomEdgeDirections(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/invertEdges_%02dpct.txt",dir, (int)(frac*100)));
        }
    }

    @Test
    @Order(7)
    public void testAddDenseSubgraph() throws IOException {
        int[] cliqueSizes = {5, 10};
        String dir = "output/similarity/addDenseSubgraph";
        FileUtils.ensureDirectoryExists(dir);
        for (int size : cliqueSizes) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.addDenseSubgraph(base, size);
            GraphUtils.saveGraph(modif, String.format("%s/addDenseSubgraph_size%d.txt",dir, size));
        }
    }

    @Test
    @Order(8)
    public void testCloneNode() throws IOException {
        String[] nodesToClone = {"174","188"};
        String dir = "output/similarity/cloneNode";
        FileUtils.ensureDirectoryExists(dir);
        for (String node : nodesToClone) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.cloneNode(base, node);
            GraphUtils.saveGraph(modif, String.format("%s/cloneNode_%s.txt", dir, node));
        }
    }

    @Test
    @Order(9)
    public void testAddIsolatedNodes() throws IOException {
        int[] nb = {10, 50};
        String dir = "output/similarity/addIsolatedNodes";
        FileUtils.ensureDirectoryExists(dir);
        for (int n : nb) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.addIsolatedNodes(base, n);
            GraphUtils.saveGraph(modif, String.format("%s/addIsolatedNodes_%d.txt", dir, n));
        }
    }

    @Test
    @Order(10)
    public void testRandomRewireEdges() throws IOException {
        String dir = "output/similarity/rewireEdges";
        FileUtils.ensureDirectoryExists(dir);
        DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.rewireEdgesPreserveDegree(base, 0.2);
        GraphUtils.saveGraph(modif, String.format("%s/rewireEdges_20pct.txt", dir));
    }
}
