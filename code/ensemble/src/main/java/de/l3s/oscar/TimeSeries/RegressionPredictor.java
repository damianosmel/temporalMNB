package de.l3s.oscar.TimeSeries;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;



/**
 * Created by damian on 26.07.17.
 */
public class RegressionPredictor implements Predictor {

   /* public WordTrajectoryData computeWordTrajectoryData(BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName) {
        return new WordTrajectoryData("hashArray", bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
    }*/

    /**
     *

     * @param wordTrajectoryData        wordTrajectoryData,  saved trajectory in specific format
     * @param numberOfClasses           int, number of classes
     * @param timeRangeStop             DateTime, time of present value
     * @return
     * Credits to https://stackoverflow.com/questions/30859029/use-common-math-library-in-java
     */
    public double [] predictWordPresentValue(WordTrajectoryData wordTrajectoryData, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName, int numberOfClasses) {

//        HashMap<DateTime, ArrayList<Double>> wordCountsInTimeRange = wordTrajectoryData.getWordTrajectoryInHash();
//        HashMap<Integer, ArrayList< double []>> wordCountsInTimeRange = wordTrajectoryData.getWordTrajectoryInArray();
        HashMap<Integer, ArrayList<double[]>> wordCountsInTimeRange = wordTrajectoryData.getWordTrajectoryInArray(timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
        double[] linearFitConditionalWordCountsPerClass = new double[numberOfClasses];
        Arrays.fill(linearFitConditionalWordCountsPerClass, 0.0D);

        //if time series is empty:
        if (wordCountsInTimeRange.size() == 0) {
//            System.out.println("predicting from empty time series");
            for(int classIndex = 0; classIndex < numberOfClasses; classIndex++){
                linearFitConditionalWordCountsPerClass[classIndex] = 0.0D;
            }
        }
        else {
            ArrayList<double[]> timesAndConditionalCountsList;

            for (int classIndex = 0; classIndex < numberOfClasses; classIndex++) {
//            timesAndConditionalCountsList = wordTrajectoryData.getWordTrajectoryInArray().get(classIndex);
                timesAndConditionalCountsList = wordCountsInTimeRange.get(classIndex);
                SimpleRegression simpleRegression = new SimpleRegression(true);
//            double [][] timesAndConditionalCounts = new double[][];
                simpleRegression.addData(timesAndConditionalCountsList.toArray(new double[timesAndConditionalCountsList.size()][2]));
                linearFitConditionalWordCountsPerClass[classIndex] = simpleRegression.predict(timeRangeStop.getMillis());
            }
        }

        return linearFitConditionalWordCountsPerClass;
    }

    public double[] loadWordTrajectoryAndPredictValue(BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName){
//        System.out.println("hoi from predictWordValue by regression");

/*
        //load word trajectory
        WordTrajectoryData wordCountsTrajectory = bagOfWordsInTime.getWordTrajectoryData(word);

//        WordTrajectoryData wordCountsTrajectory = computeWordTrajectoryData(bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);

        //predict its' next value
        return predictWordPresentValue(wordCountsTrajectory, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName, bagOfWordsInTime.getNumberOfClasses());
*/

        //load word trajectory
        WordTrajectoryData wordCountsTrajectory = bagOfWordsInTime.getWordTrajectoryData(word);
        /**
         * if word is already followed by the time series get the data
         * */
        if(wordCountsTrajectory != null){
//            System.out.println("not null trajectory for word " + word);
            return predictWordPresentValue(wordCountsTrajectory,timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName, bagOfWordsInTime.getNumberOfClasses());
        }
        /**
         * otherwise get pseudo zero data
         * */
        else{
//            System.out.println("null trajectory for word " + word);
            boolean resizeAfterCollecting = false;
            int numberOfClasses = 2;
            int aggregationPeriod = 5;
            String aggregationGranularity = "minute";
            boolean trackWord = false;
            WordTrajectoryData zeroWordCountsTrajectory = new WordTrajectoryData(word, numberOfClasses, aggregationPeriod, aggregationGranularity, resizeAfterCollecting, trackWord);
            zeroWordCountsTrajectory.addZeroEntry(timeRangeStop, bagOfWordsInTime.getNumberOfClasses());

            return predictWordPresentValue(zeroWordCountsTrajectory, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName, bagOfWordsInTime.getNumberOfClasses());
        }
    }
}
