package de.l3s.oscar.Evaluation;

import moa.core.Example;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Class to collect basic statistics for the data set
 * and write them to excel files
 *
 * Created by damian on 01.05.17.
 */
public class CollectAndWriteDataSetStats {

    /**
     * basic statistics to be collected
     * */
    private double [] positivesInPeriod;
    private double [] negativesInPeriod;

    /**
     * DateTime for each time period
     * */
    private ArrayList<DateTime> currentDateTimeOfPeriods;
    private ArrayList<DateTime> nextOfCurrentDateTimeOfPeriods;
    private DateTimeFormatter dateFormatter;
    /**
     * buffers for statistics
     * */
    private ArrayList<BufferedWriter> collectedStatsBuffers;
    boolean allBuffersCreated = true;
    boolean allBuffersClosed = true;

    /**
     * the dataset
     * */
    private BayesianStreamBig trainingStream;

    /**
     * number of periods for which we collect statistics
     * size of each period
     * */
    private int numberOfTimePeriods;
    private int [] timePeriodCounter;
    private ArrayList<String> namesOfTimePeriods;

    public CollectAndWriteDataSetStats(BayesianStreamBig trainingStream, ArrayList<String> timePeriodsForStats, File outputDirectory){
        //dataFromDB, ArrayList<String>, outputDirectory
        /**
         * set-up the class counts
         * */
        this.positivesInPeriod = new double[timePeriodsForStats.size()];
        this.negativesInPeriod = new double[timePeriodsForStats.size()];

        Arrays.fill(this.positivesInPeriod, 0.0D);
        Arrays.fill(this.negativesInPeriod, 0.0D);

        /**
         * initialize statistics buffer
         * */
        initializeAllStatisticsBuffers(outputDirectory, timePeriodsForStats);

        /**
         * set-up the data set
         * */
        this.trainingStream = trainingStream;

        /**
         * set-up the number of time periods
         * */
        this.numberOfTimePeriods = timePeriodsForStats.size();

        this.timePeriodCounter = new int[this.numberOfTimePeriods];
        Arrays.fill(timePeriodCounter,0);


        /**
         * initialize the date time for the time periods
         * */
        this.currentDateTimeOfPeriods = new ArrayList<DateTime>();

        for (int timePeriodIndex=0; timePeriodIndex< this.numberOfTimePeriods; timePeriodIndex++){
            this.currentDateTimeOfPeriods.add(null);
//            this.nextOfCurrentDateTimeOfPeriods.add(null);
        }

        this.namesOfTimePeriods = timePeriodsForStats;

        /**
         * initialize the formatter for the date of each row of statistics
         * */
        dateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy");
    }

//    public void initializeStatsBufferForOnePeriod

    public BufferedWriter initializeStatisticsBuffer(File outputDirectory, String csvFileName){

        String csvFile = outputDirectory.getPath() + File.separator + csvFileName; //evaluationStatisticsCSVFile;
        FileWriter csvFileWriter = null;
        BufferedWriter csvBufferedWriter = null;

        try {
            csvFileWriter = new FileWriter(csvFile);
            csvBufferedWriter = new BufferedWriter(csvFileWriter);
        } catch (IOException e) {
            System.out.println("Error: Cannot create csv file holding evaluation statistics to output directory\n" + e.toString());
            /*runLog += "Error: Cannot create csv file holding evaluation statistics to output directory\n" + e.toString();
            if (this.verbose)
            {
                System.out.println("Error: Cannot create csv file holding evaluation statistics to output directory\n" + e.toString());
            }*/
        }
        return csvBufferedWriter;
    }

    public void initializeAllStatisticsBuffers(File outputDirectory, ArrayList<String> timePeriodsForStats){
        String statsCSVFileName;
        Iterator<String> timePeriodForStats = timePeriodsForStats.iterator();
        BufferedWriter currentPeriodBuffer = null;

        this.collectedStatsBuffers = new ArrayList<BufferedWriter>();

        while(timePeriodForStats.hasNext()){
            statsCSVFileName = "classStats_" + timePeriodForStats.next() + ".csv";
            currentPeriodBuffer = initializeStatisticsBuffer(outputDirectory, statsCSVFileName);
            if(currentPeriodBuffer == null){
                this.allBuffersCreated = false;
            }
            else{
                this.collectedStatsBuffers.add(currentPeriodBuffer);
            }

        }
    }

