package de.l3s.oscar.Evaluation;


import com.yahoo.labs.samoa.instances.Instance;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.*;

/**
 * Created by damian on 13.02.17.
 */
public class SlidingWindowOfInstances {

    //protected com.yahoo.labs.samoa.instances.Instances totalDataset;
//    protected Instances dataSetInstances;
    //protected ArrayList<Instance> totalDataset = new ArrayList<Instance>();
    protected TreeMap<DateTime,ArrayList<SamoaInstanceWithTrainingIndex>> totalDataset = new TreeMap<DateTime, ArrayList<SamoaInstanceWithTrainingIndex>>(); //Instance
    /**protected Map<DateTime,Instance> totalDatasetMap = Collections.synchronizedMap(totalDatasetTreeMap);
    protected Map<DateTime,Instance> totalDataset = Collections.synchronizedMap(new TreeMap<DateTime,Instance>());*/
    protected ArrayList<Double> slidingWindowTimePeriods = new ArrayList<Double>(); //[12,1,1,1]
    protected ArrayList<String> slidingWindowTimePeriodsDescription = new ArrayList<String>(); //["hours","days","weeks","months"]
    protected ArrayList<Boolean> isWindowSlided = new ArrayList<Boolean>();
    //[window_1[firstInstanceIndex, lastInstanceIndex], ..,window_i[firstInstanceIndex, lastInstanceIndex], .. ]
/**    protected ArrayList<ArrayList<Integer>> currentSlidingWindowFirstLastInstanceIndices = new ArrayList<ArrayList<Integer>>();*/
    protected ArrayList<ArrayList<DateTime>> currentSlidingWindowFirstLastInstanceDateTime = new ArrayList<ArrayList<DateTime>>();
    //remember the first and last elements of previous slided window
    /** protected ArrayList<ArrayList<Integer>> previousSlidingWindowFirstLastInstanceIndices = new ArrayList<ArrayList<Integer>>();*/
    protected ArrayList<ArrayList<DateTime>> previousSlidingWindowFirstLastInstanceDateTime = new ArrayList<ArrayList<DateTime>>();
//    protected ArrayList<Long> slidingWindowFirstInstanceIndex = new ArrayList<Long>();
//    protected ArrayList<Long> slidingWindowLastInstanceIndex = new ArrayList<Long>();
    protected ArrayList<Long> currentSlidingWindowNumberOfInstances = new ArrayList<Long>();
    protected ArrayList<Long> previousSlidingWindowNumberOfInstances = new ArrayList<Long>();
    protected ArrayList<Double> currentSlidingWindowFirstInstanceTimestamps = new ArrayList<Double>();
    protected int numberOfSlidingWindows = 0;
    double timestampEpsilon = 10e-6;

    //debugging
    protected static boolean debuggingSlidingWindow = false;
    protected String debugLog = "=== SlidingWindow ===" + "\n";


    //Instance veryFirstInstance,
    public SlidingWindowOfInstances(int numberOfSlidingWindows, ArrayList<Double> slidingWindowTimePeriods, ArrayList<String> slidingWindowTimePeriodsDescription, SamoaInstanceWithTrainingIndex veryFirstInstance){ //Instance veryFirstInstance
//        this.dataSetInstances = new Instances(getCLICreationString(veryFirstInstance.getTrainingInstance().getClass()));

        ArrayList<Integer> currentWindowFirstLastInstanceIndices = new ArrayList<Integer>(Arrays.asList(0,0));//
        ArrayList<Integer> previousWindowFirstLastInstanceIndices = new ArrayList<Integer>(Arrays.asList(-1,-1)); //

        /** With Instances
        this.totalDataset = new Instances(); */
//        totalDataset.add(veryFirstInstance);

//        DateTime currentInstanceDateTime = new DateTime( (long) veryFirstInstance.getTrainingInstance().weight() * 1000);
        DateTime currentInstanceDateTime = new DateTime( (long) veryFirstInstance.getDateOfInstance() * 1000);
        //TODO:
//        DateTime currentInstanceDateTime = new DateTime((long) veryFirstInstance.getTrainingInstance().attribute(veryFirstInstance.getTrainingInstance().numAttributes()-1).);
        DateTime zeroDateTime = new DateTime(0);
        ArrayList<SamoaInstanceWithTrainingIndex> samoaInstancesCurrentTime = new ArrayList<SamoaInstanceWithTrainingIndex>();
        samoaInstancesCurrentTime.add(veryFirstInstance);
        //old but good
        //        this.totalDataset.put(currentInstanceDateTime, veryFirstInstance);
        this.totalDataset.put(currentInstanceDateTime, samoaInstancesCurrentTime);

//        this.dataSetInstances.add(veryFirstInstance.getTrainingInstance());

        //initialize variables
        this.numberOfSlidingWindows = numberOfSlidingWindows;
        for (int i=0; i < numberOfSlidingWindows; i++){

            this.currentSlidingWindowFirstLastInstanceDateTime.add(new ArrayList<DateTime>());
            this.currentSlidingWindowFirstLastInstanceDateTime.get(i).add(currentInstanceDateTime);
            this.currentSlidingWindowFirstLastInstanceDateTime.get(i).add(currentInstanceDateTime);

            this.previousSlidingWindowFirstLastInstanceDateTime.add(new ArrayList<DateTime>());
            this.previousSlidingWindowFirstLastInstanceDateTime.get(i).add(zeroDateTime);
            this.previousSlidingWindowFirstLastInstanceDateTime.get(i).add(zeroDateTime);

            //this.currentSlidingWindowNumberOfInstances.add(0);
            this.currentSlidingWindowNumberOfInstances.add(1L);
            this.previousSlidingWindowNumberOfInstances.add(0L);
            this.slidingWindowTimePeriods.add(slidingWindowTimePeriods.get(i));
            this.slidingWindowTimePeriodsDescription.add(slidingWindowTimePeriodsDescription.get(i));
//            this.currentSlidingWindowFirstInstanceTimestamps.add(1.0);
//            this.currentSlidingWindowFirstInstanceTimestamps.add(veryFirstInstance.getTrainingInstance().weight()); //veryFirstInstance.weight()
            this.currentSlidingWindowFirstInstanceTimestamps.add(veryFirstInstance.getDateOfInstance());
            this.isWindowSlided.add(false);

            if(this.debuggingSlidingWindow){
//                printSlidingWindowInstances(i);
            }
        }
    }



