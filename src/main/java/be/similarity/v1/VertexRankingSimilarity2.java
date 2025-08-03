package be.similarity.v1;

import org.jgrapht.graph.DirectedMultigraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VertexRankingSimilarity2<V, E> {

    public double vertexRankingSimilarity(DirectedMultigraph<V, E> g1, DirectedMultigraph<V, E> g2, Map<String, Double> pr1, Map<String, Double>  pr2) {
        // 1. Calcule l’union des ensembles de sommets des deux graphes.
        Set<V> union = new HashSet<>();
        union.addAll(g1.vertexSet());
        union.addAll(g2.vertexSet());

        // 2. Pour chaque sommet de l’union, on attribue son score de qualité dans chaque graphe (0.0 s’il est absent).
        Map<V, Double> qualityG1 = new HashMap<>();
        Map<V, Double> qualityG2 = new HashMap<>();
        for (V v : union) {
            qualityG1.put(v, g1.containsVertex(v) ? pr1.get(v) : 0.0);
            qualityG2.put(v, g2.containsVertex(v) ? pr2.get(v) : 0.0);
        }

        // 3. On construit et trie la liste des sommets pour chaque graphe, par score décroissant (classement).
        List<V> rankingG1 = new ArrayList<>(g1.vertexSet());
        List<V> rankingG2 = new ArrayList<>(g2.vertexSet());
        rankingG1.sort((a, b) -> Double.compare(qualityG1.get(b), qualityG1.get(a)));
        rankingG2.sort((a, b) -> Double.compare(qualityG2.get(b), qualityG2.get(a)));

        // 4. Calcule le rang de chaque sommet dans chaque graphe.
        //    Si le sommet est absent, il prend le rang maximal (dernière position +1).
        Map<V, Integer> rankG1 = new HashMap<>();
        Map<V, Integer> rankG2 = new HashMap<>();
        int maxRankG1 = g1.vertexSet().size() + 1;
        int maxRankG2 = g2.vertexSet().size() + 1;

        for (int i = 0; i < rankingG1.size(); i++) {
            rankG1.put(rankingG1.get(i), i + 1);
        }
        for (int i = 0; i < rankingG2.size(); i++) {
            rankG2.put(rankingG2.get(i), i + 1);
        }
        for (V v : union) {
            if (!rankG1.containsKey(v)) rankG1.put(v, maxRankG1);
            if (!rankG2.containsKey(v)) rankG2.put(v, maxRankG2);
        }

        // 5. Calcule le poids w_v pour chaque sommet:
        //    - si le sommet existe dans les deux graphes, w_v = moyenne des scores.
        //    - sinon, w_v = score dans le seul graphe où il apparaît.
        Map<V, Double> w = new HashMap<>();
        for (V v : union) {
            double score1 = qualityG1.get(v);
            double score2 = qualityG2.get(v);
            if (g1.containsVertex(v) && g2.containsVertex(v))
                w.put(v, (score1 + score2) / 2.0);
            else
                w.put(v, score1 + score2); // un seul score est non nul
        }

        // 6. Calcule le facteur de normalisation D
        int M = Stream.concat(g1.vertexSet().stream(), g2.vertexSet().stream())
                .collect(Collectors.toSet())
                .size(); // nombre de sommets dans l’union
        double D = 0.0;
        for (V v : union) {
            D += w.get(v) * Math.pow(M - 1, 2);
        }

        // 7. Calcule le numérateur de la formule (somme pondérée des carrés des différences de rangs)
        double numerator = 0.0;
        for (V v : union) {
            int pi1 = rankG1.get(v);
            int pi2 = rankG2.get(v);
            numerator += w.get(v) * Math.pow(pi1 - pi2, 2);
        }

        // 8. Calcule la similarité finale selon la formule normalisée
        double simVR = 1.0 - 2.0 * numerator / D;
        return simVR;
    }
}
