package de.l3s.oscar.Evaluation;

/*
 *    SpaceSaving.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Bernhard Pfahringer (bernhard at cs dot waikato dot ac dot nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import com.github.javacliparser.IntOption;
import moa.options.AbstractOptionHandler;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of the so called SpaceSaving algorithm
 * for counting frequent items, actually String tokens in this version
 *
 */
public class SpaceSaving extends AbstractOptionHandler implements Sketch {

    private static final long serialVersionUID = 1L;

    public IntOption capacityOption = new IntOption("capacity", 'c', "Number of attributes to use", 10000);

    protected int _top = 0;

    protected Map<String, Node> _map = null;

    protected ArrayList<Node> _nodes = null;

    protected int numDoc = 0;

    protected double numTerms = 0;

    /**
     * Initialise a set of counters for String tokens
     *
     */
    @Override
    protected void prepareForUseImpl(TaskMonitor arg0, ObjectRepository arg1) {
        _top = 0;
        _map = new HashMap<String, Node>();
        _nodes = new ArrayList<Node>();
    }

    public int addToken(String token, int freq, int classIndex) {
        return addToken(token, freq);
    }

    /**
     * Retrieve the (estimated, upper-bound) count for any token
     *
     * @param token
     * @return count (or 0 if not being counted currently, i.e. not frequent enough)
     */
    public double getCount(String token) {
        Node node = _map.get(token);
        if (node == null) {
            return 0.0;
        } else {
            return node.getCount();
        }
    }

    /**
     * Get the attribute index of the token specified.
     *
     * @param token
     * @return
     */
    public int getAttIndex(String token) {
        Node node = _map.get(token);
        if (node != null) {
            return node.attrIndex;
        } else {
            return -1;
        }
    }

    /**
     * Add a token to be counted.
     *
     * @param token
     * @param freq
     * @return attributeIndex which is useful for constructing Instance objects
     */
    public int addToken(String token, int freq) {
        Node node = _map.get(token);
        if (node == null) {
            if (_top < this.capacityOption.getValue()) {
                fill(token, freq);
            } else {
                newToken(token, freq);
            }
        } else {
            addCount(node, freq);
            updatePosition(node);
        }

        return _map.get(token).attrIndex;
    }

    protected void fill(String token, int freq) {
        Node node = newNode(token, _top, freq);
        _nodes.add(node);
        _map.put(token, node);
        updatePosition(node);
        _top++;
    }

    protected void newToken(String token, int freq) {
        Node node = _nodes.get(0);
        _map.remove(node.token);
        _map.put(token, node);
        node.token = token;
        addCount(node, freq);
        updatePosition(node);
    }

    protected void updatePosition(Node node) {
        boolean isDescending = false;

        int offset = node.index + 1;
        while ((offset < _nodes.size()) && (node.getCount() > _nodes.get(offset).getCount())) {
            offset++;
        }
        offset--;

        //Descend in the list order if count was reduced
        if (offset == node.index) {
            offset = node.index - 1;
            while ((offset >= 0) && (node.count < _nodes.get(offset).getCount())) {
                offset--;
                isDescending = true;
            }
            offset++;
        }

        if (offset != node.index) {
            int oldIndex = node.index;

            if (isDescending) {
                _nodes.add(offset, node);
                //showNodes();System.out.println("===");
                _nodes.remove(oldIndex + 1);
            } else {
                _nodes.add(offset + 1, node);
                _nodes.remove(oldIndex);
            }
        }
        //System.out.println("Updating "+node);
        if (_nodes != null) {
            for (int i = 0; i < _nodes.size(); i++) {
                _nodes.get(i).index = i;
            }
        }
        //showNodes();
    }

    /**
     * Print all nodes to System.out, useful for debugging
     *
     */
    public void showNodes() {
        for (Node node : _nodes) {
            if (node != null) {
                System.out.println(node);
            }
        }
    }

    protected boolean addCount(Node node, int freq) {
        node.addCount((double) freq, this.numDoc);
        return false;
    }

    public void addDoc(double docSize) {
        this.numDoc++;
        this.numTerms += docSize;
    }

    protected Node newNode(String token, int index, int freq) {
        return new Node(token, index, freq);
    }

    public void remove(String token) {
    }

    static class Node implements Serializable, Comparable<Node> {

        private static final long serialVersionUID = 1L;

        int index;

        int attrIndex;

        double count;

        String token;

        Node() {
        }

        Node(String token, int index, int freq) {
            this.index = index;
            this.attrIndex = index;
            this.token = token;
            initCount(freq);
        }

        protected boolean addCount(double freq, int doc) {
            // We don't use doc, only on SpaceSavingAdwin
            this.count += freq;
            return false;
        }

        protected double getCount() {
            return this.count;
        }

        protected String getToken() {
            return this.token;
        }

        protected void initCount(int freq) {
            this.count = freq;
        }

        public int compareTo(Node other) {
            if (getCount() < other.getCount()) {
                return -1;
            }
            if (getCount() > other.getCount()) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "" + index + "," + attrIndex + "," + count + "," + token;
        }
    }

//    @Override
    public void getDescription(StringBuilder arg0, int arg1) {
    }

    public double getFreqWord(String word) {
        return getCount(word) / this.numTerms;
    }
}

