///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.graph;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Stores a graph a list of lists of edges adjacent to each node in the
 * graph, with an additional list storing all of the edges in the graph. The
 * edges are of the form N1 *-# N2. Multiple edges may be added per node pair to
 * this graph, with the caveat that all edges of the form N1 *-# N2 will be
 * considered equal. For randomUtil, if the edge X --> Y is added to the graph,
 * another edge X --> Y may not be added, although an edge Y --> X may be added.
 * Edges from nodes to themselves may also be added.</p>
 *
 * @author Joseph Ramsey
 * @author Erin Korber additions summer 2004
 * @see Endpoint
 */
public class EdgeListGraphSingleConnections extends EdgeListGraph {
    static final long serialVersionUID = 23L;


    //==============================CONSTUCTORS===========================//

    /**
     * Constructs a new (empty) EdgeListGraph.
     */
    public EdgeListGraphSingleConnections() {
        edgeLists = new ConcurrentHashMap<>();
        nodes = new ArrayList<>();
        edgesSet = new HashSet<>();
    }

    /**
     * Constructs a EdgeListGraph using the nodes and edges of the given graph.
     * If this cannot be accomplished successfully, an exception is thrown. Note
     * that any graph constraints from the given graph are forgotten in the new
     * graph.
     *
     * @param graph the graph from which nodes and edges are is to be
     *              extracted.
     * @throws IllegalArgumentException if a duplicate edge is added.
     */
    public EdgeListGraphSingleConnections(Graph graph) throws IllegalArgumentException {
        this();

        if (graph instanceof EdgeListGraphSingleConnections) {
            EdgeListGraphSingleConnections _graph = (EdgeListGraphSingleConnections) graph;
            nodes = new ArrayList<>(_graph.nodes);
            edgesSet = new HashSet<>(_graph.edgesSet);
            edgeLists = new ConcurrentHashMap<>();
            for (Node node : nodes) this.edgeLists.put(node, new ArrayList<>(_graph.edgeLists.get(node)));
            ambiguousTriples = new HashSet<>(_graph.ambiguousTriples);
            underLineTriples = new HashSet<>(_graph.underLineTriples);
            dottedUnderLineTriples = new HashSet<>(_graph.dottedUnderLineTriples);
            stuffRemovedSinceLastTripleAccess = _graph.stuffRemovedSinceLastTripleAccess;
            highlightedEdges = new HashSet<>(_graph.highlightedEdges);
            namesHash = new HashMap<>(_graph.namesHash);
        } else

        {
            if (graph == null) {
                throw new NullPointerException("Graph must not be null.");
            }

            transferNodesAndEdges(graph);
            this.ambiguousTriples = graph.getAmbiguousTriples();
            this.underLineTriples = graph.getUnderLines();
            this.dottedUnderLineTriples = graph.getDottedUnderlines();

            for (Edge edge : graph.getEdges()) {
                if (graph.isHighlighted(edge)) {
                    setHighlighted(edge, true);
                }
            }

            for (Node node : nodes) {
                namesHash.put(node.getName(), node);
            }
        }
    }

    /**
     * Constructs a new graph, with no edges, using the the given variable
     * names.
     */
    public EdgeListGraphSingleConnections(List<Node> nodes) {
        this();

        if (nodes == null) {
            throw new NullPointerException();
        }

        this.nodes = new ArrayList<>(nodes);

        for (Node node : nodes) {
            edgeLists.put(node, new ArrayList<Edge>());
            namesHash.put(node.getName(), node);
        }
    }

