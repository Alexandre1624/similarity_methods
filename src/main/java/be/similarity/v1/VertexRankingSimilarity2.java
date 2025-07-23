package be.similarity.v1;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.*;
import java.util.stream.Stream;

public class VertexRankingSimilarity2<V, E> {

        public double vertexRankingSimilarity(DirectedMultigraph<V, E> g1, DirectedMultigraph<V, E> g2, PageRank<V, E> pr1, PageRank<V, E> pr2) {
            //Union des sommets
            Set<V> union = new HashSet<>();
            union.addAll(g1.vertexSet());
            union.addAll(g2.vertexSet());

            Map<V, Double> qualityG1 = new HashMap<>();
            Map<V, Double> qualityG2 = new HashMap<>();
            for (V v : union) {
                qualityG1.put(v, g1.containsVertex(v) ? pr1.getVertexScore(v) : 0.0);
                qualityG2.put(v, g2.containsVertex(v) ? pr2.getVertexScore(v) : 0.0);
            }

            //Construction des listes triées (classement) pour chaque graphe
            List<V> rankingG1 = new ArrayList<>(g1.vertexSet());
            List<V> rankingG2 = new ArrayList<>(g2.vertexSet());
            rankingG1.sort((a, b) -> Double.compare(qualityG1.get(b), qualityG1.get(a)));
            rankingG2.sort((a, b) -> Double.compare(qualityG2.get(b), qualityG2.get(a)));

            //Calcul des rangs (sommets absents prennent le rang max)
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

            //Calcul du poids (w_v)
            Map<V, Double> w = new HashMap<>();
            for (V v : union) {
                double score1 = qualityG1.get(v);
                double score2 = qualityG2.get(v);
                if (g1.containsVertex(v) && g2.containsVertex(v))
                    w.put(v, (score1 + score2) / 2.0);
                else
                    w.put(v, score1 + score2); // un seul score est non nul
            }

            //Calcul de D (normalisation)  int M = Stream.concat(g1.vertexSet().stream(), g2.vertexSet().stream())
            //                    .collect(Collectors.toSet()).size();
            int M = (int) Stream.concat(g1.vertexSet().stream(), g2.vertexSet().stream()).count();
            double D = 0.0;
            for (V v : union) {
                D += w.get(v) * Math.pow(M - 1, 2);
            }

            //Numérateur
            double numerator = 0.0;
            for (V v : union) {
                int pi1 = rankG1.get(v);
                int pi2 = rankG2.get(v);
                numerator += w.get(v) * Math.pow(pi1 - pi2, 2);
            }

            // Similarité finale
            double simVR = 1.0 - 2.0 * numerator / D;
            return simVR;
        }
}