    public boolean createAllHeadersStatisticsCSV(boolean verbose){
        ArrayList<String> statisticsHeader = new ArrayList<String>();
        statisticsHeader.add("DayMonth");
        statisticsHeader.add("numberOfPositives");
        statisticsHeader.add("numberOfNegatives");

        String csvUpdateLog;
        boolean isHeaderWrittenToBuffer = true;
        for (int timePeriodIndex = 0; timePeriodIndex< this.numberOfTimePeriods; timePeriodIndex++){
            csvUpdateLog = updateCSVbyArrayListLine(statisticsHeader, timePeriodIndex, false, verbose);
            isHeaderWrittenToBuffer = checkIfBufferIsWritten(csvUpdateLog);
            if (isHeaderWrittenToBuffer == false){
                break;
            }
        }
        return isHeaderWrittenToBuffer;
    }

    public void updateClassCountsForTimePeriod(com.yahoo.labs.samoa.instances.Instance currentSamoaInstance, int timePeriodIndex){
        if(currentSamoaInstance.classValue() == 1.0){
            this.positivesInPeriod[timePeriodIndex] += 1.0;
        }
        else if (currentSamoaInstance.classValue() == 2.0){
            this.negativesInPeriod[timePeriodIndex] += 1.0;
        }
    }

    public DateTime getOnePeriodBackTrainingInstance(DateTime currentInstanceDateTime, int timePeriodIndex){
        DateTime onePeriodBackTrainingInstance = null;

        if(this.namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("day") ){
            onePeriodBackTrainingInstance = currentInstanceDateTime.minusDays(1);
        }
        else if (this.namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("week")){
            onePeriodBackTrainingInstance = currentInstanceDateTime.minusWeeks(1);
//            onePeriodBackTrainingInstance = currentInstanceDateTime.minusDays(7);
        }
        else if (this.namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("month")){
            onePeriodBackTrainingInstance = currentInstanceDateTime.minusMonths(1);
        }
        return onePeriodBackTrainingInstance;
    }

    public DateTime getOnePeriodAfterTrainingInstance(DateTime currentInstanceDateTime, int timePeriodIndex){
        DateTime onePeriodAfterTrainingInstance = null;

        if (this.namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("day")){
            onePeriodAfterTrainingInstance = currentInstanceDateTime.plusDays(1);
        }
        else if (this.namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("week")){
            onePeriodAfterTrainingInstance = currentInstanceDateTime.plusWeeks(1);
//            onePeriodAfterTrainingInstance = currentInstanceDateTime.plusDays(7);
        }
        else if (this.namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("month")){
            onePeriodAfterTrainingInstance = currentInstanceDateTime.plusMonths(1);
        }
        return onePeriodAfterTrainingInstance;
    }

    public void cleanClassCountsForTimePeriod(int timePeriodIndex){
        this.positivesInPeriod[timePeriodIndex] = 0.0D;
        this.negativesInPeriod[timePeriodIndex] = 0.0D;
    }

    protected long calculateTimestampsDifference(DateTime firstTimestamp, DateTime secondTimestamp, int timePeriodIndex){

        String nameOfTimePeriod = this.namesOfTimePeriods.get(timePeriodIndex);
        Duration timestampDifferenceDuration = new Duration(firstTimestamp, secondTimestamp);

        if(nameOfTimePeriod.equalsIgnoreCase("day")){
            return timestampDifferenceDuration.getStandardHours();
        }
        else if (nameOfTimePeriod.equalsIgnoreCase("week")){
            return timestampDifferenceDuration.getStandardDays();
        }
        else if (nameOfTimePeriod.equalsIgnoreCase("month")){
            return timestampDifferenceDuration.getStandardDays();
        }
        else{
            //not implemented case of time period
            return -1;
        }
    }

    /*public boolean isDateBeforeCurrentInstanceDate(DateTime nextDate, DateTime currentInstanceDate, int timePeriodIndex){
        boolean isDateBeforeCurrentInstanceDate = false;
        long dateDifference = calculateTimestampsDifference(nextDate, currentInstanceDate, timePeriodIndex);

    }*/

