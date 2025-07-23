package be;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {
    public static  DirectedMultigraph<String, DefaultEdge> parseGraph(String filename) throws IOException {
        DirectedMultigraph<String, DefaultEdge> G =
                new DirectedMultigraph<>(DefaultEdge.class);

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // Ignore les lignes vides
                String[] parts = line.split("\\s+");
                if (parts.length == 1) {
                    String su = parts[0].trim();
                    if (!G.containsVertex(su)) G.addVertex(su);
                } else if (parts.length == 2) {
                    String su = parts[0].trim();
                    String sv = parts[1].trim();
                    if (!G.containsVertex(su)) G.addVertex(su);
                    if (!G.containsVertex(sv)) G.addVertex(sv);
                    if (!su.equals(sv)) {
                        G.addEdge(su, sv);
                    }
                } else {
                    System.err.println("Ligne non valide : " + line);
                }
            }
        }

        return G;
    }


    public static void main(String[] args) {
        try {
            DirectedMultigraph<String, DefaultEdge> G = parseGraph("src/main/resources/Email.txt");
//            BiconnectivityInspector<String, DefaultEdge> bi = new BiconnectivityInspector<>(G);
//            Set<String> articulationPoints = bi.getCutpoints();
//            System.out.println("Points d’articulation : " + articulationPoints);
//
//            BitSet sig1 = SignatureSimilarity5.computeSignature(G);
//            BitSet sig2 = SignatureSimilarity5.computeSignature(g2);
//
//            double veo   = veoSimilarity(G, g2);
//            double vr    = (new VertexRankingSimilarity2<String, DefaultEdge>()).vertexRankingSimilarity(G, g2);
//            double vs    = vertexEdgeVectorSimilarity(G, g2);
//            double simH  = SignatureSimilarity5.computeSimilarity(sig1, sig2);
//            List<String> seqG = serializeGraph(G);
//            List<String> seqGp = serializeGraph(g2);
//
//            Set<String> shinglesG = shingles(seqG, 3);
//            Set<String> shinglesGp = shingles(seqGp, 3);
//
//            double similarity = jaccardSimilarity(shinglesG, shinglesGp);
//
//            System.out.printf(" VEO  similarity = %.4f%n", veo);
//            System.out.printf(" VR   similarity = %.4f%n", vr);
//            System.out.printf(" VS   similarity = %.4f%n", vs);
//            System.out.printf(" SeqS similarity = %.4f%n", similarity);
//            System.out.printf(" SS similarity = %.4f%n%n", simH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void supprimerUnSousGraphe(DirectedMultigraph<String, DefaultEdge> graph, String articulationPoint) {
        // Faire une copie sans le point d'articulation
        DirectedMultigraph<String, DefaultEdge> copie =
                new DirectedMultigraph<>(DefaultEdge.class);
        Graphs.addGraph(copie, graph);
        copie.removeVertex(articulationPoint);

        // Trouver les composantes fortement connexes
        KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector =
                new KosarajuStrongConnectivityInspector<>(copie);
        List<Set<String>> sccs = inspector.stronglyConnectedSets();

        // Choisir une composante
        for (Set<String> composante : sccs) {
            if (!composante.isEmpty()) {
                // Supprimer ces sommets dans le graphe original
                for (String v : composante) {
                    graph.removeVertex(v);
                }
                // supprime un seul sous-graphe, donc on arrête
                break;
            }
        }
    }
}