    public long removeInstancesFromSlidingWindows(int slidingWindowIndex){
        DateTime firstInstance2RemoveDateTime = getPreviousSlidingWindowFirstLastInstanceDateTimes(slidingWindowIndex).get(0);
        DateTime firstInstance2KeepDateTime = getCurrentSlidingWindowFirstLastInstanceDateTimes(slidingWindowIndex).get(0);
        long numberOfRemovedInstancesFromWindow = 0;

        /*System.out.println("--- ---");
        System.out.println("the size of total data set is " + Integer.toString(this.totalDataset.size()));
        System.out.println("the key set is " + this.totalDataset.keySet());*/
        if(this.debuggingSlidingWindow){
            this.debugLog +=   "~~~ Start of removing instances from all sliding windows ~~~" + "\n";

            System.out.println("=== ===");
            for(int i=0; i<getNumberOfSlidingWindows(); i++) {
                System.out.println("first last of current window: ");
                /**System.out.println(this.getCurrentSlidingWindowFirstLastInstanceIndices(i).toString());*/
                System.out.println(this.getCurrentSlidingWindowFirstLastInstanceDateTimes(i).toString());
            }
            /**System.out.println("We are going to make null instances from " + Integer.toString(firstIndexOfInstance2Remove) + " to " + firstLastIndexOfInstances2Remove.toString());*/
            System.out.println("We are going to make null instances from " + firstInstance2RemoveDateTime.toString() + " to " + firstInstance2KeepDateTime.toString());
            System.out.println("=== ===");
        }

/*
        long firstInstance2KeepTrainingIndex = this.totalDataset.get(firstInstance2KeepDateTime).getTrainingIndex();
        long firstInstance2DeleteTrainingIndex = this.totalDataset.get(firstInstance2RemoveDateTime).getTrainingIndex();
*/
/*
        //old but working
        numberOfRemovedInstancesFromWindow = this.totalDataset.get(firstInstance2KeepDateTime).getTrainingIndex() - this.totalDataset.get(firstInstance2RemoveDateTime).getTrainingIndex();
*/
        numberOfRemovedInstancesFromWindow = this.totalDataset.get(firstInstance2KeepDateTime).get(this.totalDataset.get(firstInstance2KeepDateTime).size() - 1).getTrainingIndex() -  this.totalDataset.get(firstInstance2RemoveDateTime).get(this.totalDataset.get(firstInstance2RemoveDateTime).size() -1).getTrainingIndex();
        /**
         * remove instances from the TreeMap
         * */
        this.totalDataset.headMap(firstInstance2KeepDateTime).clear();

        /**
         * free-up the space for deleted instances
         * */
/*
        for(long deletedInstancesIndex = firstInstance2DeleteTrainingIndex; deletedInstancesIndex < firstInstance2KeepTrainingIndex; deletedInstancesIndex++){
            this.dataSetInstances.delete((int) deletedInstancesIndex);
        }
*/

        if (this.debuggingSlidingWindow){
            this.debugLog += "~~~ End of removing instances from all sliding windows ~~~" + "\n";
        }

        /*System.out.println("after removing instances the tree's size is: " + Integer.toString(this.totalDataset.size()));
        System.out.println("the key set is " + this.totalDataset.keySet());
        System.out.println("--- ---");*/
        return numberOfRemovedInstancesFromWindow;
    }

    public boolean isLongestWindowSlided(int indexOfLongestWindow){
        if (this.debuggingSlidingWindow) {
            assert indexOfLongestWindow <= this.isWindowSlided.size() - 1 : "Error: Index of the longest slided window: index of the longest slided window > number of windows.";
            this.debugLog += "isLongestWindowSlided: Passing assert, indexOfLongestWindow <= this.isWindowSlided.size()  - OK" + "\n";
        }
        return this.isWindowSlided.get(indexOfLongestWindow);
    }


