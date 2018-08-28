package de.l3s.oscar.Evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.DoubleVector;
import moa.core.Example;
import org.joda.time.DateTime;
import java.util.Arrays;

/**
 * Class to implement utilities for accuracy updated ensemble
 * based on the paper: Accuracy Updated Ensemble for Data Streams
 * with Concept Drift (http://www.cs.put.poznan.pl/dbrzezinski/publications/HAIS2011.pdf)
 * Created by damian on 14.05.17.
 */
public class AccuracyUpdatedEnsembleUtilities {

    /**
     * maximum number of instances per processed chunk
     * */
    private int chunkSize;
    private int numberOfProcessedInstances;

    /**
     * time stamp of first instance in current chunk
     * */
    private DateTime currentChunkDateTime = null;

    /**
     * mean squared error per instance in chunk
     * weights of base learners, updated by their accuracy
     * */
    private double [] baseLearnerMses;
    private double [] baseLearnerWeights;

    /**
     * one value performance for each base learner
     * */
    private EvaluationOneValuePerformance [] oneValuePerformance4BaseLearners;

    /**
     * counts for TP,TN,FP and FN
     * */
    private long [] truePositives;
    private long [] trueNegatives;
    private long [] falsePositives;
    private long [] falseNegatives;

    /**
     * total number of base learners in the ensemble
     * maximum number of used base learners for voting
     * */
    private int totalNumberOfBaseLearners;
    private int maxNumberOfBaseLearnersForVoting;

    /**
     * number of classes
     * empirical distributions of targeted classes
     * classDistributions[0] -> positives, classDistribution[1] -> negatives
     * */
    private int numberOfClasses;
    private double [] classDistributions;


    /**
     * size of batch in time
     * */
    private int numberOfHoursInChunk;

    /**
     * counter of updating the weights
     * */
    private int updateWeightsCounter;

    public AccuracyUpdatedEnsembleUtilities(int chunkSize, int totalNumberOfBaseLearners, int maxNumberOfBaseLearnersForVoting, int numberOfClasses, int numberOfHoursInChunk){

        /**
         * initialize number of instances per chunk
         */
        this.chunkSize = chunkSize;
        this.numberOfProcessedInstances = 0;

        /**
         * initialize the variables for the ensemble
         * */
        if(totalNumberOfBaseLearners >=1){
            this.totalNumberOfBaseLearners = totalNumberOfBaseLearners;
        }
        else{
            System.out.println("Caution: why only one base learner??");
            this.totalNumberOfBaseLearners = 1;
        }
        this.maxNumberOfBaseLearnersForVoting = maxNumberOfBaseLearnersForVoting; //TODO: deprecated to be removed..

        this.baseLearnerWeights = new double[totalNumberOfBaseLearners];
//        Arrays.fill(this.baseLearnerWeights, 1.0 / (double) totalNumberOfBaseLearners);
        Arrays.fill(this.baseLearnerWeights, 1.0D);
        this.baseLearnerMses = new double[totalNumberOfBaseLearners];
        Arrays.fill(this.baseLearnerMses, 1.0D);
//        System.out.println("after initialization the weights are: " + Double.toString(this.baseLearnerWeights[0]));

        /**
         * initialize the class empirical distributions
         * */
        this.numberOfClasses = numberOfClasses;
        this.classDistributions = new double[numberOfClasses];
        Arrays.fill(classDistributions, 0.0D);

        /**
         * initialize one value performance measure for each base learner
         * */
        this.oneValuePerformance4BaseLearners = new EvaluationOneValuePerformance[totalNumberOfBaseLearners];
        for (int baseLearnerIndex = 0; baseLearnerIndex < this.totalNumberOfBaseLearners; baseLearnerIndex++){
            this.oneValuePerformance4BaseLearners[baseLearnerIndex] = new EvaluationOneValuePerformance(0,0,0,0,0,0);
        }
        /**
         * initialize the confusion matrix counts
         * */
        this.truePositives = new long[totalNumberOfBaseLearners];
        this.trueNegatives = new long[totalNumberOfBaseLearners];
        this.falsePositives = new long[totalNumberOfBaseLearners];
        this.falseNegatives = new long[totalNumberOfBaseLearners];
        Arrays.fill(this.truePositives, 0L);
        Arrays.fill(this.trueNegatives, 0L);
        Arrays.fill(this.falsePositives, 0L);
        Arrays.fill(this.falseNegatives, 0L);

        /**
         * initialize the batch size in time terms
         * */
        this.numberOfHoursInChunk = numberOfHoursInChunk;

        /**
         * initialize the counter for updating the weights
         * */
        this.updateWeightsCounter = 0;
    }

