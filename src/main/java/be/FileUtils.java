package be;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class FileUtils {
    public static void ensureDirectoryExists(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        Files.createDirectories(dir);
    }
    public static Map<String, Double> readScoresFromCSV(String filename) throws Exception {
        Map<String, Double> scores = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String node = parts[0];
                    Double score = Double.parseDouble(parts[1]);
                    scores.put(node, score);
                }
            }
        }
        return scores;
    }
    public static void saveScoresToCSV(Map<String, Double> scores, String filename) throws Exception {
        try (PrintWriter writer = new PrintWriter(filename)) {
            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
        }
    }
}