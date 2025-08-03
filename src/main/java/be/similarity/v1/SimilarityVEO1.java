package be.similarity.v1;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DirectedMultigraph;

import java.util.Set;
import java.util.stream.Collectors;

public class SimilarityVEO1 {
    // Calcule la similarité VEO (Vertex/Edge Overlap) entre deux graphes dirigés.
    // VEO mesure la proportion de sommets et d'arêtes partagés par rapport à l'ensemble des sommets et arêtes des deux graphes.
    public static <V,E> double veoSimilarity(DirectedMultigraph<V,E> g1, DirectedMultigraph<V,E> g2) {
        // On récupère les ensembles de sommets de chaque graphe.
        Set<V> vs1 = g1.vertexSet();
        Set<V> vs2 = g2.vertexSet();

        // On compte le nombre de sommets communs (intersection des deux ensembles).
        long commonV = vs1.stream().filter(vs2::contains).count();

        // On construit l'ensemble des arêtes de g1 sous forme de paires (source, cible).
        Set<Pair<V, V>> edges1 = g1.edgeSet().stream()
                .map(e -> Pair.of(g1.getEdgeSource(e), g1.getEdgeTarget(e)))
                .collect(Collectors.toSet());

        // Idem pour g2.
        Set<Pair<V, V>> edges2 = g2.edgeSet().stream()
                .map(e -> Pair.of(g2.getEdgeSource(e), g2.getEdgeTarget(e)))
                .collect(Collectors.toSet());

        // On garde uniquement les arêtes communes aux deux graphes (intersection des ensembles de paires).
        edges1.retainAll(edges2); // intersection

        // Nombre d'arêtes communes.
        int commonE = edges1.size();

        // Tailles des ensembles de sommets et d'arêtes pour chaque graphe.
        int sizeV1 = vs1.size(), sizeV2 = vs2.size();
        int sizeE1 = g1.edgeSet().size(), sizeE2 = g2.edgeSet().size();

        // Calcul de la similarité VEO selon la formule
        // simVEO = 2 * (nb sommets communs + nb arêtes communes) / (total sommets + total arêtes)
        return 2.0 * (commonV + commonE)
                / (sizeV1 + sizeV2 + sizeE1 + sizeE2);
    }
}