    public void clearOneValuePerformances(){
        for(int baseLearnerIndex = 0; baseLearnerIndex < this.totalNumberOfBaseLearners; baseLearnerIndex++){
            this.oneValuePerformance4BaseLearners[baseLearnerIndex].clearPerformanceCounts();
        }
    }
    public void clearClassDistributions(){
        Arrays.fill(classDistributions,0.0D);
    }

    public void updateClassDistributions(Example<Instance> trainingInstanceExample){

//        this.classDistributions[(int) trainingInstanceExample.getData().classValue()]++;
        if(trainingInstanceExample.getData().classValue() == 1.0){
            this.classDistributions[0]++;
        }
        else if(trainingInstanceExample.getData().classValue() == -1.0){
            this.classDistributions[1]++;
        }

    }

    public double [] getBaseLearnerWeights(){
        return this.baseLearnerWeights;
    }

    /**
     * Computes the MSEr threshold inside the whole chunk.
     *
     * @return The MSEr threshold inside the whole chunk.
     */
    protected double calculateMserOnChunk() {
        double p_c;
        double mse_r = 0;

        for (int i = 0; i < this.classDistributions.length; i++) {
//            p_c = (double) this.classDistributions[i] / (double) this.chunkSize;
            p_c = (double) this.classDistributions[i] / (double) this.numberOfProcessedInstances;
            mse_r += p_c * ((1 - p_c) * (1 - p_c));
        }

        return mse_r;
    }

    /**
     * Calculates (normalizes) the mses of each base learner based on the length of the chunk size
     * */
    protected void calculateMseOfBaseLeanerOnChunk(){
        for (int baseLearnerIndex = 0; baseLearnerIndex < this.baseLearnerMses.length; baseLearnerIndex++){
//            this.baseLearnerMses[baseLearnerIndex] /= this.chunkSize;
            this.baseLearnerMses[baseLearnerIndex] /= this.numberOfProcessedInstances;
        }
    }


    public void clearMseOfBaseLearners(){
        Arrays.fill(baseLearnerMses,1.0D);
    }


    public void updateWeightedMseOfBaseLearnersOnInstance(Example<Instance> trainingInstanceExample, double [][] votesPerBaseLearner){
        double f_ci;
        double voteSum;
        double weightOfClassError = 1.0D;
        for (int baseLearnerIndex = 0; baseLearnerIndex < votesPerBaseLearner.length; baseLearnerIndex++){
            try {
                voteSum = 0;
                for (double classVote : votesPerBaseLearner[baseLearnerIndex]) {
                    voteSum += classVote;
                }

                if (voteSum > 0) {
                    f_ci = votesPerBaseLearner[baseLearnerIndex][(int) trainingInstanceExample.getData().classValue()];
                    baseLearnerMses[baseLearnerIndex] += (1-f_ci) * (1-f_ci);
                }
                else{
                    baseLearnerMses[baseLearnerIndex] += 1;
                }
            } catch (Exception e){
                baseLearnerMses[baseLearnerIndex] += 1;
            }
        }
    }

    /**
     * Update mean square error of each base learner for current instance
     *
     * */

    public void updateMseOfBaseLearnersOnInstance(Example<Instance> trainingInstanceExample, double [][] votesPerBaseLearner){
        double f_ci;
        double voteSum;

        for (int baseLearnerIndex = 0; baseLearnerIndex < votesPerBaseLearner.length; baseLearnerIndex++){
            try {
                voteSum = 0;
                for (double classVote : votesPerBaseLearner[baseLearnerIndex]) {
                    voteSum += classVote;
                }

                if (voteSum > 0) {
                    f_ci = votesPerBaseLearner[baseLearnerIndex][(int) trainingInstanceExample.getData().classValue()];
                    baseLearnerMses[baseLearnerIndex] += (1-f_ci) * (1-f_ci);
                }
                else{
                    baseLearnerMses[baseLearnerIndex] += 1;
                }
            } catch (Exception e){
                baseLearnerMses[baseLearnerIndex] += 1;
            }
        }
    }

