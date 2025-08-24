package be.similarity.v1;

import com.google.common.hash.Hashing;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.*;

public class SequenceSimilarityJaccard4MinHash {
    private static final int PRIME = 429496731;

    // Sérialisation du graphe ---
    public static List<String> serializeGraphMinHash(DirectedMultigraph<String, DefaultEdge> graph,  Map<String, Double> scores) {
        List<String> sequence = new ArrayList<>();
        Set<String> visited = new HashSet<>();


        List<String> sortedVertices = new ArrayList<>(graph.vertexSet());
        sortedVertices.sort(Comparator.comparingDouble(scores::get).reversed());
        Set<String> vertexSet = new LinkedHashSet<>(sortedVertices);

        String currentNode = null;

        while (sequence.size() < graph.vertexSet().size()) {
            if (currentNode == null) {
                for (String v : vertexSet) {
                    if (!visited.contains(v)) {
                        currentNode = v;
                        break;
                    }
                }
                if (currentNode == null) break;
            }

            if (!visited.add(currentNode)) {
                currentNode = null;
                continue;
            }

            sequence.add(currentNode);
            vertexSet.remove(currentNode);

            List<String> neighbors = new ArrayList<>();
            for (DefaultEdge e : graph.outgoingEdgesOf(currentNode)) {
                String tgt = graph.getEdgeTarget(e);
                if (!visited.contains(tgt)) {
                    neighbors.add(tgt);
                }
            }

            currentNode = neighbors.stream()
                    .max(Comparator.comparingDouble(scores::get))
                    .orElse(null);
        }

        return sequence;
    }
    public static int shingleMurmur(String shingle) {
        return Hashing.murmur3_32_fixed().hashUnencodedChars(shingle).asInt();
    }
    // Extraction de shingles sous forme d'IDs ---
    public static Set<Integer> shingleIds(List<String> seq, int k) {
        Set<Integer> shingleSet = new HashSet<>();
        for (int i = 0; i <= seq.size() - k; i++) {
            List<String> window = seq.subList(i, i + k);
            String shingle = String.join(" ", window).toLowerCase();
            int id = Math.abs(shingleMurmur(shingle));
            shingleSet.add(id);
        }
        return shingleSet;
    }

    // -Génération des fonctions de hachage MinHash ---
    // Génère m fonctions de hachage indépendantes du type h(x) = (a*x + b) % PRIME,
    // où a et b sont choisis aléatoirement.
    public static List<int[]> generateHashFunctions(int m) {
        Random rand = new Random();
        List<int[]> functions = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int a = rand.nextInt(Integer.MAX_VALUE - 1) + 1; // a != 0
            int b = rand.nextInt(Integer.MAX_VALUE); // b quelconque
            functions.add(new int[]{a, b});
        }
        return functions;
    }

    // Calcul de la signature MinHash ---
    // Calcule le vecteur de signature MinHash du set de shingles fourni.
    // La signature a une case par fonction de hachage (m en tout) : pour chaque fonction, on prend le plus petit hash sur tous les shingles.
    public static int[] minHashSignature(Set<Integer> shingleIds, List<int[]> hashFunctions) {
        int[] signature = new int[hashFunctions.size()];
        // On initialise chaque case à la valeur maximale possible.
        Arrays.fill(signature, Integer.MAX_VALUE);

        // Pour chaque fonction de hachage
        for (int i = 0; i < hashFunctions.size(); i++) {
            int a = hashFunctions.get(i)[0];
            int b = hashFunctions.get(i)[1];

            // Pour chaque shingle, on calcule le hash et on garde la plus petite valeur obtenue.
            for (int id : shingleIds) {
                int hash = (int) (((long) a * id + b) % PRIME);
                if (hash < signature[i]) {
                    signature[i] = hash;
                }
            }
        }
        return signature;
    }

    // Similarité estimée MinHash
    public static double estimateMinHashSimilarity(int[] sig1, int[] sig2) {
        if (sig1.length != sig2.length) throw new IllegalArgumentException("Signatures de tailles différentes");
        int match = 0;
        // Compte le nombre de composantes identiques dans les deux signatures.
        for (int i = 0; i < sig1.length; i++) {
            if (sig1[i] == sig2[i]) match++;
        }
        // Retourne le pourcentage de cases identiques : estimation de la similarité de Jaccard.
        return (double) match / sig1.length;
    }

    public static void main(String[] args) {
        // Création de deux petits graphes exemples
        DirectedMultigraph<String, DefaultEdge> graph1 = new DirectedMultigraph<>(DefaultEdge.class);
        graph1.addVertex("A");
        graph1.addVertex("B");
        graph1.addVertex("C");
        graph1.addEdge("A", "B");
        graph1.addEdge("B", "C");

        DirectedMultigraph<String, DefaultEdge> graph2 = new DirectedMultigraph<>(DefaultEdge.class);
        graph2.addVertex("A");
        graph2.addVertex("B");
        graph2.addVertex("D");
        graph2.addEdge("A", "B");
        graph2.addEdge("B", "D");

        int k = 3;
        int m = 500;

        List<String> seq1 = serializeGraphMinHash(graph1, null);
        List<String> seq2 = serializeGraphMinHash(graph2, null);

        Set<Integer> shingles1 = shingleIds(seq1, k);
        Set<Integer> shingles2 = shingleIds(seq2, k);

        List<int[]> hashFunctions = generateHashFunctions(m);

        int[] sig1 = minHashSignature(shingles1, hashFunctions);
        int[] sig2 = minHashSignature(shingles2, hashFunctions);

        double similarity = estimateMinHashSimilarity(sig1, sig2);
        System.out.printf("Similarité estimée MinHash : %.3f%n", similarity);
    }
}