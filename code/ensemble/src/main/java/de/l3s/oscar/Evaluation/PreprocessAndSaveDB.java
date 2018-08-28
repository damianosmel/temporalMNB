package de.l3s.oscar.Evaluation;

import de.l3s.oscar.UserCommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class to preprocess and save data into DB
 *
 * @since Jan 2017
 * @author Damianos Melidis
 */
public class PreprocessAndSaveDB extends Evaluation {
    public PreprocessAndSaveDB(UserCommandLine.ParsedCommandLine parsedCommandLine)
    {
        super(parsedCommandLine);
    }

    public void run()
    {
        super.readData();
        super.saveData2DB();
    }

    public void collectAndWriteDataSetStats_run(){
        boolean runIsSuccessful = true;
        String runLog = "=== Running collect and write dataset statistics ===\n";

        File outputDirectory = super.createOutputDirectory(this.rootOutputDirectory);

        if (outputDirectory == null) // && !usingDeken
        {
            runIsSuccessful = false;
            runLog += "Error: Cannot create output directory.\n";
            if (this.verbose){
                System.out.println("Error: Cannot create output directory.");
            }
        }
        else {
            runLog += "Info: Output directory " + outputDirectory.getPath() + " was created successfully.\n";

            if (this.verbose) {
                System.out.println("Info: Output directory " + outputDirectory.getPath() + " was created successfully.");
            }
            ReadDataFromDBResult readDataFromDBResult = super.readDataFromDB(this.mode,this.savedDBTitle,this.verbose);
            runLog += readDataFromDBResult.getLog();
            ArrayList<String> timePeriodsForCollecting = new ArrayList<String>(Arrays.asList("day","week","month"));
//            ArrayList<String> timePeriodsForCollecting = new ArrayList<String>(Arrays.asList("week"));
            CollectAndWriteDataSetStats collectAndWriteDataSetStats = new CollectAndWriteDataSetStats(readDataFromDBResult.getTrainingStream(), timePeriodsForCollecting, outputDirectory);

            /**
             * create header for statistics csv files
             * */
            collectAndWriteDataSetStats.initializeAllStatisticsBuffers(outputDirectory, timePeriodsForCollecting);

            boolean isAllHeadersWritten = collectAndWriteDataSetStats.createAllHeadersStatisticsCSV(this.verbose);

            if (isAllHeadersWritten == false){
                runLog += "Error: updating csv line\n";
                runIsSuccessful = false;
            }

            /**
             * now get the statistics for each time period for the whole stream
             * */
            collectAndWriteDataSetStats.collectAndWriteStatistics(this.verbose);

            /**
             * finally, close the buffers
             * */
            boolean allStatsBuffersAreClosed = collectAndWriteDataSetStats.flushAndCloseAllBuffers();

            if (allStatsBuffersAreClosed == false){
                runLog += "Error: cannot flush and close buffers holding data set statistics for whole data set.\n";
                runIsSuccessful = false;
            }

            runLog += "=== End of running offline evaluation ===\n";
            String writingLog = super.saveStringLogToFile(outputDirectory,runLog,"runLog.txt",verbose);
            runLog += writingLog;

        }
    }
    //TODO: just do it! Save run statistics in the excel
    public void saveStatistics()
    {

    }

    //TODO: just do it! Save the log of run
    public void saveLog()
    {

    }

}
