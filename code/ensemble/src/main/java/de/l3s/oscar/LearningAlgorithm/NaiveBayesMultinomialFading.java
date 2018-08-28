package de.l3s.oscar.LearningAlgorithm;
/*
 *    NaiveBayesMultinomial.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

import moa.classifiers.AbstractClassifier;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import com.github.javacliparser.FloatOption;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import weka.core.Instances;
import weka.core.Utils;

import java.util.Arrays;
import java.util.HashMap;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * <!-- globalinfo-start --> Class for building and using a multinomial Naive
 * Bayes classifier. Performs text classic bayesian prediction while making
 * naive assumption that all inputs are independent. For more information
 * see,<br/> <br/> Andrew Mccallum, Kamal Nigam: A Comparison of Event Models
 * for Naive Bayes Text Classification. In: AAAI-98 Workshop on 'Learning for
 * Text Categorization', 1998.<br/> <br/> The core equation for this
 * classifier:<br/> <br/> P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)<br/>
 * <br/> where Ci is class i and D is a document.<br/> <br/> Incremental version
 * of the algorithm.
 * <p/>
 * <!-- globalinfo-end --> * <!-- technical-bibtex-start --> BibTeX:
 * <pre>
 * &#64;inproceedings{Mccallum1998,
 *    author = {Andrew Mccallum and Kamal Nigam},
 *    booktitle = {AAAI-98 Workshop on 'Learning for Text Categorization'},
 *    title = {A Comparison of Event Models for Naive Bayes Text Classification},
 *    year = {1998}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 */
public class NaiveBayesMultinomialFading extends SlidingWindowClassifier {

