package de.l3s.oscar.LearningAlgorithm;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.core.Utils;

import java.util.Arrays;
import java.util.HashMap;

import moa.classifiers.bayes.NaiveBayesMultinomial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by damian on 27.09.17.
 */
public class NaiveBayesMultinomial4Sketch extends SlidingWindowClassifier {
    public FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection", 'l', "Laplace correction factor.", 1.0D, 0.0D, 2.147483647E9D);
    private static final long serialVersionUID = -7204398796974263187L;
    protected double[] m_classTotals;
    protected Instances m_headerInfo;
    protected int m_numClasses;
    protected double[] m_probOfClass;
    protected DoubleVector[] m_wordTotalForClass;
    protected HashMap<Integer, String> index2AttrName = new HashMap<>(); //copy with replaced words from sketch
    protected DoubleVector[] m_wordDocForClass; //save how many documents have words per class
    protected boolean reset = false;

    //to print the time of instance with replaced words
//    private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy");
    private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    protected int dateAttributeIndex = 1;

    public NaiveBayesMultinomial4Sketch() {
        System.out.println("=== ===");
        System.out.println(this.getPurposeString());
        System.out.println("=== ===");
    }

    public String getPurposeString() {
        return "MNB4Sketch: Classifier using feature values from sketch.";
    }

    public void resetLearningImpl() {
        this.reset = true;
    }


