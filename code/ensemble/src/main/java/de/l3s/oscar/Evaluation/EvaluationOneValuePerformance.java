package de.l3s.oscar.Evaluation;

import com.yahoo.labs.samoa.instances.Instance;
import moa.core.Utils;
/**
 * Created by damian on 24.04.17.
 * Class to have all performance measures that are one value for the whole stream (such as total accuracy of the model)
 */
public class EvaluationOneValuePerformance extends EvaluationPerformance {

//    private long totalNumberCorrectInstances;
//    private long totalNumberTrainingInstances;
    private long falsePositives;
    private long falseNegatives;
    private long truePositives;
    private long trueNegatives;
    private long totalPositives;
    private long totalNegatives;
    private long totalTestingHoldoutEvaluation = 0; //total number of testing instances in holdout evaluation

    public EvaluationOneValuePerformance(long totalCorrectSamples, long totalSamples, long truePositives, long falsePositives, long trueNegatives, long falseNegatives){
        super(totalCorrectSamples,totalSamples);
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;

        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;

        this.totalPositives = this.truePositives + this.falseNegatives;
        this.totalNegatives = this.trueNegatives + this.falsePositives;
    }

    public EvaluationOneValuePerformance(EvaluationResult evaluationResult){
        super(evaluationResult.getTotalNumberCorrectInstances(), evaluationResult.getTotalNumberTrainingInstances());
        this.truePositives = evaluationResult.getTruePositives();
        this.falsePositives = evaluationResult.getFalsePositives();
        this.trueNegatives = evaluationResult.getTrueNegatives();
        this.falseNegatives = evaluationResult.getFalseNegatives();

        this.totalPositives = this.truePositives + this.falseNegatives; // evaluationResult.getTruePositives() + evaluationResult.getFalseNegatives();
        this.totalNegatives = this.trueNegatives + this.falsePositives; //evaluationResult.getTrueNegatives() + evaluationResult.getFalsePositives();

    }

    public void clearPerformanceCounts(){
        super.setTotalCorrectSamples(0L);
        super.setTotalSamples(0L);

        this.truePositives = 0;
        this.falsePositives = 0;
        this.totalPositives = 0;

        this.trueNegatives = 0;
        this.falseNegatives = 0;
        this.totalNegatives = 0;
    }


    public double calculateWholeStreamAccuracy(long totalCorrectSamples, long totalSamples){
        if (totalSamples == 0){
            return 0F;
        }
        else {
            return  (100.0 * ((double) totalCorrectSamples / totalSamples)); //(double)
        }
    }


    public double calculateGeometricMean(double sensitivity, double specificity){
        if (sensitivity == 0.0){
            sensitivity = 100.0;
        }
        if (specificity == 0.0){
            specificity = 100.0;
        }

        if( sensitivity < 0.0 || specificity < 0.0){
            return 0.00001;
        }
        else {
            return Math.sqrt(sensitivity * specificity);
        }
    }

    /**
     * Calculate the sensitivity (and for binary classification the recall) value for the whole stream evaluation
     * @return                      double, sensitivity value
     * @param truePositives         long, number of true positives
     * @param falseNegatives        long, number of false negatives
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Wiki for ML evaluation statistics</a>
     * @since Feb 2017
     * @author Damianos Melidis
     */
    public double calculateSensitivity_Recall(long truePositives, long falseNegatives)
    {
        /**
         * System.out.println("--- Recall");
         System.out.println("true positives: " + Long.toString(truePositives));
         System.out.println("false negatives: " + Long.toString(falseNegatives));
         System.out.println("--- Recall");
         */
        if ((truePositives + falseNegatives) == 0){
            System.out.println("** zero sensitivity");
            return 0.0F;
        }
        else {
            return  ( 100.0 * ((double) truePositives / ( truePositives + falseNegatives)) );
        }
    }

    public double calculateSensitivity_Recall(){
        if ((this.truePositives + this.falseNegatives) == 0){
            System.out.println("** zero sensitivity");
            return 100.0F;
        }
        else {
            return  ( 100.0 * ((double) this.truePositives / ( this.truePositives + this.falseNegatives)) );
        }
    }