    public int findLongestTimePeriodOfWindows(){

        double maxSliceOfTimePeriodOfWindow = -1.0;
        String maxTimePeriodOfWindow = "";
        int maxTimePeriodOfWindowIndex = 0;

        DateTime currentWindowTimePeriod = new DateTime().withTime(0,0,0,0);
        long currentWindowTimePeriodInMillis = 0; //= new DateTime().withTime(0, 0, 0, 0);
        long maxTimePeriodWindowInMillis=0; //= new DateTime().withTime(0, 0, 0, 0); //http://stackoverflow.com/questions/29380681/how-to-set-time-property-in-java-using-joda-time
        int indexOfMaxWindow = 0;

        for (int i=0; i<this.slidingWindowTimePeriodsDescription.size(); i++){
            if(this.slidingWindowTimePeriodsDescription.get(i).equals("seconds")){
                currentWindowTimePeriod = currentWindowTimePeriod.plusSeconds(slidingWindowTimePeriods.get(i).intValue());
            }
            else if(this.slidingWindowTimePeriodsDescription.get(i).equals("hours")){
                currentWindowTimePeriod = currentWindowTimePeriod.plusHours(slidingWindowTimePeriods.get(i).intValue());
            }
            else if( this.slidingWindowTimePeriodsDescription.get(i).equals("days")){
                currentWindowTimePeriod = currentWindowTimePeriod.plusDays(slidingWindowTimePeriods.get(i).intValue());
            }
            else if(this.slidingWindowTimePeriodsDescription.get(i).equals("weeks")){
                currentWindowTimePeriod = currentWindowTimePeriod.plusWeeks(slidingWindowTimePeriods.get(i).intValue());
            }
            else if(this.slidingWindowTimePeriodsDescription.get(i).equals("months")){
                currentWindowTimePeriod = currentWindowTimePeriod.plusMonths(slidingWindowTimePeriods.get(i).intValue());
            }
//            System.out.println("currentWindowTimePeriod: " + currentWindowTimePeriod.toString());
            currentWindowTimePeriodInMillis = currentWindowTimePeriod.getMillis();
            if (currentWindowTimePeriodInMillis >= maxTimePeriodWindowInMillis){
                maxTimePeriodWindowInMillis = currentWindowTimePeriodInMillis;
                indexOfMaxWindow = i;
            }
            currentWindowTimePeriod = new DateTime().withTime(0,0,0,0);
        }
        return indexOfMaxWindow;
    }

    public String getThenCleanDebugLog(){
        String currentDebugLog = this.debugLog + "=== Sliding Window ===" + "\n";
        this.debugLog = "=== Sliding Window ===" + "\n";

        return currentDebugLog;
    }

    public static void setDebuggingSlidingWindow(boolean userDebuggingSlidingWindow){
        debuggingSlidingWindow = userDebuggingSlidingWindow;
    }

    public ArrayList<Instance> getSamoaInstancesInTimePoint(DateTime instanceDateTime){
        ArrayList<Instance> samoaInstancesInTimePoint = new ArrayList<Instance>();
        Iterator<SamoaInstanceWithTrainingIndex> instancesIteratorCurrentTime = this.totalDataset.get(instanceDateTime).iterator();

        while(instancesIteratorCurrentTime.hasNext()){
            samoaInstancesInTimePoint.add(instancesIteratorCurrentTime.next().getTrainingInstance());
        }

        return samoaInstancesInTimePoint;
    }

/*
    //old but working
    public Instance getSamoaInstanceFromSlidingWindows(DateTime instanceDateTime){
        return this.totalDataset.get(instanceDateTime).getTrainingInstance();
    }
*/


    /**
     * Return all keys higher or equal to firstInstanceDateTime
     * */
    public NavigableSet<DateTime> getKeysHigherOrEqualThanInstance(DateTime firstInstanceOfPreviousWindowDateTime){
        NavigableSet keysHigherThanInstance = (NavigableSet) this.totalDataset.navigableKeySet().tailSet(firstInstanceOfPreviousWindowDateTime, true);
        return keysHigherThanInstance;
    }

    /**
     * Return all keys less than or to lastInstanceDateTime
     * */
    public NavigableSet<DateTime> getKeysUpToLastInstance(DateTime lastInstanceDateTime){
        NavigableSet keysUpToLastInstance = (NavigableSet) this.totalDataset.navigableKeySet().headSet(lastInstanceDateTime, true); // some random position
        return keysUpToLastInstance;
    }

/*
    //old but working
    *//*public Instance getInstanceOfSlidingWindow(int instanceIndex){ //, int slidingWindowIndex*//*
    public SortedMap<DateTime,SamoaInstanceWithTrainingIndex> getInstancesOfSlidingWindow(DateTime firstInstanceDateTime, DateTime lastInstanceDateTime){ //Instance

        *//** With Instances
        return this.totalDataset.instance(instanceIndex); *//*
        *//*if(debuggingSlidingWindow){
            this.debugLog += "Getting instance with index " + Integer.toString(instanceIndex)+ "\n";
            return this.totalDataset.get(instanceIndex);
        }*//*


        if (debuggingSlidingWindow){
            this.debugLog += "Getting instances from " + firstInstanceDateTime.toString() + " (inclusive) to " + lastInstanceDateTime.toString() + " (inclusive) \n";
        }
        return totalDataset.subMap(firstInstanceDateTime,true, lastInstanceDateTime, true);

    }*/

