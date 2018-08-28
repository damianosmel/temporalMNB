package de.l3s.oscar.TimeSeries;

import org.joda.time.DateTime;



/**
 * Created by damian on 25.07.17.
 */
public interface Predictor {

//    public WordTrajectoryData computeWordTrajectoryData(BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName);
//    public double [] predictWordPresentValue(WordTrajectoryData wordCountsInTimeRange, int numberOfClasses, DateTime timeRangeStop);
    public double [] predictWordPresentValue(WordTrajectoryData wordTrajectoryData, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName, int numberOfClasses);
    public double [] loadWordTrajectoryAndPredictValue(BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName);
}
