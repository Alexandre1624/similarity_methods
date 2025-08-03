package webbaiduBaike;

import be.similarity.v1.SignatureSimilarity5;
import be.similarity.v1.VertexRankingSimilarity2;
import be.similarity.v1.vertexEdgeVectorSimilarityVS3;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static be.FileUtils.readScoresFromCSV;
import static be.Main.parseGraph;
import static be.similarity.v1.SequenceSimilarityJaccard4.*;
import static be.similarity.v1.SequenceSimilarityJaccard4MinHash.*;
import static be.similarity.v1.SimilarityVEO1.veoSimilarity;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AllTest {

    static final DirectedMultigraph<String, DefaultEdge> reference;
    static final Map<String, Double> pr1;
    static final  List<int[]> hashFunctions400;
    static {
        try {
            hashFunctions400 = generateHashFunctions(400);
            reference = parseGraph("output/small_graphs/web-baidu-baike_small_00000.txt");
            pr1 = readScoresFromCSV("pageRanks/pagerank_scores_web-baidu-baike_small_00000.txt.csv");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private interface SimilarityFunction {
        double compute(DirectedMultigraph<String, DefaultEdge> g1, DirectedMultigraph<String, DefaultEdge> g2,   Map<String, Double> pr1, Map<String, Double> pr2);
    }

    public static final List<Map.Entry<String, SimilarityFunction>> METHODS = List.of(

            Map.entry("veo", (g1, g2, pr1, pr2) -> veoSimilarity(g1, g2)),
            Map.entry("VertexRankingSimilarity", (g1, g2,pr1, pr2) -> new VertexRankingSimilarity2<String, DefaultEdge>().vertexRankingSimilarity(g1, g2, pr1, pr2)),
            Map.entry("vertexEdgeSimilarity", vertexEdgeVectorSimilarityVS3::vertexEdgeVectorSimilarity),
            Map.entry("shingles", (g1, g2, pr1,pr2) -> {
                List<String> seqG1 = serializeGraph(g1, pr1);
                List<String> seqG2 = serializeGraph(g2, pr2);
                Set<String> shG1 = shingles(seqG1, 3);
                Set<String> shG2 = shingles(seqG2, 3);
                return jaccardSimilarity(shG1, shG2);
            }),
            Map.entry("minHash400", (g1, g2, pr1,pr2) -> {

                List<String> seq1 = serializeGraphMinHash(g1, pr1);
                List<String> seq2 = serializeGraphMinHash(g2, pr2);

                Set<Integer> shingles1 = shingleIds(seq1, 3);
                Set<Integer> shingles2 = shingleIds(seq2, 3);

                int[] sig1J = minHashSignature(shingles1, hashFunctions400);
                int[] sig2J = minHashSignature(shingles2, hashFunctions400);

                return estimateMinHashSimilarity(sig1J, sig2J);
            }),
            Map.entry("signature128", (g1, g2, pr1,pr2) -> {
                BitSet sig1 = SignatureSimilarity5.computeSignature(g1, pr1, 128);
                BitSet sig2 = SignatureSimilarity5.computeSignature(g2, pr2, 128);
                return SignatureSimilarity5.computeSimilarity(sig1, sig2, 128);
            }),
            Map.entry("signature512", (g1, g2, pr1, pr2) -> {
                BitSet sig1 = SignatureSimilarity5.computeSignature(g1, pr1, 512);
                BitSet sig2 = SignatureSimilarity5.computeSignature(g2, pr2, 512);
                return SignatureSimilarity5.computeSimilarity(sig1, sig2, 512);
            })
    );

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

    void benchmarkAllMethodsOnIncreasingSize(
            String rootDir,
            List<Map.Entry<String, SimilarityFunction>> methods
    ) throws Exception {


        File[] files = getFilesToCompare(rootDir);
        String outCsv = "results/vertexChangeGraphs/increasingSize_all_methods.csv";
        File outFile = new File(outCsv);
        outFile.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
            // en-tête
            pw.print("filename,vertexes,edges");
            for (var method : methods) pw.print("," + method.getKey());
            pw.println();

            // Pour chaque graphe
            for (File f : files) {
                DirectedMultigraph<String, DefaultEdge> g = parseGraph(f.toString());
                String graphFileName = f.getName();
                String pagerankCsvPath = "pageRanks/pagerank_scores_" + graphFileName + ".csv";
                Map<String, Double> pr2 = readScoresFromCSV(pagerankCsvPath);

                pw.printf(Locale.US,"%s,%d,%d", graphFileName, g.vertexSet().size(), g.edgeSet().size());
                for (var method : methods) {
                    long start = System.nanoTime();
                    method.getValue().compute(reference, g, pr1, pr2);
                    double elapsed = (System.nanoTime() - start) / 1e9;
                    pw.printf(Locale.US,",%.4f", elapsed);
                }
                pw.println();
            }
        }
    }

    void benchmarkAllMethodsOnDensities(
            String referencePath,
            String pr1Path,
            String rootDir,
            String outCsv,
            double[] densities,
            List<Map.Entry<String, SimilarityFunction>> methods,
            boolean printConsole // facultatif, pour choisir d’afficher ou pas
    ) throws Exception {
        DirectedMultigraph<String, DefaultEdge> ref = parseGraph(referencePath);
        Map<String, Double> pr1local = readScoresFromCSV(pr1Path);

        File[] files = getFilesToCompare(rootDir);
        File outFile = new File(outCsv);
        outFile.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
            // En-tête CSV
            pw.print("filename,vertexes,edges,density");
            for (var method : methods) pw.print("," + method.getKey());
            pw.println();

            for (Double density : densities) {
                String densityStr = String.format("_%.3f", density); // "_0.200"
                File f1 = Arrays.stream(files)
                        .filter(f -> f.getName().contains(densityStr))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Aucun fichier trouvé pour la densité " + density));

                DirectedMultigraph<String, DefaultEdge> g = parseGraph(f1.toString());
                String graphFileName = f1.getName();
                String pagerankCsvPath = "pageRanks/pagerank_scores_" + graphFileName + ".csv";
                Map<String, Double> pr2 = readScoresFromCSV(pagerankCsvPath);

                pw.printf(Locale.US, "%s,%d,%d,%.2f", graphFileName, g.vertexSet().size(), g.edgeSet().size(), density*100);
                for (var method : methods) {
                    long start = System.nanoTime();
                    method.getValue().compute(ref, g, pr1local, pr2);
                    double elapsed = (System.nanoTime() - start) / 1e9;
                    if (printConsole)
                        System.out.println(method.getKey() + " elapsed=" + elapsed);
                    pw.printf(Locale.US, ",%.4f", elapsed);
                }
                pw.println();
            }
        }
    }




    void benchmarkAllMethodsExecutionCount(String rootDir,
                                           List<Map.Entry<String, SimilarityFunction>> methods
    ) throws Exception {
        File[] files = getFilesToCompare(rootDir);
        String outCsv = "results/small_graphs/executionTime/all_methods.csv";
        File outFile = new File(outCsv);
        outFile.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
            pw.println("method,count"); // en-tête du CSV

            for (var method : methods) {
                int count = 0;
                long DUREE = TimeUnit.MINUTES.toNanos(1); // 1 minute
                long timeOnlyInComparaisonSimilarity = 0;

                int i = 0;
                while (timeOnlyInComparaisonSimilarity < DUREE) {
                    if (i == files.length) i = 0; // boucle sur les fichiers
                    DirectedMultigraph<String, DefaultEdge> g = parseGraph(files[i].toString());
                    String graphFileName = files[i].getName();
                    String pagerankCsvPath = "pageRanks/pagerank_scores_" + graphFileName + ".csv";
                    Map<String, Double> pr2 = readScoresFromCSV(pagerankCsvPath);

                    long begin = System.nanoTime();
                    method.getValue().compute(reference, g, pr1, pr2);
                    long end = System.nanoTime();

                    timeOnlyInComparaisonSimilarity += (end - begin);
                    count++;
                    i++;

                    if (timeOnlyInComparaisonSimilarity > DUREE) break;
                }
                System.out.printf("[%s] %d graphes comparés en 1 min%n", method.getKey(), count);
                pw.printf("%s,%d%n", method.getKey(), count);
            }
        }
    }

    @Test
    public void smallGraphAllInOneFile() throws Exception {
        String rootDir = "output/small_graphs";
        benchmarkAllMethodsExecutionCount(rootDir, METHODS);

    }


    @Test
    @Tag("vertexChangeGraphs")
    @Order(9)
    public void increasingSize() throws Exception {
        String rootDir = "output/vertexChangeGraphs";
        benchmarkAllMethodsOnIncreasingSize(rootDir, METHODS);

    }

    @Test
    @Tag("vertexChangePercentage")
    @Order(10)
    public void vertexChanges() throws Exception {
        double[] densities = {0.1, 0.2, 0.5};
        benchmarkAllMethodsOnDensities(
                "src/main/resources/web-baidu-baike.txt",
                "pageRanks/pagerank_scores_web-baidu-baike.txt.csv",
                "output/vertexChangePercentageGraphs",
                "results/vertexChangePercentageGraphs/density_all_methods.csv",
                densities,
                METHODS,
                true
        );
    }


    @Test
    @Tag("densityStaticSize")
    @Order(11)
    public void densityStaticSize() throws Exception {
        double[] densities = {0.1, 0.2, 0.5};
        benchmarkAllMethodsOnDensities(
                "src/main/resources/web-baidu-baike.txt",
                "pageRanks/pagerank_scores_web-baidu-baike.txt.csv",
                "output/densityStaticGraphs",
                "results/densityStaticGraphs/density_all_methods.csv",
                densities,
                METHODS,
                true
        );
    }
}
