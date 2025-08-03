package Gnutella;

import be.similarity.v1.SignatureSimilarity5;
import be.similarity.v1.VertexRankingSimilarity2;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static be.Main.parseGraph;
import static be.similarity.v1.SequenceSimilarityJaccard4.*;
import static be.similarity.v1.SequenceSimilarityJaccard4MinHash.*;
import static be.similarity.v1.SimilarityVEO1.veoSimilarity;
import static be.similarity.v1.vertexEdgeVectorSimilarityVS3.vertexEdgeVectorSimilarity;

public class testAllSimilarities {


    static final DirectedMultigraph<String, DefaultEdge> reference;
    static final PageRank<String, DefaultEdge> pr1;
    static final  List<int[]> hashFunctions400 = generateHashFunctions(400);

    static {
        try {
            reference = parseGraph("src/main/resources/p2p-Gnutella31.txt");
            pr1 = new PageRank<>(reference, 0.85);
            System.out.println("graph de référence vertex: " + reference.vertexSet().size() + " edges: " + reference.edgeSet().size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void batchAllSimilaritiesAllCases() throws IOException {
        String[] allCases = {
                "removeRandomVertices",
                "removeRandomEdges",
                "RemoveConnectedSubgraph",
                "addRandomEdges",
                "addDenseSubgraph",
                "rewireEdges",
                "invertEdges"
        };
        for (String cas : allCases) {
            String dir = "output/similarity/" + cas;
            batchOneCase(dir, cas);
        }
    }

    private void batchOneCase(String rootDir, String caseName) {
        File[] files = getFilesToCompare(rootDir);
        String csv = String.format("output/results_%s.csv", caseName);
        try (FileWriter fw = new FileWriter(csv)) {
            fw.write("file,nodes,edges,veo,vertexRanking,vertexEdgeVector,shingle,minHashJaccard400,signature128,signature512\n");
            for (File f : files) {
                DirectedMultigraph<String, DefaultEdge> g = parseGraph(f.toString());
                PageRank<String, DefaultEdge> pr2 = new PageRank<>(g, 0.85);
                double veo = veoSimilarity(reference, g);
                double vr = new VertexRankingSimilarity2<String, DefaultEdge>().vertexRankingSimilarity(reference, g, pr1.getScores(), pr2.getScores());
                double vs = vertexEdgeVectorSimilarity(reference, g, pr1.getScores(), pr2.getScores());
                List<String> seqG1 = serializeGraph(reference, pr1.getScores());
                List<String> seqG2 = serializeGraph(g, pr2.getScores());
                double shingle = jaccardSimilarity(shingles(seqG1, 3), shingles(seqG2, 3));
                List<String> seq1 = serializeGraphMinHash(reference, pr1.getScores());
                List<String> seq2 = serializeGraphMinHash(g, pr2.getScores());
                Set<Integer> shingles1 = shingleIds(seq1, 3);
                Set<Integer> shingles2 = shingleIds(seq2, 3);

                // Pour MinHash 400 hash functions
                int[] sig1J400 = minHashSignature(shingles1, hashFunctions400);
                int[] sig2J400 = minHashSignature(shingles2, hashFunctions400);
                double minHashJaccard400 = estimateMinHashSimilarity(sig1J400, sig2J400);

                // Signature similarity (512 et 128)
                BitSet sig1_512 = SignatureSimilarity5.computeSignature(reference, pr1.getScores(), 512);
                BitSet sig2_512 = SignatureSimilarity5.computeSignature(g, pr2.getScores(), 512);
                double signature512 = SignatureSimilarity5.computeSimilarity(sig1_512, sig2_512, 512);

                BitSet sig1_128 = SignatureSimilarity5.computeSignature(reference, pr1.getScores(), 128);
                BitSet sig2_128 = SignatureSimilarity5.computeSignature(g, pr2.getScores(), 128);
                double signature128 = SignatureSimilarity5.computeSimilarity(sig1_128, sig2_128, 128);

                fw.write(String.format(Locale.US, "%s,%d,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f\n",
                        f.getName(), g.vertexSet().size(), g.edgeSet().size(),
                        veo, vr, vs, shingle, minHashJaccard400, signature128, signature512 ));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Résultats enregistrés dans " + csv);
    }

    static File[] getFilesToCompare(String rootDir) {
        String dirPath = String.format("%s/", rootDir);
        File dir = new File(dirPath);
        File[] all = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (all == null) return new File[0];
        Arrays.sort(all, Comparator.comparing(File::getName));
        List<File> filtered = new ArrayList<>(Arrays.asList(all));
        return filtered.toArray(new File[0]);
    }
}