    public void trainOnInstanceImpl(Instance inst) {
        if (this.reset) {
            this.m_numClasses = inst.numClasses();
            double classIndex = this.laplaceCorrectionOption.getValue();
/*            int w = inst.numAttributes(); //currently we have 2 extra attributes class and date*/
            int w = getNumberOfRealAttributes(inst);
            this.m_probOfClass = new double[this.m_numClasses];
            Arrays.fill(this.m_probOfClass, classIndex);
            this.m_classTotals = new double[this.m_numClasses];
            Arrays.fill(this.m_classTotals, classIndex * (double) w);
            this.m_wordTotalForClass = new DoubleVector[this.m_numClasses];         //tf

            this.m_wordDocForClass = new DoubleVector[this.m_numClasses];           //idf

            for (int i = 0; i < this.m_numClasses; ++i) {
                this.m_wordTotalForClass[i] = new DoubleVector();
                this.m_wordDocForClass[i] = new DoubleVector();
            }

            this.reset = false;
        }

        int var12 = inst.classIndex();
        int classValue = (int) inst.classValue();
        double instanceWeight = inst.weight();
        this.m_probOfClass[classValue] += instanceWeight;
        this.m_classTotals[classValue] += instanceWeight * this.totalSize(inst);
        double var10000 = this.m_classTotals[classValue];

        double wordCount;
        boolean indexChangedName;
        String attrName;
        int numberOfReplacedWords = 0;

        for (int i1 = 0; i1 < inst.numValues(); ++i1) {
            int index = inst.index(i1);
            if (index != var12 && !inst.isMissing(i1) && !inst.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                double laplaceCorrection = 0.0D;
                attrName = inst.attribute(index).name();
                indexChangedName = indexChangedName(index, attrName);
                if (indexChangedName) { //if word replaced another word
                    /**
                     * when word replaced, reset the wordTotalForClass
                     * */
                    for (int classIndex = 0; classIndex < this.m_numClasses; classIndex++) {
                        this.m_wordTotalForClass[classIndex].setValue(index, 0.0D);
                    }
                    numberOfReplacedWords++;
                }
                if (this.m_wordTotalForClass[classValue].getValue(index) == 0.0D) {
                    laplaceCorrection = this.laplaceCorrectionOption.getValue();
                }
                wordCount = inst.valueSparse(i1);           //word count in document
                this.m_wordDocForClass[classValue].addToValue(index, 1);            //word appearances in all documents of specific class
                this.m_wordTotalForClass[classValue].addToValue(index, instanceWeight * wordCount + laplaceCorrection);           //word count

                this.index2AttrName.put(index, attrName); //add relation index -> attribute name
            }
        }
        /*if(numberOfReplacedWords !=0){
            System.out.println(" #Replaced Words, size of doc:" + "\t" + numberOfReplacedWords + "\t" + (int) this.totalSize(inst) + "\t" + dateFormatter.print(getInstanceDate(inst, this.dateAttributeIndex)));
        }*/
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

    public double[] getVotesForInstance(Instance instance) {

        if (this.reset) {
            return new double[instance.numClasses()];
        } else {
            double[] probOfClassGivenDoc = new double[this.m_numClasses];
            double totalSize = this.totalSize(instance);

            double trueLabel = instance.classValue(); //testing

            double sumProbOfClass = aggregateCountsForAllLabels(this.m_probOfClass);
            double sumClassTotals = aggregateCountsForAllLabels(this.m_classTotals);
            int i;
            for (i = 0; i < this.m_numClasses; ++i) {
//                probOfClassGivenDoc[i] = Math.log((this.m_probOfClass[i] + this.laplaceCorrectionOption.getValue()) / sumProbOfClass);
                probOfClassGivenDoc[i] = Math.log((this.m_probOfClass[i] + this.laplaceCorrectionOption.getValue()) / sumProbOfClass) - totalSize * Math.log((this.m_classTotals[i] + this.laplaceCorrectionOption.getValue()) / sumClassTotals);
            }

            double sumAttribute;
            String attributeName;
            double[] probPerAttr = new double[this.m_numClasses];
            boolean indexChangedName = false;
            int numOfChangedNames = 0;
            for (i = 0; i < instance.numValues(); ++i) {
                int index = instance.index(i);
                if (index != instance.classIndex() && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                    /**
                     * testing
                     * */
                    Arrays.fill(probPerAttr, 0.0D);

                    attributeName = instance.attribute(index).name();
                    indexChangedName = indexChangedName(index, attributeName);
                    if(indexChangedName){
                        numOfChangedNames += 1;
                    }
                    sumAttribute = 0;
                    double wordCount = instance.valueSparse(i);
                    double weightWord = 0.0;

                    for (int c = 0; c < this.m_numClasses; ++c) {

                        if (indexChangedName) { //if the word is new then create 0.0 cond counts for each class
                            weightWord = 0.0D;
                            double[] newWordCountPerClass = new double[this.m_numClasses];
                            Arrays.fill(newWordCountPerClass, 0.0D);
                            sumAttribute = aggregateCountsForAllLabels(newWordCountPerClass);
                        } else {
                            weightWord = this.m_wordTotalForClass[c].getValue(index);
                            sumAttribute = aggregateCountsForAllLabels(this.m_wordTotalForClass, index);
                        }
                        probOfClassGivenDoc[c] += wordCount * Math.log((weightWord + this.laplaceCorrectionOption.getValue()) / sumAttribute);
                        /**
                         * testing
                         * */
                        probPerAttr[c] = wordCount * Math.log((weightWord + this.laplaceCorrectionOption.getValue()) / sumAttribute);
                    }
                }
            }
//            if (numOfChangedNames != 0) {
//                System.out.println("---");
//                System.out.println("inst time: " + getInstanceDate(instance, this.dateAttributeIndex));
//                System.out.println("Test: " + numOfChangedNames + " have been replaced");
//                System.out.println("---");
//            }
            return Utils.logs2probs(probOfClassGivenDoc);
        }
    }

    /**
     * updated to exclude the date attribute from the size of features of the current instance
     */
    public double totalSize(Instance instance) {
        int classIndex = instance.classIndex();
        double total = 0.0D;

        for (int i = 0; i < instance.numValues(); ++i) {
            int index = instance.index(i);
            if (index != classIndex && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                double count = instance.valueSparse(i);
                if (count >= 0.0D) {
                    total += count;
                }
            }
        }

        return total;
    }

    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    public void getModelDescription(StringBuilder result, int indent) {
        StringUtils.appendIndented(result, indent, "xxx MNB1 xxx\n\n");
        result.append("The independent probability of a class\n");
        result.append("--------------------------------------\n");

        int w;
        for (w = 0; w < this.m_numClasses; ++w) {
            result.append(this.m_headerInfo.classAttribute().value(w)).append("\t").append(Double.toString(this.m_probOfClass[w])).append("\n");
        }

        result.append("\nThe probability of a word given the class\n");
        result.append("-----------------------------------------\n\t");

        for (w = 0; w < this.m_numClasses; ++w) {
            result.append(this.m_headerInfo.classAttribute().value(w)).append("\t");
        }

        result.append("\n");

        for (w = 0; w < this.m_headerInfo.numAttributes(); ++w) {
            if (w != this.m_headerInfo.classIndex()) {
                result.append(this.m_headerInfo.attribute(w).name()).append("\t");

                for (int c = 0; c < this.m_numClasses; ++c) {
                    double value = this.m_wordTotalForClass[c].getValue(w);
                    if (value == 0.0D) {
                        value = this.laplaceCorrectionOption.getValue();
                    }

                    result.append(value / this.m_classTotals[c]).append("\t");
                }

                result.append("\n");
            }
        }

        StringUtils.appendNewline(result);
    }

    public boolean isRandomizable() {
        return false;
    }
}