    public SortedMap<DateTime, ArrayList<SamoaInstanceWithTrainingIndex>> getInstanceOfSlidingWindow(DateTime firstInstanceDateTime, DateTime lastInstanceDateTime){
        if(debuggingSlidingWindow){
            this.debugLog += "Getting instances from " + firstInstanceDateTime.toString() + " (inclusive) to " + lastInstanceDateTime.toString() + " (inclusive) \n";
        }

        return this.totalDataset.subMap(firstInstanceDateTime, true, lastInstanceDateTime, true);
    }

    public boolean getIsWindowSlided(int slidingWindowIndex){
        return this.isWindowSlided.get(slidingWindowIndex);
    }

    /*

    public void printSlidingWindowInstances(int slidingWindowIndex){
        */
/**int slidingWindowFirstInstanceIndex = this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(0);
        int slidingWindowLastInstanceIndex = this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(1);*//*

        DateTime slidingWindowFirstInstanceDateTime = this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(0);
        DateTime slidingWindowLastInstanceDateTime = this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1);
        System.out.println("first instance of sliding window: " + slidingWindowFirstInstanceDateTime.toString());
        System.out.println("second instance of sliding window: " + slidingWindowLastInstanceDateTime.toString());
        SortedMap<DateTime,SamoaInstanceWithTrainingIndex> allInstancesSlidingWindow = getInstancesOfSlidingWindow(slidingWindowFirstInstanceDateTime, slidingWindowLastInstanceDateTime); //Instance
        Set<DateTime> allinstancesSlidingWindowDateTime = allInstancesSlidingWindow.keySet();
        int slidingWindowInstanceCounter = 0;
        long slidingWindowNumberOfInstances = getSlidingWindowNumberOfInstances(slidingWindowIndex);
//        int numberOfPrintedInstances = 0;
        int numberOfProcessedInstances = 0;

        //calculate how many instances will be skipped from printing
        int skippingNumberOfInstances = 1;
        if (slidingWindowNumberOfInstances >= 10){
            skippingNumberOfInstances = 10;
        }
        else if (slidingWindowNumberOfInstances >= 100){
            skippingNumberOfInstances = 100;
        }
        else if (slidingWindowNumberOfInstances >= 1000){
            skippingNumberOfInstances = 1000;
        }

        if(debuggingSlidingWindow) {
            this.debugLog += "~~~ Start of window of " + this.slidingWindowTimePeriodsDescription.get(slidingWindowIndex) + " ~~~" + "\n";
            this.debugLog += "with length= " + Long.toString(slidingWindowNumberOfInstances) + "\n";
            System.out.println("~~~ Start of window of " + this.slidingWindowTimePeriodsDescription + " ~~~");
            System.out.println("with length= " + Long.toString(slidingWindowNumberOfInstances));
        }

        System.out.println("== number of instances: " + Integer.toString(allinstancesSlidingWindowDateTime.size()));

        for(DateTime instanceToPrintDateTime : allinstancesSlidingWindowDateTime){
            System.out.println(instanceToPrintDateTime.toString());
            if (numberOfProcessedInstances % skippingNumberOfInstances == 0) {
                SamoaInstanceWithTrainingIndex currentInstance = allInstancesSlidingWindow.get(instanceToPrintDateTime); //Instance
                if (debuggingSlidingWindow) {
                    */
/**System.out.println("first last of current window: ");
                    System.out.println(this.getCurrentSlidingWindowFirstLastInstanceDateTimes(slidingWindowIndex).toString());*//*

                    System.out.println("current instance: " + currentInstance.getTrainingInstance().toString());
                    System.out.println("with time stamp: " + instanceToPrintDateTime.toString());
                    this.debugLog += "current instance: " + currentInstance.getTrainingInstance().toString() + "\n";
                    this.debugLog += "with time stamp: " + instanceToPrintDateTime.toString() + "\n";
                }
            }
            else{
                System.out.println("skip one print");
                System.out.println("number of processed instances: " + Integer.toString(numberOfProcessedInstances));
            }
            numberOfProcessedInstances += 1;
        }

        if(debuggingSlidingWindow){
            this.debugLog += "~~~ End of window of " + this.slidingWindowTimePeriodsDescription.get(slidingWindowIndex) + " ~~~" + "\n";
            System.out.println("~~~ End of window of " + this.slidingWindowTimePeriodsDescription + " ~~~");
        }
    }

*/

    public int getNumberOfSlidingWindows(){
        return this.numberOfSlidingWindows;
    }

    //TODO: Write it up! For the case of adaptive creation of sliding windows
    public void updateSlidingWindowCounter(int slidingWindowCounter){
//        is not ENOUGH you have to update all the arraylists
//        this.slidingWindowCounter = slidingWindowCounter
    }

    public ArrayList<DateTime> getPreviousSlidingWindowFirstLastInstanceDateTimes(int slidingWindowIndex){
        DateTime firstInstanceDateTime = this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(0);
        DateTime lastInstanceDateTime = this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1);

