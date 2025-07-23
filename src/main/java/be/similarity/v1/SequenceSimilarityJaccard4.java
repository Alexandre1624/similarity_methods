package be.similarity.v1;

import org.jgrapht.graph.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;

public class SequenceSimilarityJaccard4 {

    // Sérialisation du graphe selon la qualité (ici degré du sommet)
    public static List<String> serializeGraph(DirectedMultigraph<String, DefaultEdge> graph, PageRank<String, DefaultEdge> pageRank) {
        List<String> sequence = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Calcul des scores PageRank si nécessaire
        if (pageRank == null) {
            pageRank = new PageRank<>(graph);
        }
        Map<String, Double> scores = pageRank.getScores();

        // Utilise LinkedHashSet pour garder l'ordre de tri et supprimer efficacement
        List<String> sortedVertices = new ArrayList<>(graph.vertexSet());
        sortedVertices.sort(Comparator.comparingDouble(scores::get).reversed());
        Set<String> vertexSet = new LinkedHashSet<>(sortedVertices);

        String currentNode = null;

        while (sequence.size() < graph.vertexSet().size()) {
            // Si on n'a pas de node courant, prends le premier non visité du set
            if (currentNode == null) {
                for (String v : vertexSet) {
                    if (!visited.contains(v)) {
                        currentNode = v;
                        break;
                    }
                }
                if (currentNode == null) break; // plus de sommets à visiter
            }

            if (!visited.add(currentNode)) {
                currentNode = null;
                continue;
            }
            sequence.add(currentNode);
            vertexSet.remove(currentNode);

            // Collecte voisins sortants non visités
            List<String> neighbors = new ArrayList<>();
            for (DefaultEdge e : graph.outgoingEdgesOf(currentNode)) {
                String tgt = graph.getEdgeTarget(e);
                if (!visited.contains(tgt)) {
                    neighbors.add(tgt);
                }
            }
            // Prends le voisin avec le plus gros score
            currentNode = neighbors.stream()
                    .max(Comparator.comparingDouble(scores::get))
                    .orElse(null);
        }
        return sequence;
    }

    // Shingling
    public static Set<String> shingles(List<String> seq, int k) {
        Set<String> shingleSet = new HashSet<>();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (int i = 0; i <= seq.size() - k; i++) {
                List<String> window = seq.subList(i, i + k);
                String shingle = String.join(" ", window).toLowerCase(); // normalisation
                byte[] hash = md.digest(shingle.getBytes(StandardCharsets.UTF_8));
                shingleSet.add(Base64.getEncoder().encodeToString(hash));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }

        return shingleSet;
    }

    // Jaccard Similarity
    public static double jaccardSimilarity(Set<String> s1, Set<String> s2) {
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);

        return (double) intersection.size() / union.size();
    }

     public static void main(String[] args) {
//        DirectedMultigraph<String, DefaultEdge> graphC = new DirectedMultigraph<>(DefaultEdge.class);
//
//        // Ajouter les sommets
//        String[] vertices = {"A", "B", "C", "D", "E", "F", "G", "H"};
//        for (String v : vertices) {
//            graphC.addVertex(v);
//        }
//
//         graphC.addEdge("A", "D");
//
//         graphC.addEdge("B", "A");
//         graphC.addEdge("B", "D");
//         graphC.addEdge("B", "E");
//
//         graphC.addEdge("C", "F");
//
//         graphC.addEdge("D", "A");
//         graphC.addEdge("D", "B");
//         graphC.addEdge("D", "E");
//
//         graphC.addEdge("E", "D");
//         graphC.addEdge("E", "F");
//         graphC.addEdge("E", "G");
//
//         graphC.addEdge("F", "C");
//         graphC.addEdge("F", "E");
//         graphC.addEdge("F", "H");
//
//// n'a pas d'outlinks donc pas d'arête sortante
//
//         graphC.addEdge("H", "F");
//
//        DirectedMultigraph<String, DefaultEdge> gp = new DirectedMultigraph<>(DefaultEdge.class);
//
//        String[] verticesC = {"A", "B", "C", "D", "E", "F", "G", "H"};
//        for (String v : verticesC) gp.addVertex(v);
//
//        gp.addEdge("A", "D");
//        gp.addEdge("B", "A");
//        gp.addEdge("B", "D");
//        gp.addEdge("B", "E");
//        gp.addEdge("C", "F");
//        gp.addEdge("D", "A");
//        gp.addEdge("D", "B");
//
//        gp.addEdge("E", "D");
//        gp.addEdge("E", "F");
//
//        gp.addEdge("F", "C");
//        gp.addEdge("F", "E");
//        gp.addEdge("F", "H");
//        gp.addEdge("G", "H"); // Nouvelle arête
//        gp.addEdge("H", "F");
//
//        List<String> seqG = serializeGraph(graphC, null);
//        List<String> seqGp = serializeGraph(gp, null);
//
//        System.out.println("Séquence G: " + seqG);
//        System.out.println("Séquence G': " + seqGp);
//
//        Set<String> shinglesG = shingles(seqG, 3);
//        Set<String> shinglesGp = shingles(seqGp, 3);
//
//        double similarity = jaccardSimilarity(shinglesG, shinglesGp);
//        System.out.println("Similarité Jaccard estimée: " + similarity);
    }
}