    public boolean datesAreEqualForTimePeriod(DateTime nextDate, DateTime currentDate, int timePeriodIndex){
        boolean areDatesEqual = false;
        String nameOfTimePeriod = this.namesOfTimePeriods.get(timePeriodIndex);
        int nextDateDayOfMonth = nextDate.getDayOfMonth();
        int nextDateDayOfWeek = nextDate.getDayOfWeek();
        int nextDateMonthOfYear = nextDate.getMonthOfYear();

        int currentDateDayOfMonth = currentDate.getDayOfMonth();
        int currentDateDayOfWeek = currentDate.getDayOfWeek();
        int currentDateMonthOfYear = currentDate.getMonthOfYear();

        if(nameOfTimePeriod.equalsIgnoreCase("day")){
            if (nextDateDayOfMonth == currentDateDayOfMonth){
                System.out.println(">> days are equal!");
                areDatesEqual = true;
            }
        }
        else if(nameOfTimePeriod.equalsIgnoreCase("week")){
            if (nextDateDayOfWeek == currentDateDayOfWeek){
//              if(nextDateDayOfMonth == currentDateDayOfMonth){
                System.out.println(">> weeks are equal!");
                areDatesEqual = true;
            }
        }
        else if(nameOfTimePeriod.equalsIgnoreCase("month")) {
            if (nextDateMonthOfYear == currentDateMonthOfYear){
                System.out.println(">> months are equal!");
                areDatesEqual = true;
            }
        }

        return areDatesEqual;
    }

    public boolean isGapBetweenDatesForTimePeriod(DateTime onePeriodAfterCurrentTimeStamp, DateTime currentInstanceDateTime, int timePeriodIndex, boolean inclusiveUpperLimit){
        boolean isGapBetweenDatesInTimePeriod = false;
        long timeDifference = calculateTimestampsDifference(onePeriodAfterCurrentTimeStamp, currentInstanceDateTime,timePeriodIndex);

        long thresholdForTimeGap = -1L;

        if(namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("day")){
            thresholdForTimeGap = 24;
        }else if(namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("week")){
            thresholdForTimeGap = 1;
        }else if(namesOfTimePeriods.get(timePeriodIndex).equalsIgnoreCase("month")){
            thresholdForTimeGap = 1;
        }

        if(inclusiveUpperLimit == true) {
            if (timeDifference >= thresholdForTimeGap) {
//                System.out.println("yes I am here inclusive");
                isGapBetweenDatesInTimePeriod = true;
            }
        }
        else{
            if (timeDifference > thresholdForTimeGap){
//                System.out.println("yes I am here ");
                isGapBetweenDatesInTimePeriod = true;
            }
        }
        /*if(currentInstanceDateTime.isAfter(onePeriodAfterCurrentTimeStamp)){
            isGapBetweenDatesInTimePeriod = true;
        }*/
        return isGapBetweenDatesInTimePeriod;
    }

    public void updateStatisticsBufferForTimePeriod(com.yahoo.labs.samoa.instances.Instance currentSamoaInstance, DateTime currentInstanceDateTime, int timePeriodIndex, boolean verbose){

        DateTime onePeriodBeforeOfTrainingInstance = getOnePeriodBackTrainingInstance(currentInstanceDateTime, timePeriodIndex);
        DateTime onePeriodAfterOfCurrentTimeStamp = null;
        /**
         * if we process the first instance
         * */
        if(this.currentDateTimeOfPeriods.get(timePeriodIndex) == null){
            this.currentDateTimeOfPeriods.set(timePeriodIndex, currentInstanceDateTime);
//            System.out.println("initializing the time stamp for period " + Integer.toString(timePeriodIndex));
            updateClassCountsForTimePeriod(currentSamoaInstance, timePeriodIndex);
        }
        /**
         * if we still process instances inside the batch
         * */
        else if(onePeriodBeforeOfTrainingInstance.isBefore(this.currentDateTimeOfPeriods.get(timePeriodIndex))){
//            System.out.println("still inside the same time period " + Integer.toString(timePeriodIndex));
            updateClassCountsForTimePeriod(currentSamoaInstance, timePeriodIndex);
        }
        /**
         * if the current instance is after the current batch
         * */
        else if(onePeriodBeforeOfTrainingInstance.isAfter(this.currentDateTimeOfPeriods.get(timePeriodIndex))){
//            System.out.println("in new time period");
            //write up the class counts for the just-passed batch (time period)
            ArrayList<String> allBatchClassCountsRow = createRowClassCountsCSV(timePeriodIndex);
            updateCSVbyArrayListLine(allBatchClassCountsRow, timePeriodIndex,false, verbose);
            /**
             * check if there is gap between the previous date and the current date
             * */
            onePeriodAfterOfCurrentTimeStamp = getOnePeriodAfterTrainingInstance(this.currentDateTimeOfPeriods.get(timePeriodIndex), timePeriodIndex);
            boolean inclusiveUpperLimit = false;
//            if(currentInstanceDateTime.isEqual(onePeriodAfterOfCurrentDateTimeOfPeriod) == false){ //if gap in time period, then update for all dates in this gap zeros
            if( isGapBetweenDatesForTimePeriod(onePeriodAfterOfCurrentTimeStamp, currentInstanceDateTime, timePeriodIndex, inclusiveUpperLimit)){
                System.out.println("===");
                System.out.println("timePeriod = " + timePeriodIndex);
                System.out.println("onePeriodAfterTimeStamp= " + onePeriodAfterOfCurrentTimeStamp.toString());
                System.out.println("current instance time stamp= " + currentInstanceDateTime.toString());
                System.out.println("we have a gap..");
                System.out.println("===");
                //write up the class counts for all the time units inside the gap |- previous date of time period --- current date of time period -|
                createAndUpdateRowClassCountsForAllDatesInGap(currentInstanceDateTime, timePeriodIndex, verbose);
            }
            /**
             * now clean class counts,
             * then update the currentDate of time period and its respective counts
             * finally set the new current date of this time period
             * */
            cleanClassCountsForTimePeriod(timePeriodIndex);
            updateClassCountsForTimePeriod(currentSamoaInstance, timePeriodIndex);
            this.currentDateTimeOfPeriods.set(timePeriodIndex, currentInstanceDateTime);
        }

    }