    public FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection",
            'l', "Laplace correction factor.",
            1.0, 0.00, Integer.MAX_VALUE);

    /**
     * for serialization
     */
    private static final long serialVersionUID = -7204398796974263187L;

    @Override
    public String getPurposeString() {
        return "MNBFading: Classifier using faded feature values based on feature time occurrences.";
    }

    /**
     * sum of weight_of_instance * word_count_of_instance for each class
     */
    protected double[] m_classTotals;

    /**
     * copy of header information for use in toString method
     */
    protected Instances m_headerInfo;

    /**
     * number of class values
     */
    protected int m_numClasses;

    /**
     * the probability of a class (i.e. Pr[H])
     */
    protected double[] m_probOfClass;

    /**
     * storing time
     */
    protected DoubleVector[] lastObservedTime;
    protected double[] lastObservedClassTime;
    private double decayDegree = 0.0;//0.0;
    protected String aggregationGranularity;
    protected HashMap<Integer, String> index2AttrName = new HashMap<>(); //copy with replaced words from sketch
    protected int dateAttributeIndex = 1;

    //perform decaying over number of instances
    protected int numberOfInstances;
    protected double instNumInPeriod;
    /**
     * probability that a word (w) exists in a class (H) (i.e. Pr[w|H]) The
     * matrix is in the this format: m_wordTotalForClass[wordAttribute][class]
     */
    protected DoubleVector[] m_wordTotalForClass;

    protected boolean reset = false;

    public NaiveBayesMultinomialFading(double decayDegree, String aggregationGranularity, double instNumInPeriod) {
        System.out.println("=== ===");
        System.out.println(this.getPurposeString());
        System.out.println("=== ===");
        this.decayDegree = decayDegree;
        this.aggregationGranularity = aggregationGranularity;
        this.instNumInPeriod = instNumInPeriod;
    }

    @Override
    public void resetLearningImpl() {
        this.reset = true;
    }

    public void setDecayDegree(double decayDegree) {
        this.decayDegree = decayDegree;
    }

    //this is the fading function
    private double decay(double time) {
        //Time is delta time (In this implementation, time interval between current time and last observed time)
        if (time == 0.0)
            return 1.0;
        double exp = (-1.0) * time * this.decayDegree;

        double returnValue = Math.pow(2.0, exp);
        return returnValue;
    }

    protected double aggregateCountsForAllLabels(double[] countsArray, double[] decayFactorArray) {
        double sumCountsAllLabels = 0.0D;

        for (int classIndex = 0; classIndex < this.numClasses; classIndex++) {
            sumCountsAllLabels += countsArray[classIndex] * decayFactorArray[classIndex] + this.laplaceCorrectionOption.getValue();
        }

        return sumCountsAllLabels;
    }

    protected double aggregateCountsForAllLabels(DoubleVector[] countsVectorAllAttr, int attrIndex, double[] decayFactorArray) {
        double sumCountsAllLabels = 0.0D;

        for (int classIndex = 0; classIndex < this.numClasses; classIndex++) {
            sumCountsAllLabels += countsVectorAllAttr[classIndex].getValue(attrIndex) * decayFactorArray[classIndex] + this.laplaceCorrectionOption.getValue();
        }

        return sumCountsAllLabels;
    }


    /**
     * Trains the classifier with the given instance.
     */
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.reset == true) {
            this.m_numClasses = inst.numClasses();
            double laplace = this.laplaceCorrectionOption.getValue();
            int numAttributes = getNumberOfRealAttributes(inst);

            this.m_probOfClass = new double[this.m_numClasses];
            Arrays.fill(this.m_probOfClass, laplace);

            this.m_classTotals = new double[this.m_numClasses];
            Arrays.fill(this.m_classTotals, laplace * numAttributes);

            this.m_wordTotalForClass = new DoubleVector[this.m_numClasses];
            for (int i = 0; i < this.m_numClasses; i++) {
                this.m_wordTotalForClass[i] = new DoubleVector();
            }

            this.lastObservedClassTime = new double[this.m_numClasses];
            Arrays.fill(this.lastObservedClassTime, 0.0D);

            this.lastObservedTime = new DoubleVector[this.m_numClasses];
            for (int i = 0; i < this.m_numClasses; i++) {
                this.lastObservedTime[i] = new DoubleVector();
            }

            this.reset = false;
        }
        numberOfInstances++; //data set without time, update # of instances
        // Update classifier
        int classIndex = inst.classIndex();
        int classValue = (int) inst.value(classIndex);

        double instanceWeight = inst.weight();
        this.m_probOfClass[classValue] += instanceWeight;

        this.m_classTotals[classValue] += instanceWeight * totalSize(inst);

        /*
        //data set with time stamp
        double timestamp = inst.valueSparse(this.dateAttributeIndex);
        //Update the class observation times
        if (timestamp > lastObservedClassTime[classValue]) {
            lastObservedClassTime[classValue] = timestamp;
        }
        */
        if (this.numberOfInstances > lastObservedClassTime[classValue]){
            lastObservedClassTime[classValue] = this.numberOfInstances;
        }
        String attrName;
        boolean indexChangedName;
        double laplaceCorrection;
        for (int i = 0; i < inst.numValues(); i++) {
            int index = inst.index(i);
            if (index != classIndex && !inst.isMissing(i) && !inst.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                laplaceCorrection = 0.0;

                attrName = inst.attribute(index).name();
                indexChangedName = indexChangedName(index, attrName);
                if (indexChangedName) { //if word replaced another word
                    for (int classInd = 0; classInd < this.m_numClasses; classInd++) { //reset word class conditional counts
                        this.m_wordTotalForClass[classInd].setValue(index, 0.0D);
                    }
                    /**
                     * for new word reset its' time stamp
                     * */
//                    lastObservedTime[classValue].setValue(index, timestamp); //data set with time stamp
                    lastObservedTime[classValue].setValue(index, this.numberOfInstances);
                }

                /*
                //data set with time stamp
                if (timestamp > lastObservedTime[classValue].getValue(index)) { //update time stamps
                    lastObservedTime[classValue].setValue(index, timestamp);
                }
                */
                if(this.numberOfInstances > lastObservedTime[classValue].getValue(index)){
                    lastObservedTime[classValue].setValue(index, this.numberOfInstances);
                }
                if (m_wordTotalForClass[classValue].getValue(index) == 0) {
                    laplaceCorrection = this.laplaceCorrectionOption.getValue();
                }

                m_wordTotalForClass[classValue].addToValue(index, instanceWeight * inst.valueSparse(i) + laplaceCorrection);

                this.index2AttrName.put(index, attrName); //add the relation index -> attribute name
            }
        }
    }

    protected boolean indexChangedName(int index, String attributeName) {
        if (!this.index2AttrName.containsKey(index)) { //if I have not seen this index then the index name relation is not changed
            return false;
        } else {
            if (this.index2AttrName.get(index).equalsIgnoreCase(attributeName)) {
                return false;
            } else {
                return true;
            }
        }
    }

    protected double convertInterval2InstPeriod(double timeInterval){
        return timeInterval / this.instNumInPeriod;
    }

    protected double convertInterval2AggregationPeriod(double timeInterval) {
        if (this.aggregationGranularity.equalsIgnoreCase("hour")) {
            return timeInterval / (1000.0 * 60.0 * 60.0); // 1hr = 60 m * 60 s
        } else if (this.aggregationGranularity.equalsIgnoreCase("sec")) {
            return timeInterval / 1000.0;
        } else {
            return 0.0D;
        }
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     * <p>
     * <p>
     * "Based on Ageing-based MNB Classifisiers Over Opinionated Data Streams",  Wagner et al. 2015
     * Section 4.2 Page 407
     * Idea: fade the conditional count of each attribute depending on the interval of its observations
     * Pseudocode:
     * timeObserved = firstTime
     * while(time){
     *      if(time > timeObserved){
     *          countAttrClass = countAttrClass * decay(time, timeObserved)
     *          timeObserved = time
     *      }
     * }
     */
    @Override
    public double[] getVotesForInstance(Instance instance) {

        if (this.reset == true) {
            return new double[instance.numClasses()];
        }
        double[] probOfClassGivenDoc = new double[this.m_numClasses];
        double totalSize = totalSize(instance);
//        double timestamp = instance.valueSparse(this.dateAttributeIndex); //data with time stamp

        double[] decayFactorPerClass = new double[this.m_numClasses];
        Arrays.fill(decayFactorPerClass, 0.0D);

        /**
         * calculate decay for class
         * */
        /*
        //data set with time stamp
        for (int classIndex = 0; classIndex < this.m_numClasses; classIndex++) {
            double classInterval = 0.0;
            if (timestamp > this.lastObservedClassTime[classIndex]) {
                classInterval = timestamp - lastObservedClassTime[classIndex];
                classInterval = convertInterval2AggregationPeriod(classInterval);
            }
            decayFactorPerClass[classIndex] = decay(classInterval);
        }
        */

        //data set without time stamp
        for(int classIndex = 0; classIndex < this.m_numClasses; classIndex++){
            double classInterval = 0.0;
            if(this.numberOfInstances > this.lastObservedClassTime[classIndex]){
                classInterval = this.numberOfInstances - lastObservedClassTime[classIndex];
                classInterval = convertInterval2InstPeriod(classInterval);
            }
            decayFactorPerClass[classIndex] = decay(classInterval);
        }

        /**
         * calculate decayed probabilities for class
         * */
        double sumDecayProbClass = aggregateCountsForAllLabels(this.m_probOfClass, decayFactorPerClass);
        double sumDecayTotalClass = aggregateCountsForAllLabels(this.m_classTotals, decayFactorPerClass);
        for (int classIndex = 0; classIndex < this.m_numClasses; classIndex++) {
            probOfClassGivenDoc[classIndex] = Math.log(((this.m_probOfClass[classIndex] * decayFactorPerClass[classIndex]) + this.laplaceCorrectionOption.getValue()) / sumDecayProbClass) - totalSize * Math.log(((this.m_classTotals[classIndex] * decayFactorPerClass[classIndex]) + this.laplaceCorrectionOption.getValue()) / sumDecayTotalClass);
        }

        double sumAttribute;
        double wordCount;
        String attrName;
        boolean indexChangedName;
        double lastTimeWordInClass = 0.0;
        double timeInterval;
        int numOfChangedNames = 0;
        for (int i = 0; i < instance.numValues(); i++) {

            int index = instance.index(i);
            if (index != instance.classIndex() && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                Arrays.fill(decayFactorPerClass, 0.0D);
                wordCount = instance.valueSparse(i);
                attrName = instance.attribute(index).name();
                indexChangedName = indexChangedName(index, attrName);
                if (indexChangedName) {
//                    System.out.println("test: we replaced " + this.index2AttrName.get(index) + " with " + attrName);
                    numOfChangedNames++;
                }
                /**
                 * calculate decay for class
                 * */
                for (int classIndex = 0; classIndex < this.m_numClasses; classIndex++) {
                    if (indexChangedName) { //if the word is new then we don't have history information
//                        lastTimeWordInClass = timestamp; //data set with time stamp
                        lastTimeWordInClass = this.numberOfInstances;
                    } else { //get last time we observed word
                        lastTimeWordInClass = this.lastObservedTime[classIndex].getValue(index);
                    }
                    timeInterval = 0.0;
/*
                    //data set with time stamp
                    if (timestamp > lastTimeWordInClass) {
                        timeInterval = timestamp - lastTimeWordInClass;
                        timeInterval = convertInterval2AggregationPeriod(timeInterval);
                    }
                    decayFactorPerClass[classIndex] = decay(timeInterval);

*/
                    if(this.numberOfInstances > lastTimeWordInClass){
                        timeInterval = this.numberOfInstances - lastTimeWordInClass;
                        timeInterval = convertInterval2InstPeriod(timeInterval);
                    }
                    decayFactorPerClass[classIndex] = decay(timeInterval);
                }

                /**
                 * calculate decayed probabilities for each attribute
                 * */
                sumAttribute = aggregateCountsForAllLabels(this.m_wordTotalForClass, index, decayFactorPerClass);
                for (int classIndex = 0; classIndex < this.m_numClasses; classIndex++) {
                    probOfClassGivenDoc[classIndex] += wordCount * Math.log(((this.m_wordTotalForClass[classIndex].getValue(index) * decayFactorPerClass[classIndex]) + this.laplaceCorrectionOption.getValue()) / sumAttribute);

                }
            }
        }
        return Utils.logs2probs(probOfClassGivenDoc);
    }


    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder result, int indent) {
        StringUtils.appendIndented(result, indent, "xxx MNB1 xxx\n\n");

        result.append("The independent probability of a class\n");
        result.append("--------------------------------------\n");

        for (int c = 0; c < m_numClasses; c++) {
            result.append(m_headerInfo.classAttribute().value(c)).append("\t").
                    append(Double.toString(m_probOfClass[c])).append("\n");
        }

        result.append("\nThe probability of a word given the class\n");
        result.append("-----------------------------------------\n\t");

        for (int c = 0; c < m_numClasses; c++) {
            result.append(m_headerInfo.classAttribute().value(c)).append("\t");
        }

        result.append("\n");

        for (int w = 0; w < m_headerInfo.numAttributes(); w++) {
            if (w == m_headerInfo.classIndex()) {
                continue;
            }
            result.append(m_headerInfo.attribute(w).name()).append("\t");
            for (int c = 0; c < m_numClasses; c++) {
                double value = m_wordTotalForClass[c].getValue(w);
                if (value == 0) {
                    value = this.laplaceCorrectionOption.getValue();
                }
                result.append(value / m_classTotals[c]).append("\t");
            }
            result.append("\n");
        }
        StringUtils.appendNewline(result);
    }


    public boolean isRandomizable() {
        return false;
    }
}
