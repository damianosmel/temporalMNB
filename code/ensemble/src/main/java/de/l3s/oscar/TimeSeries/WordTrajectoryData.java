package de.l3s.oscar.TimeSeries;

import de.l3s.oscar.Evaluation.TimeSeriesPredPerformance;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.File;
import java.util.*;

//for arima
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

import static com.github.signaflo.math.operations.DoubleFunctions.arrayFrom;
import static com.github.signaflo.math.operations.DoubleFunctions.sqrt;

/**
 * Wrapper class to save
 * different representations of the time series
 * Created by damian on 26.07.17.
 */
public class WordTrajectoryData {
    protected String wordName;
    protected boolean newAggrPeriodFound;

    protected int windowTimeSize;
    protected String windowTimeName;
    protected boolean resizeAfterCollecting;

    protected DateTime startWindowTime;
    protected DateTime endWindowTime;

    protected int numberOfClasses;
    /**
     * for "Poisson"
     */
    protected DateTime previousDocumentTime = null;

    protected WordStatisticsForPoisson wordStatisticsForPoisson; //data set with time stamp
    protected WordStatisticsForPoisson wordStatisticsForSeasonalPoisson; //data set with time stamp
    protected WordStatisticsForPoissonInst wordStatisticsForPoissonInst; //data set without time stamp
    protected WordStatisticsForPoissonInst wordStatisticsForSeasonalPoissonInst; //data set without time stamp
    protected WordStatisticsForARIMA wordStatisticsForARIMA;
    protected WordStatisticsForEWMA wordStatisticsForEWMA;
    protected WeightedWindowEnsemble ensembleWW;

    /**
     * for each time -> {conditionalCountForClass1, .., conditionalCountForClassC, cumulative_conditionalCountForClass1, .., cumulative_conditionalCountForClassC}
     */
    protected TreeMap<DateTime, ArrayList<Double>> wordTrajectoryInTime = new TreeMap<DateTime, ArrayList<Double>>();

    protected HashMap<DateTime, ArrayList<Double>> wordTrajectoryInHash = new HashMap<DateTime, ArrayList<Double>>();
    protected HashMap<Integer, ArrayList<double[]>> wordTrajectoryInArray = new HashMap<Integer, ArrayList<double[]>>();

    /**
     * light-weight version
     */
    /*protected DateTime previousObservedTimePoint;
    protected DateTime currentObservedTimePoint;*/
    protected ArrayList<Double> currentConditionalCounts;
//    protected ArrayList<Double> condCountsInst;
    /**
     * Aggregate time points in periods
     */
    static DateTime defaultTime = new DateTime(); //for models to use.
    private DateTime firstTimeInPeriod;
    private int aggregationPeriod = 0;
    private String aggregationPeriodGranularity;
    private int observedTimes;
    /**
     * for reporting time series prediction
     */
    private boolean trackWord;
    protected TimeSeriesPredPerformance timeSeriesPredPerformance;
    File outputDirectory;
    private boolean lowEntropyInLastTurn = false;

    public WordTrajectoryData(String wordName, int numberOfClasses, int aggregationPeriod, String aggregationPeriodGranularity, boolean resizeAfterCollecting, boolean trackWord) {
        this.wordName = new String(wordName);
        this.resizeAfterCollecting = resizeAfterCollecting;
        this.numberOfClasses = numberOfClasses;
        this.newAggrPeriodFound = false;
        this.observedTimes = 0;

        /**
         * Light-weight version
         * */
        this.currentConditionalCounts = getZeroConditionalCounts();
//        this.condCountsInst = getZeroConditionalCounts();
        /**
         * aggregation
         * */
        this.firstTimeInPeriod = defaultTime;

        if(this.wordName.equalsIgnoreCase("classLabel") || this.wordName.equalsIgnoreCase("classTotal")){
//            this.aggregationPeriod = 1;//2;//email_data//10000, spam_data
            this.aggregationPeriod = aggregationPeriod; //tweets data
            this.aggregationPeriodGranularity = aggregationPeriodGranularity;
        }
        else{
//            this.aggregationPeriod = 1;//2;//email_data, spam_data
            this.aggregationPeriod = 60 * aggregationPeriod; //1min //for all words use a sampling rate of 60 sec -> 1 min //tweets data
            this.aggregationPeriodGranularity = aggregationPeriodGranularity; //aggregationPeriodGranularity;
        }

        /**
         * === Initialize word models ===
         * */

        //Poisson
        initializePoissonStats(); //data set with time stamp
//        initializePoissonInstStats(); //data set without time stamp

        //Seasonal Poisson
        /**
         * common options for (alpha, gamma)
         * (0.4, 8) or (0.7, 4) or (0.9, 2) or (1.0, 1)
         * */
        //data set with time stamp
        double alpha = 0.9D;
        int gamma = 2;
        initializeSeasonalStats(alpha, gamma);

/*
        //data set without time stamp
        double alpha = 0.9D;
        int gamma = 2;
        double maxNumInstInBucket = 25;//email, spam data
        initializeSeasonalInstStats(maxNumInstInBucket, alpha, gamma);
*/
        //ARIMA
//        int timeSeriesMaxSizeARIMA = 150;//100;//for email_data, spam_data
        int timeSeriesMaxSizeARIMA = 100; //aggregation periods for tweets data

        initializeARIMAStats(timeSeriesMaxSizeARIMA);

        //EWMA
        /**
         * (a, gamma) = (0.1, 22), (0.5, 6) and (0.9, 2)
         * */
        double alpha4EWMA = 0.1D; //0.9; email_data, spam_data
        int timeSeriesMaxSizeEWMA = 22; //2; email_data, spam_data


        initializeEWMAStats(timeSeriesMaxSizeEWMA, alpha4EWMA);

        //Ensemble (weighted average algorithm WWA)
        /**
         * Find c >= \tilde{c_L} using equation 7 of
         * Kivinen J, Warmuth MK. Averaging expert predictions.
         * In European Conference on Computational Learning Theory 1999 Mar 29 (pp. 153-167). Springer, Berlin, Heidelberg.
         * */
        double maxB; //maximum upper value to predict
        double c;//for square loss in the range [0,B] the \tilde{c_L} = 2 B^2
        if(this.wordName.equalsIgnoreCase("classLabel")){
//            maxB = this.aggregationPeriod * 30.0D;//tweets_data
            maxB = 5.0D;// for email_data, spam_data
        }
        else{
            maxB = 5.0D;
//            maxB = 3.0 * this.aggregationPeriod; //max occurence of a word in a document = 3 for email_data, spam_data
//            maxB = this.aggregationPeriod / 2.0; //tweets_data
        }
        c = 2 * Math.pow(maxB,2.0D);
        int expertsNum = 4;
        String lossName = new String("square");
        initializeEnsemble(expertsNum, maxB, c, lossName);

        /**
         * for reporting time series prediction
         * */
        this.trackWord = trackWord;
    }

    public boolean getIfNewAggregationFound() {
        return this.newAggrPeriodFound;
    }


    //Poisson model for data set with time stamp
    public void initializePoissonStats() {
        int numberOfShifts = 6;
        int startingTimeOfFirstShift = 2;
        String granularityOfHistory = "week";
        int countOfHistory = 1;
        double alpha = 1.0D;
        this.wordStatisticsForPoisson = new WordStatisticsForPoisson(this.numberOfClasses, numberOfShifts, startingTimeOfFirstShift, granularityOfHistory, countOfHistory, alpha);
    }

    //Poisson model for data set without time stamp
    public void initializePoissonInstStats(){
        int gamma = 1;
        double alpha = 1.0D;
        double maxNumInstInBucket = 0.0D;
        this.wordStatisticsForPoissonInst = new WordStatisticsForPoissonInst(this.numberOfClasses, maxNumInstInBucket, alpha, gamma);
    }

    //Seasonal Poisson for data set with time stamp
    public void initializeSeasonalStats(double alpha, int gamma) {
        int numberOfShifts = 6;
        int startingTimeOfFirstShift = 2;
        String granularityOfHistory = "week";

        int countOfHistory = gamma; //gamma weeks
        this.wordStatisticsForSeasonalPoisson = new WordStatisticsForPoisson(this.numberOfClasses, numberOfShifts, startingTimeOfFirstShift, granularityOfHistory, countOfHistory, alpha);
    }

    //Seasonal Poisson for data set without time stamp
    public void initializeSeasonalInstStats(double maxNumInstInBucket, double alpha, int gamma){
        this.wordStatisticsForSeasonalPoissonInst = new WordStatisticsForPoissonInst(this.numberOfClasses, maxNumInstInBucket, alpha, gamma);
    }

    public void initializeARIMAStats(int timeSeriesMaxSize) {
        this.wordStatisticsForARIMA = new WordStatisticsForARIMA(this.numberOfClasses, timeSeriesMaxSize);
    }

    public void initializeEWMAStats(int timeSeriesMaxSize, double alpha) {

        this.wordStatisticsForEWMA = new WordStatisticsForEWMA(this.numberOfClasses, timeSeriesMaxSize, alpha);
    }

    public void setOutputDirectory(File outputDirectory) {

        this.outputDirectory = outputDirectory;
//        System.out.println("wordTraj: output " + this.outputDirectory);
    }

    /**
     * for reporting predictions
     */
    public TimeSeriesPredPerformance getTimeSeriesPerf() {
        return this.timeSeriesPredPerformance;
    }

    /**
     * for reporting the predictions
     */
    public void initializePredPerformance(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        int numberOfPredictors = 6; //1 + 5 -> 1 for the true outcome + 4 experts and 1 ensemble
        this.timeSeriesPredPerformance = new TimeSeriesPredPerformance(this.outputDirectory, 0, numberOfPredictors, this.numberOfClasses, this.wordName);

        String createHeaderCSVLog = this.timeSeriesPredPerformance.createHeaderTimeSeriesPredictionCSV();
        boolean ifBufferWritten = this.timeSeriesPredPerformance.checkIfBufferIsWritten(createHeaderCSVLog);
    }

