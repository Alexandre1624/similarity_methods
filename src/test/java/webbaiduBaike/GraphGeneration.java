package webbaiduBaike;

import be.FileUtils;
import be.GraphUtils;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.Test;


import java.io.IOException;
import java.util.*;

import static be.Main.parseGraph;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class GraphGeneration {


    private static  DirectedMultigraph<String, DefaultEdge> base;
    static {
        long maxHeap = Runtime.getRuntime().maxMemory();
        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
        try {
            base = parseGraph("src/main/resources/web-baidu-baike.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 2. Application de modifications des sommets et sauvegarde
     */
    @Test
    public void generateRandomModificationsOnVertexAndSave() throws IOException {
        long maxHeap = Runtime.getRuntime().maxMemory();
        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
        double[] fractions = {0.10, 0.20, 0.50};
        String dir = "output/vertexChangePercentageGraphs";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomVertexRemoval(base, frac);
            String filename = String.format("%s/web-baidu-baike_modified_%02dpct.txt", dir, (int)(frac*100));
            GraphUtils.saveGraph(modif, filename);
            System.out.println("Modif " + (int)(frac*100) + "% : Sommets = " + modif.vertexSet().size() + " | Arêtes = " + modif.edgeSet().size());
        }
    }

    @Test
    public void generateRandomModificationsButKeepEdgeAt14MillionsOnVertexAndSave() throws IOException {
        long maxHeap = Runtime.getRuntime().maxMemory();
        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
        double[] fractions = {0.10, 0.20, 0.50};
        String dir = "output/vertexChangePercentageButKeepEdgeAt14MillionsMaxGraphs";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomVertexRemovalWithEdgeCompensation(base, frac);
            String filename = String.format("%s/web-baidu-baike_modified_%02dpct.txt", dir, (int)(frac*100));
            GraphUtils.saveGraph(modif, filename);
            System.out.println("Modif " + (int)(frac*100) + "% : Sommets = " + modif.vertexSet().size() + " | Arêtes = " + modif.edgeSet().size());
        }
    }

    /**
     * 3. Croissance des temps d’exécution par découpage du graphe (variation taille/densité)
     */
    @Test
    public void generateSubgraphByVertexSize() throws IOException {
        long maxHeap = Runtime.getRuntime().maxMemory();
        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
        int[] sizes = {10000, 100000, 1000000};
        String dir = "output/vertexChangeGraphs";
        FileUtils.ensureDirectoryExists(dir);
        int count = 0;
        for (int size : sizes) {
                DirectedMultigraph<String, DefaultEdge> subgraph = GraphUtils.randomSubgraph(base, size, 1);

                String filename = String.format("%s/web-baidu-baike_sub_%dk_.txt", dir, size/1000);
                GraphUtils.saveGraph(subgraph, filename);
                System.out.printf("Sous-graphe #%d : Sommets = %d | Arêtes = %d%n", ++count, subgraph.vertexSet().size(), subgraph.edgeSet().size());
        }
    }

    /**
     * 4. Impact de la densité sur les performances (taille constante, densité variable)
     */
    @Test
    public void generateDensityImpactAtConstantSize() throws IOException {
        long maxHeap = Runtime.getRuntime().maxMemory();
        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
        int size = base.vertexSet().size();
        double[] densities = {0.1, 0.2, 0.5};
        String dir = "output/densityStaticGraphs";
        FileUtils.ensureDirectoryExists(dir);
        for (double dens : densities) {
            DirectedMultigraph<String, DefaultEdge> subgraph = GraphUtils.subgraphWithEdgePercentage(base, dens);
            String filename = String.format("%s/web-baidu-baike_density_%d_%.3f.txt", dir, size, dens);
            GraphUtils.saveGraph(subgraph, filename);
            System.out.printf("Densité %.3f : Sommets = %d | Arêtes = %d%n", dens, subgraph.vertexSet().size(), subgraph.edgeSet().size());
        }
    }

//    /**
//     * 4. Genere des graphs en fonctions des points d articulations
//     */
//    @Test
//    public void generateAndSaveSubgraphFromArticulationPoints() throws IOException {
//        long maxHeap = Runtime.getRuntime().maxMemory();
//        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
//
//
//        AsUndirectedGraph<String, DefaultEdge> und =
//                new AsUndirectedGraph<>(base);
//        ConnectivityInspector<String, DefaultEdge> insp =
//                new ConnectivityInspector<>(und);
//
//        List<Set<String>> comps = insp.connectedSets();
//        int i = 1;
//        String dir = "output/subsGraphs";
//        Files.createDirectories(Paths.get(dir));
//        for (Set<String> comp : comps) {
//            if (comp.size() < 5) continue;
//            List<String> list = new ArrayList<>(comp);
//            Set<String> subV = new HashSet<>(list.subList(0, list.size()));
//
//            AsSubgraph<String, DefaultEdge> sub =
//                    new AsSubgraph<>(base, subV);
//            String filename = String.format("%s/web-baidu-baike_subgraph_%05d.txt", dir, i);
//            saveGraph(sub, filename);
//            i++;
//        }
//    }


    @Test
    public void generate1000SmallGraphs() throws IOException {
        long maxHeap = Runtime.getRuntime().maxMemory();
        System.out.println("Heap max en Mo: " + (maxHeap / (1024 * 1024)) + " MB");
        int count = 0;
        int n = 100;
        String dir = "output/small_graphs";
        FileUtils.ensureDirectoryExists(dir);
        Set<String> seen = new HashSet<>();

        while (count < 1000) {
            DirectedMultigraph<String, DefaultEdge> sg = GraphUtils.connectedRandomSubgraph(base, n, 1);

            // Crée un identifiant unique pour ce sous-graphe (par exemple, trié par source/target)
            List<String> edgeList = new ArrayList<>();
            for (DefaultEdge e : sg.edgeSet()) {
                String src = sg.getEdgeSource(e);
                String tgt = sg.getEdgeTarget(e);
                // Utilisation de "src->tgt" ou "tgt->src" pour les graphes dirigés
                edgeList.add(src + "->" + tgt);
            }
            Collections.sort(edgeList); // important pour que l'ordre ne change pas l'identifiant
            String subgraphId = String.join(";", edgeList);

            if (!seen.contains(subgraphId) && !sg.vertexSet().isEmpty() && !sg.edgeSet().isEmpty()) {
                seen.add(subgraphId);
                GraphUtils.saveGraph(sg, String.format("%s/web-baidu-baike_small_%05d.txt", dir, count));
                count++;
            }
        }
        System.out.println("Nombre de petits sous-graphes générés et sauvegardés : " + count);
    }


}
