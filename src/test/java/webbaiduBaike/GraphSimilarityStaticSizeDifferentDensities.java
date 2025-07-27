package webbaiduBaike;

import be.similarity.v1.SignatureSimilarity5;
import be.similarity.v1.VertexRankingSimilarity2;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

import static be.Main.parseGraph;
import static be.similarity.v1.SequenceSimilarityJaccard4.*;
import static be.similarity.v1.SimilarityVEO1.veoSimilarity;
import static be.similarity.v1.vertexEdgeVectorSimilarityVS3.vertexEdgeVectorSimilarity;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class GraphSimilarityStaticSizeDifferentDensities {
    static final DirectedMultigraph<String, DefaultEdge> reference;
    static final PageRank<String, DefaultEdge> pr1;

    static {
        try {
            reference = parseGraph("src/main/resources/web-baidu-baike.txt");
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
        List<File> filtered = new ArrayList<>(Arrays.asList(all));
        return filtered.toArray(new File[0]);
    }

    void benchmarkMethodOnDensities(
            String rootDir,
            double[] densities,
            GraphSimilarityBenchmarkSmallGraphTest.SimilarityFunction func
    ) throws IOException {
        File[] files = getFilesToCompare(rootDir);
        for (Double density : densities) {
            String densityStr = String.format("_%.3f", density); // "_0.200"

            File f1 = Arrays.stream(files)
                    .filter(f -> f.getName().contains(densityStr))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Aucun fichier trouvé pour la densité " + density));
            DirectedMultigraph<String, DefaultEdge> g = parseGraph(f1.toString());
            var pr2 = new PageRank<>(g, 0.85);
            long start = System.nanoTime();
            Object result = func.compute(reference, g, pr2);  // <-- récupère le résultat
            double elapsed = (System.nanoTime() - start) / 1e9;
            NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);

            System.out.printf(
                    "graphe de taille %s, de densité %.2f et avec %s d'arêtes comparés en %.2fs ; Similarité : %s%n",
                    g.vertexSet().size(), density * 100, nf.format(g.edgeSet().size()), elapsed, result
            );
        }
    }

    @Test
    @Tag("densityStaticSize")
    @Order(6)
    public void testVeoSimilarityOnDensities1() throws IOException {
        double[] densities = {0.1, 0.2, 0.5};
        String rootDir = "output/densityStaticGraphs";
        benchmarkMethodOnDensities(rootDir, densities, (g1, g2, pr2) -> veoSimilarity(g1, g2));
    }

    @Test
    @Tag("densityStaticSize")
    @Order(7)
    public void testVertexRankingSimilarity2OnDensities2() throws IOException {
        double[] densities = {0.1, 0.2, 0.5};
        String rootDir = "output/densityStaticGraphs";
        benchmarkMethodOnDensities(rootDir, densities, (g1, g2, pr2) -> new VertexRankingSimilarity2<String, DefaultEdge>().vertexRankingSimilarity(g1, g2, pr1, pr2));
    }

    @Test
    @Tag("densityStaticSize")
    @Order(8)
    public void testVertexEdgeVectorSimilarityOnDensities3() throws IOException {
        double[] densities = {0.1, 0.2, 0.5};
        String rootDir = "output/densityStaticGraphs";
        benchmarkMethodOnDensities(rootDir, densities, (g1, g2, pr2) -> vertexEdgeVectorSimilarity(g1, g2, pr1, pr2));
    }

    @Test
    @Tag("densityStaticSize")
    @Order(9)
    public void testJaccardShingleSimilarityOnDensities4() throws IOException {
        double[] densities = {0.1, 0.2, 0.5};
        String rootDir = "output/densityStaticGraphs";
        benchmarkMethodOnDensities(rootDir, densities, (g1, g2, pr2) -> {
            List<String> seqG1 = serializeGraph(g1, pr1);
            List<String> seqG2 = serializeGraph(g2, pr2);
            Set<String> shG1 = shingles(seqG1, 3);
            Set<String> shG2 = shingles(seqG2, 3);
            return jaccardSimilarity(shG1, shG2);
        });
    }

    @Test
    @Tag("densityStaticSize")
    @Order(10)
    public void testSignatureSimilarityOnDensities5() throws IOException {
        double[] densities = {0.1, 0.2, 0.5};
        String rootDir = "output/densityStaticGraphs";
        benchmarkMethodOnDensities(rootDir, densities, (g1, g2, pr2 ) -> {
            BitSet sig1 = SignatureSimilarity5.computeSignature(g1, pr1);
            BitSet sig2 = SignatureSimilarity5.computeSignature(g2, pr2);
            return SignatureSimilarity5.computeSimilarity(sig1, sig2);
        });
    }
}
