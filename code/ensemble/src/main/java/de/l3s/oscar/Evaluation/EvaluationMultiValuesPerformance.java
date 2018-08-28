package de.l3s.oscar.Evaluation;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.core.Example;
import moa.core.Utils;
import moa.evaluation.FadingFactorClassificationPerformanceEvaluator;
import moa.evaluation.WindowClassificationPerformanceEvaluator;
import com.yahoo.labs.samoa.instances.Instance;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by damian on 24.04.17.
 */
public class EvaluationMultiValuesPerformance extends EvaluationPerformance {

    /**
     * buffers for multi-values statistics over the stream
     */
    private BufferedWriter wholeStreamStatisticsBuffer = null;
    private BufferedWriter slidingWindowsCountsBuffer = null;
    private BufferedWriter ensembleDiversityBuffer = null;
    private BufferedWriter baseLearnersStatisticsBuffer = null;
    private BufferedWriter baseLearnersWeightBuffer = null;

    /**
     * for significance test
     */
    private BufferedWriter wholeStreamStats4SignificanceBuffer = null;
    private int numberOfLines4Significance = 2; //for each instance you keep the true label and the predicted one.

    boolean allBuffersCreated = true;
    boolean allBuffersClosed = true;


//    double accuracySlidingWindow_mean = 0.0D;
//    double accuracySlidingWindow_std = 0.0D;
//    double kappaSlidingWindow_mean =
//    this

    /**
     * Multi-value performance measures
     */
    private WindowClassificationPerformanceEvaluator evaluator;
    private WindowClassificationPerformanceEvaluator.WindowEstimator windowEstimatorForAccuracy;
    private WindowClassificationPerformanceEvaluator.WindowEstimator windowEstimatorForKappa;

    private FadingFactorClassificationPerformanceEvaluator fadingEvaluator;
    private FadingFactorClassificationPerformanceEvaluator.FadingFactorEstimator windowFadingFactorEstimatorForAccuracy;
    private FadingFactorClassificationPerformanceEvaluator.FadingFactorEstimator windowFadingFactorEstimatorForKappa;

    private FadingFactorClassificationPerformanceEvaluator fadingEvaluator2;
    private FadingFactorClassificationPerformanceEvaluator.FadingFactorEstimator windowFadingFactorEstimator2ForAccuracy;
    private FadingFactorClassificationPerformanceEvaluator.FadingFactorEstimator windowFadingFactorEstimator2ForKappa;

    private int numberOfPerformanceSlidingWindows;

    private int windowEstimatorSize = 1000;
    private long significanceCounter = 0;

    /**
     * performance variables for base learners
     */
    private ArrayList<WindowClassificationPerformanceEvaluator> baseLearnersEvaluators;
    private ArrayList<WindowClassificationPerformanceEvaluator.WindowEstimator> baseLearnersWindowEstimatorForAccuracy;


    /**
     * Variables for aggregating performances of each sliding window per each of day (=batch)
     * into mean and standard deviation
     */
    private DateTime currentBatchDateTime = null;
    private int currentBatchCounter = 0;
    private double[] currentBatch_sumAccuracy;
    private double[] currentBatch_sumOfSquaredAccuracy;
    private double[] currentBatch_sumKappa;
    private double[] currentBatch_sumOfSquaredKappa;

    /**
     * Stable Incremental standard deviation
     */
    private double[] currentBatch_stdAccuracy;
    private double[] currentBatch_stdKappa;

    private double[] currentAccOverWindow;
    private double[] currentKappaOverWindow;

    /**
     * performance counts for each base learner
     */
    private ArrayList<Double> baseLearners_currentBatch_sumAccuracy;
    private ArrayList<Double> baseLearners_currentBatch_sumOfSquaredAccuracy;

    /**
     * weights and size for each base learner
     */
    private ArrayList<Double> baseLearners_currentBatch_sumWeights;
    private ArrayList<Double> baseLearners_currentBatch_sumOfSquaredWeights;

    private ArrayList<Double> baseLearners_currentBatch_sumSizes;
    private ArrayList<Double> baseLearners_currentBatch_sumOfSquaredSizes;

    /**
     * for significance test
     */
    private ArrayList<Double> stats4Significance_currentInstance;

//    private Long [] baseLearners_currentBatch_maxSize;

    /**
     * create formatter for date of each sample
     */
    private DateTimeFormatter dateFormatter;

    /**
     * number of base learners
     */
    private int numberOfBaseLearners = 0;

    public EvaluationMultiValuesPerformance(File outputDirectory, int numberOfPerformanceSlidingWindows) {
        super();
        this.numberOfPerformanceSlidingWindows = numberOfPerformanceSlidingWindows;

        /**
         * initialize buffers to write statistics results
         * */
        this.wholeStreamStatisticsBuffer = initializeStatisticsBuffer(outputDirectory, "slidingWindowPerformanceWholeStream.csv");
        this.baseLearnersStatisticsBuffer = initializeStatisticsBuffer(outputDirectory, "baseLearnersPerformanceWholeStream.csv");
        this.wholeStreamStats4SignificanceBuffer = initializeStatisticsBuffer(outputDirectory, "stats4SignificanceWholeStream.csv");

        /**
         * for ensemble:
         * 1) size of each base learner
         * 2) weight of each base learner
         * 3) diversity of learners
         * */
        this.slidingWindowsCountsBuffer = initializeStatisticsBuffer(outputDirectory, "slidingWindowsCounts.csv");
        this.baseLearnersWeightBuffer = initializeStatisticsBuffer(outputDirectory, "baseLearnersWeights.csv");
        this.ensembleDiversityBuffer = initializeStatisticsBuffer(outputDirectory, "ensembleDiversityPerBatch.csv");

        /**
         * initialize arrays to keep the mean and standard deviation of sliding window performances
         * */
        this.currentBatch_sumAccuracy = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentBatch_sumAccuracy, 0.0D);
        this.currentBatch_sumOfSquaredAccuracy = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentBatch_sumOfSquaredAccuracy, 0.0D);
        this.currentBatch_sumKappa = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentBatch_sumKappa, 0.0D);
        this.currentBatch_sumOfSquaredKappa = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentBatch_sumOfSquaredKappa, 0.0D);

        /**
         * stable std
         * */
        this.currentBatch_stdAccuracy = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentBatch_stdAccuracy, 0.0D);
        this.currentBatch_stdKappa = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentBatch_stdKappa, 0.0D);

        this.currentAccOverWindow = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentAccOverWindow, 0.0D);
        this.currentKappaOverWindow = new double[numberOfPerformanceSlidingWindows];
        Arrays.fill(this.currentKappaOverWindow, 0.0D);

        /**
         * initialize the array to keep the true label and the predicted one for each instance
         * */
