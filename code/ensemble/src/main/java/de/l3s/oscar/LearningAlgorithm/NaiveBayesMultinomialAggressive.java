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
 * <!-- globalinfo-end -->* <!-- technical-bibtex-start --> BibTeX:
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
public class NaiveBayesMultinomialAggressive extends SlidingWindowClassifier {

    public FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection",
            'l', "Laplace correction factor.",
            1.0, 0.00, Integer.MAX_VALUE);

    /**
     * for serialization
     */
    private static final long serialVersionUID = -7204398796974263187L;

    @Override
    public String getPurposeString() {
        return "MNBAggressive: Classifier using added faded feature values based on feature time occurrences.";
    }

    /**
     * sum of weight_of_instance * word_count_of_instance for each class
     */
    protected double[] m_classTotals;
    protected double[] m_temporalClassTotals;
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
    protected double[] m_temporalProbOfClass;
    /**
     * storing time
     */
    protected DoubleVector[] lastObservedWordTime;
    protected double[] lastObservedClassTime;
    private double decayDegree = 0.0;//0.0;
    protected String aggregationGranularity;
    protected HashMap<Integer, String> index2AttrName = new HashMap<>(); //copy with replaced words from sketch
    protected int dateAttributeIndex = 1;

    protected int numberOfInstances;
    protected double instNumInPeriod;
    
    private double smallestProb = 200.0;

    /**
     * probability that a word (w) exists in a class (H) (i.e. Pr[w|H]) The
     * matrix is in the this format: m_wordTotalForClass[wordAttribute][class]
     */
    protected DoubleVector[] m_wordTotalForClass;

    protected DoubleVector[] m_temporalWordTotalForClass;

    protected boolean reset = false;

    public NaiveBayesMultinomialAggressive(double decayDegree, String aggregationGranularity, double instNumInPeriod) {
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
        //System.out.println("exp: " + exp);
        double returnValue = Math.pow(2.0, exp);
        //System.out.println("Return Value: " + returnValue);
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
     * <p>
     * "Based on Ageing-based MNB Classifisiers Over Opinionated Data Streams",  Wagner et al. 2015
     * Section 4.3 Page 408
     * Idea: accumulate faded counts of each attribute
     * Pseudocode:
     * timeObserved = firstTime
     * while(time){
     *      if (time == timeObserved){
     *          temporalCountAttrClass++
     *      }
     *      else{
     *          countAttrClass += temporalCountAttrClass * decay(timeObserved,time)
     *          timeObserved = time
     *          temporalCountAttrClass = 0
     *      }
     * }
     */
    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.reset == true) {
            this.m_numClasses = inst.numClasses();
            double laplace = this.laplaceCorrectionOption.getValue();
            int numAttributes = getNumberOfRealAttributes(inst);

            m_probOfClass = new double[m_numClasses];
            Arrays.fill(m_probOfClass, laplace);

            m_temporalProbOfClass = new double[m_numClasses];
            Arrays.fill(m_temporalProbOfClass, laplace);

            m_classTotals = new double[m_numClasses];
            Arrays.fill(m_classTotals, laplace * numAttributes);

            m_temporalClassTotals = new double[m_numClasses];
            Arrays.fill(m_temporalClassTotals, laplace * numAttributes);

            m_wordTotalForClass = new DoubleVector[m_numClasses];
            for (int i = 0; i < m_numClasses; i++) {
                //Arrays.fill(wordTotal, laplace);
                m_wordTotalForClass[i] = new DoubleVector();
            }

            m_temporalWordTotalForClass = new DoubleVector[m_numClasses];
            for (int i = 0; i < m_numClasses; i++) {
                //Arrays.fill(wordTotal, laplace);
                m_temporalWordTotalForClass[i] = new DoubleVector();
            }

            this.lastObservedClassTime = new double[this.m_numClasses];
            Arrays.fill(this.lastObservedClassTime, 0.0D);

            this.lastObservedWordTime = new DoubleVector[this.m_numClasses];
            for (int i = 0; i < this.m_numClasses; i++) {
                this.lastObservedWordTime[i] = new DoubleVector();
            }

            this.reset = false;
        }
        this.numberOfInstances++; //data set without time, update # of instances

        // Update classifier
        int classIndex = inst.classIndex();
        int classValue = (int) inst.value(classIndex);

        //double w = inst.weight();
        double instanceWeight = inst.weight();
        m_temporalProbOfClass[classValue] += instanceWeight;
//        m_probOfClass[classValue] += instanceWeight;

        m_temporalClassTotals[classValue] += instanceWeight * totalSize(inst);