    public void updateStatisticsBufferForTimePeriodForLastInstance(int timePeriodIndex, boolean verbose){
        ArrayList<String> allBatchPerformanceRow = createRowClassCountsCSV(timePeriodIndex);
        updateCSVbyArrayListLine(allBatchPerformanceRow, timePeriodIndex,false, verbose);
    }

    public void collectAndWriteStatistics(boolean verbose){
        Example currentInstanceExample;
        com.yahoo.labs.samoa.instances.Instance currentSamoaInstance;
        DateTime currentInstanceDateTime;
        long numberOfProcessedInstances = 0;
        while (trainingStream.hasMoreInstances()) {
//            currentInstanceExample = trainingStream.nextInstance();
//            System.out.println("Please wait, getting  basic statistics for instance " + Long.toString(numberOfProcessedInstances) + "..");

            currentSamoaInstance = trainingStream.nextSamoaInstance();
            currentInstanceDateTime = new DateTime((long) currentSamoaInstance.weight() * 1000);
//            System.out.println("current instance date: " + currentInstanceDateTime.toString());

            /**
             * for each give time period update the class counts
             * as the aggregated sum of instances inside that time period
             * */
            for(int timePeriodIndex = 0; timePeriodIndex < this.numberOfTimePeriods; timePeriodIndex++){

//                updateStatisticsBufferForTimePeriod(currentInstanceExample, currentInstanceDateTime, timePeriodIndex, verbose);
                updateStatisticsBufferForTimePeriod(currentSamoaInstance, currentInstanceDateTime, timePeriodIndex, verbose);
            }
            numberOfProcessedInstances++;
        }//close while for all instances in stream

        /**
         * write the statistics for the last instance
         * */
        for (int timePeriodIndex = 0; timePeriodIndex < this.numberOfTimePeriods; timePeriodIndex++){
            updateStatisticsBufferForTimePeriodForLastInstance(timePeriodIndex, verbose);
        }
    }

    /**
     *
     * Gap in time period
     *     this.currentDateTimeOfPeriods.get(timePeriodIndex)
     *                   |
     * |instance_1 .. instance_i|               ..no instances..                currentInstanceDateTime
     * |-   timePeriod_length  -|                       |                               |
     *                                     gap of instances in time period          the new this.currentInstanceDateTimePeriods.get(timePeriodIndex)
     *
     * So, for each empty time period add zero's statistics to its row
     * */
    public void createAndUpdateRowClassCountsForAllDatesInGap(DateTime currentInstanceDateTime, int timePeriodIndex, boolean verbose){
        /*System.out.println("===");
        System.out.println("filling the gap for current instance: " + currentInstanceDateTime.toString());*/
        DateTime previousCurrentInstanceDateTime = this.currentDateTimeOfPeriods.get(timePeriodIndex);
        DateTime nextDateTimeInGap = getOnePeriodAfterTrainingInstance(previousCurrentInstanceDateTime, timePeriodIndex);
        ArrayList<String> rowClassCountsZerosTimeGap;
        boolean inclusiveUpperLimit = true;
        System.out.println("---");
        System.out.println("inside the gap of period " + timePeriodIndex);
        System.out.println("the currentInstanceDate to reach is " + currentInstanceDateTime.toString());
        while(nextDateTimeInGap.isBefore(currentInstanceDateTime) && datesAreEqualForTimePeriod(nextDateTimeInGap, currentInstanceDateTime, timePeriodIndex) == false){
            System.out.println("nextDate in gap is " + nextDateTimeInGap.toString());
            rowClassCountsZerosTimeGap = createRowClassCountsCSVForNextDateInGap(nextDateTimeInGap, timePeriodIndex);
            updateCSVbyArrayListLine(rowClassCountsZerosTimeGap, timePeriodIndex, false, verbose);
            previousCurrentInstanceDateTime = nextDateTimeInGap;
            nextDateTimeInGap = getOnePeriodAfterTrainingInstance(previousCurrentInstanceDateTime, timePeriodIndex);
        }
        System.out.println("---");
    }


