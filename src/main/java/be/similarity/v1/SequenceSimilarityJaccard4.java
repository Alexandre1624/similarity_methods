package be.similarity.v1;

import org.jgrapht.graph.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.jgrapht.graph.DefaultEdge;

public class SequenceSimilarityJaccard4 {

    // Sérialisation du graphe selon la qualité (ici degré du sommet)
    public static List<String> serializeGraph(DirectedMultigraph<String, DefaultEdge> graph, Map<String, Double> pageRank) {
        List<String> sequence = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Utilise LinkedHashSet pour garder l'ordre de tri et supprimer efficacement
        List<String> sortedVertices = new ArrayList<>(graph.vertexSet());
        sortedVertices.sort(Comparator.comparingDouble(pageRank::get).reversed());
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
                    .max(Comparator.comparingDouble(pageRank::get))
                    .orElse(null);
        }
        return sequence;
    }

    // Génère l'ensemble des shingles (k-grams) à partir d'une séquence de tokens
    public static Set<String> shingles(List<String> seq, int k) {
        Set<String> shingleSet = new HashSet<>();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            // On parcourt la séquence en extrayant toutes les fenêtres de taille k (k-shingles)
            for (int i = 0; i <= seq.size() - k; i++) {
                List<String> window = seq.subList(i, i + k);
                // On normalise le shingle
                String shingle = String.join(" ", window).toLowerCase();
                // On calcule un hash du shingle pour éviter de manipuler directement les chaînes
                byte[] hash = md.digest(shingle.getBytes(StandardCharsets.UTF_8));
                // On encode le hash en base64 pour le stocker sous forme de chaîne compacte
                shingleSet.add(Base64.getEncoder().encodeToString(hash));
            }
        } catch (NoSuchAlgorithmException e) {
            // Gestion d’erreur au cas où l’algorithme de hachage SHA-1 n’est pas disponible
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }

        return shingleSet;
    }

    // Calcule la similarité de Jaccard entre deux ensembles
    public static double jaccardSimilarity(Set<String> s1, Set<String> s2) {
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);

        return (double) intersection.size() / union.size();
    }

     public static void main(String[] args) {
        DirectedMultigraph<String, DefaultEdge> graphC = new DirectedMultigraph<>(DefaultEdge.class);

        // Ajouter les sommets
        String[] vertices = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String v : vertices) {
            graphC.addVertex(v);
        }

         graphC.addEdge("A", "D");

         graphC.addEdge("B", "A");
         graphC.addEdge("B", "D");
         graphC.addEdge("B", "E");

         graphC.addEdge("C", "F");

         graphC.addEdge("D", "A");
         graphC.addEdge("D", "B");
         graphC.addEdge("D", "E");

         graphC.addEdge("E", "D");
         graphC.addEdge("E", "F");
         graphC.addEdge("E", "G");

         graphC.addEdge("F", "C");
         graphC.addEdge("F", "E");
         graphC.addEdge("F", "H");

// n'a pas d'outlinks donc pas d'arête sortante

         graphC.addEdge("H", "F");

        DirectedMultigraph<String, DefaultEdge> gp = new DirectedMultigraph<>(DefaultEdge.class);

        String[] verticesC = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String v : verticesC) gp.addVertex(v);

        gp.addEdge("A", "D");
        gp.addEdge("B", "A");
        gp.addEdge("B", "D");
        gp.addEdge("B", "E");
        gp.addEdge("C", "F");
        gp.addEdge("D", "A");
        gp.addEdge("D", "B");

        gp.addEdge("E", "D");
        gp.addEdge("E", "F");

        gp.addEdge("F", "C");
        gp.addEdge("F", "E");
        gp.addEdge("F", "H");
        gp.addEdge("G", "H"); // Nouvelle arête
        gp.addEdge("H", "F");

        List<String> seqG = serializeGraph(graphC, null);
        List<String> seqGp = serializeGraph(gp, null);

        System.out.println("Séquence G: " + seqG);
        System.out.println("Séquence G': " + seqGp);

        Set<String> shinglesG = shingles(seqG, 3);
        Set<String> shinglesGp = shingles(seqGp, 3);

        double similarity = jaccardSimilarity(shinglesG, shinglesGp);
        System.out.println("Similarité Jaccard estimée: " + similarity);
    }
}