        if (this.debuggingSlidingWindow){
            assert lastInstanceDateTime.isAfter(firstInstanceDateTime) : "Error: For previous sliding window: last instance time stamp <= first instance time stamp.";
            this.debugLog += "getPreviousSlidingWindowFirstLastInstanceDateTimes: Passing assert, last instance time stamp > first instance time stamp - OK" + "\n";
        }

        return this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex);
    }

/**

    public ArrayList<Integer> getPreviousSlidingWindowFirstLastInstanceIndices(int slidingWindowIndex){
        int firstInstanceIndex = this.previousSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(0);
        int lastInstanceIndex = this.previousSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(1);

        if (this.debuggingSlidingWindow) {
            assert lastInstanceIndex > firstInstanceIndex : "Error: For previous sliding window: last instance index <= first instance index.";
            this.debugLog += "getPreviousSlidingFirstLastIndices: Passing assert, lastInstanceIndex > firstInstanceIndex - OK" + "\n";
        }
        return this.previousSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex);
    }
*/

    public ArrayList<DateTime> getCurrentSlidingWindowFirstLastInstanceDateTimes(int slidingWindowIndex){
        DateTime firstInstanceDateTime = this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(0);
        DateTime lastInstanceDateTime = this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1);

        if(this.debuggingSlidingWindow){
            assert lastInstanceDateTime.isAfter(firstInstanceDateTime) : "Error: For current sliding window: last instance time stamp <= first instance time stamp.";
            this.debugLog += "getCurrentSlidingWindowFirstLastInstanceIndices: Passing assert, last instance time stamp > first instance time stamp - OK" + "\n";
        }

        return this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex);
    }
    /**
    public ArrayList<Integer> getCurrentSlidingWindowFirstLastInstanceIndices(int slidingWindowIndex){
        int firstInstanceIndex = this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(0);
        int lastInstanceIndex = this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(1);

        if (this.debuggingSlidingWindow) {
            assert lastInstanceIndex > firstInstanceIndex : "Error: For current sliding window: last instance index <= first instance index.";
            this.debugLog += "getCurrentSlidingFirstLastIndices: Passing assert, lastInstanceIndex > firstInstanceIndex - OK" + "\n";
        }
        return this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex);
    }
*/

    public void add(SamoaInstanceWithTrainingIndex currentInstanceWithTrainingIndex){ //Instance

        //1) add instance to total dataset

        /** With Instances
        currentInstance.setDataset(totalDataset);
        this.totalDataset.add(currentInstance);

        int currentInstanceIndex = this.totalDataset.numInstances() - 1;
        */

        /** this.totalDataset.add(currentInstance);*/ //this.totalDataset.size() - 1
//        double currentInstanceTimestamp = currentInstanceWithTrainingIndex.getTrainingInstance().weight();

//        DateTime currentInstanceDateTime = new DateTime( (long) currentInstanceWithTrainingIndex.getTrainingInstance().weight() * 1000);
        DateTime currentInstanceDateTime = new DateTime( (long) currentInstanceWithTrainingIndex.getDateOfInstance() * 1000);

        if (this.debuggingSlidingWindow) {
            System.out.println("~~~ Start adding instance to window ~~~");
            System.out.println("time stamp of instance to add: " + currentInstanceDateTime.toString());//+ currentInstance.numValues() .toString()
            this.debugLog += "~~~ Start adding instance to window ~~~" + "\n";
            this.debugLog += "time stamp of instance to be added: " + currentInstanceDateTime.toString() + "\n";
            this.debugLog += "instance to add: " + currentInstanceWithTrainingIndex.getTrainingInstance().toString() + "\n";
        }

        /**
         * 1) add instance to samoa instances and tree
         * */
        if(this.totalDataset.containsKey(currentInstanceDateTime)){

            this.totalDataset.get(currentInstanceDateTime).add(currentInstanceWithTrainingIndex);
            /** Debugging prints
             * System.out.println("now the current moment has " + this.totalDataset.get(currentInstanceDateTime).size());*/
        }
        else{
            ArrayList<SamoaInstanceWithTrainingIndex> samoaInstancesInCurrentTime = new ArrayList<SamoaInstanceWithTrainingIndex>();
            samoaInstancesInCurrentTime.add(currentInstanceWithTrainingIndex);
            this.totalDataset.put(currentInstanceDateTime, samoaInstancesInCurrentTime);
        }
        /*
        //old but working
        totalDataset.put(currentInstanceDateTime, currentInstanceWithTrainingIndex);*/
//        dataSetInstances.add(currentInstanceWithTrainingIndex.getTrainingInstance());

        DateTime hypotheticalInstanceOnePeriodBackTimeStamp = null;
        DateTime firstInstanceGreaterThanHypotheticalDateTime = null;
/**
        int currentInstanceIndex = this.totalDataset.size() - 1;
//        System.out.println("total size of data set= " + Integer.toString(this.totalDataset.size()));
        //2) using FirstInstanceTimeStamp check to how many windows to place this instance
        long timestampDifference;
*/


        for(int i=0; i < this.numberOfSlidingWindows; i++){
            if(this.debuggingSlidingWindow){
                System.out.println("For sliding window " + Integer.toString(i));
                this.debugLog += "For sliding window " + Integer.toString(i) + "\n";
            }

            //get an hypothetical instance one period back in time and try to find its' time stamp at the tree
            hypotheticalInstanceOnePeriodBackTimeStamp = calculateOnePeriodBackTimestamp(currentInstanceDateTime,i);
            firstInstanceGreaterThanHypotheticalDateTime = totalDataset.ceilingKey(hypotheticalInstanceOnePeriodBackTimeStamp);

            if(this.debuggingSlidingWindow){

                System.out.println("Time stamp of first instance of the window: " + getCurrentSlidingWindowFirstLastInstanceDateTimes(i).get(0).toString());
                System.out.println("One period back time stamp: " + hypotheticalInstanceOnePeriodBackTimeStamp.toString());
                System.out.println("Time stamp of first instance >= of the hypothetical one period back time stamp: " +  firstInstanceGreaterThanHypotheticalDateTime.toString());
                this.debugLog += "Time stamp of first instance of the window: " + getCurrentSlidingWindowFirstLastInstanceDateTimes(i).get(0).toString() + "\n";
                this.debugLog += "One period back time stamp: " + hypotheticalInstanceOnePeriodBackTimeStamp.toString() + "\n";
                this.debugLog += "Time stamp of first instance >= of the current instance " + firstInstanceGreaterThanHypotheticalDateTime.toString() + "\n";
                /*System.out.println("Time difference (current instance - first instance of window)= " + Double.toString(timestampDifference));
                this.debugLog += "first instance of window time date= " + slidingWindowFirstInstanceDateTime.toString() + "\n";
                this.debugLog += "current instance time date= " + currentInstanceDateTime.toString() + "\n";
                this.debugLog +=  "Time difference (current instance - first instance of window)= " + Double.toString(timestampDifference) + "\n";*/
            }
            /**
             * @see <a href="http://stackoverflow.com/questions/6837007/comparing-float-double-values-using-operator"> Comparing double values </a>
             */
//            System.out.println("time period of current window= " + Double.toString(slidingWindowTimePeriods.get(i)));
            if (firstInstanceGreaterThanHypotheticalDateTime != null){ // if firstInstanceGreaterThanHypotheticalDateTime == currentInstanceDateTime
//                if (firstInstanceGreaterThanHypotheticalDateTime.equals(currentSlidingWindowFirstLastInstanceDateTime.get(i).get(0))){
                if (firstInstanceGreaterThanHypotheticalDateTime.isBefore(currentSlidingWindowFirstLastInstanceDateTime.get(i).get(0)) || firstInstanceGreaterThanHypotheticalDateTime.equals(currentSlidingWindowFirstLastInstanceDateTime.get(i).get(0))){
                    //populate the window
                    populateWindowWithInstance(currentInstanceDateTime, i);
                    this.isWindowSlided.set(i, false);
                    if(this.debuggingSlidingWindow){
                        System.out.println("Populate window " + Integer.toString(i) + " with instance " + currentInstanceWithTrainingIndex.getTrainingInstance().toString());
                        this.debugLog += "Populate window " + Integer.toString(i) + " with instance " + currentInstanceWithTrainingIndex.getTrainingInstance().toString() + "\n";
                    }
                }
                else if (firstInstanceGreaterThanHypotheticalDateTime.isAfter(currentSlidingWindowFirstLastInstanceDateTime.get(i).get(0))){
                    //slide the window
                    /*System.out.println("--- ---");
                    System.out.println("sliding the window " + Integer.toString(i));
                    System.out.println("currentInstanceDateTime: " + currentInstanceDateTime.toString());
                    System.out.println("hypotheticalInstanceOnePeriodBackTimeStamp: " + hypotheticalInstanceOnePeriodBackTimeStamp.toString());
                    System.out.println("firstInstanceGreaterThanHypotheticalDateTime: " + firstInstanceGreaterThanHypotheticalDateTime.toString());
                    System.out.println("first instance of window: " + currentSlidingWindowFirstLastInstanceDateTime.get(i).get(0).toString());
                    System.out.println("--- ---");*/
                    slideWindowWithInstance(currentInstanceDateTime, firstInstanceGreaterThanHypotheticalDateTime, i);
                    this.isWindowSlided.set(i, true);

                    /*System.out.println("~~~ ~~~");
                    System.out.println("first instance of window: " + currentSlidingWindowFirstLastInstanceDateTime.get(i).get(0).toString());
                    System.out.println("last instance of window: " + currentSlidingWindowFirstLastInstanceDateTime.get(i).get(1).toString());
                    System.out.println("~~~ ~~~");*/

                    if(this.debuggingSlidingWindow){
                        System.out.println("Slide window " + Integer.toString(i) + " with instance " + currentInstanceWithTrainingIndex.getTrainingInstance().toString());
                        this.debugLog += "Slide window " + Integer.toString(i) + " with instance " + currentInstanceWithTrainingIndex.getTrainingInstance().toString() + "\n";
                    }
                }
            }
            else{ //very start of the instance windows
                if(this.debuggingSlidingWindow){
                    System.out.println("Time stamp of first instance >= of the current instance is null");
                    this.debugLog += "Time stamp of first instance >= of the current instance is null" + "\n";
                }
                //populate window with very first instances
                populateWindowWithInstance(currentInstanceDateTime, i);
                this.isWindowSlided.set(i,false);
                if(this.debuggingSlidingWindow){
                    System.out.println("Populate window " + Integer.toString(i) + " with instance " + currentInstanceWithTrainingIndex.getTrainingInstance().toString());
                    this.debugLog += "Populate window " + Integer.toString(i) + " with instance " + currentInstanceWithTrainingIndex.getTrainingInstance().toString() + "\n";
                }
            }

            if(this.debuggingSlidingWindow){
                System.out.println("isWindowSlided? -> " + Boolean.toString(this.getIsWindowSlided(i)));
                this.debugLog += "isWindowSlided? -> " + Boolean.toString(this.getIsWindowSlided(i)) + "\n";
            }

            if(this.debuggingSlidingWindow){
                System.out.println("After populate or slide window: ");
                this.debugLog += "After populate or slide window: "+ "\n";
//                this.printSlidingWindowInstances(i);
            }

        }//close adding instance for all windows
        if (this.debuggingSlidingWindow) {
            System.out.println("~~~ End of adding instance to window ~~~");
            this.debugLog += "~~~ End of adding instance to window ~~~" + "\n";
        }
    }


    protected DateTime calculateOnePeriodBackTimestamp(DateTime currentInstanceDateTime, int slidingWindowIndex){
        String selectedSlidingWindowTimePeriodDescription = this.slidingWindowTimePeriodsDescription.get(slidingWindowIndex);
        int slidingWindowTimePeriod = this.slidingWindowTimePeriods.get(slidingWindowIndex).intValue();
        DateTime hypotheticalInstanceOnePeriodBackDateTime = null;

        if(selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("seconds")){
            hypotheticalInstanceOnePeriodBackDateTime = currentInstanceDateTime.minusSeconds(slidingWindowTimePeriod);
        }
        if(selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("hours")){
            hypotheticalInstanceOnePeriodBackDateTime = currentInstanceDateTime.minusHours(slidingWindowTimePeriod); //12
        }
        else if (selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("days")){
            hypotheticalInstanceOnePeriodBackDateTime = currentInstanceDateTime.minusDays(slidingWindowTimePeriod);
        }
        else if (selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("weeks")){
            hypotheticalInstanceOnePeriodBackDateTime = currentInstanceDateTime.minusWeeks(slidingWindowTimePeriod);
        }
        else if (selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("months")){
            hypotheticalInstanceOnePeriodBackDateTime = currentInstanceDateTime.minusMonths(slidingWindowTimePeriod);
        }

        return hypotheticalInstanceOnePeriodBackDateTime;

    }
    /**
     * @see <a href="http://stackoverflow.com/questions/12851934/how-to-find-difference-between-two-joda-time-datetimes-in-minutes"> joda Duration() class </a>
     */
    /**
     * @deprecated not used anymore to be removed..
     * */
    protected long calculateTimestampsDifferenceForSlidingWindowPeriod(DateTime firstTimestamp, DateTime secondTimestamp, int slidingWindowIndex){

        String selectedSlidingWindowTimePeriodDescription = slidingWindowTimePeriodsDescription.get(slidingWindowIndex);
        Duration timestampDifferenceDuration = new Duration(firstTimestamp, secondTimestamp);
        if(selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("seconds")){
            return timestampDifferenceDuration.getStandardSeconds();
        }
        else if (selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("hours")){
            return timestampDifferenceDuration.getStandardHours();//.getStandardHours();
        }
        else if ( selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("days")){
            return timestampDifferenceDuration.getStandardDays();//.getStandardHours();//getStandardDays();
        }
        else if( selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("weeks")){
            return timestampDifferenceDuration.getStandardDays() / 7;
        }
        else if (selectedSlidingWindowTimePeriodDescription.equalsIgnoreCase("months")){
            return timestampDifferenceDuration.getStandardDays() / 30;
        }
        else{
            return -1;
        }


    }