    public double classDistributionInPercentage(int classIndex){
        if (classIndex < this.classDistributions.length){
            return (this.classDistributions[classIndex] / (double) this.numberOfProcessedInstances) * 100.0;
        }
        else{
            return 0.0D;
        }
    }
    public void updateBaseLearnerSpecSensWeightsOnChunk(){
        this.updateWeightsCounter++;
        double gMeanSpecificitySensitivity = 0.0;
        //this.classDistributions[1] -> empirical distribution of positives
        //this.classDistributions[2] -> empirical distribution of negatives
        gMeanSpecificitySensitivity = oneValuePerformance4BaseLearners[0].calculateGeometricMean(classDistributionInPercentage(1), classDistributionInPercentage(2));
        gMeanSpecificitySensitivity = gMeanSpecificitySensitivity / 100.0;
        double[] gMeanAllLearners = new double[this.totalNumberOfBaseLearners];

        double baseLearnerGmean = 0.01;
        double baseLearnerSensitivity = 0.01;
        double baseLearnerSpecificity = 0.01;
        /*System.out.println("---");
        System.out.println("positives: " + Double.toString(classDistributionInPercentage(1)));
        System.out.println("negatives: " + Double.toString(classDistributionInPercentage(2)));
        System.out.println("geometric mean of random " + Double.toString(gMeanSpecificitySensitivity));*/
        double minGmean = 2.0;
        for(int baseLearnerIndex = 0; baseLearnerIndex < this.totalNumberOfBaseLearners; baseLearnerIndex++){
//            System.out.println("for base learner: " + Integer.toString(baseLearnerIndex));
            baseLearnerSensitivity = this.oneValuePerformance4BaseLearners[baseLearnerIndex].calculateSensitivity_Recall();
            baseLearnerSpecificity = this.oneValuePerformance4BaseLearners[baseLearnerIndex].calculateSpecificity();
            baseLearnerGmean = this.oneValuePerformance4BaseLearners[baseLearnerIndex].calculateGeometricMean(baseLearnerSensitivity, baseLearnerSpecificity) / 100.0;
            if(baseLearnerGmean < minGmean){
                minGmean = baseLearnerGmean;
            }

            gMeanAllLearners[baseLearnerIndex] = baseLearnerGmean;
        }

//        System.out.println("min gmean: " + Double.toString(minGmean));

        for(int baseLearnerIndex = 0; baseLearnerIndex < this.totalNumberOfBaseLearners; baseLearnerIndex++){
            /*System.out.println("for base learner: " + Integer.toString(baseLearnerIndex));*/
//            this.baseLearnerWeights[baseLearnerIndex] = baseLearnerGmean / gMeanSpecificitySensitivity;
//            this.baseLearnerWeights[baseLearnerIndex] = baseLearnerGmean;
//            this.baseLearnerWeights[baseLearnerIndex] = Math.exp(baseLearnerGmean);
//            this.baseLearnerWeights[baseLearnerIndex] = Math.exp(baseLearnerGmean / gMeanSpecificitySensitivity);


            /**
             * min
             * w_i = e^(gmean_i / min(gmean))
             * */
//            this.baseLearnerWeights[baseLearnerIndex] = Math.exp(gMeanAllLearners[baseLearnerIndex] / minGmean);

            /**
             * best
             * w_i = e^(gmean_i / gmean_best)
             * */
            this.baseLearnerWeights[baseLearnerIndex] = Math.exp(gMeanAllLearners[baseLearnerIndex] / gMeanSpecificitySensitivity);


            /*System.out.println("sensitivity " + Double.toString(baseLearnerSensitivity));
            System.out.println("specificity " + Double.toString(baseLearnerSpecificity));
            System.out.println("geometric mean " + Double.toString(baseLearnerGmean));
            System.out.println("min geometric mean " + Double.toString(minGmean));
            System.out.println("weight of learner: " + Double.toString(this.baseLearnerWeights[baseLearnerIndex]));*/
        }
//        System.out.println("---");

    }

    public void updateBaseLearnerAUCWeightsOnChunk(){ //Example<Instance> trainingInstanceExample, double [][] votesPerBaseLearner
       double mse_r;
        /**
         * calculate the mse for a random classifier for the whole chunk
         */
       mse_r = this.calculateMserOnChunk();

       //calculate the mses for the base learner
       this.calculateMseOfBaseLeanerOnChunk();

       /**
        * update the weights of each base learner
        * */
//       System.out.println("=== New weights ===");
       for(int baseLearnerIndex = 0; baseLearnerIndex < baseLearnerWeights.length; baseLearnerIndex++){
          this.baseLearnerWeights[baseLearnerIndex] = 1.0 / (mse_r + this.baseLearnerMses[baseLearnerIndex] + Double.MIN_VALUE);
//          System.out.println("base learner "+ Integer.toString(baseLearnerIndex+1) + ": " + Double.toString(this.baseLearnerWeights[baseLearnerIndex]));
       }
//       System.out.println("=== ===");
    }


