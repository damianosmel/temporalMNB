package de.l3s.oscar.Evaluation;

/**
 * Created by damian on 27.09.17.
 */
/*
 *    SpaceSavingExpDecay.java
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

/**
 * Implementation of the so called SpaceSaving algorithm
 * for counting frequent items, actually String tokens in this version
 * using exponential decays. Cormode et al.
 *
 */
public class SpaceSavingExpDecay extends SpaceSaving {

    protected double lambda = 0.01;

    public void setLambda(double l) {
        this.lambda = l;
    }

    public double getLambda() {
        return this.lambda;
    }

    /**
     * Retrieve the (estimated, upper-bound) count for any token
     *
     * @param token
     * @return count (or 0 if not being counted currently, i.e. not frequent enough)
     */
    @Override
    public double getCount(String token) {
        Node node = _map.get(token);
        if (node == null) {
            return 0.0;
        } else {
            return node.count * Math.exp(-lambda * this.numDoc);
        }
    }

    @Override
    protected boolean addCount(Node node, int freq) {
        double c = node.count;
        node.addCount(((double) freq) * Math.exp(lambda * (double) this.numDoc), this.numDoc);
        return false;
    }

    @Override
    public double getFreqWord(String word) {
        return getCount(word);
    }
}