package be.similarity.v1;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class vertexEdgeVectorSimilarityVS3 {
    public static double vertexEdgeVectorSimilarity(
            DirectedMultigraph<String, DefaultEdge> G,
            DirectedMultigraph<String, DefaultEdge> Gp, PageRank<String, DefaultEdge> prG, PageRank<String, DefaultEdge> prGp) {

        // Scores de qualité (PageRank)
        Map<String,Double> qG  = prG.getScores();
        Map<String,Double> qGp = prGp.getScores();

        // Construire E ∪ E' en ne prenant que les arêtes sortantes
        Set<PairCusstom> unionEdges = new HashSet<>();
        for (DefaultEdge e : G.edgeSet()) {
            String u = G.getEdgeSource(e), v = G.getEdgeTarget(e);
            unionEdges.add(new PairCusstom(u, v));
        }
        for (DefaultEdge e : Gp.edgeSet()) {
            String u = Gp.getEdgeSource(e), v = Gp.getEdgeTarget(e);
            unionEdges.add(new PairCusstom(u, v));
        }

        // Pour chaque (u,v) de l’union, calculer γ et la différence normalisée
        double totalNormDiff = 0.0;
        for (PairCusstom uv : unionEdges) {
            String u = uv.u, v = uv.v;

            double qu   = qG.getOrDefault(u,  0.0);
            double qup  = qGp.getOrDefault(u, 0.0);

            // #outlinks(u->v) dans G et Gp
            double outG_uv  = Optional.ofNullable(G.getAllEdges(u,  v)).map(Set::size).orElse(0);
            double outGp_uv = Optional.ofNullable(Gp.getAllEdges(u, v)).map(Set::size).orElse(0);

            // Dénominateurs : total des sorties de u dans chaque graphe
            double sumOutG_u = 0;
            try {
                sumOutG_u = Gp.outgoingEdgesOf(u)
                        .stream()
                        .filter(eGp -> {
                            String ve = Gp.getEdgeTarget(eGp);
                            return G.containsEdge(u, ve);
                        })
                        .count();
            } catch (IllegalArgumentException ex) {
                sumOutG_u = 0;
            }

            double sumOutGp_u = 0;
            try {
                sumOutGp_u = G.outgoingEdgesOf(u)
                        .stream()
                        .filter(eGp -> {
                            String ve = G.getEdgeTarget(eGp);
                            return Gp.containsEdge(u, ve);
                        })
                        .count();
            } catch (IllegalArgumentException ex) {
                sumOutGp_u = 0;
            }

            double gamma   = (sumOutG_u  > 0) ? qu  * outG_uv  / sumOutG_u  : 0.0;
            double gamma_p = (sumOutGp_u > 0) ? qup * outGp_uv / sumOutGp_u : 0.0;

            // diff normalisée pour cet arc
            double maxγ = Math.max(gamma, gamma_p);
            double normDiff = (maxγ > 0)
                    ? Math.abs(gamma - gamma_p) / maxγ
                    : 0.0;

            totalNormDiff += normDiff;
        }

        int m = unionEdges.size();
        return 1.0 - (totalNormDiff / m);
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
