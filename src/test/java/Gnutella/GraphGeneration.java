package Gnutella;

import be.FileUtils;
import be.GraphUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.Set;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GraphGeneration {
    private static DirectedMultigraph<String, DefaultEdge> base = GraphUtils.loadGraph("src/main/resources/p2p-Gnutella31.txt");


    /**
     * Cas 1 : Suppression de la pluis grosse composante fortement connectée (sous-graphe local)
     * ------------------------------------------------------------
     * On sélectionne une composante fortement connectée d'au moins 10 sommets.
     * On supprime tous les sommets de cette composante (et donc leurs arêtes).
     * Objectif expérimental :
     * - Simuler la disparition brutale d'un groupe de pairs fortement reliés.
     * - Tester la sensibilité des mesures à la perte d'une "communauté" ou d'un cluster cohésif.
     * - Perturbation de type "locale mais dense".
     */
    @Test
    @Order(1)
    public void testRemoveConnectedSubgraph() throws IOException {
        String dir = "output/similarity/removeConnectedSubgraph";
        FileUtils.ensureDirectoryExists(dir);
        Set<String> comp = GraphUtils.getStronglyConnectedComponent(base);
        DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.removeVertices(base, comp);
        GraphUtils.saveGraph(modif, String.format("%s/removeConnectedSubgraph.txt",dir));
    }


    /**
     * Cas 2 : Suppression aléatoire d'un pourcentage de sommets (5%, 10%, 20%)
     * ------------------------------------------------------------
     * On retire au hasard une fraction donnée des nœuds du graphe.
     * Les arêtes associées à ces sommets sont également supprimées.
     * Objectif expérimental :
     * - Simuler des pannes aléatoires, déconnexions ou disparitions naturelles de pairs.
     * - Évaluer la robustesse des métriques face à des pertes de structure "diffuses" (non ciblées).
     * - Scénario réaliste pour des réseaux dynamiques.
     */
    @Test
    @Order(2)
    public void testRemoveRandomVertices() throws IOException {
        double[] fractions = {0.05, 0.10, 0.20};
        String dir = "output/similarity/removeRandomVertices";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomVertexRemoval(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/removeRandomVertices_%02dpct.txt", dir, (int)(frac*100)));
        }
    }

    /**
     * Cas 3 : Suppression aléatoire d'un pourcentage d'arêtes (5%, 15%, 20%)
     * ------------------------------------------------------------
     * On enlève au hasard un certain pourcentage des arêtes du graphe.
     * Les sommets restent en place, seuls les liens sont affectés.
     * Objectif expérimental :
     * - Simuler des coupures de connexion aléatoires dans le réseau.
     * - Tester la stabilité des mesures face à du "bruit" structurel.
     * - Vérifier la capacité des métriques à distinguer des modifications "légères".
     */
    @Test
    @Order(3)
    public void testRemoveRandomEdges() throws IOException {
        double[] fractions = {0.05, 0.15, 0.20};
        String dir = "output/similarity/removeRandomEdges";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomEdgeRemoval(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/removeRandomEdges_%02dpct.txt", dir, (int)(frac*100)));
        }
    }

    /**
     * Cas 4 : Ajout aléatoire d'un pourcentage d'arêtes (5%, 10%, 20%)
     * ------------------------------------------------------------
     * On ajoute de nouvelles arêtes entre des paires de sommets choisis aléatoirement.
     * Objectif expérimental :
     * - Simuler la densification du réseau (nouvelles connexions spontanées ou temporaires).
     * - Étudier l'effet sur les métriques de similarité de l'apparition de nouveaux chemins/courts-circuits.
     * - Vérifier la sensibilité à la création de "raccourcis" structurels.
     */
    @Test
    @Order(4)
    public void testAddRandomEdges() throws IOException {
        double[] fractions = {0.05, 0.10, 0.20};
        String dir = "output/similarity/addRandomEdges";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.applyRandomEdgeAddition(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/addRandomEdges_%02dpct.txt",dir, (int)(frac*100)));
        }
    }

    /**
     * Cas 5 : Inversion de la direction d'un pourcentage d'arêtes (5%, 10%, 20%)
     * ------------------------------------------------------------
     * On sélectionne une partie des arêtes et on inverse leur sens.
     * Objectif expérimental :
     * - Simuler une modification directionnelle des flux dans le graphe (inversion d'autorité, changements de routage).
     * - Tester la robustesse des mesures à la directionnalité (ce que captent certaines métriques mais pas toutes).
     * - Perturber la structure orientée tout en gardant la connectivité globale similaire.
     */
    @Test
    @Order(5)
    public void testInvertEdgeDirection() throws IOException {
        double[] fractions = {0.05, 0.10, 0.20};
        String dir = "output/similarity/invertEdges";
        FileUtils.ensureDirectoryExists(dir);
        for (double frac : fractions) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.invertRandomEdgeDirections(base, frac);
            GraphUtils.saveGraph(modif, String.format("%s/invertEdges_%02dpct.txt",dir, (int)(frac*100)));
        }
    }


    /**
     * Cas 6 : Ajout d'un sous-graphe dense (clique) de taille 5 ou 10
     * ------------------------------------------------------------
     * On crée un nouveau petit sous-graphe dans lequel tous les sommets sont interconnectés, puis on relie ce cluster au graphe de base.
     * Objectif expérimental :
     * - Simuler l'apparition d'une nouvelle communauté ou d'un groupe de pairs très fortement reliés.
     * - Tester si les métriques détectent l'émergence de structures "anormales" ou denses dans un graphe initialement épars.
     * - Perturbation "mésoscopique" (niveau communautés/clusters).
     */
    @Test
    @Order(6)
    public void testAddDenseSubgraph() throws IOException {
        int[] cliqueSizes = {5, 10};
        String dir = "output/similarity/addDenseSubgraph";
        FileUtils.ensureDirectoryExists(dir);
        for (int size : cliqueSizes) {
            DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.addDenseSubgraph(base, size);
            GraphUtils.saveGraph(modif, String.format("%s/addDenseSubgraph_size%d.txt",dir, size));
        }
    }

    /**
     * Cas 7 : Réaffectation (rewiring) aléatoire de 20% des arêtes
     * ------------------------------------------------------------
     * On choisit une fraction des arêtes et on réassigne leur extrémité (tout en conservant le même nombre d'arêtes et la même distribution de degrés sortants).
     * Objectif expérimental :
     * - Simuler une reconfiguration structurelle globale, tout en conservant les propriétés locales (degrés des sommets).
     * - Évaluer si la métrique de similarité détecte des réarrangements diffus (modification "permutative" sans perte/gain net d'arêtes).
     * - Tester la sensibilité aux modifications topologiques sans variation du nombre de liens.
     */
    @Test
    @Order(7)
    public void testRandomRewireEdges() throws IOException {
        String dir = "output/similarity/rewireEdges";
        FileUtils.ensureDirectoryExists(dir);
        DirectedMultigraph<String, DefaultEdge> modif = GraphUtils.rewireEdgesPreserveDegree(base, 0.2);
        GraphUtils.saveGraph(modif, String.format("%s/rewireEdges_20pct.txt", dir));
    }
}
