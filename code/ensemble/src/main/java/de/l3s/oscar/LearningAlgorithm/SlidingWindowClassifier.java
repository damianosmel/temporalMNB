package de.l3s.oscar.LearningAlgorithm;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Instance;
import de.l3s.oscar.Evaluation.TimeSeriesPredPerformance;
import de.l3s.oscar.TimeSeries.BagOfWordsInTime;
import moa.classifiers.AbstractClassifier;
import moa.core.DoubleVector;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by damian on 15.02.17.
 */
public abstract class SlidingWindowClassifier extends AbstractClassifier {
    protected int numClasses = 2;
    protected FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection", 'l', "Laplace correction factor.", 1.0D, 0.0D, 2.147483647E9D);
    /**
     * get time series perf
     * */
    public void setOutputDirectory(File outputDirectory){}
    public HashMap<String, TimeSeriesPredPerformance> getTrackedWordTimeSeriesPerf(){
        HashMap<String, TimeSeriesPredPerformance> trackedWordTimeSeriesPref = new HashMap<String, TimeSeriesPredPerformance>();
        return trackedWordTimeSeriesPref;
    }
//    public File outputDirectory;
//    public HashMap<String, TimeSeriesPredPerformance> trackedWordTimeSeriesPerf;
    public void discardInstanceFromModel(Instance instance){}

    public static void setDebugMNBVanilla(boolean userDebugMNBVanilla){
    }

    public String getThenCleanDebugLog() {
        String debugLog = null;
        return debugLog;
    }

    //Weka instances
    public void setHeaderInfo(weka.core.Instances trainingSet){}

    //Samoa instances
    public void setHeaderInfo(com.yahoo.labs.samoa.instances.Instances samoaTrainingSet){}

    public void loadWordSentimentFromCSV(String csvAbsolutePath, boolean isFormat4Average){
    }

    public double [] getProbOfClass(){
        double [] probOfClass = new double[3];
        return probOfClass;
    }

    public double [] getClassTotals(){
        double [] classTotals = new double[3];
        return classTotals;
    }

    public moa.core.DoubleVector [] getWordTotalForClass(){
        DoubleVector [] wordTotalForClass = new DoubleVector[3];
        for (int i = 0; i < 3; i++) {
            //Arrays.fill(wordTotal, laplace);
            wordTotalForClass[i] = new DoubleVector();
        }
        return wordTotalForClass;
    }

    public boolean getCurrentTweetHasEntity(){
        return false;
    }

    public void setCurrentTweetHasEntity(boolean currentTweetHasEntity){

    }

    public boolean getResetClassifierFlag(){return false;}

    /**
     * SentiWordNet
     * */
    public void createEntitiesHashFromFile(String filePath) {}
    public int getTotalNumberOfUsedWordsFromHash(){
        return -1;
    }
    public void createHashWithSentiWordNetFromFile(String filePath){}
    /**
     * SentiWordNet
     * */

    /**
     * --- Time series ---
     * */
    /**
     * for memory efficiency if a word is removed from the filter,
     * remove it from the time series data
     * */
    public void removeWordsFromTimeSeries(List<String> wordsToRemove){}

    public int getSizeOfInvertedIndexOfWords2Times(){
        return 0;
    }
    /**
     * test & train with time-series of words
     * */
    public void initializeWordTimeSeries(){}
    public void trainOnInstanceWithWordTimeSeries(Instance instance, DateTime instanceDateTime){
    }

    public double [] getVotesForInstanceWithWordTimeSeries(Instance instance, String predictorName, DateTime timeRangeStart, DateTime timeRangeStop, int windowSize, String windowName){
        double [] votePerClass = new double[3];
        return votePerClass;
    }

    public BagOfWordsInTime getWordsTimeSeries(){
        int numberOfClasses = 2;
        int aggregationPeriod = 5;
        String aggregationGranularity = "minute";
        BagOfWordsInTime bagOfWordsInTime = new BagOfWordsInTime(numberOfClasses, aggregationPeriod, aggregationGranularity);
        return bagOfWordsInTime;
    }

    public void printWordsTimeSeries(){}

    public void createEntitiesHashFromList(ArrayList<String> entitiesList){}
    /**
     * --- Time series ---
     * */


    /**
     * --- Usable by all ---
     * */

    public int getNumberOfRealAttributes(Instance inst){
        int numberOfRealAttributes = inst.numAttributes() - 1;
        int a = inst.dataset().numAttributes();
        System.out.println(a);
        numberOfRealAttributes--; //the class does not count as attribute
        for (int i= 0; i < inst.numValues(); i++){
            int index = inst.index(i);
            if(inst.attribute(index).name().equalsIgnoreCase("dateTimeStamp")){
                numberOfRealAttributes--;
            }
        }
        return numberOfRealAttributes;
    }

    /**
     * updated to exclude the date attribute from the size of features of the current instance
     * */
    public double totalSize(Instance instance) {
        int classIndex = instance.classIndex();
        double total = 0.0D;

        for(int i = 0; i < instance.numValues(); ++i) {
            int index = instance.index(i);
            if(index != classIndex && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                double count = instance.valueSparse(i);
                if(count >= 0.0D) {
                    total += count;
                }
            }
        }

        return total;
    }

    /**
     * get time stamp
     * */
    protected DateTime getInstanceDate(Instance inst, int dateAttributeIndex){
        double currentInstanceDate = inst.valueSparse(dateAttributeIndex);
        return new DateTime((long) currentInstanceDate * 1000);
    }

    protected double aggregateCountsForAllLabels(double [] countsArray){
        double sumCountsAllLabels = 0.0D;

        for(int classIndex = 0; classIndex < this.numClasses; classIndex++){
            if(Double.isNaN(countsArray[classIndex])){
                countsArray[classIndex] = 0.0D;
            }
            sumCountsAllLabels += countsArray[classIndex] + this.laplaceCorrectionOption.getValue();
        }

        return sumCountsAllLabels;
    }

    protected double aggregateCountsForAllLabels(DoubleVector [] countsVectorAllAttr, int attrIndex){
        double sumCountsAllLabels = 0.0D;

        for(int classIndex = 0; classIndex < this.numClasses; classIndex++){
            sumCountsAllLabels += countsVectorAllAttr[classIndex].getValue(attrIndex) + this.laplaceCorrectionOption.getValue();
        }

        return sumCountsAllLabels;
    }

    /**
     * For testing
     * */
    public boolean thereIsDiffTrue2Pred(double[] predictionProb, double classLabel) {
        boolean diffTrue2Pred = false;

        if (classLabel == 0.0D) {
            if (predictionProb[0] < predictionProb[1]) {
                diffTrue2Pred = true;
            }
        } else if (classLabel == 1.0D) {
            if (predictionProb[0] > predictionProb[1]) {
                diffTrue2Pred = true;
            }
        }
        return diffTrue2Pred;
    }
    /**
     * --- Usable by all ---
     * */
}
