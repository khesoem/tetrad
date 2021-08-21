package edu.cmu.tetrad.search;

import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.Collections.shuffle;

/**
 * Implements a scorer as in Teyssier, M., & Koller, D. (2012). Ordering-based search: A simple and effective
 * algorithm for learning Bayesian networks. arXiv preprint arXiv:1207.1429. You give it a score function
 * and a variable ordering, and it computes the score. You can move any variable left or right and it will
 * keep track of the score using the Teyssier and Kohler method. You can move a vairable to a new position,
 * and you can bookmark a state and come baqck to it.
 *
 * @author josephramsey
 */
public class TeyssierScorer {
    private final Map<ScoreKey, Pair> cache = new HashMap<>();
    private final List<Node> variables;
    private final ParentCalculation parentCalculation = ParentCalculation.IteratedGrowShrink;
    private final Map<Node, Integer> variablesHash;
    private LinkedList<Node> bookmarkedOrder = new LinkedList<>();
    private LinkedList<Pair> bookmarkedScores = new LinkedList<>();
    private HashMap<Node, Integer> bookmarkedNodesHash = new HashMap<>();
    private Score score;
    private IndependenceTest test;
    private LinkedList<Node> order;
    private LinkedList<Pair> scores;
    private boolean cachingScores = true;
//    private IKnowledge knowledge = new Knowledge2();
    private LinkedList<Set<Node>> prefixes;
    private ScoreType scoreType = ScoreType.Edge;
    private Map<Node, Integer> orderHash = new HashMap<>();

    public TeyssierScorer(Score score) {
        this.score = score;
        this.order = new LinkedList<>(score.getVariables());
        this.orderHash = nodesHash(order);
        this.variables = score.getVariables();
        this.variablesHash = nodesHash(variables);
    }

    public TeyssierScorer(IndependenceTest test) {
        this.test = test;
        this.variables = test.getVariables();
        this.variablesHash = nodesHash(variables);
    }

//    public void setKnowledge(IKnowledge knowledge) {
//        this.knowledge = knowledge;
//    }

    public double score(List<Node> order) {
        this.order = new LinkedList<>(order);
        initializeScores();
        this.bookmarkedOrder = new LinkedList<>(this.order);
        this.bookmarkedScores = new LinkedList<>(this.scores);
        return score();
    }

    private Map<Node, Integer> nodesHash(List<Node> order) {
        HashMap<Node, Integer> nodesHash = new HashMap<>();

        for (int i = 0; i < order.size(); i++) {
            nodesHash.put(order.get(i), i);
        }

        return nodesHash;
    }

    public double score() {
        return sum();
    }

    public boolean promote(Node v) {
        int index = orderHash.get(v);
        if (index == 0) return false;

        Node v1 = order.get(index - 1);
        Node v2 = order.get(index);
//
//        if (knowledge.isForbidden(v2.getName(), v1.getName())) {
//            demote(v);
//            return true;
//        }

        order.set(index - 1, v2);
        order.set(index, v1);

        recalculate(index - 1);
        recalculate(index);

        orderHash.put(v1, index);
        orderHash.put(v2, index - 1);

        return true;
    }

    public boolean demote(Node v) {
        int index = orderHash.get(v);
        if (index >= size() - 1) return false;
        if (index == -1) return false;

        Node v1 = order.get(index);
        Node v2 = order.get(index + 1);

//        if (knowledge.isForbidden(v2.getName(), v1.getName())) {
//            promote(v);
//            return true;
//        }

        order.set(index, v2);
        order.set(index + 1, v1);

        recalculate(index);
        recalculate(index + 1);

        orderHash.put(v1, index + 1);
        orderHash.put(v2, index);

        return true;
    }

    public void moveTo(Node v, int toIndex) {
        int vindex = indexOf(v);

        order.remove(v);
        order.add(toIndex, v);

//        if (!validKnowledgeOrder(order)) {
//            order.remove(v);
//            order.add(vindex, v);
//        }

        updateScores();
    }

//    private boolean validKnowledgeOrder(List<Node> order) {
//        for (int i = 0; i < order.size(); i++) {
//            for (int j = i + 1; j < order.size(); j++) {
//                if (knowledge.isForbidden(order.get(i).getName(), order.get(j).getName())) {
//                    return false;
//                }
//            }
//        }
//
//        return true;
//    }

    public void moveToFirst(Node v) {
        int vindex = indexOf(v);

        order.remove(v);
        order.addFirst(v);

//        if (!validKnowledgeOrder(order)) {
//            order.remove(v);
//            order.add(vindex, v);
//        }

        updateScores();
    }

