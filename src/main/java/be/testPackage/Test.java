package be.testPackage;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


public class Test {
    public static int[] minhashSignature(List<String> seq, int k, int numHashes) {
        int[] minHashes = new int[numHashes];
//        Arrays.fill(minHashes, String);
//
//        try {
//            for (int i = 0; i <= seq.size() - k; i++) {
//                List<String> window = seq.subList(i, i + k);
//                String shingle = String.join(" ", window).toLowerCase();
//
//                for (int h = 0; h < numHashes; h++) {
//                    String shingleSalted = h + ":" + shingle;
//                    int hash = shingleSalted.hashCode();
//                    if (hash < minHashes[h]) minHashes[h] = hash;
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
        return minHashes;
    }
    public static void main(String[] args) throws NoSuchAlgorithmException {
        DirectedMultigraph<String, DefaultEdge> G  = new DirectedMultigraph<>(DefaultEdge.class);
        DirectedMultigraph<String, DefaultEdge> Gp = new DirectedMultigraph<>(DefaultEdge.class);

        for (String v : new String[]{"A","B","C"})      G.addVertex(v);
        for (String v : new String[]{"A","B","C"}) Gp.addVertex(v);
//        G.addEdge("A","B");
//        G.addEdge("A","C");
//        G.addEdge("B","D");
//        G.addEdge("C","A");
//        G.addEdge("C","D");
//        G.addEdge("D","C");
//
//        Gp.addEdge("A","B");
//        Gp.addEdge("B","A");
//        Gp.addEdge("B","D");
//        Gp.addEdge("C","D");
//        Gp.addEdge("D","C");

        G.addEdge("A","B");
        G.addEdge("A","C");
        G.addEdge("B","C");

        Gp.addEdge("A","B");
        Gp.addEdge("B","C");

        PageRank<String,DefaultEdge> pr1 = new PageRank<>(G, 0.85);
        PageRank<String,DefaultEdge> pr2 = new PageRank<>(Gp, 0.85);

        System.out.println(pr1.getScores());
        System.out.println(pr2.getScores());
        String input = "montexteasgadsfgadfgesaasdddddddddgasdgadfdddsagaddasffgasdddddddddddddddddddddddddddcdfuriser";
        MessageDigest md = MessageDigest.getInstance("SHA3-512");
        byte[] hash = md.digest(input.getBytes());
        String hashString = Base64.getEncoder().encodeToString(hash);
        for (int i = 0; i < 512; i++) {
            int bit = ((hash[i / 8] >> (7 - (i % 8))) & 1);
            System.out.println(bit);
            if((i+1) %  8 == 0) {
                System.out.println("pauise");

            }
        }
        System.out.println(hashString);



        List<String> seq1 = Arrays.asList("je", "suis", "un", "test", "de", "shingle");
        List<String> seq2 = Arrays.asList("de","je", "suis", "un", "test","shingle");

        int[] sig1 = minhashSignature(seq1, 3, 100);
        int[] sig2 = minhashSignature(seq2, 3, 100);

            // Similarité MinHash
        int count = 0;
        for (int i = 0; i < sig1.length; i++) {
            if (sig1[i] == sig2[i]) count++;
        }
        double minhashSim = count / (double) sig1.length;
        System.out.println("MinHash similarity: " + minhashSim);

    }
}
