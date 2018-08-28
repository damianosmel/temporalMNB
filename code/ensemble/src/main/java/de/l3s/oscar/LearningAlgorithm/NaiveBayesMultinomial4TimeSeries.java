package de.l3s.oscar.LearningAlgorithm;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import de.l3s.oscar.Evaluation.TimeSeriesPredPerformance;
import de.l3s.oscar.TimeSeries.BagOfWordsInTime;
import moa.core.DoubleVector;
import moa.core.Measurement;
import moa.core.StringUtils;
import moa.core.Utils;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by damian on 30.11.17.
 */
public class NaiveBayesMultinomial4TimeSeries extends SlidingWindowClassifier {
    public FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection", 'l', "Laplace correction factor.", 1.0D, 0.0D, 2.147483647E9D);
    private static final long serialVersionUID = -7204398796974263187L;
    protected double[] m_classTotals;
    protected Instances m_headerInfo;
    protected int m_numClasses;
    protected double[] m_probOfClass;
    protected DoubleVector[] m_wordTotalForClass;
    protected boolean reset = false;
    protected DateTime generalTimeStamp = new DateTime(2018, 5, 20, 12, 0 , 0); //this time stamp will be used for data sets without time stamps
    /**
     * bag of words
     * */
    protected BagOfWordsInTime bagOfWordsInTime;
    int aggregationPeriod = 1;//1;//10,20,30,40,50 sec //min: 5, 15, 30;
    String aggregationGranularity = "second";//"second";//"second";//"minute";
    protected List<String> removedWords;

    /**
     * for reporting time series predictions of words
     * */
    //senti140
    ArrayList<String> trackedWords = new ArrayList<String>(Arrays.asList("classLabel", "love", "sleep", "late", "easter", "summer", "iphone", "politic", "football", "weather", "travel", "followfriday", "t-storm", "cnn"));
    /*
    //hspam14
    ArrayList<String> trackedWords = new ArrayList<String>(Arrays.asList("classLabel", "classTotal", "coin", "morsi", "news", "music", "easter", "summer", "finals", "uefa", "iphone", "android", "mayweather", "zamfara", "cricket")); // "classTotal",
    */
    //email_data
//    ArrayList<String> trackedWords = new ArrayList<String>(Arrays.asList("classLabel", "basebal", "medicin", "space")); //, "classTotal"
    /**
     *
     * */
    HashMap<String, TimeSeriesPredPerformance> trackedWordTimeSeriesPerf = new HashMap<String, TimeSeriesPredPerformance>();
    File outputDirectory;

    protected int dateAttributeIndex = 1;
    public NaiveBayesMultinomial4TimeSeries() {
        System.out.println("=== ===");
        System.out.println(this.getPurposeString());
        System.out.println("=== ===");
    }

    public String getPurposeString() {
        return "MNB4TimeSeries: Classifier using feature values from time series predictions.";
    }

    public void resetLearningImpl() {
        this.reset = true;
    }

    /**
     * for reporting time series perf
     * */
    public void setOutputDirectory(File outputDirectory){
        System.out.println("MNB: adding outputDirectory");
        this.outputDirectory = outputDirectory;
    }

    public HashMap<String, TimeSeriesPredPerformance> getTrackedWordTimeSeriesPerf(){
        return this.trackedWordTimeSeriesPerf;
    }

    public int getSizeOfInvertedIndexOfWords2Times(){
        return bagOfWordsInTime.getSizeOfInvertedIndex();
    }
    /**
     * For memory efficiency words removed from the filter
     * are removed from the data of time series.
     * */
    public void removeWordsFromTimeSeries(List<String> wordsToRemove){
        if(wordsToRemove.size() != 0){
            bagOfWordsInTime.removeWords(wordsToRemove);
        }
    }

    public boolean attributeEqualsEntity(String attribute){
        boolean attributeEqualsEntity = false;
        for (String entityToTrack : this.trackedWords){
            if(entityToTrack.equalsIgnoreCase(attribute)){
                attributeEqualsEntity = true;
                break;
            }
        }
        return attributeEqualsEntity;
    }