    public void initializeEnsemble(int expertsNum, double b, double c, String lossName) {
        this.ensembleWW = new WeightedWindowEnsemble(expertsNum, this.numberOfClasses, b, c, lossName);
    }

    public void setFirstTimeOfWord(DateTime firstTimeOfWord) {
        this.startWindowTime = firstTimeOfWord;
    }

    public DateTime boundStartTimeToWindowSize(DateTime timeRangeStop, int windowTimeSize, String windowTimeName) {
        DateTime boundedTimeRangeStart = null;
        if (windowTimeSize != 0) {
            if (windowTimeName.equalsIgnoreCase("day")) {
                boundedTimeRangeStart = timeRangeStop.minusDays(windowTimeSize);
            } else if (windowTimeName.equalsIgnoreCase("week")) {
                boundedTimeRangeStart = timeRangeStop.minusWeeks(windowTimeSize);
            } else if (windowTimeName.equalsIgnoreCase("month")) {
                boundedTimeRangeStart = timeRangeStop.minusMonths(windowTimeSize);
            }
        } else {
            boundedTimeRangeStart = this.startWindowTime;
        }
        return boundedTimeRangeStart;
    }

    //TODO: remove after testing
    @Deprecated
    public WordTrajectoryData(String trajectoryFormat, BagOfWordsInTime bagOfWordsInTime, String word, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName) {

        if (windowTimeSize != 0) {
            if (windowTimeName.equalsIgnoreCase("day")) {
                timeRangeStart = timeRangeStop.minusDays(windowTimeSize);
            }
        }

        if (trajectoryFormat.equalsIgnoreCase("hash")) {
            this.wordTrajectoryInHash = bagOfWordsInTime.getWordTrajectoryHash(word, timeRangeStart, timeRangeStop);
        } else if (trajectoryFormat.equalsIgnoreCase("hashArray")) {
            this.wordTrajectoryInArray = bagOfWordsInTime.getWordTrajectoryArray(word, timeRangeStart, timeRangeStop);
        }
    }