/*
    protected Date? calculateTimestampsDifference(){

    }
   */
    /** protected void slideWindowWithInstance(double instanceToAddTimestamp, int instance2BeAddedIndex, int slidingWindowIndex){*/
    protected void slideWindowWithInstance(DateTime instanceToAddDateTime, DateTime firstInstanceGreaterThanCurrentDateTime, int slidingWindowIndex){
        if(this.debuggingSlidingWindow){
            System.out.println("=== Slide Window ===");
            this.debugLog += "~~~ Slide Window ~~~" + "\n";
        }
        DateTime upperLimitCandidateDateTime = this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1);

        if (firstInstanceGreaterThanCurrentDateTime.equals(upperLimitCandidateDateTime)) {
            if (this.debuggingSlidingWindow) {
                System.out.println("Moving first element of window reached last possible position..");
                this.debugLog += "Moving first element of window, we have reached last possible position.." + "\n";
            }
        }

        /**
         * update the indices for the first and last instance of previous and current window
         * */
        this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).set(0, this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(0));
        this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).set(1, this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1));
        this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).set(0, firstInstanceGreaterThanCurrentDateTime);
        this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).set(1, instanceToAddDateTime);

        /**
         * update the size of previous and current sliding window
         **/
        this.previousSlidingWindowNumberOfInstances.set(slidingWindowIndex, this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex));
