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

package edu.cmu.tetrad.data;

import edu.cmu.tetrad.graph.IndependenceFact;
import edu.cmu.tetrad.graph.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores a list of independence facts.
 *
 * @author Joseph Ramsey
 */
public class IndependenceFacts implements DataModel {
    static final long serialVersionUID = 23L;

    private Set<IndependenceFact> unsortedFacts = new HashSet<>();
    private String name = "";
    private IKnowledge knowledge = new Knowledge2();
    private List<Node> order;

    public IndependenceFacts() {
        // blank
    }

    public IndependenceFacts(IndependenceFacts facts) {
        this();
        this.unsortedFacts = new HashSet<>(facts.unsortedFacts);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static IndependenceFacts serializableInstance() {
        return new IndependenceFacts();
    }

    public void add(IndependenceFact fact) {
        this.unsortedFacts.add(fact);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (IndependenceFact fact : unsortedFacts) {
            builder.append(fact).append("\n");
        }

        return builder.toString();
    }

    @Override
    public boolean isContinuous() {
        return false;
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public boolean isMixed() {
        return false;
    }

    @Override
    public Node getVariable(String name) {
        return null;
    }

    @Override
    public DataModel copy() {
        return null;
    }

    public void remove(IndependenceFact fact) {
//        this.facts.remove(fact);
        this.unsortedFacts.remove(fact);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIndependent(Node x, Node y, Node... z) {
        IndependenceFact fact = new IndependenceFact(x, y, z);
        return unsortedFacts.contains(fact);
    }

    public boolean isIndependent(Node x, Node y, List<Node> z) {
        IndependenceFact fact = new IndependenceFact(x, y, z);
        boolean contains = unsortedFacts.contains(fact);
//        if (contains) {
//            System.out.println("Looking up " + fact + (contains ? " Independence fact holds" : ""));
//        }
        return contains;
    }

    public IKnowledge getKnowledge() {
        return this.knowledge;
    }

    public void setKnowledge(IKnowledge knowledge) {
        if (knowledge == null) throw new NullPointerException();
        this.knowledge = knowledge;
    }

    public List<Node> getVariables() {
        Set<Node> variables = new HashSet<>();

        for (IndependenceFact fact : unsortedFacts) {
            variables.add(fact.getX());
            variables.add(fact.getY());
            variables.addAll(fact.getZ());
        }

        if (order != null) {
            if (new HashSet<>(variables).equals(new HashSet<>(order))) {
                return order;
            } else {
                throw new IllegalArgumentException("The supplied order is not precisely for the variables in the facts.");
            }
        }

        return new ArrayList<>(variables);
    }

    public List<String> getVariableNames() {
        List<Node> variables = getVariables();
        List<String> names = new ArrayList<>();

        for (Node node : variables) {
            names.add(node.getName());
        }

        return names;
    }

    public void setOrder(List<Node> order) {
        this.order = order;
    }
}