    /**
     * Light-weight version
     */
    public ArrayList<Double> getZeroConditionalCounts() {
        ArrayList<Double> zeroConditionalCounts = new ArrayList<Double>();
        for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
            zeroConditionalCounts.add(0.0D);
        }
        return zeroConditionalCounts;
    }

    /**
     * Light-weight version
     */
    public void setConditionalCounts(ArrayList<Double> wordConditionalCounts) {
        for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
            this.currentConditionalCounts.set(classIndex, wordConditionalCounts.get(classIndex));
        }
    }

    /**
     * Light-weight version
     */
    public void updateConditionalCounts(ArrayList<Double> wordConditionalCounts) {
        double newConditionalCounts = 0.0D;
        for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
            newConditionalCounts = this.currentConditionalCounts.get(classIndex) + wordConditionalCounts.get(classIndex);
            this.currentConditionalCounts.set(classIndex, newConditionalCounts);
        }

    }

    public void printConditionalCounts() {
        System.out.println("=== Conditional Counts ===");
        for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
            System.out.println("class " + Integer.toString(classIndex) + ": " + this.currentConditionalCounts.get(classIndex));
        }
        System.out.println("=== ===");
    }

    public void setConditionalCounts(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {
        int numberOfClasses = wordConditionalCounts.size();

        for (int classIndex = 0; classIndex < numberOfClasses; classIndex++) {
            this.wordTrajectoryInTime.get(documentTime).set(classIndex, wordConditionalCounts.get(classIndex));
        }
    }

    public ArrayList<Double> updateCumulativeConditionalCounts(ArrayList<Double> observationsOfPreviousTime, int numberOfClasses) {
        ArrayList<Double> cumulativeConditionalCounts = new ArrayList<Double>();
        for (int classIndex = 0; classIndex < numberOfClasses; classIndex++) {
            cumulativeConditionalCounts.add(observationsOfPreviousTime.get(classIndex) + observationsOfPreviousTime.get(classIndex + numberOfClasses));
        }

        return cumulativeConditionalCounts;
    }


    public ArrayList<Double> getZeroCumulativeConditionalCounts(int numberOfClasses) {
        ArrayList<Double> zeroCumulativeConditionalCounts = new ArrayList<Double>();
        for (int classIndex = 0; classIndex < numberOfClasses; classIndex++) {
            zeroCumulativeConditionalCounts.add(0.0D);
        }
        return zeroCumulativeConditionalCounts;
    }

    public ArrayList<Double> getZeroConditionalCounts(int numberOfClasses) {
        ArrayList<Double> zeroConditionalCounts = new ArrayList<Double>();
        for (int classIndex = 0; classIndex < numberOfClasses; classIndex++) {
            zeroConditionalCounts.add(0.0D);
        }
        return zeroConditionalCounts;
    }

    public void addZeroEntry(DateTime documentTime, int numberOfClasses) {
        this.wordTrajectoryInTime.put(documentTime, getZeroConditionalCounts(numberOfClasses));
    }

    public void addToWordStatisticsForPoisson(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {

        //light-weight version
        this.wordStatisticsForPoisson.populateWithoutSlide(documentTime, this.currentConditionalCounts); //data set with time stamp
//        this.wordStatisticsForPoissonInst.populateWithoutSlide(this.currentConditionalCounts); //data set without time stamp
    }

    public void addToWordStatisticsForSeasonalPoisson(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {
        this.wordStatisticsForSeasonalPoisson.populate(documentTime, wordConditionalCounts); //data set with time stamp
//        this.wordStatisticsForSeasonalPoissonInst.populate(this.currentConditionalCounts); //data set without time stamp
    }

    public void addToWordStatisticsForARIMA(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {
        this.wordStatisticsForARIMA.populateWithSlide(documentTime, wordConditionalCounts);
    }

    public void addToWordStatisticsForEWMA(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {
        this.wordStatisticsForEWMA.populateWithSlide(wordConditionalCounts);
    }

    public void addToWordLightWeightTrajectoryInstBased(DateTime documentTime, ArrayList<Double> wordConditionalCounts){
        if(this.observedTimes > this.aggregationPeriod){ //first instance outside aggregation period
            //populate models
            addToWordStatisticsForPoisson(documentTime,this.currentConditionalCounts);
            addToWordStatisticsForSeasonalPoisson(documentTime, this.currentConditionalCounts);
            addToWordStatisticsForARIMA(documentTime, this.currentConditionalCounts);
            addToWordStatisticsForEWMA(documentTime, this.currentConditionalCounts);

            //ensemble observes real value
            this.ensembleWW.receiveTrueOutcome(this.currentConditionalCounts);

            if (this.trackWord) {
                this.timeSeriesPredPerformance.updateTrueOutcome(this.currentConditionalCounts);
            }

            //then updates the weights
            this.ensembleWW.updateWeights();

            //re-initialize counts and first time in period
            setConditionalCounts(wordConditionalCounts);

            this.observedTimes = 0;
        }
        else{//inside aggregation period
            updateConditionalCounts(wordConditionalCounts);
        }
        this.observedTimes++;
    }

    /**
     * light-weight version
     *
     * @param documentTime
     * @param wordConditionalCounts
     */
    public void addToWordLightWeightTrajectory(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {
        //aggregate time points every P time units
        if (this.firstTimeInPeriod.isEqual(defaultTime)) { //initialize
            updateConditionalCounts(wordConditionalCounts);
            this.firstTimeInPeriod = documentTime;
        } else {
            // ===
            // TODO: better as a function
            //get how far is document in the aggregation period
            Duration timeDifference = new Duration(this.firstTimeInPeriod, documentTime);

            long differenceInTime = 0L;

            if (this.aggregationPeriodGranularity.equals("minute")) {
                differenceInTime = timeDifference.getStandardMinutes();
            } else if (this.aggregationPeriodGranularity.equals("second")) {
                differenceInTime = timeDifference.getStandardSeconds();
            } else if (this.aggregationPeriodGranularity.equals("hour")) {
                differenceInTime = timeDifference.getStandardHours();
            } else if (this.aggregationPeriodGranularity.equals("millis")){
                differenceInTime = timeDifference.getMillis();
            } else {
                differenceInTime = 0L;
            }
            // ===
            if (differenceInTime > this.aggregationPeriod) { //first instance of time point outside the time period
                updateConditionalCounts(wordConditionalCounts);

                //populate models
                addToWordStatisticsForPoisson(documentTime, this.currentConditionalCounts);
                addToWordStatisticsForSeasonalPoisson(documentTime, this.currentConditionalCounts);
                addToWordStatisticsForARIMA(documentTime, this.currentConditionalCounts);
                addToWordStatisticsForEWMA(documentTime, this.currentConditionalCounts);

                //set new aggregation period to true
                this.newAggrPeriodFound = true;

                //ensemble observes real value
                this.ensembleWW.receiveTrueOutcome(this.currentConditionalCounts);

                if (this.trackWord) {
                    this.timeSeriesPredPerformance.updateTrueOutcome(this.currentConditionalCounts);
                }

                //then updates the weights
                this.ensembleWW.updateWeights();

                //re-initialize counts and first time in period
                setConditionalCounts(getZeroConditionalCounts());
                this.firstTimeInPeriod = documentTime;
            } else {//inside time period
                updateConditionalCounts(wordConditionalCounts);
            }
        }
    }

    public void addToWordTrajectory(DateTime documentTime, ArrayList<Double> wordConditionalCounts) {

        //if time has not been registered
        if (this.wordTrajectoryInTime.containsKey(documentTime) == false) {
            /**
             * observations for word = conditional counts + cumulative counts
             * */
            ArrayList<Double> wordObservationCounts = new ArrayList<Double>();
            ArrayList<Double> conditionalCounts = getZeroConditionalCounts(wordConditionalCounts.size());
            ArrayList<Double> cumulativeConditionalCounts;// = getZeroCumulativeConditionalCounts(wordConditionalCounts.size());

            if (this.previousDocumentTime != null) {
                cumulativeConditionalCounts = updateCumulativeConditionalCounts(this.wordTrajectoryInTime.get(this.previousDocumentTime), wordConditionalCounts.size());
            } else {
                cumulativeConditionalCounts = getZeroCumulativeConditionalCounts(wordConditionalCounts.size());
            }
            this.previousDocumentTime = documentTime;
            wordObservationCounts.addAll(conditionalCounts);
            wordObservationCounts.addAll(cumulativeConditionalCounts);

            this.wordTrajectoryInTime.put(documentTime, wordObservationCounts);
//            this.wordTrajectoryInTime.put(documentTime, getZeroConditionalCounts(wordConditionalCounts.size()) );

        }
        //now the data for the current time
        setConditionalCounts(documentTime, wordConditionalCounts);
    }


    public void resizeWordTimeTreeToWindowSize(DateTime timeRangeStart, int windowTimeSize, String windowTimeName) {
        if (windowTimeSize >= 1) {
            if (windowTimeName.equalsIgnoreCase("day")) {
                if (this.startWindowTime.isBefore(timeRangeStart.minusDays(windowTimeSize)) && this.wordTrajectoryInTime.higherKey(timeRangeStart) != null) {
                    this.wordTrajectoryInTime.headMap(timeRangeStart).clear();
                    this.startWindowTime = (DateTime) this.wordTrajectoryInTime.firstKey();
                }
            }
        }
    }

    public SortedMap<DateTime, ArrayList<Double>> resizeTrajectoryToWindowSize(DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName) {

        //get the word trajectory bounded to the window
        SortedMap<DateTime, ArrayList<Double>> windowedWordTrajectory = this.wordTrajectoryInTime.tailMap(timeRangeStart);

        return windowedWordTrajectory;
    }

    public HashMap<DateTime, ArrayList<Double>> getWordTrajectoryInHash(DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName) {
        HashMap<DateTime, ArrayList<Double>> wordCountsInTimeRange = new HashMap<DateTime, ArrayList<Double>>();

        //apply windowing for the start point of collecting data from the time series
        DateTime boundedTimeRangeStart = boundStartTimeToWindowSize(timeRangeStop, windowTimeSize, windowTimeName);

        //get the trajectory inside the window
        SortedMap<DateTime, ArrayList<Double>> windowedWordTrajectory = resizeTrajectoryToWindowSize(boundedTimeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);

        //get the statistics to a hashMap of time
        for (DateTime wordTime : windowedWordTrajectory.keySet()) {
            wordCountsInTimeRange.put(wordTime, windowedWordTrajectory.get(wordTime));
        }

        //if flag on, resize the timeTree of the word
        if (this.resizeAfterCollecting) {
            resizeWordTimeTreeToWindowSize(boundedTimeRangeStart, windowTimeSize, windowTimeName);
        }

        return wordCountsInTimeRange;
    }

    public HashMap<Integer, ArrayList<double[]>> getWordTrajectoryInArray(DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName) {

        //
        HashMap<Integer, ArrayList<double[]>> wordTrajectoryHashArray = new HashMap<Integer, ArrayList<double[]>>();

        //apply window for the start point of collecting data from the time series
        DateTime boundedTimeRangeStart = boundStartTimeToWindowSize(timeRangeStop, windowTimeSize, windowTimeName);
        //get the trajectory inside the window
        SortedMap<DateTime, ArrayList<Double>> windowedWordTrajectory = resizeTrajectoryToWindowSize(boundedTimeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);

        //get the statistics to hashMap of integer (classIndex)
        double timeOfWordInMillis;
        double wordConditionalCount;

        for (DateTime wordTime : windowedWordTrajectory.keySet()) {
            for (int classIndex = 0; classIndex < this.wordTrajectoryInTime.get(wordTime).size(); classIndex++) {
                timeOfWordInMillis = (double) wordTime.getMillis();
                wordConditionalCount = this.wordTrajectoryInTime.get(wordTime).get(classIndex);

                if (wordTrajectoryHashArray.containsKey(classIndex) == false) { //initialize the hashArray for class
                    wordTrajectoryHashArray.put(classIndex, new ArrayList<double[]>());
                }
                //add conditional count for class in current time
                wordTrajectoryHashArray.get(classIndex).add(new double[]{timeOfWordInMillis, wordConditionalCount});
            }
        }

        //if flag on, resize the timeTree of the word
        if (this.resizeAfterCollecting) {
            resizeWordTimeTreeToWindowSize(boundedTimeRangeStart, windowTimeSize, windowTimeName);
        }

        return wordTrajectoryHashArray;
    }

    public double[] predictConditionalCountsEnsemble(DateTime documentTime) {
        /**
         * predictions per expert and all together
         * */
        ArrayList<String> expertNames = new ArrayList<String>(Arrays.asList("poisson", "seasonalPoisson", "arima", "ewma"));
        double[] predictedCountsExp;
        ArrayList<ArrayList<Double>> predictedCountsAllExp = new ArrayList<ArrayList<Double>>();

        double[] predictedCountsEns;  //ensemble prediction

        /**
         * initialize values
         * */
        for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
            predictedCountsAllExp.add(new ArrayList<Double>());
        }
        /**
         * collect predictions per expert to ensemble
         * */
        for (String expertName : expertNames) {
            predictedCountsExp = predictConditionalCounts(documentTime, expertName);
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                predictedCountsAllExp.get(classIndex).add(predictedCountsExp[classIndex]);
            }
        }
        /**
         * now ensemble aggregates predictions and predicts
         * */
        this.ensembleWW.collectPredictionsExp(predictedCountsAllExp);

        predictedCountsEns = this.ensembleWW.predict();

        if (this.trackWord) {
            this.timeSeriesPredPerformance.updatePredictorValues(predictedCountsAllExp, predictedCountsEns);
            boolean toFlush = false;
            this.timeSeriesPredPerformance.updateCSVbyArrayListOfArrayListLine(documentTime, "timeSeriesPerformance", toFlush);
        }

        return predictedCountsEns;
    }

    public double[] predictConditionalCounts(DateTime documentTime, String predictorName) {
        double[] predictedCounts;
        boolean binaryValue = false;//true;//if TRUE, Poisson and seasonal Poisson will predict 0/1 values

        if (predictorName.equalsIgnoreCase("poisson")) { //time-varying Poisson
            predictedCounts = this.wordStatisticsForPoisson.predictConditionalCountsByPoissonDistribution(documentTime);//data set with time stamp
//            predictedCounts = this.wordStatisticsForPoissonInst.predictConditionalCountsByPoissonDistribution(binaryValue); //data set without time stamp
        } else if (predictorName.equalsIgnoreCase("seasonalPoisson")) { //seasonal Poisson
            predictedCounts = this.wordStatisticsForSeasonalPoisson.predictConditionalCountsBySeasonalPoisson(documentTime);//data set with time stamp
//            predictedCounts = this.wordStatisticsForSeasonalPoissonInst.predictConditionalCountsBySeasonalPoisson(binaryValue); //data set without time stamp
        } else if (predictorName.equalsIgnoreCase("arima")) { // ARIMA
            predictedCounts = this.wordStatisticsForARIMA.predictConditionalCountsByARIMA();
        } else if (predictorName.equalsIgnoreCase("ewma")) { //EWMA
            predictedCounts = this.wordStatisticsForEWMA.predictConditionalCountsByEWMA();
        }
        else { //if not known predictor is requested, return -1.0
            int numberOfClasses = this.wordStatisticsForPoissonInst.getNumberOfClasses();
            predictedCounts = new double[numberOfClasses];
            Arrays.fill(predictedCounts, -1.0);
        }

        return predictedCounts;
    }

    public double[] observeRealConditionalCounts(DateTime documentTime) {
        // d) pseudo-evaluate
        double[] realValues = new double[this.wordStatisticsForPoissonInst.getNumberOfClasses()];
        Arrays.fill(realValues, 0.0);


        for (int classIndex = 0; classIndex < this.wordStatisticsForPoissonInst.getNumberOfClasses(); classIndex++) {
            realValues[classIndex] = this.wordTrajectoryInTime.get(documentTime).get(classIndex);
        }

        return realValues;
    }



    public class WordStatisticsForEWMA {
        private ArrayList<ArrayList<Double>> stats4EWMA;
        private int numberOfClasses;
        private int timeSeriesMaxLength;
        private double alpha;

        public WordStatisticsForEWMA(int numberOfClasses, int timeSeriesMaxLength, double alpha) {
            this.numberOfClasses = numberOfClasses;
            this.timeSeriesMaxLength = timeSeriesMaxLength;
            this.alpha = alpha;

            this.stats4EWMA = new ArrayList<ArrayList<Double>>();
            initializeStatisticsForEWMA();
        }

        public void initializeStatisticsForEWMA() {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                this.stats4EWMA.add(new ArrayList<Double>());
            }
        }

        public void printTimeSeries() {
//            System.out.println("=== Time Series ===");
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
//                System.out.println("Class: " + classIndex);
                for (int timeSeriesIndex = 0; timeSeriesIndex < this.stats4EWMA.get(classIndex).size(); timeSeriesIndex++) {
//                    System.out.println(Double.toString(this.stats4EWMA.get(classIndex).get(timeSeriesIndex)));
                }
//                System.out.println("---");
            }
//            System.out.println("=== ===");
        }

        public void populateWithSlide(ArrayList<Double> conditionalCounts) {
            /**
             //slide depending on the longest time-series
             int classIndexWithLongestCondCounts = findLongestListOfCondCounts();
             */

            //assume list of conditional counts are of the same length of each class
            int timeSeriesLength = 0;
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
//                if(conditionalCounts.get(classIndex) != 0.0D) {
                    timeSeriesLength = this.stats4EWMA.get(classIndex).size();

                    if (timeSeriesLength == this.timeSeriesMaxLength) {
                        this.stats4EWMA.get(classIndex).remove(0);
                    }

                    this.stats4EWMA.get(classIndex).add(conditionalCounts.get(classIndex));
            }
        }

        public double computeSumAlphas(int classIndex) {
            double sumAlphas = 0.0D;
            int condCountTimeSeriesSize = this.stats4EWMA.get(classIndex).size();

            for (int instanceIndex = 0; instanceIndex < condCountTimeSeriesSize; instanceIndex++) {
                sumAlphas += Math.pow((1.0 - this.alpha), instanceIndex - 1.0) * this.alpha;
            }

            return sumAlphas;
        }

        public double computeEWMA(int classIndex) {
            double sumCounts = 0.0D;
            double sumAlphas = 0.0D;
            double ewma = 0.0D;

            for (double condCountAtInstance : this.stats4EWMA.get(classIndex)) {
                sumCounts = this.alpha * sumCounts + (1 - this.alpha) * condCountAtInstance;
            }
            //normalize the average
            // by get sum of alphas
            sumAlphas = computeSumAlphas(classIndex);

            if (sumAlphas != 0.0D) {
                ewma = sumCounts / sumAlphas;
            }
            assert sumAlphas != 0.0D : "EWMA: sum of alphas is 0 -> incorrect computation of average";
            return ewma;
        }

        public double[] predictConditionalCountsByEWMA() {
            double[] predictedCounts = new double[this.numberOfClasses];
            Arrays.fill(predictedCounts, 0.0D);
            int timeSeriesSize = 0;

            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                timeSeriesSize = this.stats4EWMA.get(classIndex).size();
                if (timeSeriesSize == 0) {//if time series is yet to be initialized
                    predictedCounts[classIndex] = 0.0D;
                } else {
                    //compute EWMA for class
                    predictedCounts[classIndex] = computeEWMA(classIndex);
                }
            }
            return predictedCounts;
        }

    } //WordStatisticsForEWMA

    public class WordStatisticsForARIMA {

        private ArrayList<ArrayList<Double>> stats4ARIMA;
        private int numberOfClasses;
        private ArimaOrder modelOrder;
        private int timeSeriesMaxLength;

        public WordStatisticsForARIMA(int numberOfClasses, int timeSeriesMaxLength) {
            this.numberOfClasses = numberOfClasses;

//            https://github.com/signaflo/java-timeseries/wiki/The-timeseries-package
            this.stats4ARIMA = new ArrayList<ArrayList<Double>>();
            initializeStatisticsForArima();
            /**
             * Based on http://people.duke.edu/~rnau/411arim.htm,
             * set up the parameters.
             * */
//            this.modelOrder = ArimaOrder.order(1,1,2); //damped trend linear exp smoothing
//            this.modelOrder = ArimaOrder.order(1, 0, 1); //ARMA
            this.modelOrder = ArimaOrder.order(1, 1, 1); //ARIMA first order
//            this.modelOrder = ArimaOrder.order(0,1,1); // simple exponential smoothing
//            this.modelOrder = ArimaOrder.order(2,0,2); //ARMA
            this.timeSeriesMaxLength = timeSeriesMaxLength;
        }

        public void initializeStatisticsForArima() {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                this.stats4ARIMA.add(new ArrayList<Double>());
            }
        }

        public void updateTimeSeriesWithoutTime(int documentClassIndex, double conditionalCount) {
            if(conditionalCount != 0.0D) {
                this.stats4ARIMA.get(documentClassIndex).add(conditionalCount);
            }
        }

        public void printTimeSeries() {
            System.out.println("=== Time Series ===");
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                System.out.println("Class: " + classIndex);
                for (int timeSeriesIndex = 0; timeSeriesIndex < this.stats4ARIMA.get(classIndex).size(); timeSeriesIndex++) {
                    System.out.println(Double.toString(this.stats4ARIMA.get(classIndex).get(timeSeriesIndex)));
                }
                System.out.println("---");
            }
            System.out.println("=== ===");
        }

        public void printTimeSeriesForecast() {
        }

        public int findLongestListOfCondCounts() {
            int indexOfLongestCondCounts = -1;
            int maxLength = -1;
            int currentLength = 0;
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                currentLength = this.stats4ARIMA.get(classIndex).size();
                if (currentLength >= maxLength) {
                    maxLength = currentLength;
                    indexOfLongestCondCounts = classIndex;
                }
            }

            assert indexOfLongestCondCounts != -1 : "Error: could not find which class has the longest time series";
            return indexOfLongestCondCounts;
        }

        public void populateWithSlide(DateTime documentTime, ArrayList<Double> conditionalCounts) {
            /**
             //slide depending on the longest time-series
             int classIndexWithLongestCondCounts = findLongestListOfCondCounts();
             */
            //assume list of conditional counts are of the same length of each class
            int timeSeriesLength = 0;
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                    timeSeriesLength = this.stats4ARIMA.get(classIndex).size();
                    if (timeSeriesLength == this.timeSeriesMaxLength) {
                        this.stats4ARIMA.get(classIndex).remove(0);
                    }

                    this.stats4ARIMA.get(classIndex).add(conditionalCounts.get(classIndex));
            }
        }

        //DateTime documentTime, ArrayList<Double> conditionalCounts
        public void populateWithoutSlide(DateTime documentTime, ArrayList<Double> conditionalCounts) {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) { //for each class index populate the matrix with statistics
                updateTimeSeriesWithoutTime(classIndex, conditionalCounts.get(classIndex));
            }
        }


        //https://github.com/signaflo/java-timeseries/wiki/ARIMA-models
        public double[] predictConditionalCountsByARIMA() {
            double[] predictedCounts = new double[this.numberOfClasses];
            Arrays.fill(predictedCounts, 0.0D);
            int timeSeriesSize = 0;
            //create ARIMA model
            //transform to time-series format
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                timeSeriesSize = this.stats4ARIMA.get(classIndex).size();
                if (timeSeriesSize > 1) {
                    //transform data
                    double[] data = arrayFrom(this.stats4ARIMA.get(classIndex));
                    TimeSeries series = TimeSeries.from(data);
                    //set up model
                    Arima model = Arima.model(series, this.modelOrder);
                    Forecast forecast = model.forecast(1);
                    predictedCounts[classIndex] = forecast.pointEstimates().at(0);
                    if (predictedCounts[classIndex] < 0.0D) { //if negative value is predicted
                        predictedCounts[classIndex] *= 0.0D;
                    } else if (Double.isNaN(predictedCounts[classIndex])) {// value is NaN
                        predictedCounts[classIndex] = 0.0D;
                    }
                } else {//too small time-series to predict from
                    predictedCounts[classIndex] = 0.0D;
                }
            }
            return predictedCounts;
        }
    }//WordStatisticsForARIMA

    public class WordStatisticsForPoissonInst{

        private int numberOfClasses;
        private double maxNumInstInBucket;

        //counts and number of instances
        private ArrayList<ArrayList<Double>> stats4Poisson;
        private ArrayList<ArrayList<Double>> numInst;

        /**
         * for seasonal Poisson
         * alpha: fading factor
         * gamma: number of historical periods
         * As defined in Definition: "Predicting Taxi-Passenger Demand using Streaming Data" Page 4.
         */
        double alpha = 0.0D;
        int gamma = 0;

        public WordStatisticsForPoissonInst(int numberOfClasses, double maxNumInstInBucket, double alpha, int gamma){
            this.numberOfClasses = numberOfClasses;
            this.maxNumInstInBucket = maxNumInstInBucket;

            this.alpha = alpha;
            this.gamma = gamma;
            initializeStatisticsForPoissonInst();
        }

        public int getNumberOfClasses(){return this.numberOfClasses;}

        /**
         * Initialize:
         * statistics for each class
         * number of instances inside each bucket of the whole history
         * */
        public void initializeStatisticsForPoissonInst(){
            this.stats4Poisson = new ArrayList<ArrayList<Double>>();
            this.numInst = new ArrayList<ArrayList<Double>>();


            for(int i = 0; i < this.gamma; i++){
                ArrayList<Double> valuesAllClasses = new ArrayList<Double>(this.numberOfClasses);
                for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                    valuesAllClasses.add(0.0);
                }
                this.stats4Poisson.add(valuesAllClasses);
                this.numInst.add(valuesAllClasses);
            }
        }

        /**
         * Populate buckets of instances,
         * assuming that the first bucket (0) holds the oldest instances
         * and last bucket (gamma-1) holds the newest instances
         * */
        public void populate(ArrayList<Double> conditionalCounts){

            assert gamma >= 1 : "Error: for seasonal Poisson at least 1 bucket of instances is needed";
            boolean aBucketIsUpdated = false;
            /**
             * walk through the bucket if you can fit the counts do so
             * */
            for(int i=0; i < this.gamma; i++){
                    for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                        if(conditionalCounts.get(classIndex) != 0.0D){
                            if(this.numInst.get(i).get(classIndex) < this.maxNumInstInBucket){
                                this.stats4Poisson.get(i).set(classIndex, this.stats4Poisson.get(i).get(classIndex) + conditionalCounts.get(classIndex));
                                this.numInst.get(i).set(classIndex, this.numInst.get(i).get(classIndex) + 1);
                                aBucketIsUpdated = true;
                                break;
                            }
                        }
                    }
            }

            /**
             * if you have found only full buckets to place the counts,
             * then you should slide the bucket by removing the first one:
             * Example: bucket1: [a,b],  bucket2: [c,d], bucket3:[e,f] -> bucket1:[c,d], bucket2:[e,f], bucket3:[new, new]
             * */
            if(aBucketIsUpdated == false){
                for(int i = 0; i < this.gamma - 1; i++){
                    for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                        if(conditionalCounts.get(classIndex) != 0.0D) {
                            this.stats4Poisson.get(i).set(classIndex, this.stats4Poisson.get(i + 1).get(classIndex));
                            this.numInst.get(i).set(classIndex, this.numInst.get(i + 1).get(classIndex));
                        }
                    }
                }
                //assert that bucket2 (gamma-2) == [e,f]
                //update last bucket3 (gamma-1)
                for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                    if(conditionalCounts.get(classIndex) != 0.0D){
                        this.stats4Poisson.get(gamma-1).set(classIndex, conditionalCounts.get(classIndex));
                        this.numInst.get(gamma-1).set(classIndex, 1.0D);
                    }
                }
            }//close sliding of buckets
        }//close populate

        /**
         * Poisson without slide
         * => gamma = 1
         * */
        public void populateWithoutSlide(ArrayList<Double> conditionalCounts){
            assert gamma == 1 : "Error: for Poisson there is only one bucket of instances";
            for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                if(conditionalCounts.get(classIndex) != 0.0D) {
                    this.stats4Poisson.get(gamma - 1).set(classIndex, this.stats4Poisson.get(gamma - 1).get(classIndex) + conditionalCounts.get(classIndex));
                    this.numInst.get(gamma - 1).set(classIndex, this.numInst.get(gamma - 1).get(classIndex) + 1);
                }
            }
        }

        public int binarizePred(double prediction){
            if(prediction == 0.0){
                return 0;
            }
            else{
                return 1;
            }
        }
        /**
         * Produce value from a Poisson distribution
         * <p>
         * <p>
         * Credits: https://stackoverflow.com/questions/1241555/algorithm-to-generate-poisson-and-binomial-random-numbers
         */
        public int getPoisson(double lambda) {
            double L = Math.exp(-lambda);
            double p = 1.0;
            int k = 0;

            do {
                k++;
                p *= Math.random();
            } while (p > L);

            return k - 1;
        }

        public double[] computeAverageRateOfBucket(int historyBucketIndex){
            double[] averageRateCondCounts = new double[this.numberOfClasses];
            Arrays.fill(averageRateCondCounts, 0.0D);

            for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                averageRateCondCounts[classIndex] = this.stats4Poisson.get(historyBucketIndex).get(classIndex) / this.numInst.get(historyBucketIndex).get(classIndex);
            }

            return averageRateCondCounts;
        }

        public double[] predictConditionalCountsByPoissonDistribution(boolean binaryValue){
            double[] conditionalCounts = new double[this.numberOfClasses];

            //a) compute average rate for bucket
            double[] averageRateBucket = computeAverageRateOfBucket(this.gamma -1);

            //b) predict the conditional counts by Poisson model with the computed time-varying average
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                if(binaryValue) {
                    conditionalCounts[classIndex] = binarizePred(getPoisson(averageRateBucket[classIndex]));
                }
                else{
                    conditionalCounts[classIndex] = getPoisson(averageRateBucket[classIndex]);
                }
            }
            return conditionalCounts;
        }

        /**
         * Def. "Predicting Taxi-Passenger using Streaming Data" Eq. 5, page 4
         */
        public double computeSumOfOmegas() {
            double sumOfOmegas = 0.0D;
            for (int weekIndex = 0; weekIndex < this.gamma; weekIndex++) {
                sumOfOmegas += this.alpha * Math.pow(this.alpha, weekIndex);
            }
            return sumOfOmegas;
        }

        /**
         * mu(t): weighted average of history
         * <p>
         * Definition: "Predicting Taxi-Passenger Demand using Streaming Data" Eq. 6, Page 4
         * <p>
         * Example:
         * <p>
         * mu(t) = {sum_i=1_gamma(lambda(t)_i * omega_i)} / sum_i=1_gamma(omega_i)
         */

        public double[] computeWeightedAverageOfHistory() {
            double[] weightedAverageOfHistory = new double[this.numberOfClasses];
            Arrays.fill(weightedAverageOfHistory, 0.0D);

            double[] averageOfSpecificBucket = new double[this.numberOfClasses];
            Arrays.fill(averageOfSpecificBucket, 0.0D);

            double[] sumFadedAverages = new double[this.numberOfClasses];
            Arrays.fill(sumFadedAverages, 0.0D);

            /**
             * compute the numerator of Eq. 6 page 4. of "Predicting Taxi-Passenger Demand using Streaming Data"
             * */
            double exponentOfAlpha = 0.0D;
            for (int bucketIndex = gamma - 1; bucketIndex >= 0; bucketIndex--) {
                averageOfSpecificBucket = computeAverageRateOfBucket(bucketIndex);
                for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                    sumFadedAverages[classIndex] += alpha * Math.pow((1 - alpha), exponentOfAlpha) * averageOfSpecificBucket[classIndex];
                }
                exponentOfAlpha += 1.0;
            }

            /**
             * compute the denominator of Eq. 6 page 4. of "Predicting Taxi-Passenger Demand using Streaming Data"
             * */
            double sumOfOmegas = computeSumOfOmegas();

            //compute whole Eq. 6
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                weightedAverageOfHistory[classIndex] = sumFadedAverages[classIndex] / sumOfOmegas;
            }
            return weightedAverageOfHistory;
        }


        public double[] predictConditionalCountsBySeasonalPoisson(boolean binaryValue) {
            double[] conditionalCounts = new double[this.numberOfClasses];
            Arrays.fill(conditionalCounts, 0.0D);

            // a) compute the time-varying average rate for found day and shift
            double[] seasonalAverageRate = computeWeightedAverageOfHistory();

            // c) predict the conditional counts by seasonal Poisson model with the faded time-varying average
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                if(binaryValue){
                    conditionalCounts[classIndex] = binarizePred(getPoisson(seasonalAverageRate[classIndex]));
                }
                else {
                    conditionalCounts[classIndex] = getPoisson(seasonalAverageRate[classIndex]);
                }
            }
            return conditionalCounts;
        }

    }//WordStatisticsForPoissonInst

    public class WordStatisticsForPoisson {
        SumStatsOverShift[][] stats4Poisson;
        private StatsStartEndTime[][] statsStartEndTime;
        /**
         * learning variables
         */
        private int numberOfClasses;

        /**
         * description of time
         */
        private int daysInWeek = 7;
        private int numberOfShifts;
        private int startingShift;
        private int shiftSize;
        private int[][] shiftLowUpLimits; //allowed maximum and minimum times for each shift => number of shifts x 2 (lower and upper limit)
        /**
         * history
         * history = historyCount * historyGranularity = 2 * week(s)
         */
        private String historyGranularity; //hour, day, week,..
        private int historyCount; //1,2,3, ..
        private int historyInDayGranularity;

        /**
         * observing statistics
         */
        private boolean[] observedForFirstTime; //per class
        private int[] firstDocumentDayIndex; //per class
        private int[] firstDocumentShiftIndex; //per class
        private DateTime[] firstDocumentTime; //per class

        /**
         * for seasonal Poisson
         * alpha: fading factor
         * gamma: number of historical periods
         * <p>
         * As defined in Definition: "Predicting Taxi-Passenger Demand using Streaming Data" Page 4.
         */
        double alpha = 0.0D;
        int gamma = 0;

        public WordStatisticsForPoisson(int numberOfClasses, int numberOfShifts, int startingShift, String historyGranularity, int historyCount, double alpha) {
            this.numberOfClasses = numberOfClasses;
            this.numberOfShifts = numberOfShifts;
            this.startingShift = startingShift;
            this.shiftSize = getSizeOfShifts(numberOfShifts);

            this.historyGranularity = historyGranularity;
            this.historyCount = historyCount;
            this.historyInDayGranularity = getHistoryInDayGranularity();
            /**
             *                  SUM Statistics over day and shift
             * -------------------------------------------------------------------------
             *       i\j          Sun  |   Mon  |  Tue  |  Wed  |  Thu  |  Fri  |  Sat  |
             *  shiftIndex_0                                |
             *  shiftIndex_1 ------------------------ stats4Poisson[i=1,j=Wed]-----------
             *      .
             *      .
             *  shiftIndex_m
             * -------------------------------------------------------------------------
             *
             * */
            this.stats4Poisson = new SumStatsOverShift[this.numberOfShifts][this.historyInDayGranularity];
            initializeStatisticsForPoisson();

            /**
             *                  time pointers over day and shift
             * -------------------------------------------------------------------------
             *      i\j         Sun  |  Mon  |  Tue  |  Wed  |  Thu  |  Fri  |  Sat  |
             *  shiftIndex_0                             |
             *  shiftIndex_1 ------------------------- startAndEnd[i=1,j=Wed]
             *      .
             *      .
             *  shiftIndex_m
             * */
            /*this.statsStartEndTime = new StatsStartEndTime[this.numberOfShifts][this.historyInDayGranularity];
            initializeStartEndTimesForPoisson();*/

            /**
             * light-weight version
             * */
//            initializeLastDocTime();

            /**
             * === Initialize hard limits for each shift
             * e.g for first shift of first day 2-6, for the second 6-10
             * */
            createLowUpLimitsForAllShifts();

            this.observedForFirstTime = new boolean[this.numberOfClasses];
            Arrays.fill(observedForFirstTime, true);

            /**
             * === Initialize indices for first and last observed elements in history
             * 1) firstDocumentDayIndex and firstDocumentShiftIndex
             * */
            this.firstDocumentDayIndex = new int[this.numberOfClasses];
            this.firstDocumentShiftIndex = new int[this.numberOfClasses];
            this.firstDocumentTime = new DateTime[this.numberOfClasses];
            Arrays.fill(firstDocumentDayIndex, 0);
            Arrays.fill(firstDocumentShiftIndex, 0);
            Arrays.fill(firstDocumentTime, defaultTime);

            /**
             * set up the alpha and gamma
             * */

            this.alpha = alpha;
            this.gamma = this.historyCount; //TODO: historyCount should be equal!!
            /**
             this.alpha = 0.7;
             this.gamma = 4;
             */
        }

        public void createLowUpLimitsForAllShifts() {
            this.shiftLowUpLimits = new int[this.numberOfShifts][2];
            int shiftLowLimit = this.startingShift;
            int shiftUpLimit = shiftLowLimit + this.shiftSize;

            for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                if (shiftIndex == 0) {
                    this.shiftLowUpLimits[shiftIndex][0] = this.startingShift;
                } else {
                    this.shiftLowUpLimits[shiftIndex][0] = shiftLowUpLimits[shiftIndex - 1][1];
                }

                this.shiftLowUpLimits[shiftIndex][1] = this.shiftLowUpLimits[shiftIndex][0] + this.shiftSize;
            }
        }

        public void printStatsMatrix() {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                String matrixOut = "=== class " + classIndex + " ===\n";
                matrixOut += ("| \t \t 0 \t \t    | \t \t 1 \t \t    | \t \t 2 \t \t    | \t \t 3 \t \t    | \t \t 4 \t \t    | \t \t 5 \t \t    | \t \t 6 \t \t    |\n");
                for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                    String allDaysOut = new String("| ");
                    for (int dayIndex = 0; dayIndex < this.daysInWeek; dayIndex++) {

                        allDaysOut += (" counts: " + this.stats4Poisson[shiftIndex][dayIndex].getConditionalCountsPerClass(classIndex));
                        allDaysOut += (" #: " + this.stats4Poisson[shiftIndex][dayIndex].getNumberOfTimePoints(classIndex));

                        allDaysOut += (" | ");
                    }
                    allDaysOut += ("\n");
                    matrixOut += (allDaysOut);
                }

                matrixOut += ("=== === ===");
                System.out.println(matrixOut);
            }
        }

        public int getNumberOfClasses() {
            return this.numberOfClasses;
        }

        public int getHistoryInDayGranularity() {
            /**
             * Implemented only for weeks
             * */
            if (this.historyGranularity.equalsIgnoreCase("week")) {
                return this.historyCount * this.daysInWeek;
            } else {
                return -1;
            }
        }

        public int getSizeOfShifts(int numberOfShifts) {
            if (numberOfShifts >= 1) {
                return 24 / numberOfShifts;
            } else {
                return 24;
            }
        }

        public void initializeStatisticsForPoisson() {
            for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                for (int dayIndex = 0; dayIndex < this.historyInDayGranularity; dayIndex++) {
                    this.stats4Poisson[shiftIndex][dayIndex] = new SumStatsOverShift(this.numberOfClasses);
                }
            }
        }

        public void initializeStartEndTimesForPoisson() {
            for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                for (int dayIndex = 0; dayIndex < this.historyInDayGranularity; dayIndex++) {
                    this.statsStartEndTime[shiftIndex][dayIndex] = new StatsStartEndTime(this.numberOfClasses);
                }
            }
        }

        /***
         * Find at which shift the documentTime belongs to
         * @param documentTime
         * @return
         */
        public int findShiftIndexOfDocumentTime(DateTime documentTime) {
            int documentHour = documentTime.getHourOfDay();
            int documentHour24Format = documentHour + 24;
            int hoursLowerLimitInShift;
            int hoursUpperLimitInShift;
            for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                hoursLowerLimitInShift = this.shiftLowUpLimits[shiftIndex][0];
                hoursUpperLimitInShift = this.shiftLowUpLimits[shiftIndex][1];
                if (documentHour >= hoursLowerLimitInShift && documentHour < hoursUpperLimitInShift) {
                    return shiftIndex;
                } else if (documentHour24Format >= hoursLowerLimitInShift && documentHour24Format < hoursUpperLimitInShift) {
                    return shiftIndex;
                }
            }

            return -1;
        }

        public boolean checkIfDocumentIsInHistory(DateTime documentTime, int documentClassIndex) {
            //compute difference of current time and first document time
            DateTime documentTimeMinusHistory = documentTime.minusDays(getHistoryInDayGranularity());
            DateTime startingTimeOfFirstDocumentInHistory = this.firstDocumentTime[documentClassIndex];
            Duration timeDifference = new Duration(startingTimeOfFirstDocumentInHistory, documentTime);
            long differenceInDays = timeDifference.getStandardDays();
            if (differenceInDays <= getHistoryInDayGranularity()) { //inside history
                if (documentTimeMinusHistory.isBefore(startingTimeOfFirstDocumentInHistory)) { //before the first observed time
                    return true;
                } else { //just after first observation - out of history
                    return false;
                }
            } else { //one or more history away
                return false;
            }
        }

        public boolean updateStartEndTimeOnShift(DateTime documentTime, int currentDayIndex, int documentClassIndex, int containedShiftIndex) {
            boolean isShiftUnused = false;
            if (this.statsStartEndTime[containedShiftIndex][currentDayIndex].getEndTimeOfClass(documentClassIndex).isAfter(documentTime)) {
                //only if the shift is never used before the end is after the time of document
                //then, save for start and end the time of document
                this.statsStartEndTime[containedShiftIndex][currentDayIndex].setStartTimeOfClass(documentClassIndex, documentTime);
                this.statsStartEndTime[containedShiftIndex][currentDayIndex].setEndTimePerClass(documentClassIndex, documentTime);
                return isShiftUnused = true;
            } else {
                //after the shift has been used, update the end of document
                this.statsStartEndTime[containedShiftIndex][currentDayIndex].setEndTimePerClass(documentClassIndex, documentTime);
            }
//            System.out.println("=== Update StartEnd ===");
//            System.out.println("word: " + wordName  + ", c: " + documentClassIndex);
//            System.out.println("shift: " + containedShiftIndex + ", day: " + currentDayIndex);
//            System.out.println("start: " + this.statsStartEndTime[containedShiftIndex][currentDayIndex].getStartTimeOfClass(documentClassIndex).toString());
//            System.out.println("stop: " + this.statsStartEndTime[containedShiftIndex][currentDayIndex].getEndTimeOfClass(documentClassIndex).toString());
//            System.out.println("=== ===");
            return isShiftUnused;
        }

        //TODO: need to change the indexRemoveLimit for an index inside the second week
        public void removeTimePointsFromWholeDays(int documentIndexRemoveLimit, int documentClassIndex) {
            for (int dayIndex = 0; dayIndex < documentIndexRemoveLimit; dayIndex++) {
                removeTimePointsFromWholeShifts(dayIndex, this.numberOfShifts, documentClassIndex);
            }
        }

        /**
         * set to default all shifts of history but the current day,shift pair
         */
        public void setUnusedShiftsForHistory(int documentDayIndex, int documentShiftIndex, int documentClassIndex) {
//            for(int dayIndex=0; dayIndex < this.daysInWeek; dayIndex++){
            for (int dayIndex = 0; dayIndex < this.historyInDayGranularity; dayIndex++) {
                for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                    if (dayIndex != documentDayIndex && shiftIndex != documentShiftIndex) {
                        setDefaultStartEndForWholeShifts(dayIndex, shiftIndex, documentClassIndex);
                    }
                }
            }
        }

        public void setDefaultStartEndForWholeDays(int documentDayIndexRemoveLimit, int documentClassIndex) {
            for (int dayIndex = 0; dayIndex < documentDayIndexRemoveLimit; dayIndex++) {
                setDefaultStartEndForWholeShifts(dayIndex, this.numberOfShifts, documentClassIndex);
            }
        }

        public void removeCountsFromWholeDays(int documentDayIndexRemoveLimit, int documentClassIndex) {
            for (int dayIndex = 0; dayIndex < documentDayIndexRemoveLimit; dayIndex++) {
                removeCountsFromWholeShifts(dayIndex, this.numberOfShifts, documentClassIndex);
            }
        }

        public void removeTimePointsFromWholeShifts(int dayIndex, int shiftIndexRemoveLimit, int documentClassIndex) {
            for (int shiftIndex = 0; shiftIndex < shiftIndexRemoveLimit; shiftIndex++) {
                this.stats4Poisson[shiftIndex][dayIndex].setNumberOfTimePoints(0L, documentClassIndex);
            }
        }

        //TODO: need to change indexRemoveLimit ?
        public void setDefaultStartEndForWholeShifts(int dayIndex, int shiftIndexRemoveLimit, int documentClassIndex) {
            for (int shiftIndex = 0; shiftIndex < shiftIndexRemoveLimit; shiftIndex++) {
                this.statsStartEndTime[shiftIndex][dayIndex].setStartTimeToDefault(documentClassIndex);
                this.statsStartEndTime[shiftIndex][dayIndex].setEndTimeToDefault(documentClassIndex);
            }
        }

        //TODO: need to change indexRemoveLimit ?
        public void removeCountsFromWholeShifts(int dayIndex, int shiftIndexRemoveLimit, int documentClassIndex) {
            for (int shiftIndex = 0; shiftIndex < shiftIndexRemoveLimit; shiftIndex++) {
                this.stats4Poisson[shiftIndex][dayIndex].setConditionalCountsPerClass(0.0, documentClassIndex);
            }
        }

        public void updateIndicesForSliding(DateTime documentTime, DateTime documentTimeMinusHistory, int documentClassIndex, int documentDayIndex, int containedShiftIndex, boolean isTimeAfter2Histories) {

            /**
             * update indices for firstDocument
             * */
            this.firstDocumentDayIndex[documentClassIndex] = documentDayIndex;
            this.firstDocumentShiftIndex[documentClassIndex] = containedShiftIndex;
            if (!isTimeAfter2Histories) {
                this.firstDocumentTime[documentClassIndex] = documentTimeMinusHistory;
            } else {
                this.firstDocumentTime[documentClassIndex] = documentTime;
            }
        }

        /**
         * Slide and update statistics for new instance outside the history
         *
         * @param documentTime
         * @param documentClassIndex
         * @param conditionalCount
         */
        public void slideAndUpdateStatsInHistory(DateTime documentTime, int documentClassIndex, double conditionalCount) {
            DateTime documentTimeMinusHistory = documentTime.minusDays(getHistoryInDayGranularity());

            int containedShiftIndex = findShiftIndexOfDocumentTime(documentTimeMinusHistory);
            /**
             * newer version
             * */
            int currentWeekIndex = getDocumentWeekIndexInHistory(documentTimeMinusHistory, documentClassIndex);
            int currentDayIndex = currentWeekIndex * this.daysInWeek + (documentTime.getDayOfWeek() - 1);
            assert currentDayIndex >= 0 && currentDayIndex <= this.historyInDayGranularity : "Error: currentDayIndex out of range";
            int dayIndex = documentTimeMinusHistory.getDayOfWeek() - 1;
            assert dayIndex > -1 && dayIndex < 7 : "Error: day index is out of [0,6]";
            boolean isTimeAfter2Histories = false;

            /**
             * sliding possibilities:
             * => if documentTimeMinusHistory < firstDocumentInHistory
             * 1. the same day as first instance
             *
             *      a) same shift+day and same starting time
             *      b) same shift+day but different starting times
             *      c) different shifts
             *
             * 2. after the day of the first instance
             *
             * => if documentTimeMinusHistory > firstDocumentInHistory
             * 3. document is after the maximum history
             * */


            DateTime startingTimeOfFirstDocumentInHistory = this.firstDocumentTime[documentClassIndex];
            Duration timeDifference = new Duration(startingTimeOfFirstDocumentInHistory, documentTime);
            long differenceInDays = timeDifference.getStandardDays();

            if (differenceInDays < 2 * getHistoryInDayGranularity()) {
                if (dayIndex == this.firstDocumentDayIndex[documentClassIndex]) {
                    if (containedShiftIndex == this.firstDocumentShiftIndex[documentClassIndex]) {
                        if (documentTimeMinusHistory.isEqual(startingTimeOfFirstDocumentInHistory)) {
                            //(inside one history back)
                            //1.a) same shift+day same starting time inside the shift
                        } else {
                            //(inside one history back)
                            //1.b) same shift+day but different starting time inside the shift
                        }
                        //remove conditional counts
                        this.stats4Poisson[containedShiftIndex][currentDayIndex].setConditionalCountsPerClass(0.0, documentClassIndex);
                        //remove time points
                        this.stats4Poisson[containedShiftIndex][dayIndex].setNumberOfTimePoints(0L, documentClassIndex);
                    } else {
                        //(inside one history back)
                        //1.c) same day but different shifts

                        // for all shifts before the current one,
                        // remove the conditional counts
                        removeCountsFromWholeShifts(currentDayIndex, containedShiftIndex, documentClassIndex);

                        //also remove the time points
                        removeTimePointsFromWholeShifts(currentDayIndex, containedShiftIndex, documentClassIndex);
                    }
                } else {
                    //(inside one history back)
                    //2. after the day of the first instance

                    /**
                     * whole removal of data
                     * */
                    //for all days up to the day of the current document
                    // empty days up to current day:
                    //remove counts
                    removeCountsFromWholeDays(currentDayIndex, documentClassIndex);
                    //also remove the time points
                    removeTimePointsFromWholeDays(currentDayIndex, documentClassIndex);

                    /**
                     * partial removal of data
                     * */
                    // empty shifts inside the day before the current shift
                    //now for the day of the current document, remove all shifts up to the shift of the current document
                    removeCountsFromWholeShifts(currentDayIndex, containedShiftIndex, documentClassIndex);
                    //also remove the time points
                    removeTimePointsFromWholeShifts(currentDayIndex, containedShiftIndex, documentClassIndex);
                }
                isTimeAfter2Histories = false;
            } else { //3. if document time minus history is after the max time of the history

                //for all days of the history
                //remove counts
                //TODO: daysInWeek -> daysInHistory
                removeCountsFromWholeDays(this.getHistoryInDayGranularity(), documentClassIndex);
                //also remove the time points
                removeTimePointsFromWholeDays(this.getHistoryInDayGranularity(), documentClassIndex);
                isTimeAfter2Histories = true;
            }

            //initialize first seen (day,index)
            /**
             * update conditional counts and number of time points
             */
            this.stats4Poisson[containedShiftIndex][currentDayIndex].setConditionalCountsPerClass(conditionalCount, documentClassIndex);
            this.stats4Poisson[containedShiftIndex][currentDayIndex].setNumberOfTimePoints(1L, documentClassIndex);

            /**
             * update indices for firstDocument and start & end of the shift
             * */
            updateIndicesForSliding(documentTime, documentTimeMinusHistory, documentClassIndex, currentDayIndex, containedShiftIndex, isTimeAfter2Histories);
        }

        public int getDocumentWeekIndexInHistory(DateTime documentTime, int documentClassIndex) {
            int currentWeekIndex = 0;
            Duration timeDifference = new Duration(this.firstDocumentTime[documentClassIndex], documentTime);
            long differenceInDays = timeDifference.getStandardDays();
//            System.out.println("diff (days) = " + differenceInDays);
            currentWeekIndex = (int) differenceInDays % this.getHistoryInDayGranularity(); //get how far the current time is from history
            currentWeekIndex = currentWeekIndex / this.daysInWeek; //get at which week the time should be mapped
//            System.out.println("currentWeekIndex = " + currentWeekIndex);
            assert currentWeekIndex >= 0 && currentWeekIndex < this.historyCount : "Error: computing the currentWeekIndex";
            return currentWeekIndex;
        }

        /**
         * @param documentTime
         * @param documentClassIndex
         * @param conditionalCount
         */
        public void updateOnShift(DateTime documentTime, int documentClassIndex, double conditionalCount) {
            int containedShiftIndex;
            int currentWeekIndex = 0;
            int currentDayIndex = 0;

            //find at which week we should update the shift
            if (this.historyCount > 1) {
                if (this.firstDocumentTime[documentClassIndex].isEqual(defaultTime)) {
                    currentWeekIndex = 0;
                } else {
                    currentWeekIndex = getDocumentWeekIndexInHistory(documentTime, documentClassIndex);
                }
                currentDayIndex = currentWeekIndex * this.daysInWeek + (documentTime.getDayOfWeek() - 1);
            } else {
                currentDayIndex = documentTime.getDayOfWeek() - 1;
                assert currentDayIndex > -1 && currentDayIndex < 7 : "Error: day index is out of [0,6]";
            }

            //find shift where the document time belongs
            containedShiftIndex = findShiftIndexOfDocumentTime(documentTime);
            assert containedShiftIndex > -1 : "Error: cannot find shift index for current document.";

            /**
             * light-weight version
             * */
            this.stats4Poisson[containedShiftIndex][currentDayIndex].increaseConditionalCount(conditionalCount, documentClassIndex);
            this.stats4Poisson[containedShiftIndex][currentDayIndex].increaseNumberOfTimePoints(1, documentClassIndex);

            if (this.observedForFirstTime[documentClassIndex]) {
                //remember the first document in history
                this.firstDocumentDayIndex[documentClassIndex] = currentDayIndex;
                this.firstDocumentShiftIndex[documentClassIndex] = containedShiftIndex;
                this.firstDocumentTime[documentClassIndex] = documentTime;
            }
        }

        public void populateWithoutSlide(DateTime documentTime, ArrayList<Double> conditionalCounts) {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) { //for each class index populate the matrix with statistics
                if (this.observedForFirstTime[classIndex]) { //first instance in history
                    updateOnShift(documentTime, classIndex, conditionalCounts.get(classIndex));
                    this.observedForFirstTime[classIndex] = false;
                } else { //for all other instances in history
                    updateOnShift(documentTime, classIndex, conditionalCounts.get(classIndex));
                }
//                }
            }
        }

        public void populate(DateTime documentTime, ArrayList<Double> conditionalCounts) {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) { //for each class index populate the matrix with statistics
                if (this.observedForFirstTime[classIndex]) {//first instance in history
//                        System.out.println("first instance in history");
                    updateOnShift(documentTime, classIndex, conditionalCounts.get(classIndex));
                    this.observedForFirstTime[classIndex] = false;
                } else if (checkIfDocumentIsInHistory(documentTime, classIndex)) { //document instance inside the history
//                        System.out.println("inside history");
                    updateOnShift(documentTime, classIndex, conditionalCounts.get(classIndex));
                } else {//document instance after history
                    //slide
                    slideAndUpdateStatsInHistory(documentTime, classIndex, conditionalCounts.get(classIndex));
                }
//                }
            }
        }

        /**
         * Def. "Predicting Taxi-Passenger using Streaming Data" Eq. 5, page 4
         */
        public double computeSumOfOmegas() {
            double sumOfOmegas = 0.0D;
            for (int weekIndex = 0; weekIndex < this.gamma; weekIndex++) {
                sumOfOmegas += this.alpha * Math.pow(this.alpha, weekIndex);
            }
            return sumOfOmegas;
        }

        /**
         * mu(t): weighted average of history
         * <p>
         * Definition: "Predicting Taxi-Passenger Demand using Streaming Data" Eq. 6, Page 4
         * <p>
         * Example:
         * <p>
         * mu(t) = {sum_i=1_gamma(lambda(t)_i * omega_i)} / sum_i=1_gamma(omega_i)
         */

        public double[] computeWeightedAverageOfHistory(int dayIndex, int shiftIndex) {
            double[] weightedAverageOfHistory = new double[this.numberOfClasses];
            Arrays.fill(weightedAverageOfHistory, 0.0D);

            double[] averageOfSpecificWeek = new double[this.numberOfClasses];
            Arrays.fill(averageOfSpecificWeek, 0.0D);

            double[] sumFadedAverages = new double[this.numberOfClasses];
            Arrays.fill(sumFadedAverages, 0.0D);

            /**
             * compute the numerator of Eq. 6 page 4.
             * */
            double exponentOfAlpha = 0.0D;
            for (int weekIndex = gamma - 1; weekIndex >= 0; weekIndex--) {
                averageOfSpecificWeek = computeTimeVaryingAverageRateOfWeek(dayIndex, shiftIndex, weekIndex);
                for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
//                    sumFadedAverages[classIndex] += alpha * Math.pow((1-alpha), weekIndex) * averageOfSpecificWeek[classIndex];
                    sumFadedAverages[classIndex] += alpha * Math.pow((1 - alpha), exponentOfAlpha) * averageOfSpecificWeek[classIndex];
                }
                exponentOfAlpha += 1.0;
            }

            /**
             * compute the denominator of Eq. 6 page 4.
             * */
            double sumOfOmegas = computeSumOfOmegas();

            //compute whole Eq. 6
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                weightedAverageOfHistory[classIndex] = sumFadedAverages[classIndex] / sumOfOmegas;
            }
            return weightedAverageOfHistory;
        }

        /**
         * Lambda(t): Time-varying average rate of week
         * <p>
         * Definition: "Predicting Taxi-Passenger Demand using Streaming Data" Page 3
         * <p>
         * Example:
         * Lambda(t) = Lambda_0 * Delta_d(t) * Eta_d(t),h(t)
         */
        public double[] computeTimeVaryingAverageRateOfWeek(int dayIndex, int shiftIndex, int weekIndex) {
            double[] timeVaryingAverageRateOfWeek = new double[this.numberOfClasses];
            Arrays.fill(timeVaryingAverageRateOfWeek, 0.0);

            dayIndex = weekIndex * this.daysInWeek + dayIndex; //move dayIndex to the correct week
            double[] averageRateOfWeek = computeAverageRateOfWeek(weekIndex);

//            System.out.println("=== Time-varying Poisson ===");
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                /**
                 * time-varying average
                 * */
//                timeVaryingAverageRateOfWeek[classIndex] = (averageRateOfWeek[classIndex] * relativeChangeForDayInWeek[classIndex] * relativeChangeForShiftInDay[classIndex]);
                /**
                 * only average
                 * */
                timeVaryingAverageRateOfWeek[classIndex] = averageRateOfWeek[classIndex];
                /*if(wordName.equals("love")) {
                    System.out.println("=== Poisson ===");
                    System.out.println("class: " + classIndex);
                    System.out.println("averageRateOfWeek: " + averageRateOfWeek[classIndex]);
                    System.out.println("relativeChangeForDayInWeek: " + relativeChangeForDayInWeek[classIndex]);
                    System.out.println("relativeChangeForShiftInDay: " + relativeChangeForShiftInDay[classIndex]);
                    System.out.println("time-varying average: " + timeVaryingAverageRateOfWeek[classIndex]);
                    System.out.println("=== ===");
                }*/
                //                System.out.println("time-varying average: " + timeVaryingAverageRateOfWeek[classIndex]);
            }
