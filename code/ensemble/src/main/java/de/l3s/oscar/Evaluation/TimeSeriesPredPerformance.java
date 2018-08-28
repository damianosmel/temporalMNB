package de.l3s.oscar.Evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.joda.time.DateTime;
/**
 * Class to create excel to report prediction of time series
 * 2 modes:
 * 1) For only one word
 * 2) For whole instance(?)
 * Created by damian on 12.02.18.
 */
public class TimeSeriesPredPerformance extends EvaluationMultiValuesPerformance {

    protected ArrayList<ArrayList<Double>> predictionsPerExpert;
    protected int numberOfPredictors;
    protected int numberOfClasses;
    protected BufferedWriter timeSeriesPred4WordBuffer = null;
    /**
     * Constructor for mode 1: saving excel with predictions and real value in excel.
     * @param           outputDirectory, File, path for excel output
     * @param           numberOfPerformanceSlidingWindows, int, number of used sliding windows
     * @param           numberOfPredictors, int, number of predictors = 1 for outcome + # of experts + 1 (ensemble) (we add one more the observed value)
     * @param           numberOfClasses, int, number of classes
     * @param           wordName, String, name of word to track
     * */
    public TimeSeriesPredPerformance(File outputDirectory, int numberOfPerformanceSlidingWindows, int numberOfPredictors, int numberOfClasses, String wordName){
        super(outputDirectory, numberOfPerformanceSlidingWindows);
        this.timeSeriesPred4WordBuffer = initializeStatisticsBuffer(outputDirectory, "timeSeriesPred_" + wordName + ".csv");
        this.numberOfPredictors = numberOfPredictors;
        this.numberOfClasses = numberOfClasses;

        initializePredictions();
    }

    public BufferedWriter getPredBuffer(){
        return this.timeSeriesPred4WordBuffer;
    }
    protected void initializePredictions(){
        this.predictionsPerExpert = new ArrayList<ArrayList<Double>>();

        for(int expertIndex = 0; expertIndex < this.numberOfPredictors; expertIndex++){
            ArrayList<Double> predPerClass = new ArrayList<Double>();
            for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                predPerClass.add(0.0D);
            }
            this.predictionsPerExpert.add(predPerClass);
        }

    }
    /**
     * save current true outcome
     * */
    public void updateTrueOutcome(ArrayList<Double> currentTrueOutcome){
        //the first index -> true outcome
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            this.predictionsPerExpert.get(0).set(classIndex, currentTrueOutcome.get(classIndex));
        }
    }

    /**
     * save predictions per expert and prediction of ensemble
     * */
    public void updatePredictorValues(ArrayList<ArrayList<Double>> expertsPred, double [] ensemblePred){

        boolean isEnsemblePred = false;
//        System.out.println(" === updatePred === ");
        for(int expertIndex = 1; expertIndex < this.numberOfPredictors; expertIndex++){
//            System.out.println("expert " + expertIndex);
            if(expertIndex == this.numberOfPredictors - 1){
                isEnsemblePred = true;
            }
            for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                if(isEnsemblePred){
                    this.predictionsPerExpert.get(expertIndex).set(classIndex, ensemblePred[classIndex]);
                }
                else{

                    this.predictionsPerExpert.get(expertIndex).set(classIndex, expertsPred.get(classIndex).get(expertIndex - 1));
                }
//                System.out.println("class " + classIndex + "saved prediction: " + this.predictionsPerExpert.get(expertIndex).get(classIndex));
            }
            isEnsemblePred = false;
        }