    public void trainOnInstanceImpl(Instance inst) {
        if(this.reset) {
            this.m_numClasses = inst.numClasses();
            double classIndex = this.laplaceCorrectionOption.getValue();
            //int w = inst.numAttributes(); //currently we have 2 extra attributes the class and the time
            int w = getNumberOfRealAttributes(inst);
            this.m_probOfClass = new double[this.m_numClasses];
            Arrays.fill(this.m_probOfClass, classIndex);
            this.m_classTotals = new double[this.m_numClasses];
            Arrays.fill(this.m_classTotals, classIndex * (double)w);
            this.m_wordTotalForClass = new DoubleVector[this.m_numClasses];

            for(int i = 0; i < this.m_numClasses; ++i) {
                this.m_wordTotalForClass[i] = new DoubleVector();
            }
            this.reset = false;
        }

        int var12 = inst.classIndex();
        int classValue = (int)inst.classValue();
        /**
         * Try the classes:
         * System.out.println("the class index is " + classValue);
         * */
        double instanceWeight = inst.weight();
        /**
         * Class Priors
         * */
        this.bagOfWordsInTime.addWordInLightWeightTime(getInstanceDate(inst, this.dateAttributeIndex), "classLabel", instanceWeight, classValue); //data set with time stamp
//        this.bagOfWordsInTime.addWordInLightWeightTime(this.generalTimeStamp, "classLabel", instanceWeight, classValue); //data set without time stamp
        this.trackedWordTimeSeriesPerf.put("classLabel", this.bagOfWordsInTime.getTimeSeriesPerf("classLabel"));

        String attributeName = new String();
        double attributeValue = 0.0D;
        double laplaceCorrection;
        for(int i1 = 0; i1 < inst.numValues(); ++i1) {
            int index = inst.index(i1);
            double [] realValues = new double[this.m_numClasses];
            Arrays.fill(realValues, 0.0D);

            if(index != var12 && !inst.isMissing(i1) && !inst.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                attributeName = inst.attribute(index).name();
                //compute the attribute value
                laplaceCorrection = 0.0D;
                if(this.m_wordTotalForClass[classValue].getValue(index) == 0.0D) {
                    laplaceCorrection = this.laplaceCorrectionOption.getValue();
                }

                attributeValue = instanceWeight * inst.valueSparse(i1);

                /**
                 * light-weight time series
                 * */
                this.bagOfWordsInTime.addWordInLightWeightTime(getInstanceDate(inst, this.dateAttributeIndex), attributeName, attributeValue, classValue); //data set with time stamp
//                this.bagOfWordsInTime.addWordInLightWeightTime(this.generalTimeStamp, attributeName, attributeValue, classValue); //data set without time stamp

                /**
                 * for reporting the time series predictions
                 * */
                if(attributeEqualsEntity(attributeName)){
                    this.trackedWordTimeSeriesPerf.put(attributeName, this.bagOfWordsInTime.getTimeSeriesPerf(attributeName));
                }
            }
        }
    }