    public ArrayList<String> createRowClassCountsCSVForNextDateInGap(DateTime nextDateInGap, int timePeriodIndex){
        ArrayList<String> allBatchRowClassCounts = new ArrayList<String>();

        //positive and negatives are equal to 0.0 (as we are in a gap)
//        allBatchRowClassCounts.add( Integer.toString(nextDateInGap.getDayOfMonth()) + "." + Integer.toString(nextDateInGap.getMonthOfYear()) );
        allBatchRowClassCounts.add(dateFormatter.print(nextDateInGap));
        allBatchRowClassCounts.add("0.0");
        allBatchRowClassCounts.add("0.0");

        return allBatchRowClassCounts;
    }


    public ArrayList<String> createRowClassCountsCSV(int timePeriodIndex){
        ArrayList<String> allBatchRowPerformance = new ArrayList<String>();
//        allBatchRowPerformance.add(Integer.toString(this.currentDateTimeOfPeriods.get(timePeriodIndex).getDayOfMonth()) + "." +  Integer.toString(this.currentDateTimeOfPeriods.get(timePeriodIndex).getMonthOfYear()));
        allBatchRowPerformance.add(dateFormatter.print(currentDateTimeOfPeriods.get(timePeriodIndex)));
        allBatchRowPerformance.add(Double.toString(this.positivesInPeriod[timePeriodIndex]));
        allBatchRowPerformance.add(Double.toString(this.negativesInPeriod[timePeriodIndex]));

        return allBatchRowPerformance;
    }



    /**
     *
     * Update buffer writer connected to the csv with values for basic statistics of sliding windows for current instance
     *
     * @param currentWindowStatisticsRow            ArrayList<String>, basic statistics of windows for current instance
     * @param timePeriodIndex                       int, the index of the time period for which we collect statistics
     * @param toFlush                               boolean, flag to show if the written string to the buffer will be flushed to the file (true) or not (false)
     * @param verbose                               boolean, flag to have (true) verbose out of step log or not (false)
     * @return stepLog                              String, step log of updating buffer with the statistic values of current evaluation window
     */
    public String updateCSVbyArrayListLine(ArrayList<String> currentWindowStatisticsRow, int timePeriodIndex, boolean toFlush, boolean verbose)
    {

        boolean isCSVUpdateSuccessful = true;
        String stepLog = "Success";
        String csvAppender = "";

        //write each statistic value at a new column of the current row of the csv
        for (String statistic : currentWindowStatisticsRow)
        {
            try {
                this.collectedStatsBuffers.get(timePeriodIndex).write(csvAppender + statistic);
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
            this.collectedStatsBuffers.get(timePeriodIndex).write("\n");

            if(toFlush){
                this.collectedStatsBuffers.get(timePeriodIndex).flush();
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

    public boolean flushAndCloseBuffer(BufferedWriter statsTimePeriodBuffer){
        boolean isBufferClosed = true;
        try {
            statsTimePeriodBuffer.flush();
            statsTimePeriodBuffer.close();
        } catch (IOException e) {
            isBufferClosed = false;
            System.out.println("Error: Cannot close csv file with statistics.\n" + e.toString());
        }
        return isBufferClosed;
    }

    public boolean flushAndCloseAllBuffers(){
        boolean  areAllBuffersClosed = true;
        boolean isCurrentBufferClosed = true;
        for (int timePeriodIndex=0; timePeriodIndex< this.numberOfTimePeriods; timePeriodIndex++){
            isCurrentBufferClosed = flushAndCloseBuffer(this.collectedStatsBuffers.get(timePeriodIndex));
            areAllBuffersClosed = areAllBuffersClosed & isCurrentBufferClosed;
        }
        this.allBuffersClosed = areAllBuffersClosed;
        return areAllBuffersClosed;
    }

    public boolean checkIfBufferIsWritten(String textLog) {
        boolean bufferIsWritten = true;
        if (!textLog.equalsIgnoreCase("Success")) {
            bufferIsWritten = false;
        }

        return bufferIsWritten;
    }

}
