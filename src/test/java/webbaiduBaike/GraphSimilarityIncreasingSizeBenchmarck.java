package webbaiduBaike;

import be.similarity.v1.SignatureSimilarity5;
import be.similarity.v1.VertexRankingSimilarity2;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static be.Main.parseGraph;
import static be.similarity.v1.SequenceSimilarityJaccard4.*;
import static be.similarity.v1.SimilarityVEO1.veoSimilarity;
import static be.similarity.v1.vertexEdgeVectorSimilarityVS3.vertexEdgeVectorSimilarity;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GraphSimilarityIncreasingSizeBenchmarck {
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
    static File[] getFilesToCompare(String rootDir) {
        String dirPath = String.format("%s/", rootDir);
        File dir = new File(dirPath);
        File[] all = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (all == null) return new File[0];
        Arrays.sort(all, Comparator.comparing(File::getName));
        // tu peux aussi filtrer le fichier de référence si besoin, comme avant
        List<File> filtered = new ArrayList<>();
        for (File f : all) {
            if (!f.getName().equals("web-baidu-baike_small_00000.txt")) {
                filtered.add(f);
            }
        }
        return filtered.toArray(new File[0]);
    }

    static DirectedMultigraph<String, DefaultEdge> removeAllEdges(DirectedMultigraph<String, DefaultEdge> g) {
        DirectedMultigraph<String, DefaultEdge> empty = new DirectedMultigraph<>(DefaultEdge.class);
        // Ajoute tous les sommets, pas les arêtes
        for (String v : g.vertexSet()) {
            empty.addVertex(v);
        }
        return empty;
    }
    void benchmarkMethodOnIncreasingSize(
            String rootDir,
            GraphSimilarityBenchmarkSmallGraphTest.SimilarityFunction func
    ) throws IOException {
        File[] files = getFilesToCompare(rootDir);
        for (File f : files) {
            DirectedMultigraph<String, DefaultEdge> g = parseGraph(f.toString());
//            DirectedMultigraph<String, DefaultEdge> gNoEdges = removeAllEdges(g);

            var pr2 = new PageRank<>(g, 0.85);
            long start = System.nanoTime();
            func.compute(reference, g, pr2);
            double elapsed = (System.nanoTime() - start) / 1e9;
            System.out.printf("graphe taille=%s noeuds, arêtes=%s comparé en %.2f s%n",
                    g.vertexSet().size(), g.edgeSet().size(), elapsed);
        }
    }

    @Test
    @Tag("VertexChangeWith100%Edges")
    @Order(6)
    public void testVeoSimilarityOnIncreasingSize1() throws IOException {
        String rootDir = "output/vertexChangeGraphs";
        benchmarkMethodOnIncreasingSize(rootDir, (g1, g2, pr2) -> veoSimilarity(g1, g2));
    }

    @Test
    @Tag("VertexChangeWith100%Edges")
    @Order(7)
    public void testVertexRankingSimilarityOnIncreasingSize2() throws IOException {
        String rootDir = "output/vertexChangeGraphs";
        benchmarkMethodOnIncreasingSize(rootDir, (g1, g2, pr2) -> new VertexRankingSimilarity2<String, DefaultEdge>().vertexRankingSimilarity(g1, g2, pr1, pr2));
    }

    @Test
    @Tag("VertexChangeWith100%Edges")
    @Order(8)
    public void testVertexEdgeVectorSimilarityOnIncreasingSize3() throws IOException {
        String rootDir = "output/vertexChangeGraphs";
        benchmarkMethodOnIncreasingSize(rootDir, (g1, g2, pr2) -> vertexEdgeVectorSimilarity(g1, g2, pr1, pr2));
    }

    @Test
    @Tag("VertexChangeWith100%Edges")
    @Order(9)
    public void testJaccardShingleSimilarityOnIncreasingSize4() throws IOException {
        String rootDir = "output/vertexChangeGraphs";
        benchmarkMethodOnIncreasingSize(rootDir, (g1, g2, pr2) -> {
            List<String> seqG1 = serializeGraph(g1, pr1);
            List<String> seqG2 = serializeGraph(g2, pr2);
            Set<String> shG1 = shingles(seqG1, 3);
            Set<String> shG2 = shingles(seqG2, 3);
            return jaccardSimilarity(shG1, shG2);
        });
    }

    @Test
    @Tag("VertexChangeWith100%Edges")
    @Order(10)
    public void testSignatureSimilarityOnIncreasingSize5() throws IOException {
        String rootDir = "output/vertexChangeGraphs";
        benchmarkMethodOnIncreasingSize(rootDir, (g1, g2, pr2 ) -> {
            BitSet sig1 = SignatureSimilarity5.computeSignature(g1, pr1);
            BitSet sig2 = SignatureSimilarity5.computeSignature(g2, pr2);
            return SignatureSimilarity5.computeSimilarity(sig1, sig2);
        });
    }
}
