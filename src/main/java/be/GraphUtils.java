package be;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class GraphUtils {

    public static DirectedMultigraph<String, DefaultEdge> applyRandomVertexRemoval(DirectedMultigraph<String, DefaultEdge> original, double vertexFraction) {
        // Clone du graphe pour ne pas modifier l'original
        DirectedMultigraph<String, DefaultEdge> g = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : original.vertexSet()) g.addVertex(v);
        for (DefaultEdge e : original.edgeSet()) {
            g.addEdge(original.getEdgeSource(e), original.getEdgeTarget(e));
        }

        Random rand = new Random();

        // Suppression de sommets uniquement
        List<String> vertices = new ArrayList<>(g.vertexSet());
        int nbToRemove = (int) (vertexFraction * vertices.size());
        Collections.shuffle(vertices, rand);
        for (int i = 0; i < nbToRemove; i++) {
            g.removeVertex(vertices.get(i));
        }

        return g;
    }

    public static void saveGraph(DirectedMultigraph<String, DefaultEdge> graph, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            // Sauve toutes les arêtes
            for (DefaultEdge edge : graph.edgeSet()) {
                String source = graph.getEdgeSource(edge);
                String target = graph.getEdgeTarget(edge);
                pw.println(source + " " + target);
            }
            // Sauve les sommets isolés (aucune arête entrante ni sortante)
            for (String v : graph.vertexSet()) {
                if (graph.inDegreeOf(v) == 0 && graph.outDegreeOf(v) == 0) {
                    pw.println(v);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DirectedMultigraph<String, DefaultEdge> connectedRandomSubgraph(
            DirectedMultigraph<String, DefaultEdge> base, int n, double edgeFraction) {

        List<String> allVertices = new ArrayList<>(base.vertexSet());
        Collections.shuffle(allVertices);

        // On part d'un sommet au hasard
        String start = allVertices.get(0);

        Set<String> selectedVertices = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        selectedVertices.add(start);
        queue.add(start);

        // On agrandit autour du point de départ (BFS)
        while (!queue.isEmpty() && selectedVertices.size() < n) {
            String v = queue.poll();
            for (DefaultEdge e : base.outgoingEdgesOf(v)) {
                String target = base.getEdgeTarget(e);
                if (!selectedVertices.contains(target)) {
                    selectedVertices.add(target);
                    queue.add(target);
                    if (selectedVertices.size() >= n) break;
                }
            }
        }

        // Création du sous-graphe induit sur ces sommets
        DirectedMultigraph<String, DefaultEdge> subgraph = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : selectedVertices) {
            subgraph.addVertex(v);
        }
        List<DefaultEdge> candidateEdges = new ArrayList<>();
        for (String v : selectedVertices) {
            for (DefaultEdge e : base.outgoingEdgesOf(v)) {
                String target = base.getEdgeTarget(e);
                if (selectedVertices.contains(target)) {
                    candidateEdges.add(e);
                }
            }
        }
        // Prend une fraction d'arêtes (optionnel)
        Collections.shuffle(candidateEdges);
        int maxEdges = (int) (edgeFraction * candidateEdges.size());
        List<DefaultEdge> selectedEdges = candidateEdges.subList(0, maxEdges);
        for (DefaultEdge e : selectedEdges) {
            String source = base.getEdgeSource(e);
            String target = base.getEdgeTarget(e);
            subgraph.addEdge(source, target);
        }

        return subgraph;
    }

    public static DirectedMultigraph<String, DefaultEdge> randomSubgraph(
            DirectedMultigraph<String, DefaultEdge> base, int n, double edgeFraction) {

        List<String> allVertices = new ArrayList<>(base.vertexSet());
        if (n > allVertices.size()) {
            throw new IllegalArgumentException("n (" + n + ") > nombre de sommets (" + allVertices.size() + ")");
        }

        // Mélange et sélection des sommets
        Collections.shuffle(allVertices);
        Set<String> selectedVertices = new HashSet<>(allVertices.subList(0, n));

        // Création du sous-graphe
        DirectedMultigraph<String, DefaultEdge> subgraph = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : selectedVertices) {
            subgraph.addVertex(v);
        }

        // Construction de la liste des arêtes candidates
        List<DefaultEdge> candidateEdges = new ArrayList<>();
        for (String v : selectedVertices) {
            for (DefaultEdge e : base.outgoingEdgesOf(v)) {
                String target = base.getEdgeTarget(e);
                if (selectedVertices.contains(target)) {
                    candidateEdges.add(e);
                }
            }
        }

        // Sélection aléatoire d'une fraction des arêtes
        Collections.shuffle(candidateEdges);
        int maxEdges = (int) (edgeFraction * candidateEdges.size());
        if (maxEdges > candidateEdges.size()) maxEdges = candidateEdges.size(); // sécurité
        List<DefaultEdge> selectedEdges = candidateEdges.subList(0, maxEdges);

        for (DefaultEdge e : selectedEdges) {
            String source = base.getEdgeSource(e);
            String target = base.getEdgeTarget(e);
            subgraph.addEdge(source, target);
        }

        return subgraph;
    }
}
