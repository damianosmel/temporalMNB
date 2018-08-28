package de.l3s.oscar.Evaluation;


import de.l3s.oscar.UserCommandLine;
import moa.core.TimingUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * Class to evaluate a learning model for already preprocessed data
 *
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class EvaluateOffline extends Evaluation{

    /**
     * <h1>EvaluateOffline Constructor</h1>
     *
     *
     * @param parsedCommandLine         object carrying the parsed user input
     * @return evaluateOffline          object of the offline evaluation
     * @since Jan 2017
     * @author Damianos Melidis
     */
    public EvaluateOffline(UserCommandLine.ParsedCommandLine parsedCommandLine)
    {
        super(parsedCommandLine);
//        System.out.println("after initializing evaluate offline class");
    }

    public RunEvaluateOfflineResult runOfflineEvaluation()
    {

        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
        boolean runIsSuccessful = true;
        String runLog = "=== Running offline evaluation ===\n";

        ApplyEvaluationResult applyEvaluationResult = null;
        boolean isApplyEvaluationSuccessful = false;
        boolean allMultiValuePerformanceBuffersClosed = true;
        boolean isdebugLogClosed = true;
        int numberOfSlidingWindowPerformances = 3; //number of sliding window performance
        if (this.verbose){
            System.out.println(runLog);
        }

        //try to create output folder, if failed then exit

        File outputDirectory = super.createOutputDirectory(this.rootOutputDirectory);

        if (outputDirectory == null) // && !usingDeken
        {
            runIsSuccessful = false;
            runLog += "Error: Cannot create output directory.\n";
            if (this.verbose){
                System.out.println("Error: Cannot create output directory.");
            }
        }
        else
        {
            runLog += "Info: Output directory " + outputDirectory.getPath() + " was created successfully.\n";

            if (this.verbose){
                System.out.println("Info: Output directory " + outputDirectory.getPath() + " was created successfully.");
            }

            //create performance measures consisting of one only value
//            double positiveClassValue = 1.0;
            EvaluationOneValuePerformance evaluationOneValuePerformance = new EvaluationOneValuePerformance(0,0,0,0,0,0);
            //create performance measures consisting of multiple values during the course of the stream
            EvaluationMultiValuesPerformance evaluationMultiValuesPerformance = new EvaluationMultiValuesPerformance(outputDirectory, numberOfSlidingWindowPerformances);

            //examine if the buffers for multi values were successfully created
            if (evaluationMultiValuesPerformance.allBuffersCreated == false){
                runIsSuccessful = false;
                runLog += "Error: Cannot create csv files holding statistic values over the course of the stream\n";
            }

            //create debug log txt file
            BufferedWriter debugLogBuffer = super.createDebugBuffer(outputDirectory);
            if (debugLogBuffer == null){
                runIsSuccessful = false;
                runLog += "Error: Cannot create debug log txt file\n";

            }

            if (this.evaluationScheme.equalsIgnoreCase("prequential") && runIsSuccessful)
            {
                /**
                 * Prequential evaluation
                 * */
                ReadDataFromDBResult readDataFromDBResult = super.readDataFromDB(this.mode,this.savedDBTitle,this.verbose);
                runLog += readDataFromDBResult.getLog();

//                boolean debuggingPrequential = true;
                boolean debuggingPrequential = false;
                super.setDebuggingPrequential(debuggingPrequential);
//                currentEvaluationOneValuePerformance
                applyEvaluationResult = super.applyPrequentialEvaluation(this.learningAlgorithm, readDataFromDBResult.getTrainingStream(), evaluationOneValuePerformance, evaluationMultiValuesPerformance, debugLogBuffer, this.verbose, outputDirectory);
                runLog += applyEvaluationResult.getStepLog();

                isApplyEvaluationSuccessful = applyEvaluationResult.getSuccess();
            }
            else if (this.evaluationScheme.equalsIgnoreCase("holdout") && runIsSuccessful)
            {
                /**
                 * Hold out evaluation
                 * */
//                System.out.println("you are here!");
                if (this.learningAlgorithm.equalsIgnoreCase("mnb")) {
                    ReadDataFromRandomDBResult readDataFromRandomDBResult = super.readDataFromRandomDB(this.mode, this.savedDBTitle, this.verbose);
                    runLog += readDataFromRandomDBResult.getLog();
//                    applyEvaluationResult = super.applyHoldoutEvaluation(this.learningAlgorithm, readDataFromRandomDBResult.getTrainingStream(), evaluationStatisticsCSVBufferedWriter, evaluationDebugLogBufferedWriter, evaluationStatisticsSlidingWindowCSVBufferedWriter, this.verbose);
                    runLog += applyEvaluationResult.getStepLog();
                    isApplyEvaluationSuccessful = applyEvaluationResult.getSuccess();
                }
                else{
                    ReadDataFromDBResult readDataFromDBResult = super.readDataFromDB(this.mode, this.savedDBTitle, this.verbose);
                    runLog += readDataFromDBResult.getLog();
                    boolean debuggingHoldout = false;
                    super.setDebuggingHoldout(debuggingHoldout);
//                    applyEvaluationResult = super.applyHoldoutEvaluationWithSequentialDB(this.learningAlgorithm, readDataFromDBResult.getTrainingStream(), evaluationStatisticsCSVBufferedWritter, evaluationDebugLogBufferedWriter, evaluationStatisticsSlidingWindowCSVBufferedWritter, this.verbose);
                }

            }

            if(isApplyEvaluationSuccessful){
                /**
                 * Close all csv files containing performance statistics over the course of the stream
                 * */
                allMultiValuePerformanceBuffersClosed = evaluationMultiValuesPerformance.flushAndCloseAllBuffers();
                if (allMultiValuePerformanceBuffersClosed == false){
                    runLog += "Error: Cannot close csv files with values over the whole stream\n";
                }
                /**
                 * close the buffer for debug logs
                 * */
                try{
                    debugLogBuffer.close();
                }catch(IOException e){
                    isdebugLogClosed = false;
//                    runIsSuccessful = false;
                    runLog += "Error: Cannot close debug log file.\n" + e.toString() + "\n";
                    if (this.verbose){
                        System.out.println("Error: Cannot close debug log file.\n" + e.toString());
                    }
                }
                runIsSuccessful = allMultiValuePerformanceBuffersClosed & isdebugLogClosed;
            }
            else{
                runIsSuccessful = false;
            }

            //after evaluation print the csv file path
            if (runIsSuccessful)
            {

                /**
                 * create performance measures consisting of one value
                 * */
                EvaluationResult evaluationResult = applyEvaluationResult.getEvaluationResult();

                /**
                 * Get one value statistics for whole stream
                 * */
                String statisticsOverWholeStreamLog = evaluationOneValuePerformance.printEvaluationStatistics(this.evaluationScheme, this.verbose);
                String writingLog = super.saveStringLogToFile(outputDirectory,statisticsOverWholeStreamLog,"statisticsOverWholeStreamEvaluation.txt",verbose);
                runLog += writingLog;
            }//close if evaluation is successful

            runLog += "=== End of running offline evaluation ===\n";
            String writingLog = super.saveStringLogToFile(outputDirectory,runLog,"runLog.txt",verbose);
            runLog += writingLog;
        } //close if output directory can be created




        if (this.verbose)
        {
            System.out.println("=== End of running offline evaluation ===");
        }

        return new RunEvaluateOfflineResult(runLog, runIsSuccessful);
    }

}
