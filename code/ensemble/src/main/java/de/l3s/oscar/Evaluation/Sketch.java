package de.l3s.oscar.Evaluation;
/*
 *    Sketch.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
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

//package moa.streams.twitter;

/**
 * Interface of a Sketch
 * for counting frequent items, actually String tokens in this version
 *
 * @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 *
 *
 */
public interface Sketch {

    public int addToken(String token, int freq); //insert()

    public int addToken(String token, int freq, int classIndex);

    public void showNodes();

    public void addDoc(double docSize);

    public double getCount(String token);

    public int getAttIndex(String token);

    public double getFreqWord(String token);

    public void remove(String token);
}