    public double[] getVotesForInstance(Instance instance) {
        if(this.reset) {
            /**
             * === Initialize the bagOfWordsInTime ===
             * First time inside the learner
             * 1) initialize the bagOfWords
             * 2) set the number of classes for the word time-series
             * */
            this.m_numClasses = instance.numClasses();
            this.bagOfWordsInTime = new BagOfWordsInTime(this.m_numClasses, this.aggregationPeriod, this.aggregationGranularity);
            this.bagOfWordsInTime.setTrackedWords(this.trackedWords);
            this.bagOfWordsInTime.setOutputDirectory(this.outputDirectory);
            return new double[instance.numClasses()];
        }
        else {
            double[] probOfClassGivenDoc = new double[this.m_numClasses];
            double totalSize = this.totalSize(instance);
            double [] zerosArray = new double[this.m_numClasses];
            Arrays.fill(zerosArray, 0.0D);

            DateTime instanceDateTime = getInstanceDate(instance, this.dateAttributeIndex); //for data set with time stamp

            /**
             * Predict the P(c) prob of class by ensemble
             * */
            double [] predictedClassValues = this.bagOfWordsInTime.getWordTrajectoryData("classLabel").predictConditionalCountsEnsemble(instanceDateTime); //data set with time stamp
//            double [] predictedClassValues = this.bagOfWordsInTime.getWordTrajectoryData("classLabel").predictConditionalCountsEnsemble(this.generalTimeStamp);
            double predictedClassValue = 0.0D;
            double sumProbOfClass = aggregateCountsForAllLabels(predictedClassValues);

            int i;
            for(i = 0; i < this.m_numClasses; ++i) {
                predictedClassValue = predictedClassValues[i];
                if(predictedClassValue < 0.0D || Double.isNaN(predictedClassValue)){
                    predictedClassValue = 0.0D;
                }
                probOfClassGivenDoc[i] = Math.log( (predictedClassValue + this.laplaceCorrectionOption.getValue()) / sumProbOfClass);
            }

            /**
             * Predict P(w|c)
             * */
            double [] predictedValues = new double[this.m_numClasses];
            Arrays.fill(predictedValues, 0.0D);
            double [] realValues = new double[this.m_numClasses];
            Arrays.fill(realValues, 0.0D);
            double predictedValue = 0.0D;
            String attributeName;
            double sumAttribute;

            double wordCount = 0.0D;
            for(i = 0; i < instance.numValues(); ++i) {
                int index = instance.index(i);
                attributeName = instance.attribute(index).name();
                if(index != instance.classIndex() && !instance.isMissing(i) && !attributeName.equalsIgnoreCase("dateTimeStamp")) {

                    wordCount = instance.valueSparse(i);
                    /**
                     * Predict cond count of word (N_{w,c}) by ensemble
                     * */
                    if(this.bagOfWordsInTime.getWordTrajectoryData(attributeName) != null) {
                        predictedValues = this.bagOfWordsInTime.getWordTrajectoryData(attributeName).predictConditionalCountsEnsemble(instanceDateTime); //data set with time stamp
//                        predictedValues = this.bagOfWordsInTime.getWordTrajectoryData(attributeName).predictConditionalCountsEnsemble(this.generalTimeStamp); //data set without time stamp
                    }
                    sumAttribute = aggregateCountsForAllLabels(predictedValues);

                    for(int c = 0; c < this.m_numClasses; ++c) { //aggregate cond count prediction for the whole instance prediction
                        predictedValue = predictedValues[c];
                        if(predictedValue < 0.0 || Double.isNaN(predictedValue)){
                            predictedValue = 0.0;
                        }
                        probOfClassGivenDoc[c] += wordCount * Math.log((predictedValue + this.laplaceCorrectionOption.getValue()) / sumAttribute);
                    }
                }
            }
            return Utils.logs2probs(probOfClassGivenDoc);
        }
    }


    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    public void getModelDescription(StringBuilder result, int indent) {
        StringUtils.appendIndented(result, indent, "xxx MNB1 xxx\n\n");
        result.append("The independent probability of a class\n");
        result.append("--------------------------------------\n");

        int w;
        for(w = 0; w < this.m_numClasses; ++w) {
            result.append(this.m_headerInfo.classAttribute().value(w)).append("\t").append(Double.toString(this.m_probOfClass[w])).append("\n");
        }

        result.append("\nThe probability of a word given the class\n");
        result.append("-----------------------------------------\n\t");

        for(w = 0; w < this.m_numClasses; ++w) {
            result.append(this.m_headerInfo.classAttribute().value(w)).append("\t");
        }

        result.append("\n");

        for(w = 0; w < this.m_headerInfo.numAttributes(); ++w) {
            if(w != this.m_headerInfo.classIndex()) {
                result.append(this.m_headerInfo.attribute(w).name()).append("\t");

                for(int c = 0; c < this.m_numClasses; ++c) {
                    double value = this.m_wordTotalForClass[c].getValue(w);
                    if(value == 0.0D) {
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
