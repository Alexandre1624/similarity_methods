package be.similarity.v1;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.*;
import java.util.stream.Collectors;

public class vertexEdgeVectorSimilarityVS3 {
    public static double vertexEdgeVectorSimilarity(
            DirectedMultigraph<String, DefaultEdge> G,
            DirectedMultigraph<String, DefaultEdge> Gp,
            PageRank<String, DefaultEdge> prG,
            PageRank<String, DefaultEdge> prGp) {

        // Scores de qualité (PageRank)
        Map<String, Double> qG = prG.getScores();
        Map<String, Double> qGp = prGp.getScores();

        // Pré-indexer les voisins sortants pour G et Gp
        Map<String, Set<String>> outNeighborsG = new HashMap<>();
        for (String u : G.vertexSet()) {
            outNeighborsG.put(u, G.outgoingEdgesOf(u).stream()
                    .map(G::getEdgeTarget).collect(Collectors.toSet()));
        }
        Map<String, Set<String>> outNeighborsGp = new HashMap<>();
        for (String u : Gp.vertexSet()) {
            outNeighborsGp.put(u, Gp.outgoingEdgesOf(u).stream()
                    .map(Gp::getEdgeTarget).collect(Collectors.toSet()));
        }

        // Construire l’union des arêtes (u,v)
        Set<PairCusstom> unionEdges = new HashSet<>();
        for (DefaultEdge e : G.edgeSet()) {
            String u = G.getEdgeSource(e), v = G.getEdgeTarget(e);
            unionEdges.add(new PairCusstom(u, v));
        }
        for (DefaultEdge e : Gp.edgeSet()) {
            String u = Gp.getEdgeSource(e), v = Gp.getEdgeTarget(e);
            unionEdges.add(new PairCusstom(u, v));
        }

        // Calcul du score
        double totalNormDiff = 0.0;
        for (PairCusstom uv : unionEdges) {
            String u = uv.u, v = uv.v;

            double qu  = qG.getOrDefault(u, 0.0);
            double qup = qGp.getOrDefault(u, 0.0);

            Set<String> outG = outNeighborsG.getOrDefault(u, Collections.emptySet());
            Set<String> outGp = outNeighborsGp.getOrDefault(u, Collections.emptySet());

            int sumOutG_u = outG.size();
            int sumOutGp_u = outGp.size();

            int outG_uv = outG.contains(v) ? 1 : 0;
            int outGp_uv = outGp.contains(v) ? 1 : 0;

            double gamma   = (sumOutG_u  > 0) ? qu  * outG_uv  / sumOutG_u  : 0.0;
            double gamma_p = (sumOutGp_u > 0) ? qup * outGp_uv / sumOutGp_u : 0.0;

            double maxγ = Math.max(gamma, gamma_p);
            double normDiff = (maxγ > 0)
                    ? Math.abs(gamma - gamma_p) / maxγ
                    : 0.0;

            totalNormDiff += normDiff;
        }

        int m = unionEdges.size();
        return 1.0 - (m > 0 ? (totalNormDiff / m) : 0.0);
    }

    private static class PairCusstom {
        final String u, v;
        PairCusstom(String u, String v) { this.u = u; this.v = v; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof PairCusstom)) return false;
            PairCusstom p = (PairCusstom)o;
            return u.equals(p.u) && v.equals(p.v);
        }
        @Override public int hashCode() {
            return 31*u.hashCode() + v.hashCode();
        }
    }

}