//        System.out.println(" ===  === ");
    }

    /**
     * Create header of CSV file that the performance measures are saved
     *
     * @return stepLog             String, step log of updating buffer with the statistic values of current evaluation window
     */
    public String createHeaderTimeSeriesPredictionCSV(){
        //@param  verbose             boolean, flag to have (true) verbose out of step log or not (false)
        ArrayList<String> performanceHeader = new ArrayList<String>();
        performanceHeader.add("timeStamp");


        for(int predictIndex = 0; predictIndex < this.numberOfPredictors; predictIndex++){
            for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                if(predictIndex == 0){
                    performanceHeader.add(Integer.toString(classIndex) + "_trueValue");
                }
                else if (predictIndex != this.numberOfPredictors -1){
                    performanceHeader.add(Integer.toString(classIndex)+ "_predictor_" + Integer.toString(predictIndex));
                }
                else{
                    performanceHeader.add(Integer.toString(classIndex) + "_ensemble");
                }
            }
        }

        return updateCSVbyArrayListLine(performanceHeader,"timeSeriesPerformance", false);
    }

    /**
     *
     * Update buffer writer connected to the csv with values for basic statistics of sliding windows for current instance
     *
     * @param currentWindowStatisticsRow            ArrayList<String>, basic statistics of windows for current instance
     * @param evaluationPerformanceType             String, text to show what type of statistics are to be coped to the buffer
     * @param toFlush                               boolean, flag to show if the written string to the buffer will be flushed to the file (true) or not (false)

     * @return stepLog                              String, step log of updating buffer with the statistic values of current evaluation window
     */
    public String updateCSVbyArrayListLine(ArrayList<String> currentWindowStatisticsRow, String evaluationPerformanceType, boolean toFlush)
    {

        boolean isCSVUpdateSuccessful = true;
        String stepLog = "Success";
        String csvAppender = "";

        //write each statistic value at a new column of the current row of the csv
        for (String statistic : currentWindowStatisticsRow)
        {
            try {
                this.timeSeriesPred4WordBuffer.write(csvAppender + statistic);

                csvAppender = ",";
            } catch (IOException e) {
                System.out.println("Error: writing statistics value to csv.\n" + e.toString());
                stepLog = "Error: writing statistics value to csv.\n" + e.toString();
                isCSVUpdateSuccessful = false;
            }
        }

        //now finish the row by adding a new line character
        try {
            this.timeSeriesPred4WordBuffer.write("\n");
        } catch (IOException e) {
            System.out.println("Error: writing new line character to csv.\n" + e.toString());
            stepLog = "Error: writing new line character to csv.\n" + e.toString();
            isCSVUpdateSuccessful = false;

        }
        return stepLog;
    }


    public String updateCSVbyArrayListOfArrayListLine( DateTime instDateTime, String evaluationPerformanceType, boolean toFlush){
        boolean isCSVUpdateSuccessful = true;
        String stepLog = "Success";
        String csvAppender = "";
        try{
            this.timeSeriesPred4WordBuffer.write(csvAppender + instDateTime.toString());
        }
        catch(IOException e){
            System.out.println("Error: writing statistics value to csv.\n" + e.toString());
            stepLog = "Error: writing statistics value to csv.\n" + e.toString();
            isCSVUpdateSuccessful = false;
        }
        csvAppender = ",";
        //write each statistic value at a new column of the current row of the csv
        for (ArrayList<Double> predictionsExp : this.predictionsPerExpert) {
            for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++) {
                try {
                    this.timeSeriesPred4WordBuffer.write(csvAppender + predictionsExp.get(classIndex));
//                    csvAppender = ",";
                } catch (IOException e) {
                    System.out.println("Error: writing statistics value to csv.\n" + e.toString());
                    stepLog = "Error: writing statistics value to csv.\n" + e.toString();
                    isCSVUpdateSuccessful = false;
                }
            }
        }

        //now finish the row by adding a new line character
        try {
            this.timeSeriesPred4WordBuffer.write("\n");
        } catch (IOException e) {
            System.out.println("Error: writing new line character to csv.\n" + e.toString());
            stepLog = "Error: writing new line character to csv.\n" + e.toString();
            isCSVUpdateSuccessful = false;

        }
        return stepLog;
    }
}