    public void moveToLast(Node v) {
        int vindex = indexOf(v);

        order.remove(v);
        order.addLast(v);

//        if (!validKnowledgeOrder(order)) {
//            order.remove(v);
//            order.add(vindex, v);
//        }

        updateScores();
    }

    public boolean swap(Node m, Node n) {
        int i = orderHash.get(m);
        int j = orderHash.get(n);

        order.set(i, n);
        order.set(j, m);

//        if (!validKnowledgeOrder(order)) {
//            order.set(i, m);
//            order.set(j, n);
//            return false;
//        }

        updateScores();
        return true;
    }

    public List<Node> getOrder() {
        return new ArrayList<>(order);
    }

    public int indexOf(Node v) {
        return orderHash.get(v);
    }

    private double sum() {
        double score = 0;

        for (int i = 0; i < order.size(); i++) {
            score += scores.get(i).getScore();
        }

        return score;
    }

    private void initializeScores() {
        this.scores = new LinkedList<>();
        for (int i1 = 0; i1 < order.size(); i1++) this.scores.add(null);

        this.prefixes = new LinkedList<>();
        for (int i1 = 0; i1 < order.size(); i1++) this.prefixes.add(null);

        updateScores();
    }

    private void updateScores() {
        for (int i = 0; i < order.size(); i++) {
            recalculate(i);
        }

        orderHash = nodesHash(order);
    }

    private double score(Node n, Set<Node> pi) {
        if (cachingScores) {
            ScoreKey key = new ScoreKey(n, pi);
            Pair pair = cache.get(key);

            if (pair != null) {
                return pair.getScore();
            }
        }

        int[] parentIndices = new int[pi.size()];

        int k = 0;

        for (Node p : pi) {
            parentIndices[k++] = variablesHash.get(p);
        }

        double v = this.score.localScore(variablesHash.get(n), parentIndices);

        if (cachingScores) {
            ScoreKey key = new ScoreKey(n, pi);
            cache.put(key, new Pair(pi, v));
        }

        return v;
    }

    private Set<Node> getPrefix(int i) {
        Set<Node> prefix = new HashSet<>();

        for (int j = 0; j < i; j++) {
            prefix.add(order.get(j));
        }

        return prefix;
    }

    private void recalculate(int p) {
        if (!getPrefix(p).equals(prefixes.get(p))) {
            scores.set(p, getParentsInternal(p));
        }
    }

    public Set<Node> getParents(int p) {
        return new HashSet<>(scores.get(p).getParents());
    }

    public Set<Node> getParents(Node v) {
        return new HashSet<>(scores.get(indexOf(v)).getParents());
    }

    public Graph getGraph(boolean pattern) {
        List<Node> order = getOrder();
        Graph G1 = new EdgeListGraph(order);

        for (int p = 0; p < order.size(); p++) {
            for (Node z : getParents(p)) {
                G1.addDirectedEdge(z, order.get(p));
            }
        }

        if (pattern) {
            return SearchGraphUtils.patternForDag(G1);
        } else {
            return G1;
        }
    }

    private Pair getParentsInternal(int p) {
        if (parentCalculation == ParentCalculation.IteratedGrowShrink) {
            if (test != null) {
                return getGrowShrinkIndep(p);
            } else {
                return getGrowShrinkScore(p);
            }
        } else if (parentCalculation == ParentCalculation.VermaPearl) {
            if (test != null) {
                return vermaPearl(p);
            } else {
                return vermaPearlScore(p);
            }
        } else {
            throw new IllegalStateException("Unrecognized parent calculation: " + parentCalculation);
        }
    }

    private Pair vermaPearl(int p) {
        Node x = order.get(p);
        Set<Node> parents = new HashSet<>();
        Set<Node> prefix = getPrefix(p);

        for (Node y : prefix) {
            List<Node> minus = new ArrayList<>(prefix);
            minus.remove(y);

            if (test.isDependent(x, y, minus)) {
                parents.add(y);
            }
        }

        return new Pair(parents, parents.size());
    }

    private Pair vermaPearlScore(int p) {
        Node x = order.get(p);
        Set<Node> parents = new HashSet<>();
        Set<Node> prefix = getPrefix(p);

        double s1 = score(x, new HashSet<>(prefix));

        for (Node y : prefix) {
            List<Node> minus = new ArrayList<>(prefix);
            minus.remove(y);

            double s2 = score(x, new HashSet<>(minus));

            if (s2 < s1) {
                parents.add(y);
            }
        }

        return new Pair(parents, parents.size());
    }