    /**
     * Calculate the specificity value for the whole stream evaluation
     * @return                      float, specificity value
     * @param trueNegatives         long, number of true negatives
     * @param falsePositives        long, number of false positives
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Wiki for ML evaluation statistics</a>
     * @since Feb 2017
     * @author Damianos Melidis
     */
    public double calculateSpecificity(long trueNegatives, long falsePositives)
    {
        if ((trueNegatives + falsePositives) == 0){
            System.out.println("** zero specificity");
            return 0.0F;
        }
        else {
            return ( 100.0 * ((double) trueNegatives / (trueNegatives + falsePositives)) );
        }
    }

    public double calculateSpecificity(){
        if ((this.trueNegatives + this.falsePositives) == 0){
            System.out.println("** zero specificity");
            return 100.0F;
        }
        else {
            return ( 100.0 * ((double) this.trueNegatives / (this.trueNegatives + this.falsePositives)) );
        }
    }

    /**
     * Calculate the precision value for the whole stream evaluation
     * @return                      double, precision value
     * @param truePositives         long, number of true positives
     * @param falsePositives        long, number of false positives
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Wiki for ML evaluation statistics</a>
     * @since Feb 2017
     * @author Damianos Melidis
     */
    public double calculatePrecision(long truePositives, long falsePositives)
    {
        if ((this.truePositives + this.falsePositives) == 0){
            return 0F;
        }
        else{
            return (100.0 * ((double) this.truePositives / (this.truePositives + this.falsePositives)) );
        }
    }

    //@param recall            long, recall value
    //@param precision         long, precision value
    /**
     * Calculate the F1 score for the whole stream evaluation

     * @return                  double, F1 score
     * @param recall            double, recall value
     * @param precision         double, precision value
     * @see <a href="https://en.wikipedia.org/wiki/Precision_and_recall"> Wiki for ML evaluation statistics</a>
     * @since Feb 2017
     * @author Damianos Melidis
     */
    public double calculateF1score(double recall, double precision)
    {

        if ((precision + recall) == 0){
            return 0.00001F;
        }
        else{
            return  2.0 * ((precision * recall) / (precision + recall));
        }
    }

    /**
     * Update total number for each class by the new training instance
     * @param trainingInstance          Instance, current training instance
     */
    public void updateClassTotalCounts(Instance trainingInstance){
        if (trainingInstance.classValue() == 1.0){
            /*if(debuggingStatisticsCalculation){

                this.debugLog += "The class of training example is " + trainingInstance.classValue() + "\n";
            }*/

            this.totalPositives++;
        }
        else if (trainingInstance.classValue() == -1.0){ //weka instances trainingInstance.classValue() == 2.0
            /*if(debuggingStatisticsCalculation) {

                this.debugLog += "The class of training example is " + trainingInstance.classValue() + "\n";
            }*/
            this.totalNegatives++;
        }
    }

    public void updatePerformanceCounts(double [] averagedVotesPerClass, Instance trainingInstance){
//        System.out.print("We decided, ");
        if ( Utils.maxIndex(averagedVotesPerClass) == trainingInstance.classValue()){
//   TODO: before             if (votes[0] >= votes[1]){
//            System.out.print("correctly for: ");
            if(averagedVotesPerClass[0] >= averagedVotesPerClass[1]){ //[1] > [2]
//                System.out.println("Happy");
//                System.out.println("TP++");
                this.truePositives++;
                this.totalPositives++;
            }
            else{
//                System.out.println("Sad");
//                System.out.println("TN++");
                this.trueNegatives++;
                this.totalNegatives++;
            }
            super.increaseByOneTotalCorrectSamples();
        }
        else {
//            System.out.println("incorrectly for: ");
//  TODO: before              if (votes[0] >= votes[1]) {
//            System.out.println("size of averagedVotes: " + Integer.toString(averagedVotesPerClass.length));
            if( averagedVotesPerClass[0] >= averagedVotesPerClass[1]){ //[1] > [2]
//                System.out.println("Happy");
//                System.out.println("FP++");
                this.falsePositives++;
                this.totalNegatives++;
            }
            else{
//                System.out.println("Sad");
//                System.out.println("FN++");
                this.falseNegatives++;
                this.totalPositives++;
            }
        }
        super.increaseByOneTotalSamples();
    }

