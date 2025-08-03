package be.similarity.v1;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.*;
import java.util.stream.Collectors;

public class vertexEdgeVectorSimilarityVS3 {
    public static double vertexEdgeVectorSimilarity(
            DirectedMultigraph<String, DefaultEdge> G,
            DirectedMultigraph<String, DefaultEdge> Gp,
            Map<String, Double> prG,
            Map<String, Double> prGp) {

        // 1. On pré-calcule pour chaque sommet l’ensemble de ses voisins sortants dans G et dans Gp.
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

        // 2. On construit l’union de toutes les arêtes (u,v) présentes dans G ou dans Gp.
        //    Cela garantit qu’on compare toutes les arêtes, même si elles n’existent que dans un seul graphe.
        Set<PairCusstom> unionEdges = new HashSet<>();
        for (DefaultEdge e : G.edgeSet()) {
            String u = G.getEdgeSource(e), v = G.getEdgeTarget(e);
            unionEdges.add(new PairCusstom(u, v));
        }
        for (DefaultEdge e : Gp.edgeSet()) {
            String u = Gp.getEdgeSource(e), v = Gp.getEdgeTarget(e);
            unionEdges.add(new PairCusstom(u, v));
        }

        // 3. On calcule, pour chaque arête de l’union, sa pondération normalisée dans G et dans Gp.
        double totalNormDiff = 0.0;
        for (PairCusstom uv : unionEdges) {
            String u = uv.u, v = uv.v;

            // Récupère la qualité du sommet source u dans G et dans Gp (souvent un score PageRank).
            double qu  = prG.getOrDefault(u, 0.0);
            double qup = prGp.getOrDefault(u, 0.0);

            // Récupère les voisins sortants de u dans G et Gp.
            Set<String> outG = outNeighborsG.getOrDefault(u, Collections.emptySet());
            Set<String> outGp = outNeighborsGp.getOrDefault(u, Collections.emptySet());

            // Nombre total de voisins sortants.
            int sumOutG_u = outG.size();
            int sumOutGp_u = outGp.size();

            // L’arête (u,v) existe-t-elle ? (1 si oui, 0 sinon)
            int outG_uv = outG.contains(v) ? 1 : 0;
            int outGp_uv = outGp.contains(v) ? 1 : 0;

            // Calcul du poids gamma (voir l’article de Papadimitriou)
            // gamma(u,v) = qualité(u) * présence(u,v) / degré sortant(u)
            double gamma   = (sumOutG_u  > 0) ? qu  * outG_uv  / sumOutG_u  : 0.0;
            double gamma_p = (sumOutGp_u > 0) ? qup * outGp_uv / sumOutGp_u : 0.0;

            // Différence relative (normalisée) entre les poids dans G et Gp.
            double maxγ = Math.max(gamma, gamma_p);
            double normDiff = (maxγ > 0)
                    ? Math.abs(gamma - gamma_p) / maxγ
                    : 0.0;

            // On cumule la différence normalisée pour toutes les arêtes.
            totalNormDiff += normDiff;
        }

        // 4. Moyenne des différences normalisées sur toutes les arêtes.
        int m = unionEdges.size();
        // 5. La similarité globale : 1 - (différence moyenne). Valeur entre 0 (très différent) et 1 (identique).
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
