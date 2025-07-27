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
import java.util.concurrent.TimeUnit;

import static be.Main.parseGraph;
import static be.similarity.v1.SequenceSimilarityJaccard4.*;
import static be.similarity.v1.SimilarityVEO1.veoSimilarity;
import static be.similarity.v1.vertexEdgeVectorSimilarityVS3.vertexEdgeVectorSimilarity;
import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GraphSimilarityBenchmarkSmallGraphTest {

	static final DirectedMultigraph<String, DefaultEdge> reference;
	static final PageRank<String, DefaultEdge> pr1;

	static {
		try {
			reference = parseGraph("output/small_graphs/web-baidu-baike_small_00000.txt");
			pr1 = new PageRank<>(reference, 0.85);


			File folder = new File("output/small_graphs");
			File[] files = folder.listFiles();
			int totalEdges = 0;
			int graphCount = 0;

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						DirectedMultigraph<String, DefaultEdge> g = parseGraph(file.toString());
						totalEdges += g.edgeSet().size();
						graphCount++;
					}
				}
			}

			if (graphCount > 0) {
				double moyenne = (double) totalEdges / graphCount;
				System.out.println("Nombre moyen d'arêtes : " + moyenne);
			} else {
				System.out.println("Aucun graphe trouvé.");
			}
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
		double compute(DirectedMultigraph<String, DefaultEdge> g1, DirectedMultigraph<String, DefaultEdge> g2, PageRank<String, DefaultEdge> pr2);
	}

	void benchmarkMethod(String name, SimilarityFunction func) throws IOException, InterruptedException {
		assertNotNull(files, "Le dossier des petits graphes est vide !");
		int count = 0;
		long DUREE = TimeUnit.MINUTES.toNanos(1); // 1 minute
 		long timeOnlyInComparaisonSimilarity = 0;
 		long timeTotal= 0;

		for(int i=0; i<files.length; i++) {
			if (i == files.length - 1) i = 0;

			long begin2 = System.nanoTime();
			DirectedMultigraph<String, DefaultEdge> g = parseGraph(files[i].toString());
			var pr2 = new PageRank<>(g, 0.85);
			long end2 = System.nanoTime();

			long begin = System.nanoTime();
			func.compute(reference, g, pr2);
			long end = System.nanoTime();
			timeOnlyInComparaisonSimilarity += (end - begin);
			timeTotal+= (end2 - begin2);
			count++;

			if (timeOnlyInComparaisonSimilarity > DUREE) break;
		}
		System.out.println("compare only: " + (timeOnlyInComparaisonSimilarity / 1_000_000_000.0) + " s");
		System.out.println("parse + page rank:  " + (timeTotal / 1_000_000_000.0) + " s");
		System.out.printf("[%s] %d graphes comparés 1 min", name, count);
		assertTrue(count > 0, "Aucune comparaison effectuée !");
	}

	@Test
	@Tag("smallGraph")
	@Order(1)
	public void testVeoSimilarity1() throws IOException, InterruptedException {
		benchmarkMethod("veoSimilarity", (g1, g2, pr2) -> veoSimilarity(g1, g2));
	}

	@Test
	@Tag("smallGraph")
	@Order(2)
	public void testVertexRankingSimilarity2() throws IOException, InterruptedException {
		benchmarkMethod("VertexRankingSimilarity", (g1, g2, pr2) ->
		  new VertexRankingSimilarity2<String, DefaultEdge>().vertexRankingSimilarity(g1, g2, pr1, pr2)
		);
	}

	@Test
	@Tag("smallGraph")
	@Order(3)
	public void testVertexEdgeVectorSimilarity3() throws IOException, InterruptedException {
		benchmarkMethod("vertexEdgeVectorSimilarity", (g1, g2, pr2) -> vertexEdgeVectorSimilarity(g1, g2, pr1,pr2));
	}

	@Test
	@Tag("smallGraph")
	@Order(4)
	public void testJaccardShingleSimilarity4() throws IOException, InterruptedException {
		benchmarkMethod("JaccardShingle", (g1, g2,pr2) -> {
			List<String> seqG1 = serializeGraph(g1, pr1);
			List<String> seqG2 = serializeGraph(g2, pr2);
			Set<String> shG1 = shingles(seqG1, 3);
			Set<String> shG2 = shingles(seqG2, 3);
			return jaccardSimilarity(shG1, shG2);
		});
	}

	@Test
	@Tag("smallGraph")
	@Order(5)
	public void testSignatureSimilarity5() throws IOException, InterruptedException {
		benchmarkMethod("SignatureSimilarity", (g1, g2, pr2) -> {
			BitSet sig1 = SignatureSimilarity5.computeSignature(g1, pr1);
			BitSet sig2 = SignatureSimilarity5.computeSignature(g2, pr2);
			return SignatureSimilarity5.computeSimilarity(sig1, sig2);
		});
	}

}
