package de.l3s.oscar.TimeSeries;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by damian on 31.07.17.
 */
public class SumPredictor implements Predictor {

    /*public WordTrajectoryData computeWordTrajectoryData(BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName){

        return new WordTrajectoryData("hash", bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
    }
*/
    public double [] predictWordPresentValue(WordTrajectoryData wordTrajectoryData, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName, int numberOfClasses){

//        HashMap<DateTime, ArrayList<Double>> wordCountsInTimeRange = wordTrajectoryData.getWordTrajectoryInHash();
        HashMap<DateTime, ArrayList<Double>> wordCountsInTimeRange = wordTrajectoryData.getWordTrajectoryInHash(timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
        double [] sumContitionalWordCountsPerClass = new double[numberOfClasses];
        /*double [] countsOfWordPresencePerClass = new double[numberOfClasses];
        double [] averageContitionalWordCountsPerClass = new double[numberOfClasses];*/

        Arrays.fill(sumContitionalWordCountsPerClass, 0.0D);
        /*Arrays.fill(countsOfWordPresencePerClass, 0.0D);
        Arrays.fill(averageContitionalWordCountsPerClass, 0.0D);*/

        //TODO: sort times - CHECK if it works..
        //from https://stackoverflow.com/questions/9047090/how-to-sort-hashmap-keys
        /*Object[] trajectoryTimes = wordCountsInTimeRange.keySet().toArray();
        DateTimeComparator timeComparator = DateTimeComparator.getInstance();// .getDateOnlyInstance();
        Arrays.sort(trajectoryTimes, timeComparator);
        for(DateTime trajectoryTime : trajectoryTimes){
        }*/

        for(DateTime trajectoryTime : wordCountsInTimeRange.keySet()){
            ArrayList<Double> wordCountsInTime = wordCountsInTimeRange.get(trajectoryTime);
            for(int classIndex = 0; classIndex < numberOfClasses; classIndex++){
                sumContitionalWordCountsPerClass[classIndex] += wordCountsInTime.get(classIndex);
                /*if(wordCountsInTime.get(classIndex) != 0){
                    countsOfWordPresencePerClass[classIndex] += 1;
                }*/
            }
        }

        /*for(int classIndex = 0; classIndex < numberOfClasses; classIndex++){
            if(countsOfWordPresencePerClass[classIndex] != 0){
                averageContitionalWordCountsPerClass[classIndex] = sumContitionalWordCountsPerClass[classIndex] / countsOfWordPresencePerClass[classIndex];
            }
            else{
                averageContitionalWordCountsPerClass[classIndex] = sumContitionalWordCountsPerClass[classIndex];
            }

        }*/

        return sumContitionalWordCountsPerClass;
    }


    public double[] loadWordTrajectoryAndPredictValue(BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName){
        /*System.out.println("hoi from predictWordValue by sum");
        System.out.println("predict for word: " + word);*/
//        WordTrajectoryData wordCountsTrajectory = computeWordTrajectoryData(bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
//        System.out.println("predict for word: " + word);
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