//            System.out.println("=== ===");
            return timeVaryingAverageRateOfWeek;
        }

        /**
         * Lambda_0: Average rate of week
         * <p>
         * Definition: "Predicting Taxi-Passenger Demand using Streaming Data" Page 3
         * <p>
         * Example:
         * <p>
         * lambda_0 = sum_i(totalNumberOfPointsOfDay_i * averageRateOfDay_i) / sum_i(totalNumberOfPointsOfDay_i), where i = 1,..,7 (daysInWeek).
         */
        public double[] computeAverageRateOfWeek(int weekIndex) {
//            System.out.println("AverageRateOfWeek: ");
            double[] averageRateOfWeek = new double[this.numberOfClasses];
            Arrays.fill(averageRateOfWeek, 0.0);

            long[] sumTotalNumberOfPointsOfWeek = new long[this.numberOfClasses];
            Arrays.fill(sumTotalNumberOfPointsOfWeek, 0L);
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                for (int dayIndex = weekIndex * this.daysInWeek + 0; dayIndex < (weekIndex + 1) * this.daysInWeek; dayIndex++) {
                    // for one week long
                    averageRateOfWeek[classIndex] += aggregateCountsOverWholeDay(dayIndex)[classIndex];
                    sumTotalNumberOfPointsOfWeek[classIndex] += calculateNumberOfTimePointsOverWholeDay(dayIndex)[classIndex];
                }
                if (sumTotalNumberOfPointsOfWeek[classIndex] != 0) {
                    averageRateOfWeek[classIndex] = averageRateOfWeek[classIndex] / sumTotalNumberOfPointsOfWeek[classIndex];
                }
//                System.out.println("class -> " + classIndex + ", " + averageRateOfWeek[classIndex]);
            }