//        double timestamp = inst.valueSparse(this.dateAttributeIndex); //data set with time stamp

        //Update the class observation times
        double classInterval = 0.0;


        /*
        //data set with time stamp
        if (timestamp > this.lastObservedClassTime[classValue]) {
            classInterval = timestamp - lastObservedClassTime[classValue];
            classInterval = convertInterval2AggregationPeriod(classInterval);
            this.m_probOfClass[classValue] += this.m_temporalProbOfClass[classValue] * decay(classInterval);
            this.m_classTotals[classValue] += this.m_temporalClassTotals[classValue] * decay(classInterval);
            this.m_temporalProbOfClass[classValue] = 0.0D; //reset count of class
            this.m_temporalClassTotals[classValue] = 0.0D; //reset total count of class
            this.lastObservedClassTime[classValue] = timestamp;
        }*/
        if(this.numberOfInstances > this.lastObservedClassTime[classValue]){
            classInterval = this.numberOfInstances - lastObservedClassTime[classValue];
            classInterval = convertInterval2InstPeriod(classInterval);
            this.m_probOfClass[classValue] += this.m_temporalProbOfClass[classValue] * decay(classInterval);
            this.m_classTotals[classValue] += this.m_temporalClassTotals[classValue] * decay(classInterval);
            this.m_temporalProbOfClass[classValue] = 0.0D; //reset count of class
            this.m_temporalClassTotals[classValue] = 0.0D; //reset total count of class
            this.lastObservedClassTime[classValue] = this.numberOfInstances;
        }

        String attrName;
        int numOfChangedNames = 0;
        boolean indexChangedName;
        boolean foundNewTimeStamp4Attr;
        double laplaceCorrection;
        double timeInterval;
        for (int i = 0; i < inst.numValues(); i++) {
            int index = inst.index(i);
            if (index != classIndex && !inst.isMissing(i) && !inst.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                laplaceCorrection = 0.0D;
                timeInterval = 0.0D;
                attrName = inst.attribute(index).name();
                indexChangedName = indexChangedName(index, attrName);
                if (indexChangedName) { //if word replaced another word
                    for (int classInd = 0; classInd < this.m_numClasses; classInd++) { //reset word class conditional counts
                        this.m_wordTotalForClass[classInd].setValue(index, 0.0D);
                    }
                    /**
                     * for new word reset its' time stamp
                     * */
//                    lastObservedWordTime[classValue].setValue(index, timestamp); //data set with time stamp
                    lastObservedWordTime[classValue].setValue(index, this.numberOfInstances);
                }

                this.m_temporalWordTotalForClass[classValue].addToValue(index, instanceWeight * inst.valueSparse(i) + laplaceCorrection);

/*
                //data set with time stamp
                if (timestamp > this.lastObservedWordTime[classValue].getValue(index)) {
                    timeInterval = timestamp - this.lastObservedWordTime[classValue].getValue(index);
                    timeInterval = convertInterval2AggregationPeriod(timeInterval);
                    this.m_wordTotalForClass[classValue].addToValue(index, this.m_temporalWordTotalForClass[classValue].getValue(index) * decay(timeInterval));
                    this.m_temporalWordTotalForClass[classValue].addToValue(index, 0.0D); //reset temporal cond count for word
                }
*/
                if(this.numberOfInstances > this.lastObservedWordTime[classValue].getValue(index)){
                    timeInterval = this.numberOfInstances - this.lastObservedWordTime[classValue].getValue(index);
                    timeInterval = convertInterval2InstPeriod(timeInterval);
                    this.m_wordTotalForClass[classValue].addToValue(index, this.m_temporalWordTotalForClass[classValue].getValue(index) * decay(timeInterval));
                    this.m_temporalWordTotalForClass[classValue].addToValue(index,0.0D); //rest temporal cond count for word
                }

                this.index2AttrName.put(index, attrName); //add relation index -> attribute name
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
     */
    @Override
    public double[] getVotesForInstance(Instance instance) {
        if (this.reset == true) {
            return new double[instance.numClasses()];
        } else {

            double[] probOfClassGivenDoc = new double[this.m_numClasses];
            double totalSize = totalSize(instance);
//            double timestamp = instance.valueSparse(this.dateAttributeIndex);

            double[] decayFactorPerClass = new double[this.m_numClasses];
            Arrays.fill(decayFactorPerClass, 0.0D);

            double sumProbOfClass = aggregateCountsForAllLabels(this.m_probOfClass);
            double sumClassTotals = aggregateCountsForAllLabels(this.m_classTotals);

            int i;
            for (i = 0; i < this.m_numClasses; ++i) {
//                probOfClassGivenDoc[i] = Math.log((this.m_probOfClass[i] + this.laplaceCorrectionOption.getValue()) / sumProbOfClass);
                probOfClassGivenDoc[i] = Math.log((this.m_probOfClass[i] + this.laplaceCorrectionOption.getValue()) / sumProbOfClass) - totalSize * Math.log((this.m_classTotals[i] + this.laplaceCorrectionOption.getValue()) / sumClassTotals);
            }

            double sumAttribute;
            String attributeName;
            boolean indexChangedName;
            int numOfChangedNames = 0;
            for (i = 0; i < instance.numValues(); ++i) {
                int index = instance.index(i);
                if (index != instance.classIndex() && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {

                    attributeName = instance.attribute(index).name();
                    indexChangedName = indexChangedName(index, attributeName);
                    if (indexChangedName) {
//                        System.out.println("test: we replaced " + this.index2AttrName.get(index) + " with " + attributeName);
                        numOfChangedNames++;
                    }
                    sumAttribute = 0;
                    double wordCount = instance.valueSparse(i);
                    double weightWord = 0.0;

                    for (int c = 0; c < this.m_numClasses; ++c) {
                        if (indexChangedName) { //if the word is new then create 0.0 cond counts for each class
                            weightWord = this.m_wordTotalForClass[c].getValue(index);               //tf
                            double[] newWordCountPerClass = new double[this.m_numClasses];
                            Arrays.fill(newWordCountPerClass, 0.0D);
                            sumAttribute = aggregateCountsForAllLabels(newWordCountPerClass);
                        } else {
                            weightWord = 0.0D;
                            sumAttribute = aggregateCountsForAllLabels(this.m_wordTotalForClass, index);
                        }
                        probOfClassGivenDoc[c] += wordCount * Math.log((weightWord + this.laplaceCorrectionOption.getValue()) / sumAttribute);
                    }
                }
            }
            /*if (numOfChangedNames != 0) {
                System.out.println("---");
                System.out.println("inst time: " + getInstanceDate(instance, this.dateAttributeIndex));
                System.out.println("Test: " + numOfChangedNames + " have been replaced");
                System.out.println("---");
            }*/
            return moa.core.Utils.logs2probs(probOfClassGivenDoc);
        }
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

    //@Override
    public boolean isRandomizable() {
        return false;
    }
}
