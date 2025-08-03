package be.similarity.v1;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SignatureSimilarity5 {

    private static final Map<String, byte[]> hashCache = new HashMap<>(); // global ou static

    /**
     * Calcule la signature SimHash d'un graphe en utilisant PageRank pour les poids.
     * @param graph Le DirectedMultigraph à traiter.
     * @return BitSet de longueur SIGNATURE_SIZE représentant la signature.
     */
    public static BitSet computeSignature(DirectedMultigraph<String, DefaultEdge> graph, Map<String, Double> pageRank, int signatureSize) {
        // Calcul des scores PageRank

        // Tableau pour accumuler les contributions de chaque bit
        double[] bitSums = new double[signatureSize];

        // Parcours des sommets : chaque sommet ajoute son score PageRank
        for (String v : graph.vertexSet()) {
            double w = pageRank.get(v);
            updateBitSums(bitSums, v, w, signatureSize);
        }

        // Parcours des arêtes : poids = PageRank(u) / outDegree(u)
        for (DefaultEdge e : graph.edgeSet()) {
            String u = graph.getEdgeSource(e);
            String v = graph.getEdgeTarget(e);
            double qU = pageRank.get(u);
            int outDeg = graph.outDegreeOf(u);
            if (outDeg > 2) {
                outDeg -= 1;
            }
            double w = (outDeg > 0) ? qU / outDeg : qU;
            updateBitSums(bitSums, u + "->" + v, w, signatureSize);
        }

        // Construction de la signature finale : bit = 1 si sum>=0, sinon 0
        BitSet signature = new BitSet(signatureSize);
        for (int i = 0; i < signatureSize; i++) {
            if (bitSums[i] >= 0) {
                signature.set(i);
            }
        }
        return signature;
    }
    private static byte[] getOrComputeHash(String feature, int signatureSize) {
        String cacheKey = feature + "_" + signatureSize;
        return hashCache.computeIfAbsent(cacheKey, (_) -> SignatureSimilarity5.encrypt(feature, signatureSize));
    }

    /**
     * Met à jour le tableau bitSums avec la contribution d'une caractéristique.
     */
    private static void updateBitSums(double[] bitSums, String feature, double weight, int signatureSize) {
        byte[] hash = getOrComputeHash(feature,signatureSize);
        if (hash.length * 8 < signatureSize) {
            throw new IllegalArgumentException(
                    "Le hash ne fait pas assez de bits (" + (hash.length * 8) +
                            ") pour signatureSize=" + signatureSize + "."
            );
        }
        for (int i = 0; i < signatureSize; i++) {
            int bit = ((hash[i / 8] >> (7 - (i % 8))) & 1);
            bitSums[i] += (bit == 1 ? +weight : -weight);
        }
    }

    /**
     * encrypte d'une chaîne.
     */
    private static byte[] encrypt(String data, int signatureSize) {
        String algo;
        int expectedLen;
        switch (signatureSize) {
            case 128:
                algo = "MD5";
                expectedLen = 16;
                break;
            case 256:
                algo = "SHA-256";
                expectedLen = 32;
                break;
            case 512:
                algo = "SHA-512";
                expectedLen = 64;
                break;
            default:
                throw new IllegalArgumentException("Signature size non supporté : " + signatureSize +
                        " (autorisé : 128, 256, 512)");
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            if (digest.length != expectedLen) {
                throw new RuntimeException("Taille de digest inattendue pour " + algo +
                        ": attendu " + expectedLen + ", obtenu " + digest.length);
            }
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(algo + " non supporté", e);
        }
    }
    /**
     * Calcule la similarité entre deux signatures (1 - distance de Hamming normalisée).
     */
    public static double computeSimilarity(BitSet s1, BitSet s2, int signatureSize) {
        BitSet xor = (BitSet) s1.clone();
        xor.xor(s2);
        int diff = xor.cardinality();
        return 1.0 - ((double) diff / signatureSize);
    }

    public static void main(String[] args) {
        DirectedMultigraph<String, DefaultEdge> g1 = new DirectedMultigraph<>(DefaultEdge.class);
        DirectedMultigraph<String, DefaultEdge> g2 = new DirectedMultigraph<>(DefaultEdge.class);
        DirectedMultigraph<String, DefaultEdge> G = new DirectedMultigraph<>(DefaultEdge.class);

        String[] vertices = {"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String v : vertices) {
            G.addVertex(v);
        }

        for (String v : vertices) {
            g2.addVertex(v);
        }

        // Ajouter les arêtes selon le graphe de l'image
        G.addEdge("A", "D");
        G.addEdge("B", "A");
        G.addEdge("B", "D");
        G.addEdge("B", "E");
        G.addEdge("C", "F");
        G.addEdge("D", "A");
        G.addEdge("D", "B");
        G.addEdge("D", "E");
        G.addEdge("E", "D");
        G.addEdge("E", "F");
        G.addEdge("E", "G");
        G.addEdge("F", "C");
        G.addEdge("F", "E");
        G.addEdge("F", "H");
        G.addEdge("H", "F");

        // missing connected subgraph
        // Exemple de construction de g2
        g2.addEdge("A", "D");
        g2.addEdge("B", "A");
        g2.addEdge("B", "D");
        g2.addEdge("B", "E");
        g2.addEdge("C", "F");
        g2.addEdge("D", "A");
        g2.addEdge("D", "B");
        g2.addEdge("D", "E");
        g2.addEdge("E", "D");
        g2.addEdge("E", "G");
        g2.addEdge("F", "C");
        g2.addEdge("F", "H");
        g2.addEdge("H", "F");


        // missing random vertices
        G.addEdge("A", "D");
        G.addEdge("B", "A");
        G.addEdge("B", "D");
        G.addEdge("B", "E");
        G.addEdge("C", "F");
        G.addEdge("D", "A");
        G.addEdge("D", "B");
        G.addEdge("D", "E");
        G.addEdge("E", "D");
        G.addEdge("E", "F");
        G.addEdge("F", "C");
        G.addEdge("F", "E");


        // change vertices
        G.addEdge("A", "D");
        G.addEdge("B", "A");
        G.addEdge("B", "D");
        G.addEdge("B", "E");
        G.addEdge("C", "F");
        G.addEdge("D", "B");
        G.addEdge("D", "E");
        G.addEdge("E", "D");
        G.addEdge("E", "F");
        G.addEdge("F", "C");
        G.addEdge("F", "E");
        G.addEdge("F", "H");
        G.addEdge("H", "F");
        G.addEdge("G", "H");
        G.addEdge("F", "G");

//
//        BitSet sig1 = computeSignature(G);
//        BitSet sig2 = computeSignature(g2);

      //  double veo  = veoSimilarity(G, g2); //1
//        double vr  = (new VertexRankingSimilarity2<String, DefaultEdge>()).vertexRankingSimilarity(G, g2); //2
//        double sima = vertexEdgeVectorSimilarity(G, g2); //3

      //  System.out.printf("simVEO(G,G') = %.4f%n", veo);
//        System.out.printf("simVR(G,G') = %.4f%n", vr);
//        System.out.printf("Similarité VS(G,Gp) = %.4f%n", sima);
//        double sim = computeSimilarity(sig1, sig2);
//        System.out.printf("SimHash similarity = %.4f%n", sim);
    }
}

