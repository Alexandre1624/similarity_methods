package be.similarity.v1;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.Set;
import java.util.stream.Collectors;

public class SimilarityVEO1 {
    public static <V,E> double veoSimilarity(DirectedMultigraph<V,E> g1, DirectedMultigraph<V,E> g2) {
        Set<V> vs1 = g1.vertexSet();
        Set<V> vs2 = g2.vertexSet();
        long commonV = vs1.stream().filter(vs2::contains).count();

        Set<Pair<V, V>> edges1 = g1.edgeSet().stream()
                .map(e -> Pair.of(g1.getEdgeSource(e), g1.getEdgeTarget(e)))
                .collect(Collectors.toSet());

        Set<Pair<V, V>> edges2 = g2.edgeSet().stream()
                .map(e -> Pair.of(g2.getEdgeSource(e), g2.getEdgeTarget(e)))
                .collect(Collectors.toSet());
        edges1.retainAll(edges2); // intersection des deux ensembles

        int commonE = edges1.size();
        int sizeV1 = vs1.size(), sizeV2 = vs2.size();
        int sizeE1 = g1.edgeSet().size(), sizeE2 = g2.edgeSet().size();

        return 2.0 * (commonV + commonE)
                / (sizeV1 + sizeV2 + sizeE1 + sizeE2);
    }
}