//            System.out.println("===");
            return averageRateOfWeek;
        }


        public long[] calculateNumberOfTimePointsOverWholeDay(int dayIndex) {
            long[] numberOfTimePointsOverWholeDay = new long[this.numberOfClasses];
            Arrays.fill(numberOfTimePointsOverWholeDay, 0L);

            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                    numberOfTimePointsOverWholeDay[classIndex] += this.stats4Poisson[shiftIndex][dayIndex].getNumberOfTimePoints(classIndex);
                }
            }
            return numberOfTimePointsOverWholeDay;
        }

        public double[] aggregateCountsOverWholeDay(int dayIndex) {
            double[] sumCountOverDay = new double[this.numberOfClasses];
            Arrays.fill(sumCountOverDay, 0.0D);
            assert dayIndex >= 0 && dayIndex < 7 : "Error: cannot compute sums of counts for day with index outside of [0,6]";

            for (int shiftIndex = 0; shiftIndex < this.numberOfShifts; shiftIndex++) {
                for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                    sumCountOverDay[classIndex] += this.stats4Poisson[shiftIndex][dayIndex].getConditionalCountsPerClass(classIndex);
                }
            }

            return sumCountOverDay;
        }

        /**
         * Produce value from a Poisson distribution
         * <p>
         * <p>
         * Credits: https://stackoverflow.com/questions/1241555/algorithm-to-generate-poisson-and-binomial-random-numbers
         */
        public int getPoisson(double lambda) {
            double L = Math.exp(-lambda);
            double p = 1.0;
            int k = 0;

            do {
                k++;
                p *= Math.random();
            } while (p > L);

            return k - 1;
        }

        public double[] predictConditionalCountsByPoissonDistribution(DateTime documentTime) {
//            System.out.println("Predicting counts by Poisson!");
            double[] conditionalCounts = new double[this.numberOfClasses];
            Arrays.fill(conditionalCounts, 0.0D);

            // 0) print the matrix of statistics
            /*if(wordName.equals("love")){
                printStatsMatrix();
            }*/
//            printStatsMatrix();

            // a) get dayIndex and shiftIndex
            int documentDayIndex = documentTime.getDayOfWeek() - 1;
            assert documentDayIndex > -1 && documentDayIndex < 7 : "Error: day index is out of [0,6]";

            //find shift where the document time belongs
            int documentShiftIndex = findShiftIndexOfDocumentTime(documentTime);

            //for time-varying Poisson our history is accumulated inside one week
            int weekIndex = 0;
            // b) compute the time-varying average rate for found day and shift
            double[] timeVaryingAverageRateOfWeek = computeTimeVaryingAverageRateOfWeek(documentDayIndex, documentShiftIndex, weekIndex);

            /*if(wordName.equals("love")){
                System.out.println("=== pred ===");
            }*/
            // c) predict the conditional counts by Poisson model with the computed time-varying average
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                conditionalCounts[classIndex] = getPoisson(timeVaryingAverageRateOfWeek[classIndex]);
//                conditionalCounts[classIndex] = timeVaryingAverageRateOfWeek[classIndex];
                /*if(wordName.equals("love")){
                    System.out.println("pred: class " + classIndex + " -> " + conditionalCounts[classIndex]);
                }*/
            }
            /*if(wordName.equals("love")){
                System.out.println("===  ===");
            }*/
            return conditionalCounts;
        }

        public double[] predictConditionalCountsBySeasonalPoisson(DateTime documentTime) {
            double[] conditionalCounts = new double[this.numberOfClasses];
            Arrays.fill(conditionalCounts, 0.0D);

            // a) get dayIndex and shiftIndex
            int documentDayIndex = documentTime.getDayOfWeek() - 1;
            assert documentDayIndex > -1 && documentDayIndex < 7 : "Error: day index is out of [0,6]";

            //find shift where the document time belongs
            int documentShiftIndex = findShiftIndexOfDocumentTime(documentTime);

            // b) compute the time-varying average rate for found day and shift
            double[] seasonalTimeVaryingAverageRate = computeWeightedAverageOfHistory(documentDayIndex, documentShiftIndex);

            // c) predict the conditional counts by seasonal Poisson model with the faded time-varying average
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                conditionalCounts[classIndex] = getPoisson(seasonalTimeVaryingAverageRate[classIndex]);
            }
            return conditionalCounts;
        }
    }//WordStatisticsForPoisson

}//WordTrajectoryData
