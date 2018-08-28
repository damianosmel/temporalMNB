package de.l3s.oscar.Evaluation;

/**
 * Created by damian on 24.04.17.
 * Class to hold performance measures out of evaluation
 */
public class EvaluationPerformance {

    private long totalCorrectSamples;
    private long totalSamples;
    protected double positiveClassValue;
    /**
     * <h1>EvaluationPerformance constructor</h1>
     * Create evaluation performance measures
     * Add the user input arguments as parameters for the evaluation
     * @since Jan 2017
     * @author Damianos Melidis
     */
//    public EvaluationPerformance(long totalCorrectSamples, long totalSamples, double positiveClassValue){
    public EvaluationPerformance(long totalCorrectSamples, long totalSamples){
        this.totalCorrectSamples = totalCorrectSamples;
        this.totalSamples = totalSamples;
//        this.positiveClassValue = positiveClassValue;
    }

    public EvaluationPerformance(){
        this.totalCorrectSamples = 0;
        this.totalSamples = 0;
    }

    public long getTotalSamples(){
        return this.totalSamples;
    }

    public long getTotalCorrectSamples(){
        return this.totalCorrectSamples;
    }

    public void setTotalSamples(long totalSamples) {this.totalSamples = totalSamples;}

    public void setTotalCorrectSamples(long totalCorrectSamples) {this.totalCorrectSamples = totalCorrectSamples;}

    public void increaseByOneTotalSamples(){
        this.totalSamples++;
    }

    public void increaseByOneTotalCorrectSamples(){
        this.totalCorrectSamples++;
    }

    public void setPositiveClassValue(double positiveClassValue){
        this.positiveClassValue = positiveClassValue;
    }


}