    // Makes a copy with the same object identical edges in it. If you make changes to those edges they will be
    // reflected here.
    public synchronized static Graph shallowCopy(EdgeListGraphSingleConnections _graph) {
        EdgeListGraphSingleConnections graph = new EdgeListGraphSingleConnections();

        graph.nodes = new ArrayList<>(_graph.nodes);
        graph.edgesSet = new HashSet<>(_graph.edgesSet);
        graph.edgeLists = new ConcurrentHashMap<>(_graph.edgeLists);
        for (Node node : graph.nodes) graph.edgeLists.put(node, new ArrayList<>(_graph.edgeLists.get(node)));
        graph.ambiguousTriples = new HashSet<>(_graph.ambiguousTriples);
        graph.underLineTriples = new HashSet<>(_graph.underLineTriples);
        graph.dottedUnderLineTriples = new HashSet<>(_graph.dottedUnderLineTriples);
        graph.stuffRemovedSinceLastTripleAccess = _graph.stuffRemovedSinceLastTripleAccess;
        graph.highlightedEdges = new HashSet<>(_graph.highlightedEdges);
        graph.namesHash = new HashMap<>(_graph.namesHash);

//        _graph.nodes = new ArrayList(graph.nodes);
//        _graph.edgesSet = new HashSet<Edge>(graph.edgesSet);
//        _graph.edgeLists = new ConcurrentHashMap<Node, List<Edge>>(graph.edgeLists);
//        for (Node node : graph.nodes) _graph.edgeLists.put(node, new ArrayList(graph.edgeLists.get(node)));
//        _graph.graphConstraints = new ArrayList<GraphConstraint>(graph.graphConstraints);
//        _graph.graphConstraintsChecked = graph.graphConstraintsChecked;
//        _graph.ambiguousTriples = new HashSet<Triple>(graph.ambiguousTriples);
//        _graph.underLineTriples = new HashSet<Triple>(graph.underLineTriples);
//        _graph.dottedUnderLineTriples = new HashSet<Triple>(graph.dottedUnderLineTriples);
//        _graph.stuffRemovedSinceLastTripleAccess = graph.stuffRemovedSinceLastTripleAccess;
//        _graph.highlightedEdges = new HashSet<Edge>(graph.highlightedEdges);
//        _graph.namesHash = new HashMap(graph.namesHash);
        return _graph;
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static EdgeListGraphSingleConnections serializableInstance() {
        return new EdgeListGraphSingleConnections();
    }


    public boolean isDirectedFromTo(Node node1, Node node2) {
        Edge edge = getEdge(node1, node2);
        return edge != null && edge.pointsTowards(node2);
    }

    public boolean isUndirectedFromTo(Node node1, Node node2) {
        Edge edge = getEdge(node1, node2);
        return edge != null && edge.getEndpoint1() == Endpoint.TAIL && edge.getEndpoint2() == Endpoint.TAIL;
    }

    /**
     * IllegalArgument exception raised (by isDirectedFromTo(getEndpoint) or by
     * getEdge) if there are multiple edges between any of the node pairs.
     */
    public boolean isDefNoncollider(Node node1, Node node2, Node node3) {
        Edge edge1 = getEdge(node1, node2);
        Edge edge2 = getEdge(node3, node2);

        if (edge1 == null || edge2 == null) {
            return false;
        }

        boolean circle12 = edge1.getProximalEndpoint(node2) == Endpoint.CIRCLE;
        boolean circle32 = edge2.getProximalEndpoint(node2) == Endpoint.CIRCLE;

        if (edge1.pointsTowards(node1)) return true;
        if (edge2.pointsTowards(node3)) return true;

        if (circle12 && circle32) return true;

//        List<Edge> edges = getEdges(node2);
//        boolean circle12 = false;
//        boolean circle32 = false;
//
//        for (int i = 0; i < edges.size(); i++) {
//            Edge edge = edges.get(i);
//            boolean _node1 = edge.getDistalNode(node2) == node1;
//            boolean _node3 = edge.getDistalNode(node2) == node3;
//
//            if (_node1 && edge.pointsTowards(node1)) return true;
//            if (_node3 && edge.pointsTowards(node3)) return true;
//
//            if (_node1 && edge.getProximalEndpoint(node2) == Endpoint.CIRCLE) circle12 = true;
//            if (_node3 && edge.getProximalEndpoint(node2) == Endpoint.CIRCLE) circle32 = true;
//            if (circle12 && circle32 && !isAdjacentTo(node1, node2)) return true;
//        }

        return false;
    }

    public boolean isDefCollider(Node node1, Node node2, Node node3) {
        Edge edge1 = getEdge(node1, node2);
        Edge edge2 = getEdge(node2, node3);

        return !(edge1 == null || edge2 == null) && edge1.getProximalEndpoint(node2) == Endpoint.ARROW && edge2.getProximalEndpoint(node2) == Endpoint.ARROW;

    }

    /**
     * @return true iff there is a directed path from node1 to node2.
     * a
     */
    public boolean existsDirectedPathFromTo(Node node1, Node node2) {
        return existsDirectedPathVisit(node1, node2, new HashSet<Node>());
    }

    public boolean existsUndirectedPathFromTo(Node node1, Node node2) {
        return existsUndirectedPathVisit(node1, node2, new HashSet<Node>());
    }

    /**
     * Determines whether a trek exists between two nodes in the graph.  A trek
     * exists if there is a directed path between the two nodes or else, for
     * some third node in the graph, there is a path to each of the two nodes in
     * question.
     */
    public boolean existsTrek(Node node1, Node node2) {
        for (Node node : getNodes()) {
            if (isAncestorOf((node), node1) && isAncestorOf((node), node2)) {
                return true;
            }
        }

        return false;
    }

    public List<Node> getDescendants(List<Node> nodes) {
        Set<Node> descendants = new HashSet<>();

        for (Object node1 : nodes) {
            Node node = (Node) node1;
            collectDescendantsVisit(node, descendants);
        }

        return new LinkedList<>(descendants);
    }

    /**
     * @return the edge connecting node1 and node2, provided a unique such edge
     * exists.
     */
    public Edge getEdge(Node node1, Node node2) {
        List<Edge> edges = edgeLists.get(node1);

        if (edges == null) return null;

        for (Edge edge : edges) {
            if (edge.getDistalNode(node1) == node2) {
                return edge;
            }
        }

        return null;
    }

    /**
     * @return the list of parents for a node.
     */
    public List<Node> getParents(Node node) {
        List<Node> parents = new ArrayList<>();
        List<Edge> edges = edgeLists.get(node);

        for (Edge edge : new ArrayList<>(edges)) {
//            if (edge == null) continue;

            Endpoint endpoint1 = edge.getDistalEndpoint(node);
            Endpoint endpoint2 = edge.getProximalEndpoint(node);

            if (endpoint1 == Endpoint.TAIL && endpoint2 == Endpoint.ARROW) {
                parents.add(edge.getDistalNode(node));
            }
        }

        return parents;
    }

    /**
     * Determines whether one node is a descendent of another.
     */
    public boolean isDescendentOf(Node node1, Node node2) {
        return node1 == node2 || GraphUtils.existsDirectedPathFromToBreathFirst(node2, node1, this);
//        return (node1 == node2) || isProperDescendentOf(node1, node2);
    }

    /**
     * added by ekorber, 2004/06/12
     *
     * @return true iff node2 is a definite nondecendent of node1
     */
    public boolean defNonDescendent(Node node1, Node node2) {
        return !(possibleAncestor(node1, node2));
    }

    public boolean isDConnectedTo(Node x, Node y, List<Node> z) {
        return GraphUtils.isDConnectedTo(x, y, z, this);
    }

    public boolean isDSeparatedFrom(List<Node> x, List<Node> y, List<Node> z) {
        return !GraphUtils.isDConnectedTo(x, y, z, this);
    }

    /**
     * Determines whether an inducing path exists between node1 and node2, given
     * a set O of observed nodes and a set sem of conditioned nodes.
     *
     * @param node1 the first node.
     * @param node2 the second node.
     * @return true if an inducing path exists, false if not.
     */
    public boolean existsInducingPath(Node node1, Node node2) {
        return GraphUtils.existsInducingPath(node1, node2, this);
    }

    /**
     * Determines whether one node is a parent of another.
     *
     * @param node1 the first node.
     * @param node2 the second node.
     * @return true if node1 is a parent of node2, false if not.
     * @see #isChildOf
     * @see #getParents
     * @see #getChildren
     */
    public boolean isParentOf(Node node1, Node node2) {
        for (Edge edge : getEdges(node1)) {
            Node sub = Edges.traverseDirected(node1, (edge));

            if (sub == node2) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether one node is a proper ancestor of another.
     */
    public boolean isProperAncestorOf(Node node1, Node node2) {
        return node1 != node2 && isAncestorOf(node1, node2);
//        return existsDirectedPathFromTo(node1, node2);
    }

    /**
     * Determines whether one node is a proper decendent of another
     */
    public boolean isProperDescendentOf(Node node1, Node node2) {
        return node1 != node2 && isDescendentOf(node1, node2);
//        return existsDirectedPathFromTo(node2, node1);
    }

    /**
     * Transfers nodes and edges from one graph to another.  One way this is
     * used is to change graph types.  One constructs a new graph based on the
     * old graph, and this method is called to transfer the nodes and edges of
     * the old graph to the new graph.
     *
     * @param graph the graph from which nodes and edges are to be pilfered.
     * @throws IllegalArgumentException This exception is thrown if adding some
     *                                  node or edge violates one of the
     *                                  basicConstraints of this graph.
     */
    public void transferNodesAndEdges(Graph graph)
            throws IllegalArgumentException {
        if (graph == null) {
            throw new NullPointerException("No graph was provided.");
        }

        for (Node node : graph.getNodes()) {
            if (!addNode(node)) {
                throw new IllegalArgumentException();
            }
        }

        for (Edge edge : graph.getEdges()) {
            if (!addEdge(edge)) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Determines whether a node in a graph is exogenous.
     */
    public boolean isExogenous(Node node) {
        return getIndegree(node) == 0;
    }

    /**
     * @return the set of nodes adjacent to the given node. If there are multiple edges between X and Y, Y will show
     * up twice in the list of adjacencies for X, for optimality; simply create a list an and array from these to
     * eliminate the duplication.
     */
    public List<Node> getAdjacentNodes(Node node) {
        List<Edge> edges = edgeLists.get(node);
        List<Node> adj = new ArrayList<>(edges.size());

        for (Edge edge : edges) {
            adj.add(edge.getDistalNode(node));
        }

        return adj;
    }

    /**
     * @return the endpoint along the edge from node to node2 at the node2 end.
     */
    public Endpoint getEndpoint(Node node1, Node node2) {
        Edge edge = getEdge(node1, node2);
        if (edge == null) return null;
        if (edge.getDistalNode(node2) == node1) return edge.getProximalEndpoint(node2);
        return null;
    }

    /**
     * If there is currently an edge from node1 to node2, sets the endpoint at
     * node2 to the given endpoint; if there is no such edge, adds an edge --#
     * where # is the given endpoint. Setting an endpoint to null, provided
     * there is exactly one edge connecting the given nodes, removes the edge.
     * (If there is more than one edge, an exception is thrown.)
     *
     * @throws IllegalArgumentException if the edge with the revised endpoint
     *                                  cannot be added to the graph.
     */
    public boolean setEndpoint(Node from, Node to, Endpoint endPoint)
            throws IllegalArgumentException {
        Edge edge = getEdge(from, to);

        if (endPoint == null) {
            throw new NullPointerException("Endpoint not specified");
        } else if (edge == null) {
            throw new NullPointerException("No such edge.");
        } else {
            Edge newEdge = new Edge(from, to, edge.getProximalEndpoint(from), endPoint);
            removeEdge(edge);
            addEdge(newEdge);
            return true;
        }
    }

    /**
     * Adds an edge to the graph if the grpah constraints permit it.
     *
     * @param edge the edge to be added
     * @return true if the edge was added, false if not.
     */
    public synchronized boolean addEdge(Edge edge) {
        if (edge == null) throw new NullPointerException();

        if (isAdjacentTo(edge.getNode1(), edge.getNode2())) {
            throw new IllegalArgumentException("Already adjacent.");
        }

        List<Edge> edgeList1 = edgeLists.get(edge.getNode1());
        List<Edge> edgeList2 = edgeLists.get(edge.getNode2());

        if (edgeList1 == null || edgeList2 == null) {
            throw new NullPointerException("Can't add an edge unless both " +
                    "nodes are in the graph: " + edge);
        }

        edgeList1 = new ArrayList<>(edgeList1);
        edgeList2 = new ArrayList<>(edgeList2);

        edgeList1.add(edge);
        edgeList2.add(edge);

        edgeLists.put(edge.getNode1(), edgeList1);
        edgeLists.put(edge.getNode2(), edgeList2);

        edgesSet.add(edge);

        return true;
    }

    /**
     * Adds a PropertyChangeListener to the graph.
     *
     * @param l the property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        getPcs().addPropertyChangeListener(l);
    }

    /**
     * Adds a node to the graph. Precondition: The proposed name of the node
     * cannot already be used by any other node in the same graph.
     *
     * @param node the node to be added.
     * @return true if the the node was added, false if not.
     */
    public boolean addNode(Node node) {
        if (node == null) {
            throw new NullPointerException();
        }

        if (edgeLists.containsKey(node)) {
            return false;
        }

        edgeLists.put(node, new ArrayList<Edge>());
        nodes.add(node);
        namesHash.put(node.getName(), node);

        return true;
    }

    /**
     * @return the list of edges connected to a particular node. No particular
     * ordering of the edges in the list is guaranteed.
     */
    public synchronized List<Edge> getEdges(Node node) {
        return edgeLists.get(node);
    }

    /**
     * Resets the graph so that it is fully connects it using #-# edges, where #
     * is the given endpoint.
     */
    public void fullyConnect(Endpoint endpoint) {
        edgesSet.clear();
        edgeLists.clear();

        for (Node node : nodes) {
            edgeLists.put(node, new ArrayList<Edge>());
        }

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node node1 = nodes.get(i);
                Node node2 = nodes.get(j);

                Edge edge = new Edge(node1, node2, endpoint, endpoint);
                addEdge(edge);
            }
        }
    }

    public void reorientAllWith(Endpoint endpoint) {
        for (Edge edge : new ArrayList<>(edgesSet)) {
            Node a = edge.getNode1();
            Node b = edge.getNode2();
            setEndpoint(a, b, endpoint);
            setEndpoint(b, a, endpoint);
        }
    }

    /**
     * @return the node with the given name, or null if no such node exists.
     */
    public Node getNode(String name) {
        Node node = namesHash.get(name);

        if (node == null /*|| !name.equals(node.getNode())*/) {
            namesHash = new HashMap<>();

            for (Node _node : nodes) {
                namesHash.put(_node.getName(), _node);
            }

            node = namesHash.get(name);
        }

        return node;
    }

    /**
     * Removes an edge from the graph. (Note: It is dangerous to make a
     * recursive call to this method (as it stands) from a method containing
     * certain types of iterators. The problem is that if one uses an iterator
     * that iterates over the edges of node A or node B, and tries in the
     * process to remove those edges using this method, a concurrent
     * modification exception will be thrown.)
     *
     * @param edge the edge to remove.
     * @return true if the edge was removed, false if not.
     */
    public synchronized boolean removeEdge(Edge edge) {
        List<Edge> edgeList1 = edgeLists.get(edge.getNode1());
        List<Edge> edgeList2 = edgeLists.get(edge.getNode2());

        edgeList1 = new ArrayList<>(edgeList1);
        edgeList2 = new ArrayList<>(edgeList2);

        edgesSet.remove(edge);
        edgeList1.remove(edge);
        edgeList2.remove(edge);
        highlightedEdges.remove(edge);
        stuffRemovedSinceLastTripleAccess = true;

        edgeLists.put(edge.getNode1(), edgeList1);
        edgeLists.put(edge.getNode2(), edgeList2);

        getPcs().firePropertyChange("edgeRemoved", edge, null);
        return true;
    }


    /**
     * Removes a node from the graph.
     */
    public boolean removeNode(Node node) {
//        if (nodes.contains(node) && !checkRemoveNode(node)) {
//            return false;
//        }

        boolean changed = false;
        List<Edge> edgeList1 = edgeLists.get(node);    //list of edges connected to that node

        for (Edge edge : new ArrayList<>(edgeList1)) {
            Node node2 = edge.getDistalNode(node);

            if (node2 != node) {
                List<Edge> edgeList2 = edgeLists.get(node2);
                edgeList2.remove(edge);
                edgesSet.remove(edge);
                changed = true;
            }

            edgeList1.remove(edge);
        }

//        for (Iterator<Edge> i = edgeList1.iterator(); i.hasNext(); ) {
//            Edge edge = (i.next());
//            Node node2 = edge.getDistalNode(node);
//
//            if (node2 != node) {
//                List<Edge> edgeList2 = edgeLists.get(node2);
//                edgeList2.remove(edge);
//                edgesSet.remove(edge);
//                changed = true;
//            }
//
//            i.remove();
////            getPcs().firePropertyChange("edgeRemoved", edge, null);
//        }

        edgeLists.remove(node);
        nodes.remove(node);
        namesHash.remove(node.getName());
        stuffRemovedSinceLastTripleAccess = true;

        getPcs().firePropertyChange("nodeRemoved", node, null);
        return changed;
    }

    //===============================PRIVATE METHODS======================//

    public void removeTriplesNotInGraph() {
//        if (!stuffRemovedSinceLastTripleAccess) return;

        for (Triple triple : new HashSet<>(ambiguousTriples)) {
            if (!containsNode(triple.getX()) || !containsNode(triple.getY()) || !containsNode(triple.getZ())) {
                ambiguousTriples.remove(triple);
                continue;
            }

            if (!isAdjacentTo(triple.getX(), triple.getY()) || !isAdjacentTo(triple.getY(), triple.getZ())) {
                ambiguousTriples.remove(triple);
            }
        }

        for (Triple triple : new HashSet<>(underLineTriples)) {
            if (!containsNode(triple.getX()) || !containsNode(triple.getY()) || !containsNode(triple.getZ())) {
                underLineTriples.remove(triple);
                continue;
            }

            if (!isAdjacentTo(triple.getX(), triple.getY()) || !isAdjacentTo(triple.getY(), triple.getZ())) {
                underLineTriples.remove(triple);
            }
        }

        for (Triple triple : new HashSet<>(dottedUnderLineTriples)) {
            if (!containsNode(triple.getX()) || !containsNode(triple.getY()) || !containsNode(triple.getZ())) {
                dottedUnderLineTriples.remove(triple);
                continue;
            }

            if (!isAdjacentTo(triple.getX(), triple.getY()) || !isAdjacentTo(triple.getY(), triple.getZ())) {
                dottedUnderLineTriples.remove(triple);
            }
        }

        stuffRemovedSinceLastTripleAccess = false;
    }

    private void collectDescendantsVisit(Node node, Set<Node> descendants) {
        descendants.add(node);
        List<Node> children = getChildren(node);

        if (!children.isEmpty()) {
            for (Object aChildren : children) {
                Node child = (Node) aChildren;
                doChildClosureVisit(child, descendants);
            }
        }
    }

    /**
     * closure under the child relation
     */
    private void doChildClosureVisit(Node node, Set<Node> closure) {
        if (!closure.contains(node)) {
            closure.add(node);

            for (Edge edge1 : getEdges(node)) {
                Node sub = Edges.traverseDirected(node, edge1);

                if (sub == null) {
                    continue;
                }

                doChildClosureVisit(sub, closure);
            }
        }
    }

    /**
     * @return this object.
     */
    private PropertyChangeSupport getPcs() {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        return pcs;
    }

    /**
     * @return true iff there is a directed path from node1 to node2.
     */
    private boolean existsUndirectedPathVisit(Node node1, Node node2, Set<Node> path) {
        path.add(node1);

        for (Edge edge : getEdges(node1)) {
            Node child = Edges.traverse(node1, edge);

            if (child == null) {
                continue;
            }

            if (child == node2) {
                return true;
            }

            if (path.contains(child)) {
                continue;
            }

            if (existsUndirectedPathVisit(child, node2, path)) {
                return true;
            }
        }

        path.remove(node1);
        return false;
    }

    private boolean existsDirectedPathVisit(Node node1, Node node2, Set<Node> path) {
        path.add(node1);

        for (Edge edge : getEdges(node1)) {
            Node child = Edges.traverseDirected(node1, edge);

            if (child == null) {
                continue;
            }

            if (child == node2) {
                return true;
            }

            if (path.contains(child)) {
                continue;
            }

            if (existsDirectedPathVisit(child, node2, path)) {
                return true;
            }
        }

        path.remove(node1);
        return false;
    }

    public List<Node> getCausalOrdering() {
        return GraphUtils.getCausalOrdering(this);
    }

    public void setHighlighted(Edge edge, boolean highlighted) {
        highlightedEdges.add(edge);
    }

    public boolean isHighlighted(Edge edge) {
        return highlightedEdges.contains(edge);
    }

    public boolean isParameterizable(Node node) {
        return true;
    }

    public boolean isTimeLagModel() {
        return false;
    }

    public TimeLagGraph getTimeLagGraph() {
        return null;
    }

    /**
     * Adds semantic checks to the default deserialization method. This method
     * must have the standard signature for a readObject method, and the body of
     * the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from
     * version to version. A readObject method of this form may be added to any
     * class, even if Tetrad sessions were previously saved out using a version
     * of the class that didn't include it. (That's what the
     * "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for help.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (nodes == null) {
            throw new NullPointerException();
        }

        if (edgesSet == null) {
            throw new NullPointerException();
        }

        if (edgeLists == null) {
            throw new NullPointerException();
        }

        if (ambiguousTriples == null) {
            ambiguousTriples = new HashSet<>();
        }

        if (highlightedEdges == null) {
            highlightedEdges = new HashSet<>();
        }

        if (underLineTriples == null) {
            underLineTriples = new HashSet<>();
        }

        if (dottedUnderLineTriples == null) {
            dottedUnderLineTriples = new HashSet<>();
        }
    }

    public void changeName(String name, String newName) {
        Node node = namesHash.get(name);
        namesHash.remove(name);
        node.setName(newName);
        namesHash.put(newName, node);
    }
}