    private Pair getGrowShrinkIndep(int p) {
        Node n = order.get(p);

        List<Node> parents = new ArrayList<>();
        boolean changed = true;

        Set<Node> prefix = getPrefix(p);

        // Grow-shrink
        while (changed) {
            changed = false;

            for (Node z0 : prefix) {
                if (parents.contains(z0)) continue;
//                if (knowledge.isForbidden(z0.getName(), n.getName())) continue;

                if (test.isDependent(n, z0, parents)) {

                    parents.add(z0);
                    changed = true;
                }

                boolean changed2 = true;

                while (changed2) {
                    changed2 = false;

                    for (Node z1 : new ArrayList<>(parents)) {
                        parents.remove(z1);

                        if (test.isIndependent(n, z1, parents)) {
                            changed2 = true;
                        } else {
                            parents.add(z1);
                        }
                    }
                }
            }
        }

        return new Pair(new HashSet<>(parents), parents.size());
    }

    @NotNull
    private Pair getGrowShrinkScore(int p) {
        Node n = order.get(p);

        Set<Node> parents = new HashSet<>();
        boolean changed = true;

        double sMax = score(n, new HashSet<>());
        Set<Node> prefix = getPrefix(p);

        // Grow-shrink
        while (changed) {
            changed = false;

            // Let z be the node that maximizes the score...
            Node z = null;

            for (Node z0 : prefix) {
                if (parents.contains(z0)) continue;

//                if (knowledge.isForbidden(z0.getName(), n.getName())) continue;
                parents.add(z0);

                double s2 = score(n, parents);

                if (s2 > sMax) {
                    sMax = s2;
                    z = z0;
                }

                parents.remove(z0);
            }

            if (z != null) {
                parents.add(z);
                changed = true;
            }

            boolean changed2 = true;

            while (changed2) {
                changed2 = false;

                Node w = null;

                for (Node z0 : new HashSet<>(parents)) {
                    parents.remove(z0);

                    double s2 = score(n, parents);

                    if (s2 > sMax) {
                        sMax = s2;
                        w = z0;
                    }

                    parents.add(z0);
                }

                if (w != null) {
                    parents.remove(w);
                    changed2 = true;
                }
            }
        }

        if (scoreType == ScoreType.Edge) {
            return new Pair(parents, parents.size());
        } else if (scoreType == ScoreType.SCORE) {
            return new Pair(parents, -sMax);
        } else {
            throw new IllegalStateException("Unexpected score type: " + scoreType);
        }
    }

    public void bookmark() {
        this.bookmarkedOrder = new LinkedList<>(order);
        this.bookmarkedScores = new LinkedList<>(scores);
        this.bookmarkedNodesHash = new HashMap<>(orderHash);
    }

    public void goToBookmark() {
        this.order = new LinkedList<>(bookmarkedOrder);
        this.scores = new LinkedList<>(bookmarkedScores);
        this.orderHash = new HashMap<>(bookmarkedNodesHash);
    }

    public void setCachingScores(boolean cachingScores) {
        this.cachingScores = cachingScores;
    }

    public int size() {
        return order.size();
    }

    public int getNumEdges() {
        int numEdges = 0;

        for (int p = 0; p < order.size(); p++) {
            numEdges += getParents(p).size();
        }

        return numEdges;
    }

    public void shuffleVariables() {
        order = new LinkedList<>(order);
        shuffle(order);
        score(order);
    }

    public void setScoreType(ScoreType scoreType) {
        this.scoreType = scoreType;
    }

    public Node get(int j) {
        return order.get(j);
    }

    public boolean adjacent(Node a, Node c) {
        return getParents(indexOf(a)).contains(c) || getParents(indexOf(c)).contains(a);
    }

    public boolean defCollider(Node a, Node b, Node c) {
        if (!adjacent(a, b)) return false;
        if (!adjacent(b, c)) return false;
        return getParents(b).contains(a) && getParents(b).contains(c);
    }

    public enum ScoreType {Edge, SCORE}

    public enum ParentCalculation {IteratedGrowShrink, VermaPearl}

    private static class Pair {
        private final Set<Node> parents;
        private final double score;

        private Pair(Set<Node> parents, double score) {
            this.parents = parents;
            this.score = score;
        }

        public Set<Node> getParents() {
            return parents;
        }

        public double getScore() {
            return score;
        }
    }

    public static class ScoreKey {
        private final Node y;
        private final Set<Node> pi;

        public ScoreKey(Node y, Set<Node> pi) {
            this.y = y;
            this.pi = new HashSet<>(pi);
        }

        public int hashCode() {
            return 3 * y.hashCode() + 7 * pi.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof ScoreKey)) {
                return false;
            }

            ScoreKey spec = (ScoreKey) o;
            return y.equals(spec.y) && this.pi.equals(spec.pi);
        }

        public Node getY() {
            return y;
        }

        public Set<Node> getPi() {
            return pi;
        }
    }
}