//        this.stats4Significance_currentInstance = new double[numberOfLines4Significance];
        /**
         * initialize the formatter for the date of each row of statistics
         * */

        dateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy");
    }

    public void initializeBaseLearnersEvaluatorsAndAccuracyCounts(int numberOfBaseLearners, int windowWidth) {
        this.numberOfBaseLearners = numberOfBaseLearners;

        /**
         * initialize with zeros the accuracy counts
         * */
        baseLearners_currentBatch_sumAccuracy = new ArrayList<Double>();
        baseLearners_currentBatch_sumOfSquaredAccuracy = new ArrayList<Double>();

        for (int i = 0; i < numberOfBaseLearners; i++) {
            baseLearners_currentBatch_sumAccuracy.add(0.0);
            baseLearners_currentBatch_sumOfSquaredAccuracy.add(0.0);
        }

        /**
         * initialize the Evaluators
         * */
        baseLearnersEvaluators = new ArrayList<WindowClassificationPerformanceEvaluator>();
        baseLearnersWindowEstimatorForAccuracy = new ArrayList<WindowClassificationPerformanceEvaluator.WindowEstimator>();
        for (int i = 0; i < numberOfBaseLearners; i++) {
            WindowClassificationPerformanceEvaluator baseLearnerWindowPerformance = new WindowClassificationPerformanceEvaluator();
            baseLearnerWindowPerformance.widthOption = new IntOption("width", 'w', "Window width", windowWidth);
            baseLearnersEvaluators.add(baseLearnerWindowPerformance);
            baseLearnersWindowEstimatorForAccuracy.add(baseLearnerWindowPerformance.new WindowEstimator(this.windowEstimatorSize));
        }

        /**
         * initialize base learners weights and sizes counts
         * */
        this.baseLearners_currentBatch_sumWeights = new ArrayList<Double>();
        this.baseLearners_currentBatch_sumOfSquaredWeights = new ArrayList<Double>();

        this.baseLearners_currentBatch_sumSizes = new ArrayList<Double>();
        this.baseLearners_currentBatch_sumOfSquaredSizes = new ArrayList<Double>();

        for (int i = 0; i < numberOfBaseLearners; i++) {
            this.baseLearners_currentBatch_sumWeights.add(0.0);
            this.baseLearners_currentBatch_sumOfSquaredWeights.add(0.0);

            this.baseLearners_currentBatch_sumSizes.add(0.0);
            this.baseLearners_currentBatch_sumOfSquaredSizes.add(0.0);
        }

        /**
         * initialize arrays to keep the maximum size of each base learner
         * */
        /*this.baseLearners_currentBatch_maxSize = new Long[this.numberOfBaseLearners];
        Arrays.fill(this.baseLearners_currentBatch_maxSize, 0L);*/

    }

    public void setWindowEstimatorSize(int windowEstimatorSize) {
        this.windowEstimatorSize = windowEstimatorSize;
    }

    public BufferedWriter initializeStatisticsBuffer(File outputDirectory, String csvFileName) {

        String csvFile = outputDirectory.getPath() + File.separator + csvFileName; //evaluationStatisticsCSVFile;
//        FileWriter csvFileWriter = null;
        BufferedWriter csvBufferedWriter = null;
        try {
            /*csvFileWriter = new FileWriter(csvFile);
            csvBufferedWriter = new BufferedWriter(csvFileWriter);*/
            /**
             * specifying UTF-8
             * */
            csvBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"), 1 * 1024 * 1024); //10 * 1024 * 1024
            /*if (csvFileName.equalsIgnoreCase("stats4SignificanceWholeStream.csv")){
                csvBufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
            }*/
        } catch (IOException e) {
            System.out.println("Error: Cannot create csv file holding evaluation statistics to output directory\n" + e.toString());
        }
        return csvBufferedWriter;
    }

    public boolean areAllBuffersCreated() {
        if (this.wholeStreamStatisticsBuffer != null && this.baseLearnersStatisticsBuffer != null && this.slidingWindowsCountsBuffer != null && this.baseLearnersWeightBuffer != null && this.ensembleDiversityBuffer != null) {
            this.allBuffersCreated = true;
        } else {
            this.allBuffersCreated = false;
        }
        return this.allBuffersCreated;
    }

    public boolean flushBuffer(String evaluationPerformanceType){
        boolean isBufferedClosed = true;
//        System.out.println("=== Flush Significance ===");
        try {
            if(evaluationPerformanceType.equalsIgnoreCase("stats4Significance")){
                this.wholeStreamStats4SignificanceBuffer.flush();
            }
        } catch (IOException e){
            isBufferedClosed = false;
            System.out.println("Error: Cannot close csv file with statistics.\n" + e.toString());
        }
//        System.out.println("=== ===");
        return isBufferedClosed;

    }
    public boolean flushAndCloseBuffer(BufferedWriter performanceBuffer) {
        boolean isBufferClosed = true;
        try {
//            performanceBuffer.flush();
            performanceBuffer.close();
        } catch (IOException e) {
            isBufferClosed = false;
            System.out.println("Error: Cannot close csv file with statistics.\n" + e.toString());
        }
        return isBufferClosed;
    }

    public boolean flushAndCloseAllBuffers() {
        boolean isWholeStreamBufferClosed = flushAndCloseBuffer(this.wholeStreamStatisticsBuffer);

        boolean isWholeStreamStats4SignificanceBufferClosed = flushAndCloseBuffer(this.wholeStreamStats4SignificanceBuffer); //for significance test

        boolean isBaseLearnersBufferClosed = true; //for single learning model
        boolean isSlidingWindowCountBufferClosed = true; //for single learning model
        boolean isBaseLearnersWeightBufferClosed = true; //for single learning model
        boolean isEnsembleDiversityBufferClosed = true; //for single learning model //flushAndCloseBuffer(this.ensembleDiversityBuffer);

        if (this.numberOfBaseLearners > 1) {
            isBaseLearnersBufferClosed = flushAndCloseBuffer(this.baseLearnersStatisticsBuffer);
            isSlidingWindowCountBufferClosed = flushAndCloseBuffer(this.slidingWindowsCountsBuffer);
            isBaseLearnersWeightBufferClosed = flushAndCloseBuffer(this.baseLearnersWeightBuffer);
            isEnsembleDiversityBufferClosed = flushAndCloseBuffer(this.ensembleDiversityBuffer);

        }
        this.allBuffersClosed = isWholeStreamBufferClosed & isWholeStreamStats4SignificanceBufferClosed & isBaseLearnersBufferClosed & isSlidingWindowCountBufferClosed & isBaseLearnersWeightBufferClosed & isEnsembleDiversityBufferClosed;

        return this.allBuffersClosed;
    }


    public void createPerformanceEvaluator(int windowWidth) {
        this.evaluator = new WindowClassificationPerformanceEvaluator();
        this.evaluator.widthOption = new IntOption("width", 'w', "Window width", windowWidth); //size of sliding window
        this.windowEstimatorForAccuracy = this.evaluator.new WindowEstimator(this.windowEstimatorSize);
        this.windowEstimatorForKappa = this.evaluator.new WindowEstimator(this.windowEstimatorSize);
        //return evaluator;
    }


    public void createFadingFactorEvaluator(double fadingFactor, int indexOfFadingWindow) {
        if (indexOfFadingWindow == 0) {
            this.fadingEvaluator = new FadingFactorClassificationPerformanceEvaluator();
            this.fadingEvaluator.alphaOption = new FloatOption("alpha", 'a', "Fading factor or exponential smoothing factor", fadingFactor);
            this.windowFadingFactorEstimatorForAccuracy = this.fadingEvaluator.new FadingFactorEstimator(fadingFactor);
            this.windowFadingFactorEstimatorForKappa = this.fadingEvaluator.new FadingFactorEstimator(fadingFactor);
        } else if (indexOfFadingWindow == 1) {
            this.fadingEvaluator2 = new FadingFactorClassificationPerformanceEvaluator();
            this.fadingEvaluator2.alphaOption = new FloatOption("alpha", 'a', "Fading factor or exponential smoothing factor", fadingFactor);
            this.windowFadingFactorEstimator2ForAccuracy = this.fadingEvaluator2.new FadingFactorEstimator(fadingFactor);
            this.windowFadingFactorEstimator2ForKappa = this.fadingEvaluator2.new FadingFactorEstimator(fadingFactor);
        }
//        return fadingEvaluator;
    }

