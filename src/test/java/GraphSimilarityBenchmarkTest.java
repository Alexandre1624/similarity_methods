import be.similarity.v1.SignatureSimilarity5;
import be.similarity.v1.VertexRankingSimilarity2;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static be.Main.parseGraph;
import static be.similarity.v1.SequenceSimilarityJaccard4.*;
import static be.similarity.v1.SimilarityVEO1.veoSimilarity;
import static be.similarity.v1.vertexEdgeVectorSimilarityVS3.vertexEdgeVectorSimilarity;
import static org.junit.jupiter.api.Assertions.*;

public class GraphSimilarityBenchmarkTest {

    static final DirectedMultigraph<String, DefaultEdge> reference;
    static final PageRank<String, DefaultEdge> pr1;

    static {
        try {
            reference = parseGraph("output/small_graphs/web-baidu-baike_small_00000.txt");
            pr1 = new PageRank<>(reference, 0.85);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static final File[] files = getFilesToCompare();

    static File[] getFilesToCompare() {
        File dir = new File("output/small_graphs");
        File[] all = dir.listFiles((d, name) -> name.endsWith(".txt"));
        // Trie et enlève le fichier 0
        Arrays.sort(all, Comparator.comparing(File::getName));
        List<File> filtered = new ArrayList<>();
        for (File f : all) {
            if (!f.getName().equals("web-baidu-baike_small_00000.txt")) {
                filtered.add(f);
            }
        }
        return filtered.toArray(new File[0]);
    }
    interface SimilarityFunction {
        double compute(DirectedMultigraph<String, DefaultEdge> g1, DirectedMultigraph<String, DefaultEdge> g2);
    }

    void benchmarkMethod(String name, SimilarityFunction func) throws IOException {
        assertNotNull(files, "Le dossier des petits graphes est vide !");
        int count = 0;
        long start = System.nanoTime();
        long limit = start + TimeUnit.MINUTES.toNanos(1);

        for (File f : files) {
            if (System.nanoTime() > limit) break;
            DirectedMultigraph<String, DefaultEdge> g = parseGraph(f.toString());
            func.compute(reference, g); // appel de la méthode de similarité
            count++;
        }
        double elapsed = (System.nanoTime() - start) / 1e9;
        System.out.printf("[%s] %d graphes comparés en %.2f s%n", name, count, elapsed);
        assertTrue(count > 0, "Aucune comparaison effectuée !");
    }

    @Test
    public void testVeoSimilarity() throws IOException {
        benchmarkMethod("veoSimilarity", (g1, g2) -> veoSimilarity(g1, g2));
    }

    @Test
    public void testVertexRankingSimilarity2() throws IOException {
        benchmarkMethod("VertexRankingSimilarity2", (g1, g2) ->
                new VertexRankingSimilarity2<String, DefaultEdge>().vertexRankingSimilarity(g1, g2, pr1)
        );
    }

    @Test
    public void testVertexEdgeVectorSimilarity() throws IOException {
        benchmarkMethod("vertexEdgeVectorSimilarity", (g1, g2) -> vertexEdgeVectorSimilarity(g1, g2, pr1));
    }

    @Test
    public void testJaccardShingleSimilarity() throws IOException {
        benchmarkMethod("JaccardShingle", (g1, g2) -> {
            List<String> seqG1 = serializeGraph(g1, pr1);
            List<String> seqG2 = serializeGraph(g2, null);
            Set<String> shG1 = shingles(seqG1, 3);
            Set<String> shG2 = shingles(seqG2, 3);
            return jaccardSimilarity(shG1, shG2);
        });
    }

    @Test
    public void testSignatureSimilarity5() throws IOException {
        benchmarkMethod("SignatureSimilarity5", (g1, g2) -> {
            BitSet sig1 = SignatureSimilarity5.computeSignature(g1, pr1);
            BitSet sig2 = SignatureSimilarity5.computeSignature(g2, null);
            return SignatureSimilarity5.computeSimilarity(sig1, sig2);
        });
    }
}
