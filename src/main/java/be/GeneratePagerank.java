package be;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static be.Main.parseGraph;

public class GeneratePagerank {


    public static List<File> getAllTxtFiles(File dir) {
        List<File> result = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                result.addAll(getAllTxtFiles(file)); // récursion
            } else if (file.getName().endsWith(".txt")) {
                result.add(file);
            }
        }
        return result;
    }
    public static void main(String[] args) throws Exception {

        List<String> dossierPaths = Arrays.asList(
                "output/small_graphs"
        );

        List<File> allTxtFiles = new ArrayList<>();
        for (String path : dossierPaths) {
            allTxtFiles.addAll(getAllTxtFiles(new File(path)));
        }
        allTxtFiles.add(new File("src/main/resources/web-baidu-baike.txt"));
        FileUtils.ensureDirectoryExists("pageRanks");
        for (File f : allTxtFiles) {
            System.out.println(f.getName());
            DirectedMultigraph<String, DefaultEdge>  reference = parseGraph(f.getAbsolutePath());
            PageRank<String, DefaultEdge>  pr1 = new PageRank<>(reference, 0.85);
            FileUtils.saveScoresToCSV(pr1.getScores(), "pageRanks/pagerank_scores_"+ f.getName() + ".csv");
        }
    }
}