//    public calculate_

    public double calculateMeanOfValues(double sumOfValues) {
//        int numberOfValues =
        return sumOfValues / this.currentBatchCounter;
    }

    /**
     * Compute standard deviation based on Welford and Knuth method
     *  link: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
     *  Calculate eq. 44 from http://people.ds.cam.ac.uk/fanf2/hermes/doc/antiforgery/stats.pdf
     * */
    public double calculateStableStandardDeviationOfValues(double currentValue, double currentSumOfValues, double currentStd) {
        double currentVariance = 0.0D;
        if (this.currentBatchCounter == 1) {
            currentVariance = 0.0D;
        } else {
            double prevMean = (currentSumOfValues - currentValue) / this.currentBatchCounter;
            double curmean = currentSumOfValues / this.currentBatchCounter;
            currentVariance =  currentStd + (currentValue - prevMean) * (currentValue - curmean);
        }
        return Math.sqrt(currentVariance);
    }

    /**
     * Computes the standard deviation (unbasied form), https://en.wikipedia.org/wiki/Standard_deviation
     * std = { (sum_i(x^2) - n * mean(x)^2)  / (n-1)}
     */
    public double calculateStandardDeviationOfValues(double sumOfValues, double sumOfSquaredValues) {
        return Math.sqrt((sumOfSquaredValues - (Math.pow(sumOfValues, 2.0) / this.currentBatchCounter)) / (this.currentBatchCounter - 1));
    }

    /**
     * Normalizes kappa statistic inside the range of [0,1]
     *
     * @param performanceWindowIndex, index of performance window
     * @return double, normalized value of kappa statistic
     */
    public double getKappaStatisticNormalized(int performanceWindowIndex) {
        double kappaNonNormalized;

        kappaNonNormalized = this.evaluator.getKappaStatistic();
        double kappaTemporal = this.evaluator.getKappaTemporalStatistic();

        /*System.out.println("kappa: " + kappaNonNormalized);
        System.out.println("temporal kappa: " + kappaTemporal);*/
        double kappaNormalized = 0.0;
        if (Double.isNaN(kappaNonNormalized) || kappaNonNormalized < 0.0) {
            kappaNormalized = 0.0;
        } else {
            kappaNormalized = kappaNonNormalized * 100.0;
        }
        /*System.out.println("Kappa: " + kappaNonNormalized);
        if(kappaNonNormalized >= 0.001 & kappaNonNormalized <= 0.01){
            System.out.println("======");
        }*/
        return kappaNormalized;
    }

    public void updateAllBaseLearnersWeight(AccuracyUpdatedEnsembleUtilities accuracyUpdatedEnsembleUtilities) {
        double baseLearnerWeight;
        double[] baseLearnersWeights = accuracyUpdatedEnsembleUtilities.getBaseLearnerWeights();

        for (int currentBaseLearnerIndex = 0; currentBaseLearnerIndex < this.numberOfBaseLearners; currentBaseLearnerIndex++) {
            this.baseLearners_currentBatch_sumWeights.set(currentBaseLearnerIndex, this.baseLearners_currentBatch_sumWeights.get(currentBaseLearnerIndex) + baseLearnersWeights[currentBaseLearnerIndex]);
            this.baseLearners_currentBatch_sumOfSquaredWeights.set(currentBaseLearnerIndex, this.baseLearners_currentBatch_sumOfSquaredWeights.get(currentBaseLearnerIndex) + Math.pow(baseLearnersWeights[currentBaseLearnerIndex], 2.0));
        }

    }

    public void updateAllBaseLearnersSize(SlidingWindowOfInstances ensembleSlidingWindowOfInstances) {
        double baseLearnerSize = 0;

        for (int currentBaseLearnerIndex = 0; currentBaseLearnerIndex < this.numberOfBaseLearners; currentBaseLearnerIndex++) {
            this.baseLearners_currentBatch_sumSizes.set(currentBaseLearnerIndex, this.baseLearners_currentBatch_sumSizes.get(currentBaseLearnerIndex) + ensembleSlidingWindowOfInstances.getSlidingWindowNumberOfInstances(currentBaseLearnerIndex));
            this.baseLearners_currentBatch_sumOfSquaredSizes.set(currentBaseLearnerIndex, this.baseLearners_currentBatch_sumOfSquaredSizes.get(currentBaseLearnerIndex) + Math.pow(ensembleSlidingWindowOfInstances.getSlidingWindowNumberOfInstances(currentBaseLearnerIndex), 2.0));
        }

    }

    public void updateAllBaseLearnersPerformance(Example<Instance> trainingInstanceExample, double[][] votesPerBaseLearner) {
        double accuracyValue;
        double accuracyOverWindow = 0.0;
        trainingInstanceExample.setWeight(1.0);

        for (int currentBaseLearnerIndex = 0; currentBaseLearnerIndex < this.numberOfBaseLearners; currentBaseLearnerIndex++) {
            this.baseLearnersEvaluators.get(currentBaseLearnerIndex).addResult(trainingInstanceExample, votesPerBaseLearner[currentBaseLearnerIndex]);
            accuracyValue = this.baseLearnersEvaluators.get(currentBaseLearnerIndex).getFractionCorrectlyClassified() * 100.0;
            this.baseLearnersWindowEstimatorForAccuracy.get(currentBaseLearnerIndex).add(accuracyValue);
            accuracyOverWindow = this.baseLearnersWindowEstimatorForAccuracy.get(currentBaseLearnerIndex).estimation();
            this.baseLearners_currentBatch_sumAccuracy.set(currentBaseLearnerIndex, this.baseLearners_currentBatch_sumAccuracy.get(currentBaseLearnerIndex) + accuracyOverWindow);
            this.baseLearners_currentBatch_sumOfSquaredAccuracy.set(currentBaseLearnerIndex, this.baseLearners_currentBatch_sumOfSquaredAccuracy.get(currentBaseLearnerIndex) + Math.pow(accuracyOverWindow, 2.0));
        }

    }



    /**
     * update performance values in batch mode
     * */
    public void updateAllPerformanceBatch(Example<Instance> trainingInstanceExample, double[] averagedVotesPerClass){
        int performanceWindowIndex = 0; //for compatibility issues
        this.evaluator.addResult(trainingInstanceExample, averagedVotesPerClass);
    }

    public void updateAllPerformanceSlidingWindows(Example<Instance> trainingInstanceExample, double[] averagedVotesPerClass) {
        double accuracyValue;
        double kappaValue;
        double accuracyOverWindow = 0.0;
        double kappaOverWindow = 0.0;


        trainingInstanceExample.setWeight(1.0);

        for (int i = 0; i < this.numberOfPerformanceSlidingWindows; i++) {
            this.evaluator.addResult(trainingInstanceExample, averagedVotesPerClass);
            accuracyValue = this.evaluator.getFractionCorrectlyClassified() * 100.0;
//                kappaValue = this.evaluator.getKappaStatistic() * 100.0;
            kappaValue = this.getKappaStatisticNormalized(i);

            if (i == 0) {
                this.windowEstimatorForAccuracy.add(accuracyValue);
                accuracyOverWindow = this.windowEstimatorForAccuracy.estimation();

                this.windowEstimatorForKappa.add(kappaValue);
                kappaOverWindow = this.windowEstimatorForKappa.estimation();
            } else if (i == 1) {
                this.windowFadingFactorEstimatorForAccuracy.add(accuracyOverWindow);
                accuracyOverWindow = this.windowFadingFactorEstimatorForAccuracy.estimation();

                this.windowFadingFactorEstimatorForKappa.add(kappaOverWindow);
                kappaOverWindow = this.windowFadingFactorEstimatorForKappa.estimation();
            } else if (i == 2) {
                this.windowFadingFactorEstimator2ForAccuracy.add(accuracyOverWindow);
                accuracyOverWindow = this.windowFadingFactorEstimator2ForAccuracy.estimation();

                this.windowFadingFactorEstimator2ForKappa.add(kappaOverWindow);
                kappaOverWindow = this.windowFadingFactorEstimator2ForKappa.estimation();
            }
            this.currentBatch_sumAccuracy[i] += accuracyOverWindow;
            this.currentBatch_sumOfSquaredAccuracy[i] += Math.pow(accuracyOverWindow, 2.0);

            this.currentBatch_sumKappa[i] += kappaOverWindow;
            this.currentBatch_sumOfSquaredKappa[i] += Math.pow(kappaOverWindow, 2.0);

            //stable std
            this.currentAccOverWindow[i] = accuracyOverWindow;
            this.currentKappaOverWindow[i] = kappaOverWindow;
        }
    }

    public void cleanAllBaseLearnersWeight() {
        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
            this.baseLearners_currentBatch_sumWeights.set(baseLearnerIndex, 0.0D);
            this.baseLearners_currentBatch_sumOfSquaredWeights.set(baseLearnerIndex, 0.0D);
        }
    }

    public void cleanAllBaseLearnersSize() {
        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
            this.baseLearners_currentBatch_sumSizes.set(baseLearnerIndex, 0.0D);
            this.baseLearners_currentBatch_sumOfSquaredSizes.set(baseLearnerIndex, 0.0D);
        }
    }

    public void cleanAllBaseLearnersPerformanceWindows() {

        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
            this.baseLearnersEvaluators.get(baseLearnerIndex).reset();
            this.baseLearnersWindowEstimatorForAccuracy.set(baseLearnerIndex, this.baseLearnersEvaluators.get(baseLearnerIndex).new WindowEstimator(this.windowEstimatorSize));
            this.baseLearners_currentBatch_sumAccuracy.set(baseLearnerIndex, 0.0D);
            this.baseLearners_currentBatch_sumOfSquaredAccuracy.set(baseLearnerIndex, 0.0D);
        }

    }

    public void cleanAllPerformanceSlidingWindows() {
//        System.out.println("Clean evaluator of batch");

        double fadingFactor = 0.0D;
        for (int i = 0; i < this.numberOfPerformanceSlidingWindows; i++) {
            if (i == 0) {
                this.evaluator.reset();
                this.windowEstimatorForAccuracy = this.evaluator.new WindowEstimator(this.windowEstimatorSize);
                this.windowEstimatorForKappa = this.evaluator.new WindowEstimator(this.windowEstimatorSize);
            } else if (i == 1) {
                fadingFactor = this.fadingEvaluator.alphaOption.getValue();
                this.fadingEvaluator.reset();
                this.windowFadingFactorEstimatorForAccuracy = this.fadingEvaluator.new FadingFactorEstimator(fadingFactor);
                this.windowFadingFactorEstimatorForKappa = this.fadingEvaluator.new FadingFactorEstimator(fadingFactor);
            } else {
                fadingFactor = this.fadingEvaluator2.alphaOption.getValue();
                this.fadingEvaluator2.reset();
                this.windowFadingFactorEstimator2ForAccuracy = this.fadingEvaluator2.new FadingFactorEstimator(fadingFactor);
                this.windowFadingFactorEstimator2ForKappa = this.fadingEvaluator2.new FadingFactorEstimator(fadingFactor);
            }
            this.currentBatch_sumAccuracy[i] = 0.0D;
            this.currentBatch_sumOfSquaredAccuracy[i] = 0.0D;

            this.currentBatch_sumKappa[i] = 0.0D;
            this.currentBatch_sumOfSquaredKappa[i] = 0.0D;

            //stable std
            this.currentBatch_stdAccuracy[i] = 0.0D;
            this.currentBatch_stdKappa[i] = 0.0D;

            this.currentAccOverWindow[i] = 0.0D;
            this.currentKappaOverWindow[i] = 0.0D;
        }
    }

    public void updateWholeStreamPerformanceForLastInstance(SlidingWindowOfInstances ensembleSlidingWindowOfInstances, AccuracyUpdatedEnsembleUtilities accuracyUpdatedEnsembleUtilities, boolean verbose) {
        /**
         * performance in sliding windows for total model
         * */
        ArrayList<String> allBatchPerformanceRow = createRowPerformanceCSV();
        updateCSVbyArrayListLine(allBatchPerformanceRow, "slidingWindowPerformance", false, verbose);

        if (this.numberOfBaseLearners > 1) {
            /**
             * performance of each base learner of the ensemble
             * */
            ArrayList<String> allBaseLearnersPerformanceRow = createRowPerformanceBaseLearnersCSV();
            updateCSVbyArrayListLine(allBaseLearnersPerformanceRow, "baseLearnersPerformance", false, verbose);

            /**
             * weight of each base learner of the ensemble
             * */
//            ArrayList<String> baseLearnersWeightRow = createRowWeightBaseLearnersCSV(accuracyUpdatedEnsembleUtilities);
            ArrayList<String> baseLearnersWeightRow = createRowWeightBaseLearnersCSV();
            updateCSVbyArrayListLine(baseLearnersWeightRow, "baseLearnersWeight", false, verbose);

            /**
             * size of each base learner
             * */
            ArrayList<String> baseLearnersCountRow = createRowCountsBaseLearnersCSV();
            updateCSVbyArrayListLine(baseLearnersCountRow, "baseLearnersCount", false, verbose);
        }
    }

    public void updateStats4Significance(Instance trainingInstance, double[] averagedVotesPerClass, boolean verbose) {
        ArrayList<String> stats4SignificanceRow = new ArrayList<String>();
        this.significanceCounter++;
        stats4SignificanceRow.add(Long.toString(this.significanceCounter));
//        stats4SignificanceRow.add(Integer.toString((int) trainingInstance.classValue()));
        /*if(trainingInstance.classValue() == 0){
            stats4SignificanceRow.add("0");
        }
        else{
            stats4SignificanceRow.add("1");
        }*/
        stats4SignificanceRow.add(Integer.toString(Utils.maxIndex(averagedVotesPerClass))); //predicted label
        if (Utils.maxIndex(averagedVotesPerClass) == trainingInstance.classValue()) { //match of predicted label with the true label
            stats4SignificanceRow.add(Integer.toString(1));
//            stats4SignificanceRow.add("1");
        } else {
//            stats4SignificanceRow.add("0");
            stats4SignificanceRow.add(Integer.toString(0));
        }
        updateCSVbyArrayListLine(stats4SignificanceRow, "stats4Significance", false, verbose);
    }

    public void updateWholeStreamPerformanceBatch(Example<Instance> trainingInstanceExample, double[] averagedVotesPerClass, boolean verbose){
        updateAllPerformanceBatch(trainingInstanceExample, averagedVotesPerClass);
        this.currentBatchCounter++;

        if(this.currentBatchCounter % this.windowEstimatorSize == 0 && this.currentBatchCounter != 0){
            System.out.println("---");
            System.out.println("New batch at " + this.currentBatchCounter);
            double accuracyValue = this.evaluator.getFractionCorrectlyClassified() * 100.0;
            double kappaValue = this.getKappaStatisticNormalized(0);
            System.out.println("acc: " + accuracyValue);
            System.out.println("kappa: " + kappaValue);
            /*
            * HERE
            * */
            for (int i = 0; i < this.numberOfPerformanceSlidingWindows; i++) {
                this.currentBatch_sumAccuracy[i] = accuracyValue;
                this.currentBatch_sumKappa[i] = kappaValue;
            }
            this.currentBatchDateTime = new DateTime(2018, 5, 19, 12, 0, 0, 0); //data set without time so all batches have the same time.
//            ArrayList<String> allBatchPerformanceRow = createRowPerformanceCSV();
            ArrayList<String> allBatchPerformanceRow = createRowPerfCSVBatch();
            updateCSVbyArrayListLine(allBatchPerformanceRow, "slidingWindowPerformance", false, verbose);
            cleanAllPerformanceSlidingWindows();
            System.out.println("---");
        }

    }

    public void updateWholeStreamPerformance(Example<Instance> trainingInstanceExample, double[] averagedVotesPerClass, double[][] votesPerBaseLearner, SlidingWindowOfInstances ensembleSlidingWindowOfInstances, AccuracyUpdatedEnsembleUtilities accuracyUpdatedEnsembleUtilities, DateTime trainingInstanceDateTime, boolean verbose) {

        DateTime oneDayBeforeOfTrainingInstance = trainingInstanceDateTime.minusDays(1);
        /**
         * if we process the very first instance
         * */
        if (this.currentBatchDateTime == null) {
            this.currentBatchDateTime = trainingInstanceDateTime;

            updateAllPerformanceSlidingWindows(trainingInstanceExample, averagedVotesPerClass);
            if (this.numberOfBaseLearners > 1) {
                updateAllBaseLearnersPerformance(trainingInstanceExample, votesPerBaseLearner);
            }
        }
        /**
         * if we still process instances inside the current batch (day)
         * */
        else if (oneDayBeforeOfTrainingInstance.isBefore(this.currentBatchDateTime)) {
            updateAllPerformanceSlidingWindows(trainingInstanceExample, averagedVotesPerClass);

            if (this.numberOfBaseLearners > 1) {
                updateAllBaseLearnersPerformance(trainingInstanceExample, votesPerBaseLearner);
                updateAllBaseLearnersSize(ensembleSlidingWindowOfInstances);
                updateAllBaseLearnersWeight(accuracyUpdatedEnsembleUtilities);
//                updateAllBaseLearnersMaxSize(ensembleSlidingWindowOfInstances);
            }
        }
        /**
         * if the current instance is after the current batch (day)
         * */
        else if (oneDayBeforeOfTrainingInstance.isEqual(this.currentBatchDateTime) || oneDayBeforeOfTrainingInstance.isAfter(this.currentBatchDateTime)) {

            /**
             * === Sliding windows of performance ===
             * 1) calculate mean and standard deviation for each performance window
             * 2) update the csv file with above values
             * 3) re-initialize the batch values
             * */
            ArrayList<String> allBatchPerformanceRow = createRowPerformanceCSV();
            updateCSVbyArrayListLine(allBatchPerformanceRow, "slidingWindowPerformance", false, verbose);
            cleanAllPerformanceSlidingWindows();
            updateAllPerformanceSlidingWindows(trainingInstanceExample, averagedVotesPerClass);


            /**
             * === Performance of each base learner ===
             * 1) calculate mean and standard deviation for each accuracy window of base learner
             * 2) update the csv file with above values
             * 3) re-initialize the batch values
             * */

            if (this.numberOfBaseLearners > 1) {
                ArrayList<String> allBaseLearnersPerformanceRow = createRowPerformanceBaseLearnersCSV();
                updateCSVbyArrayListLine(allBaseLearnersPerformanceRow, "baseLearnersPerformance", false, verbose);
                cleanAllBaseLearnersPerformanceWindows();
                updateAllBaseLearnersPerformance(trainingInstanceExample, votesPerBaseLearner);

                /**
                 * === Weights for each base learner ===
                 * */
//                ArrayList<String> allBaseLearnersWeightRow = createRowWeightBaseLearnersCSV(accuracyUpdatedEnsembleUtilities);
                ArrayList<String> allBaseLearnersWeightRow = createRowWeightBaseLearnersCSV();
                updateCSVbyArrayListLine(allBaseLearnersWeightRow, "baseLearnersWeight", false, verbose);

                /**
                 * === Max size for each base learner ===
                 * */
                ArrayList<String> allBaseLearnersCountRow = createRowCountsBaseLearnersCSV();
                updateCSVbyArrayListLine(allBaseLearnersCountRow, "baseLearnersCount", false, verbose);

                /**
                 * now clear the sizes and weights
                 * and
                 * get the new statistics for sizes and weights for the first instance of the new day
                 * */
                cleanAllBaseLearnersWeight();
                cleanAllBaseLearnersSize();
                updateAllBaseLearnersSize(ensembleSlidingWindowOfInstances);
                updateAllBaseLearnersWeight(accuracyUpdatedEnsembleUtilities);

                /*Arrays.fill(this.baseLearners_currentBatch_maxSize, 0L);
                updateAllBaseLearnersMaxSize(ensembleSlidingWindowOfInstances);*/
            }

            this.currentBatchDateTime = trainingInstanceDateTime;

            this.currentBatchCounter = 0;
        }
        this.currentBatchCounter++;

    }

    public ArrayList<String> createRowCountsBaseLearnersCSV() {
        ArrayList<String> baseLearnerRowCounts = new ArrayList<String>();
        /**
         * add date of starting instance of batch
         * */
        baseLearnerRowCounts.add(dateFormatter.print(this.currentBatchDateTime));

        /**
         * for each base learner output
         * 1) its' size (#instances)
         * 2) flag to show if it was slided
         * */
//        int isCurrentBaseLearnerSlided = 0;

        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
//            baseLearnerRowCounts.add(Long.toString(baseLearners_currentBatch_maxSize[baseLearnerIndex]));
            baseLearnerRowCounts.add(Double.toString(calculateMeanOfValues(this.baseLearners_currentBatch_sumSizes.get(baseLearnerIndex))));
            baseLearnerRowCounts.add(Double.toString(calculateStandardDeviationOfValues(this.baseLearners_currentBatch_sumSizes.get(baseLearnerIndex), this.baseLearners_currentBatch_sumOfSquaredSizes.get(baseLearnerIndex))));
        }

        return baseLearnerRowCounts;
    }

    //    public ArrayList<String> createRowWeightBaseLearnersCSV(AccuracyUpdatedEnsembleUtilities accuracyUpdatedEnsembleUtilities){
    public ArrayList<String> createRowWeightBaseLearnersCSV() {
        ArrayList<String> baseLearnersRowWeight = new ArrayList<String>();
//        double [] baseLearnersWeight = accuracyUpdatedEnsembleUtilities.getBaseLearnerWeights();

        /**
         * add date of starting instance of batch
         * */
        baseLearnersRowWeight.add(dateFormatter.print(this.currentBatchDateTime));

        /**
         * for each base learner output its' weight for the weighted voting
         * */
        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
//            baseLearnersRowWeight.add(Double.toString(baseLearnersWeight[baseLearnerIndex]));
            baseLearnersRowWeight.add(Double.toString(calculateMeanOfValues(this.baseLearners_currentBatch_sumWeights.get(baseLearnerIndex))));
            baseLearnersRowWeight.add(Double.toString(calculateStandardDeviationOfValues(this.baseLearners_currentBatch_sumWeights.get(baseLearnerIndex), this.baseLearners_currentBatch_sumOfSquaredWeights.get(baseLearnerIndex))));
        }

        return baseLearnersRowWeight;
    }

    public ArrayList<String> createRowPerformanceBaseLearnersCSV() {
        ArrayList<String> allBaseLearnersRowPerformance = new ArrayList<String>();

        /**
         * add date of starting instance of batch
         * */
        allBaseLearnersRowPerformance.add(dateFormatter.print(this.currentBatchDateTime));
        allBaseLearnersRowPerformance.add(Integer.toString(this.currentBatchCounter));

        /**
         * for each base learner calculate as performance:
         * the mean and standard deviation of accuracy values
         * */
        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
            allBaseLearnersRowPerformance.add(Double.toString(calculateMeanOfValues(this.baseLearners_currentBatch_sumAccuracy.get(baseLearnerIndex))));
            allBaseLearnersRowPerformance.add(Double.toString(calculateStandardDeviationOfValues(this.baseLearners_currentBatch_sumAccuracy.get(baseLearnerIndex), this.baseLearners_currentBatch_sumOfSquaredAccuracy.get(baseLearnerIndex))));
        }
        return allBaseLearnersRowPerformance;
    }

    /**
     * Used for the batch evaluation where we need only the value not average and standard deviation
     * */
    public ArrayList<String> createRowPerfCSVBatch(){
        ArrayList<String> allBatchRowPerformance = new ArrayList<String>();

        /**
         * add date of starting instance of batch
         * */
        allBatchRowPerformance.add(dateFormatter.print(this.currentBatchDateTime));
        allBatchRowPerformance.add(Integer.toString(this.currentBatchCounter));

        for(int i = 0; i < this.numberOfPerformanceSlidingWindows; i++){
            //kappa
            allBatchRowPerformance.add(Double.toString(this.currentBatch_sumKappa[i]));
            allBatchRowPerformance.add(Double.toString(0.0D));
            //accuracy
            allBatchRowPerformance.add(Double.toString(this.currentBatch_sumAccuracy[i]));
            allBatchRowPerformance.add(Double.toString(0.0D));
        }
        return allBatchRowPerformance;
    }

    //data set without time stamp
    public ArrayList<String> createRowPerformanceBatchCSV(){
        ArrayList<String> allBatchRowPerformance = new ArrayList<>();

        /**
         * add date of starting instance for batch
         * */
        allBatchRowPerformance.add(dateFormatter.print(this.currentBatchDateTime));
        allBatchRowPerformance.add(Integer.toString(this.currentBatchCounter));

        /**
         * for each sliding window of performance calculate:
         * the mean of kappa and accuracy values
         * the standard deviation of kappa and accuracy values
         * */
        double zeroStd = 0.0D;

        for(int i =0; i < this.numberOfPerformanceSlidingWindows; i++){
            //kappa values
            allBatchRowPerformance.add(Double.toString(this.currentBatch_sumKappa[i]));
            allBatchRowPerformance.add(Double.toString(zeroStd));

            //accuracy values
            allBatchRowPerformance.add(Double.toString(this.currentBatch_sumAccuracy[i]));
            allBatchRowPerformance.add(Double.toString(zeroStd));
        }

        return allBatchRowPerformance;
    }

    public ArrayList<String> createRowPerformanceCSV() {
        ArrayList<String> allBatchRowPerformance = new ArrayList<String>();
//        allBatchRowPerformance.add( Integer.toString(this.currentBatchDateTime.getDayOfMonth()) + "." +  Integer.toString(this.currentBatchDateTime.getMonthOfYear()));

        /**
         * add date of starting instance of batch
         * */
        allBatchRowPerformance.add(dateFormatter.print(this.currentBatchDateTime));
        allBatchRowPerformance.add(Integer.toString(this.currentBatchCounter));

        /**
         * for each sliding window of performance calculate:
         * the mean of kappa and accuracy values
         * the standard deviation  of kappa and accuracy values
         * */
        double zeroStd = 0.0D;
        for (int i = 0; i < this.numberOfPerformanceSlidingWindows; i++) {
            //kappa values
            allBatchRowPerformance.add(Double.toString(calculateMeanOfValues(this.currentBatch_sumKappa[i])));
            //stable std
            this.currentBatch_stdKappa[i] = calculateStableStandardDeviationOfValues(this.currentKappaOverWindow[i], this.currentBatch_sumKappa[i], this.currentBatch_stdKappa[i]);
            allBatchRowPerformance.add(Double.toString(this.currentBatch_stdKappa[i]));

            //accuracy values
            allBatchRowPerformance.add(Double.toString(calculateMeanOfValues(this.currentBatch_sumAccuracy[i])));
            //stable std
            this.currentBatch_stdAccuracy[i] = calculateStableStandardDeviationOfValues(this.currentAccOverWindow[i], this.currentBatch_sumAccuracy[i], this.currentBatch_stdAccuracy[i]);
            allBatchRowPerformance.add(Double.toString(this.currentBatch_stdAccuracy[i]));
        }

        return allBatchRowPerformance;
    }

    public String createHeaderCountsBaseLearnerCSV(boolean verbose) {
        ArrayList<String> countHeaderBaseLearner = new ArrayList<String>();
        countHeaderBaseLearner.add("MonthDay");

        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
//            countHeaderBaseLearner.add("maxSize_baseLearner_" + Integer.toString(baseLearnerIndex));
//            countHeaderBaseLearner.add("isSlided_baseLearner_"+ Integer.toString(baseLearnerIndex));
            countHeaderBaseLearner.add("mean_size_baseLearner_" + Integer.toString(baseLearnerIndex));
            countHeaderBaseLearner.add("std_size_baseLearner_" + Integer.toString(baseLearnerIndex));
        }

        return updateCSVbyArrayListLine(countHeaderBaseLearner, "baseLearnersCount", false, verbose);

    }


    public String createHeaderWeightBaseLearnerCSV(boolean verbose) {
        ArrayList<String> weightHeaderBaseLearner = new ArrayList<String>();
        weightHeaderBaseLearner.add("MonthDay");
//        weightHeaderBaseLearner.add("numberOfSamples"); //Do you need it?

        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
            weightHeaderBaseLearner.add("mean_weight_baseLearner_" + Integer.toString(baseLearnerIndex));
            weightHeaderBaseLearner.add("std_weight_baseLearner_" + Integer.toString(baseLearnerIndex));
        }

        return updateCSVbyArrayListLine(weightHeaderBaseLearner, "baseLearnersWeight", false, verbose);

    }

    public String createHeaderPerformanceBaseLearnerCSV(boolean verbose) {
        ArrayList<String> performanceHeaderBaseLearner = new ArrayList<String>();
        performanceHeaderBaseLearner.add("MonthDay");
        performanceHeaderBaseLearner.add("numberOfSamples");

        for (int baseLearnerIndex = 0; baseLearnerIndex < this.numberOfBaseLearners; baseLearnerIndex++) {
            performanceHeaderBaseLearner.add("mean_accuracy_window_baseLearner_" + Integer.toString(baseLearnerIndex));
            performanceHeaderBaseLearner.add("std_accuracy_window_baseLearner_" + Integer.toString(baseLearnerIndex));
        }

        return updateCSVbyArrayListLine(performanceHeaderBaseLearner, "baseLearnersPerformance", false, verbose);
    }

    /**
     * Create header of CSV file that the performance measures are saved
     *
     * @param verbose boolean, flag to have (true) verbose out of step log or not (false)
     * @return stepLog             String, step log of updating buffer with the statistic values of current evaluation window
     */
    public String createHeaderPerformanceCSV(boolean verbose) {
        ArrayList<String> performanceHeader = new ArrayList<String>();
        performanceHeader.add("MonthDay");
        performanceHeader.add("numberOfSamples");

        for (int i = 1; i < this.numberOfPerformanceSlidingWindows + 1; i++) {
            if (i < 2) {
                performanceHeader.add("mean_kappaAccuracy_window_" + Integer.toString(i));
                performanceHeader.add("std_kappaAccuracy_window_" + Integer.toString(i));
                performanceHeader.add("mean_accuracy_window_" + Integer.toString(i));
                performanceHeader.add("std_accuracy_window_" + Integer.toString(i));
            } else {
                performanceHeader.add("mean_kappaAccuracy_FadingWindow_" + Integer.toString(i));
                performanceHeader.add("std_kappaAccuracy_FadingWindow_" + Integer.toString(i));
                performanceHeader.add("mean_accuracy_FadingWindow_" + Integer.toString(i));
                performanceHeader.add("std_accuracy_FadingWindow_" + Integer.toString(i));
            }
        }

        return updateCSVbyArrayListLine(performanceHeader, "slidingWindowPerformance", false, verbose);
    }

    public String createHeaderStats4SignificanceCSV(boolean verbose) {
        /**
         * Write predicted label and the match between it and the true label as header for significance test to csv file
         * */
        ArrayList<String> stats4PerformanceHeader = new ArrayList<String>();
        stats4PerformanceHeader.add("Num");
        stats4PerformanceHeader.add("PredictedLabel");
        stats4PerformanceHeader.add("TruePredictedMatch");

        return updateCSVbyArrayListLine(stats4PerformanceHeader, "stats4Significance", false, verbose);
    }

    /**
     * Update buffer writer connected to the csv with values of current evaluation window
     *
     * @param currentWindowStatisticsRow String[], current accuracy and kappa statistics
     * @param evaluationPerformanceType  String, text to show what type of statistics are to be coped to the buffer
     * @param verbose                    boolean, flag to have (true) verbose out of step log or not (false)
     * @param toFlush                    boolean, flag to show if the written string to the buffer will be flushed to the file (true) or not (false)
     * @return stepLog                              String, step log of updating buffer with the statistic values of current evaluation window
     */
    public String updatePerformanceCSVbyLine(String[] currentWindowStatisticsRow, String evaluationPerformanceType, boolean toFlush, boolean verbose) //@param bufferedWriter                        BufferedWriter, buffer for writing to the csv
    {
        boolean isCSVUpdateSuccessful = true;
        String stepLog = "Success";
        String csvAppender = "";
//        BufferedWriter performance
        //write each statistic value at a new column of the current row of the csv
        for (String statistic : currentWindowStatisticsRow) {
            try {
//                bufferedWriter.write(csvAppender + statistic);
                this.wholeStreamStatisticsBuffer.write(csvAppender + statistic);
                csvAppender = ",";
            } catch (IOException e) {
                if (verbose) {
                    System.out.println("Error: writing statistics value to csv.\n" + e.toString());
                    stepLog = "Error: writing statistics value to csv.\n" + e.toString();
                    isCSVUpdateSuccessful = false;
                }
            }
        }

        //now finish the row by adding a new line character
        try {
            this.wholeStreamStatisticsBuffer.write("\n");
            if (toFlush) {
                this.wholeStreamStatisticsBuffer.flush();
            }
        } catch (IOException e) {
            if (verbose) {
                System.out.println("Error: writing new line character to csv.\n" + e.toString());
                stepLog = "Error: writing new line character to csv.\n" + e.toString();
                isCSVUpdateSuccessful = false;
            }
        }
        return stepLog;
    }

    /**
     * Update buffer writer connected to the csv with values for basic statistics of sliding windows for current instance
     *
     * @param currentWindowStatisticsRow ArrayList<String>, basic statistics of windows for current instance
     * @param evaluationPerformanceType  String, text to show what type of statistics are to be coped to the buffer
     * @param toFlush                    boolean, flag to show if the written string to the buffer will be flushed to the file (true) or not (false)
     * @param verbose                    boolean, flag to have (true) verbose out of step log or not (false)
     * @return stepLog                              String, step log of updating buffer with the statistic values of current evaluation window
     */
    public String updateCSVbyArrayListLine(ArrayList<String> currentWindowStatisticsRow, String evaluationPerformanceType, boolean toFlush, boolean verbose) {

        boolean isCSVUpdateSuccessful = true;
        String stepLog = "Success";
        String csvAppender = "";
        String newString = new String("");
//        if(evaluationPerformanceType.equalsIgnoreCase("stats4Significance")){
        /*System.out.println("=== ===");
        System.out.println(currentWindowStatisticsRow);
        System.out.println("===");*/
//        }
        //write each statistic value at a new column of the current row of the csv
        for (String statistic : currentWindowStatisticsRow) {
            try {
//                bufferedWriter.write(csvAppender + statistic);
                if (evaluationPerformanceType.equalsIgnoreCase("slidingWindowPerformance")) {
//                    System.out.println("update csv");
                    this.wholeStreamStatisticsBuffer.write(csvAppender + statistic);
                } else if (evaluationPerformanceType.equalsIgnoreCase("baseLearnersPerformance")) {
                    this.baseLearnersStatisticsBuffer.write(csvAppender + statistic);
                } else if (evaluationPerformanceType.equalsIgnoreCase("baseLearnersWeight")) {
                    this.baseLearnersWeightBuffer.write(csvAppender + statistic);
                } else if (evaluationPerformanceType.equalsIgnoreCase("baseLearnersCount")) {
                    this.slidingWindowsCountsBuffer.write(csvAppender + statistic);
                } else if (evaluationPerformanceType.equalsIgnoreCase("stats4Significance")) { //for significance test
//                    System.out.println("significance");
                    this.wholeStreamStats4SignificanceBuffer.write(csvAppender + statistic);
                    newString = newString + csvAppender + statistic;
                }

//                this.slidingWindowsCountsBuffer.write(csvAppender + statistic);
                csvAppender = ",";
            } catch (IOException e) {
                if (verbose) {
                    System.out.println("Error: writing statistics value to csv.\n" + e.toString());
                    stepLog = "Error: writing statistics value to csv.\n" + e.toString();
                    isCSVUpdateSuccessful = false;
                }
            }
        }

        //now finish the row by adding a new line character
        try {
            if (evaluationPerformanceType.equalsIgnoreCase("slidingWindowPerformance")) {
                this.wholeStreamStatisticsBuffer.write("\n");
            } else if (evaluationPerformanceType.equalsIgnoreCase("baseLearnersPerformance")) {
                this.baseLearnersStatisticsBuffer.write("\n");
            } else if (evaluationPerformanceType.equalsIgnoreCase("baseLearnersWeight")) {
                this.baseLearnersWeightBuffer.write("\n");
            } else if (evaluationPerformanceType.equalsIgnoreCase("baseLearnersCount")) {
                this.slidingWindowsCountsBuffer.write("\n");
            } else if (evaluationPerformanceType.equalsIgnoreCase("stats4Significance")) {
                this.wholeStreamStats4SignificanceBuffer.write("\n");
                newString = newString + "\n";
//                System.out.println(newString);
            }
        } catch (IOException e) {
            if (verbose) {
                System.out.println("Error: writing new line character to csv.\n" + e.toString());
                stepLog = "Error: writing new line character to csv.\n" + e.toString();
                isCSVUpdateSuccessful = false;
            }
        }

        return stepLog;
    }


    public boolean checkIfBufferIsWritten(String textLog) {
        boolean bufferIsWritten = true;
        if (!textLog.equalsIgnoreCase("Success")) {
            bufferIsWritten = false;
        }

        return bufferIsWritten;
    }


}