    public void updateBaseLearnersPerformanceOnInstance(Example<Instance> trainingInstanceExample, double [][] votesPerBaseLearner){
//        System.out.println("~~~");
        for(int baseLearnerIndex=0; baseLearnerIndex <= this.totalNumberOfBaseLearners-1; baseLearnerIndex++){
         /*   System.out.println("===");
            System.out.println("base learner " + Integer.toString(baseLearnerIndex));
            System.out.println("votesPerBaseLearner: ");*/
            /*for(int i = 0; i < votesPerBaseLearner[baseLearnerIndex].length; i++){
                System.out.println("for class the value is " + Double.toString(votesPerBaseLearner[baseLearnerIndex][i]));
            }*/
            this.oneValuePerformance4BaseLearners[baseLearnerIndex].updatePerformanceCounts(votesPerBaseLearner[baseLearnerIndex], trainingInstanceExample.getData());
//            System.out.println("===");
        }
//        System.out.println("~~~");

    }

    public void updateEnsembleStatisticsForChunk(Example<Instance> trainingInstanceExample, DateTime trainingInstanceDateTime, double [][] votesPerBaseLearner){

/*
        */
/**
         * for each instance inside the chunk:
         * update the number of instances,
         * the class distributions
         * the mse of each base learner
         * */

        DateTime oneChunkBeforeOfTrainingInstance = trainingInstanceDateTime.minusHours(this.numberOfHoursInChunk);
        /**
         * if we process the very first instance
         * */
        if(this.currentChunkDateTime == null ){
            this.currentChunkDateTime = trainingInstanceDateTime;
            /*System.out.println("initializing the chunk date time");
            System.out.println("first chunk date time: "+ this.currentChunkDateTime.toString());*/
            /*this.updateClassDistributions(trainingInstanceExample);
            this.updateMseOfBaseLearnersOnInstance(trainingInstanceExample, votesPerBaseLearner);*/
            /*updateAllPerformanceSlidingWindows(trainingInstanceExample, averagedVotesPerClass);
            if(this.numberOfBaseLearners > 1){
                updateAllBaseLearnersPerformance(trainingInstanceExample, votesPerBaseLearner);
            }*/
        }
        /**
         * if we still process instances inside the current chunk
         * */
        else if ( oneChunkBeforeOfTrainingInstance.isBefore(this.currentChunkDateTime)){
            /*System.out.println("we are inside the chunk");
            System.out.println("current instance date time: " + trainingInstanceDateTime.toString());*/
            /*this.updateClassDistributions(trainingInstanceExample);
            this.updateMseOfBaseLearnersOnInstance(trainingInstanceExample, votesPerBaseLearner);*/
        }
        /**
         * if the current instance is after the current chunk (day)
         * */
        else if ( oneChunkBeforeOfTrainingInstance.isEqual(this.currentChunkDateTime) || oneChunkBeforeOfTrainingInstance.isAfter(this.currentChunkDateTime) ){

            /**
             * update weights of each base learner for their accuracy out the chunk
             * */

            /**
             * Accuracy updated
             * */
//            this.updateBaseLearnerAUCWeightsOnChunk();

            /**
             * Geometric mean updated
             * */
            this.updateBaseLearnerSpecSensWeightsOnChunk();

            /**
             * reset the statistics of the ensemble for the next chunk
             */
            /*System.out.println("+++");
            for(int i=0; i< this.numberOfClasses; i++){
                System.out.print("For class " + Integer.toString(i));
                System.out.println(" number of instances " + Double.toString(this.classDistributions[i]));
            }
            System.out.println("+++");*/

            /**
             * update statistics for the new chunk
             * */
            this.numberOfProcessedInstances = 0;
            this.clearClassDistributions();
            this.currentChunkDateTime = trainingInstanceDateTime;
            /**
             * accuracy updated
             * */
//            this.clearMseOfBaseLearners();

            /**
             * Geometric mean updated
             * */
            this.clearOneValuePerformances();

//            System.out.println("New chunk date time: "+ this.currentChunkDateTime.toString());

        }
        this.numberOfProcessedInstances++;
        this.updateClassDistributions(trainingInstanceExample);

        /**
         * accuracy updated ensemble
         * */
        /*this.updateMseOfBaseLearnersOnInstance(trainingInstanceExample, votesPerBaseLearner);
        this.updateWeightedMseOfBaseLearnersOnInstance(trainingInstanceExample, votesPerBaseLearner);*/

        /**
         * specificity-sensitivity updated ensemble
         * */
        this.updateBaseLearnersPerformanceOnInstance(trainingInstanceExample, votesPerBaseLearner);
    }

