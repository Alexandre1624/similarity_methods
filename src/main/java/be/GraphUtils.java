package be;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.BiconnectivityInspector;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class GraphUtils {

    public static DirectedMultigraph<String, DefaultEdge> applyRandomVertexRemoval(DirectedMultigraph<String, DefaultEdge> original, double vertexFraction) {
        // Clone du graphe pour ne pas modifier l'original
        DirectedMultigraph<String, DefaultEdge> g = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : original.vertexSet()) g.addVertex(v);
        for (DefaultEdge e : original.edgeSet()) {
            g.addEdge(original.getEdgeSource(e), original.getEdgeTarget(e));
        }

        Random rand = new Random();

        // Suppression de sommets uniquement
        List<String> vertices = new ArrayList<>(g.vertexSet());
        int nbToRemove = (int) (vertexFraction * vertices.size());
        Collections.shuffle(vertices, rand);
        for (int i = 0; i < nbToRemove; i++) {
            g.removeVertex(vertices.get(i));
        }

        return g;
    }

    public static DirectedMultigraph<String, DefaultEdge> applyRandomVertexRemovalWithEdgeCompensation(
            DirectedMultigraph<String, DefaultEdge> original, double vertexFraction) {

        DirectedMultigraph<String, DefaultEdge> g = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : original.vertexSet()) g.addVertex(v);
        for (DefaultEdge e : original.edgeSet()) {
            g.addEdge(original.getEdgeSource(e), original.getEdgeTarget(e));
        }

        Random rand = new Random();

        // Suppression aléatoire de sommets
        List<String> vertices = new ArrayList<>(g.vertexSet());
        int nbToRemove = (int) (vertexFraction * vertices.size());
        Collections.shuffle(vertices, rand);
        for (int i = 0; i < nbToRemove; i++) {
            g.removeVertex(vertices.get(i));
        }

        int originalEdgeCount = original.edgeSet().size();
        int remainingEdgeCount = g.edgeSet().size();
        final int MAX_EDGES = original.edgeSet().size();
        int toAdd = Math.min(originalEdgeCount - remainingEdgeCount, MAX_EDGES - remainingEdgeCount);

        if (toAdd > 0) {
            List<String> remainingVertices = new ArrayList<>(g.vertexSet());
            int n = remainingVertices.size();
            int added = 0, attempts = 0, maxAttempts = 10 * toAdd; // Evite boucle infinie
            while (added < toAdd && attempts < maxAttempts) {
                String src = remainingVertices.get(rand.nextInt(n));
                String tgt = remainingVertices.get(rand.nextInt(n));
                if (!src.equals(tgt) && !g.containsEdge(src, tgt)) {
                    g.addEdge(src, tgt);
                    added++;
                }
                attempts++;
            }

            if (added < toAdd) System.out.println("Arêtes ajoutées : " + added + " (sur " + toAdd + "), impossible d'en ajouter plus.");
        }

        return g;
    }

    public static void saveGraph(DirectedMultigraph<String, DefaultEdge> graph, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            // Sauve toutes les arêtes
            for (DefaultEdge edge : graph.edgeSet()) {
                String source = graph.getEdgeSource(edge);
                String target = graph.getEdgeTarget(edge);
                pw.println(source + " " + target);
            }
            // Sauve les sommets isolés (aucune arête entrante ni sortante)
            for (String v : graph.vertexSet()) {
                if (graph.inDegreeOf(v) == 0 && graph.outDegreeOf(v) == 0) {
                    pw.println(v);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DirectedMultigraph<String, DefaultEdge> connectedRandomSubgraph(
            DirectedMultigraph<String, DefaultEdge> base, int n, double edgeFraction) {

        List<String> allVertices = new ArrayList<>(base.vertexSet());
        Collections.shuffle(allVertices);

        // On part d'un sommet au hasard
        String start = allVertices.get(0);

        Set<String> selectedVertices = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        selectedVertices.add(start);
        queue.add(start);

        // On agrandit autour du point de départ (BFS)
        while (!queue.isEmpty() && selectedVertices.size() < n) {
            String v = queue.poll();
            for (DefaultEdge e : base.outgoingEdgesOf(v)) {
                String target = base.getEdgeTarget(e);
                if (!selectedVertices.contains(target)) {
                    selectedVertices.add(target);
                    queue.add(target);
                    if (selectedVertices.size() >= n) break;
                }
            }
        }

        // Création du sous-graphe induit sur ces sommets
        DirectedMultigraph<String, DefaultEdge> subgraph = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : selectedVertices) {
            subgraph.addVertex(v);
        }
        List<DefaultEdge> candidateEdges = new ArrayList<>();
        for (String v : selectedVertices) {
            for (DefaultEdge e : base.outgoingEdgesOf(v)) {
                String target = base.getEdgeTarget(e);
                if (selectedVertices.contains(target)) {
                    candidateEdges.add(e);
                }
            }
        }
        // Prend une fraction d'arêtes (optionnel)
        Collections.shuffle(candidateEdges);
        int maxEdges = (int) (edgeFraction * candidateEdges.size());
        List<DefaultEdge> selectedEdges = candidateEdges.subList(0, maxEdges);
        for (DefaultEdge e : selectedEdges) {
            String source = base.getEdgeSource(e);
            String target = base.getEdgeTarget(e);
            subgraph.addEdge(source, target);
        }

        return subgraph;
    }

    public static DirectedMultigraph<String, DefaultEdge> subgraphWithEdgePercentage(
            DirectedMultigraph<String, DefaultEdge> base,
            double edgePercentage
    ) {
        // Nouveau graphe avec mêmes sommets
        DirectedMultigraph<String, DefaultEdge> subgraph = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : base.vertexSet()) {
            subgraph.addVertex(v);
        }

        // Liste des arêtes à piocher aléatoirement
        List<DefaultEdge> edges = new ArrayList<>(base.edgeSet());
        Collections.shuffle(edges);

        int numEdgesToKeep = (int) Math.round(edges.size() * edgePercentage);

        for (int i = 0; i < numEdgesToKeep; i++) {
            DefaultEdge e = edges.get(i);
            String src = base.getEdgeSource(e);
            String tgt = base.getEdgeTarget(e);
            subgraph.addEdge(src, tgt);
        }

        return subgraph;
    }

    public static DirectedMultigraph<String, DefaultEdge> randomSubgraph(
            DirectedMultigraph<String, DefaultEdge> base, int n, double edgeFraction) {

        List<String> allVertices = new ArrayList<>(base.vertexSet());
        if (n > allVertices.size()) {
            throw new IllegalArgumentException("n (" + n + ") > nombre de sommets (" + allVertices.size() + ")");
        }

        // Mélange et sélection des sommets
        Collections.shuffle(allVertices);
        Set<String> selectedVertices = new HashSet<>(allVertices.subList(0, n));

        // Création du sous-graphe
        DirectedMultigraph<String, DefaultEdge> subgraph = new DirectedMultigraph<>(DefaultEdge.class);
        for (String v : selectedVertices) {
            subgraph.addVertex(v);
        }

        // Construction de la liste des arêtes candidates
        List<DefaultEdge> candidateEdges = new ArrayList<>();
        for (String v : selectedVertices) {
            for (DefaultEdge e : base.outgoingEdgesOf(v)) {
                String target = base.getEdgeTarget(e);
                if (selectedVertices.contains(target)) {
                    candidateEdges.add(e);
                }
            }
        }

        // Sélection aléatoire d'une fraction des arêtes
        Collections.shuffle(candidateEdges);
        int maxEdges = (int) (edgeFraction * candidateEdges.size());
        if (maxEdges > candidateEdges.size()) maxEdges = candidateEdges.size(); // sécurité
        List<DefaultEdge> selectedEdges = candidateEdges.subList(0, maxEdges);

        for (DefaultEdge e : selectedEdges) {
            String source = base.getEdgeSource(e);
            String target = base.getEdgeTarget(e);
            subgraph.addEdge(source, target);
        }

        return subgraph;
    }



    // Charge un graphe à partir d'un fichier (format edge list, 1 arc par ligne: src dest)
    public static DirectedMultigraph<String, DefaultEdge> loadGraph(String file) {
        DirectedMultigraph<String, DefaultEdge> graph = new DirectedMultigraph<>(DefaultEdge.class);
        try (Scanner sc = new Scanner(new java.io.File(file))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;
                graph.addVertex(parts[0]);
                graph.addVertex(parts[1]);
                graph.addEdge(parts[0], parts[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur de lecture du fichier " + file, e);
        }
        return graph;
    }

    // Sauvegarde au format edge list (simple à charger par Gephi ou autre)
    public static void saveGraph(Graph<String, DefaultEdge> graph, String filename) throws IOException {
        try (FileWriter fw = new FileWriter(filename)) {
            for (DefaultEdge e : graph.edgeSet()) {
                fw.write(graph.getEdgeSource(e) + "\t" + graph.getEdgeTarget(e) + "\n");
            }
        }
    }

    // Récupère une composante fortement connectée (la plus grande par défaut ou de taille cible)
    public static Set<String> getStronglyConnectedComponent(DirectedMultigraph<String, DefaultEdge> graph, int minSize) {
        var sccs = new KosarajuStrongConnectivityInspector<>(graph).stronglyConnectedSets();
        return sccs.stream().filter(scc -> scc.size() >= minSize).findFirst().orElseGet(() -> sccs.get(0));
    }

    // Supprime un ensemble de sommets (et toutes leurs arêtes)
    public static DirectedMultigraph<String, DefaultEdge> removeVertices(DirectedMultigraph<String, DefaultEdge> graph, Set<String> toRemove) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) ((DirectedMultigraph<String, DefaultEdge>)graph).clone();
        for (String v : toRemove) clone.removeVertex(v);
        return clone;
    }

//    // Supprime un pourcentage aléatoire de sommets
//    public static DirectedMultigraph<String, DefaultEdge> applyRandomVertexRemoval(DirectedMultigraph<String, DefaultEdge> graph, double frac) {
//        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
//        List<String> vertices = new ArrayList<>(clone.vertexSet());
//        Collections.shuffle(vertices);
//        int nb = (int)(vertices.size() * frac);
//        for (int i = 0; i < nb; i++) clone.removeVertex(vertices.get(i));
//        return clone;
//    }

    // Supprime les k nœuds de plus haut degré sortant
    public static DirectedMultigraph<String, DefaultEdge> removeTopDegreeVertices(DirectedMultigraph<String, DefaultEdge> graph, int k) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        List<String> sorted = clone.vertexSet().stream()
                .sorted((a, b) -> Integer.compare(clone.outDegreeOf(b), clone.outDegreeOf(a)))
                .collect(Collectors.toList());
        for (int i = 0; i < Math.min(k, sorted.size()); i++) clone.removeVertex(sorted.get(i));
        return clone;
    }

    // Supprime un pourcentage d'arêtes aléatoires
    public static DirectedMultigraph<String, DefaultEdge> applyRandomEdgeRemoval(DirectedMultigraph<String, DefaultEdge> graph, double frac) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        List<DefaultEdge> edges = new ArrayList<>(clone.edgeSet());
        Collections.shuffle(edges);
        int nb = (int)(edges.size() * frac);
        for (int i = 0; i < nb; i++) clone.removeEdge(edges.get(i));
        return clone;
    }

    // Ajoute un pourcentage d'arêtes aléatoires (sans doublons)
    public static DirectedMultigraph<String, DefaultEdge> applyRandomEdgeAddition(DirectedMultigraph<String, DefaultEdge> graph, double frac) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        List<String> vertices = new ArrayList<>(clone.vertexSet());
        int possible = vertices.size() * vertices.size();
        int toAdd = (int)(clone.edgeSet().size() * frac);
        Random rnd = new Random();
        Set<String> edgesExist = clone.edgeSet().stream().map(e -> clone.getEdgeSource(e) + "|" + clone.getEdgeTarget(e)).collect(Collectors.toSet());
        int added = 0;
        while (added < toAdd && added < possible) {
            String src = vertices.get(rnd.nextInt(vertices.size()));
            String dst = vertices.get(rnd.nextInt(vertices.size()));
            if (src.equals(dst)) continue;
            String key = src + "|" + dst;
            if (edgesExist.contains(key)) continue;
            clone.addEdge(src, dst);
            edgesExist.add(key);
            added++;
        }
        return clone;
    }

    // Inverse l'orientation d'un pourcentage d'arêtes choisies aléatoirement
    public static DirectedMultigraph<String, DefaultEdge> invertRandomEdgeDirections(DirectedMultigraph<String, DefaultEdge> graph, double frac) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        List<DefaultEdge> edges = new ArrayList<>(clone.edgeSet());
        Collections.shuffle(edges);
        int nb = (int)(edges.size() * frac);
        for (int i = 0; i < nb; i++) {
            DefaultEdge e = edges.get(i);
            String src = clone.getEdgeSource(e);
            String dst = clone.getEdgeTarget(e);
            clone.removeEdge(e);
            clone.addEdge(dst, src);
        }
        return clone;
    }

    // Ajoute un sous-graphe dense (clique) de taille n relié par une arête à un sommet existant
    public static DirectedMultigraph<String, DefaultEdge> addDenseSubgraph(DirectedMultigraph<String, DefaultEdge> graph, int size) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        List<String> baseVertices = new ArrayList<>(clone.vertexSet());
        Random rnd = new Random();
        String attach = baseVertices.get(rnd.nextInt(baseVertices.size()));
        List<String> newNodes = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String name = "CLQ_" + i + "_" + System.currentTimeMillis();
            clone.addVertex(name);
            newNodes.add(name);
        }
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (i != j) clone.addEdge(newNodes.get(i), newNodes.get(j));
        // Relie la clique au graphe
        clone.addEdge(attach, newNodes.get(0));
        return clone;
    }

    // Duplique un nœud (ajoute un jumeau avec les mêmes arcs sortants/entrants)
    public static DirectedMultigraph<String, DefaultEdge> cloneNode(DirectedMultigraph<String, DefaultEdge> graph, String node) {
        if (!graph.containsVertex(node)) return (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        String newNode = node + "_clone_" + System.currentTimeMillis();
        clone.addVertex(newNode);
        // Même arcs sortants
        for (DefaultEdge out : clone.outgoingEdgesOf(node)) {
            String tgt = clone.getEdgeTarget(out);
            clone.addEdge(newNode, tgt);
        }
        // Même arcs entrants
        for (DefaultEdge in : clone.incomingEdgesOf(node)) {
            String src = clone.getEdgeSource(in);
            clone.addEdge(src, newNode);
        }
        return clone;
    }

    // Ajoute n nœuds isolés
    public static DirectedMultigraph<String, DefaultEdge> addIsolatedNodes(DirectedMultigraph<String, DefaultEdge> graph, int n) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        for (int i = 0; i < n; i++) {
            clone.addVertex("ISO_" + i + "_" + System.currentTimeMillis());
        }
        return clone;
    }

    // Rewire : pourcentage d'arêtes sont réassignées aléatoirement, les degrés sortants sont conservés (approximatif)
    public static DirectedMultigraph<String, DefaultEdge> rewireEdgesPreserveDegree(DirectedMultigraph<String, DefaultEdge> graph, double frac) {
        DirectedMultigraph<String, DefaultEdge> clone = (DirectedMultigraph<String, DefaultEdge>) graph.clone();
        List<DefaultEdge> edges = new ArrayList<>(clone.edgeSet());
        List<String> vertices = new ArrayList<>(clone.vertexSet());
        Random rnd = new Random();
        int nb = (int)(edges.size() * frac);
        for (int i = 0; i < nb; i++) {
            DefaultEdge e = edges.get(i);
            String src = clone.getEdgeSource(e);
            clone.removeEdge(e);
            String newTgt = vertices.get(rnd.nextInt(vertices.size()));
            while (newTgt.equals(src)) newTgt = vertices.get(rnd.nextInt(vertices.size()));
            clone.addEdge(src, newTgt);
        }
        return clone;
    }
}