//        long currentSlidingWindowNumberOfInstances = totalDataset.get(instanceToAddDateTime).getTrainingIndex() - totalDataset.get(firstInstanceGreaterThanCurrentDateTime).getTrainingIndex() + 1;
        long currentSlidingWindowNumberOfInstances = this.totalDataset.get(instanceToAddDateTime).get(this.totalDataset.get(instanceToAddDateTime).size() - 1).getTrainingIndex() - this.totalDataset.get(firstInstanceGreaterThanCurrentDateTime).get(this.totalDataset.get(firstInstanceGreaterThanCurrentDateTime).size() -1).getTrainingIndex() + 1;
        this.currentSlidingWindowNumberOfInstances.set(slidingWindowIndex, currentSlidingWindowNumberOfInstances);

        if(debuggingSlidingWindow){
            System.out.println("previous window - time stamp of first last elements = " + this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).toString());
            System.out.println("current window - indices of first last elements = " + this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).toString());
            System.out.println("current window - number of instances = " + Long.toString(getSlidingWindowNumberOfInstances(slidingWindowIndex)));
//            System.out.println("current window - number of instances = " + Integer.toString(this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex)));
            System.out.println("=== slide window===");
            this.debugLog += "previous window - indices of first last elements = " + this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).toString() + "\n";
            this.debugLog += "current window - indices of first last elements = " + this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).toString() + "\n";
            this.debugLog += "current window - number of instances = " + Long.toString(getSlidingWindowNumberOfInstances(slidingWindowIndex)) + "\n";
            //            this.debugLog += "current window - number of instances = " + Integer.toString(this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex)) + "\n";
            this.debugLog += "~~~ Slide Window ~~~" + "\n";
        }
    }

    /** protected void populateWindowWithInstance(int instanceToAddIndex, int slidingWindowIndex){*/
    protected void populateWindowWithInstance(DateTime currentInstanceDateTime, int slidingWindowIndex){
        if(this.debuggingSlidingWindow){
//            System.out.println("=== populate window ===");
            this.debugLog += "~~~ Populate Window ~~~" + "\n";
        }
        //update last element
        /**
         * this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).set(1,instanceToAddIndex);
        this.currentSlidingWindowNumberOfInstances.set(slidingWindowIndex, getSlidingWindowNumberOfInstances(slidingWindowIndex));*/
        this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).set(1, currentInstanceDateTime);
        this.currentSlidingWindowNumberOfInstances.set(slidingWindowIndex, this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex) + 1);
        /** TODO: check if you need it
        this.currentSlidingWindowNumberOfInstances.set(slidingWindowIndex, this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex) + 1);*/

        if(this.debuggingSlidingWindow){

            /** this.debugLog += "Index of the last instance of window= " + Integer.toString(this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(1)) + "\n";*/
            this.debugLog += "TimeDate of the last instance of window = " + currentInstanceDateTime.toString() + "\n";
            this.debugLog += "Number of instances of window= " + Long.toString(getSlidingWindowNumberOfInstances(slidingWindowIndex)) + "\n";//Integer.toString(this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex)) + "\n";
            this.debugLog += "~~~ Populate Window ~~~" + "\n";
        }

    }

    protected long getSlidingWindowNumberOfInstances(int slidingWindowIndex){
        /** int numberOfInstances = this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(1) - this.currentSlidingWindowFirstLastInstanceIndices.get(slidingWindowIndex).get(0) + 1;*/
        /**
        long numberOfInstances = this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex);
        */
        //int numberOfInstances = (totalDataset.subMap(this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(0), true, this.currentSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1), true)).size();
        /**
        if(this.debuggingSlidingWindow) {
            assert (numberOfInstances > 1) : "Error: current windows has <= 1 instances.";
            this.debugLog += "getSlidingWindowNumberOfInstances: Passing assert, current window has > 1 instances - OK" + "\n";
        }
         */
        /*assert (numberOfInstances > 0){
            System.out.println("The number of instances <= 0");
        }*/
        /**
        return numberOfInstances;
         */

        return this.currentSlidingWindowNumberOfInstances.get(slidingWindowIndex);
    }

    protected long getPreviousSlidingWindowNumberOfInstances(int slidingWindowIndex){
     /*   long previousSlidingWindowNewestInstanceIndex = totalDataset.get(this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(1)).getTrainingIndex();
        long previousSlidingWindowOldestInstanceIndex = totalDataset.get(this.previousSlidingWindowFirstLastInstanceDateTime.get(slidingWindowIndex).get(0)).getTrainingIndex();

        return previousSlidingWindowNewestInstanceIndex - previousSlidingWindowOldestInstanceIndex + 1;*/
        return this.previousSlidingWindowNumberOfInstances.get(slidingWindowIndex);
    }

    /* //TODO: Write me up!
    public void calculateAverageLagTimeForSlidingWindow(){}
    public void calculateNumberOfInstancesForSlidingWindow(){}
    public void writeStatisticsPerSlidingWindow(){}
    */

//    public addInstanceToTotalDataset(){}

//    protected updateSlidingWindowWithIndex(){}

//    protected increaseFirstInstanceIndex(int slidingWindow){}

//    protected increaseLastInstanceIndex(int slidingWindow){}


}