    public double calculateSumOfAllLearnersWeight(){
        double sumOfAllLearnersWeight = 0.0D;

        for(int baseLearnerIndex= 0; baseLearnerIndex < this.totalNumberOfBaseLearners; baseLearnerIndex++){
            sumOfAllLearnersWeight += this.baseLearnerWeights[baseLearnerIndex];
        }
        return sumOfAllLearnersWeight;
    }

    /**
     * Averaging the probabilities of each class for the MNB base learners of the ensemble
     * @param votesPerClassifier            double[numberOfClassifiers][numberOfClasses], array with probabilities of each class for each classifier
     * @return averagedVotesPerClass        double[numberOfClasses], array with the average probability for each class
     * @since Feb 2017
     * @author Damianos Melidis
     */
    public double[] averagingWeigthedVotesPerClass(double[][] votesPerClassifier){

        int numberOfClassifiers = votesPerClassifier.length;

        int numberOfClasses = votesPerClassifier[0].length;
        double[] averagedWeightedVotesPerClass = new double[numberOfClasses];

        double totalSumOfWeights = this.calculateSumOfAllLearnersWeight();
        double sumOfWeightedVotesPerClass;

        for(int j=0; j<numberOfClasses; j++)
        {
            sumOfWeightedVotesPerClass = 0.0;
            for(int i=0; i< numberOfClassifiers; i++)
            {
                sumOfWeightedVotesPerClass += votesPerClassifier[i][j] * this.baseLearnerWeights[i];
            }
//            averagedWeightedVotesPerClass[j] = sumOfVotesPerClass / numberOfClassifiers;
            averagedWeightedVotesPerClass[j] = sumOfWeightedVotesPerClass / totalSumOfWeights;
        }

        return averagedWeightedVotesPerClass;
    }


    /**
     * Calculate the weighted average votes per class
     * @param votesPerBaseLearner               double [][], votes[baseLearner_i][class_j]
     * @param isFirstInstanceProcessed          boolean, is first instance processed
     * @return weighted average votes for each class double [], averagedVote[baseLearner_i]
     */
    public double [] calculateWeightedVotePerClass(double [][] votesPerBaseLearner, boolean isFirstInstanceProcessed){
        DoubleVector averagedVote = new DoubleVector();
//        System.out.println("hi");
        for(int baseLearnerIndex = 0; baseLearnerIndex < this.totalNumberOfBaseLearners; baseLearnerIndex++){
//            System.out.println("hi");
            if(this.baseLearnerWeights[baseLearnerIndex] > 0.0){
//                System.out.println("hi!");
                /*System.out.println("---");
                for (int i = 0; i < votesPerBaseLearner[baseLearnerIndex].length; i++){
                    System.out.println("for class i: " + Integer.toString(i));
                    System.out.println("votes for class: " + Double.toString(votesPerBaseLearner[baseLearnerIndex][i]));
                }
                System.out.println("---");*/
                DoubleVector baseLearnerVote = new DoubleVector(votesPerBaseLearner[baseLearnerIndex]);

                if(baseLearnerVote.sumOfValues() > 0.0 || isFirstInstanceProcessed == false){
//                    System.out.println("hi!!");
                    baseLearnerVote.normalize();
                    //scale weight and prevent overflow
                    baseLearnerVote.scaleValues(this.baseLearnerWeights[baseLearnerIndex] / (1.0 * this.totalNumberOfBaseLearners + 1.0));
                    averagedVote.addValues(baseLearnerVote);
                }
            }
        }
        /*System.out.println("+++");
        for(int i= 0; i <= averagedVote.numValues(); i++){
            System.out.println("weighted votes: " + Double.toString(averagedVote.getValue(i)));
        }
        System.out.println("+++");*/
        return averagedVote.getArrayRef();
    }

}