    public void increaseByOneTotalTrainingInstances(){
        super.increaseByOneTotalSamples();
    }
    // * @param evaluationResult          EvaluationResult, object holding all basic statistics from stream evaluation
    /**
     * Prints performance measures for whole stream evaluation

     * @param evaluationScheme          String, evaluation scheme (prequential or holdout)
     * @param verbose                   boolean, flag to have (true) verbose out of step log or not (false)
     * @return stepLog                  String, message with printed all performance measures
     *
     * @since Feb 2017
     * @author Damianos Melidis
     */
    public String printEvaluationStatistics( String evaluationScheme, boolean verbose) //EvaluationResult evaluationResult
    {

//        long totalTestingHoldoutEvaluation = 0;

        if (evaluationScheme.equalsIgnoreCase("holdout")){
            this.totalTestingHoldoutEvaluation = (int) (0.3 * super.getTotalSamples()); //evaluationResult.getTotalNumberTrainingInstances()
        }

        String statisticsLog = "~~~ Statistics over the total evaluation period ~~~\n";
        statisticsLog += "Total number of processed instances= " + Long.toString( super.getTotalSamples() ) + "\n"; //evaluationResult.getTotalNumberTrainingInstances()
        if (evaluationScheme.equalsIgnoreCase("holdout")) {
            statisticsLog += "out of which number of total testing instances= " + Long.toString(this.totalTestingHoldoutEvaluation) + "\n";
        }
        statisticsLog += "out of which total correctly classified= " + Long.toString(super.getTotalCorrectSamples()) + "\n"; // evaluationResult.getTotalNumberCorrectInstances()
        statisticsLog += "---" + "\n";
        statisticsLog += "True Positives= " + Long.toString(this.truePositives) + "\n"; // evaluationResult.getTruePositives()
        statisticsLog += "False Negatives= " + Long.toString(this.falseNegatives) + "\n"; // evaluationResult.getFalseNegatives()
        statisticsLog += "Total Positives= " + Long.toString(this.totalPositives) + "\n";
        statisticsLog += "True Negatives= " + Long.toString(this.trueNegatives) + "\n"; // evaluationResult.getTrueNegatives()
        statisticsLog += "False Positives= " + Long.toString(this.falsePositives) +"\n"; //evaluationResult.getFalsePositives()
        statisticsLog += "Total Negatives= " + Long.toString(this.totalNegatives) + "\n"; //evaluationResult.getTotalNegatives()
        statisticsLog += "---" + "\n";
        if (evaluationScheme.equalsIgnoreCase("holdout")) {
            statisticsLog += "Accuracy= " + Double.toString(this.calculateWholeStreamAccuracy(super.getTotalCorrectSamples(),this.totalTestingHoldoutEvaluation)) + "\n";
        }
        else{
            statisticsLog += "Accuracy= " + Double.toString(calculateWholeStreamAccuracy(super.getTotalCorrectSamples(), super.getTotalSamples())) + "\n"; // evaluationResult.getTotalNumberCorrectInstances(), evaluationResult.getTotalNumberTrainingInstances()
        }
        statisticsLog += "Recall(binary classification)/Sensitivity/True Positive Rate= " + Double.toString(calculateSensitivity_Recall(this.truePositives, this.falseNegatives )) + "\n"; //evaluationResult.getTruePositives(), evaluationResult.getFalseNegatives()
        statisticsLog += "Specificity/True Negative Rate= " + Double.toString(calculateSpecificity(this.trueNegatives, this.falsePositives)) + "\n"; // evaluationResult.getTrueNegatives(), evaluationResult.getFalsePositives()
        statisticsLog += "Precision= " + Double.toString(calculatePrecision(this.truePositives, this.falsePositives)) + "\n"; // evaluationResult.getTruePositives(), evaluationResult.getFalsePositives()
        statisticsLog += "F1 score of recall and precision= " + Double.toString(calculateF1score(calculateSensitivity_Recall(this.truePositives, this.falseNegatives), calculatePrecision(this.truePositives, this.falsePositives))) + "\n"; // evaluationResult.getTruePositives(), evaluationResult.getFalseNegatives()   evaluationResult.getTruePositives(), evaluationResult.getFalsePositives()
        statisticsLog += "~~~ End of statistics over the total evaluation period ~~~\n";

        if (verbose){
            System.out.println(statisticsLog);
        }

        return statisticsLog;
    }



}
