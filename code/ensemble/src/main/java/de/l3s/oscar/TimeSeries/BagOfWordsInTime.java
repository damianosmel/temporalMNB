package de.l3s.oscar.TimeSeries;

import de.l3s.oscar.Evaluation.TimeSeriesPredPerformance;
import org.joda.time.DateTime;

import java.io.BufferedWriter;
import java.io.File;
import java.util.*;

/**
 * Created by damian on 23.07.17.
 */
public class BagOfWordsInTime {

    /**
     * tree saving all times of the stream, having as leaves:
     * leaf_i: [key, value] ->
     * key: timeOfDocument(s)
     * value: [ hashMap with keys, the words found that time and values the class counts of the words, hashMap with values the class names and values the prior counts of classes and the total counts of classes for this time
     * explained:
     * value: [{wordFoundInTimeOfDocument:[classXCount,..], anotherWordFoundInTimeOfDocument:[classXCount,..] } , {classX: [priorCount, classTotalCount], classY:[priorCount, classTotalCount]}]
     * */
    protected TreeMap<DateTime, ArrayList<HashMap<String, ArrayList<Double>>>> timeTree4WordStatistics; //= new TreeMap<DateTime, ArrayList<HashMap<String, ArrayList<Double> >>>();

    /**
     * inverted index that maps words to times that it appears in the stream
     * key: word -> value: [time_1, .. time_i, ..]
     * */
    //TODO: what if the ArrayList is a sorted map?
    protected HashMap<String, ArrayList<DateTime>> wordToTimesInvertedIndex; //= new HashMap<String, ArrayList<DateTime>>();


    /**
     * keep this structure to get more efficiently the word counts
     * */
    protected HashMap<String, WordTrajectoryData> wordsTimeTrees;
//    protected ArrayList<TreeMap<DateTime, ArrayList<Double>>>  = new

    protected int numberOfClasses = 0;

    protected DateTime veryFirstInstanceInTime;

    /**
     * aggregate all time points belongs on the same time period
     * */
    int aggregationPeriod = 0;
    String aggregationGranularity;

    /**
     * For reporting time series prediction
     * */
    ArrayList<String> trackedWords;
    File outputDirectory;

    public BagOfWordsInTime(int numberOfClasses, int aggregationPeriod, String aggregationGranularity){
//        System.out.println("Creating new BagOfWordsInTime");
        this.timeTree4WordStatistics = new TreeMap<DateTime, ArrayList<HashMap<String, ArrayList<Double> >>>();
        this.wordToTimesInvertedIndex = new HashMap<String, ArrayList<DateTime>>();
        this.wordsTimeTrees = new HashMap<String, WordTrajectoryData>();
        this.veryFirstInstanceInTime = null;

        this.numberOfClasses = numberOfClasses;
        /**
         * aggregation period
         * */
        this.aggregationPeriod = aggregationPeriod;
        this.aggregationGranularity = aggregationGranularity;

        /**
         * for reporting time series prediction
         * */
        this.trackedWords = new ArrayList<String>();

    }

    /**
     * set which words to track
     * */
    public void setTrackedWords(ArrayList<String> trackedWords){
        this.trackedWords = trackedWords;
//        Collections.copy(this.trackedWords, trackedWords);
    }

    public void setOutputDirectory(File outputDirectory){
//        System.out.println("BagOfWords: setting up output dir");
        this.outputDirectory = outputDirectory;
    }
    public void setNumberOfClasses(int numberOfClasses){
        this.numberOfClasses = numberOfClasses;
    }

    public WordTrajectoryData getWordTrajectoryData(String word){

        if(this.wordsTimeTrees.containsKey(word) == true){
            return this.wordsTimeTrees.get(word);
        }
        else{
//            System.out.println("return null");
            return null;
        }

//        assert (this.wordsTimeTrees.containsKey(word) == true) : "Error: this word is not followed\n";

//        return this.wordsTimeTrees.get(word);
    }

    public int getNumberOfClasses() {return this.numberOfClasses;}

    public HashMap<DateTime, ArrayList<Double>> getZeroWordCountsHash(DateTime timeOfWord){
        HashMap<DateTime, ArrayList<Double>> wordCountsHash = new HashMap<DateTime, ArrayList<Double>>();
        wordCountsHash.put(timeOfWord, getZeroWordClassCounts());
        return wordCountsHash;
    }

    public HashMap<String, ArrayList<Double>> getZeroClassCountsHash(){
        HashMap<String, ArrayList<Double>> classIndexToTotalCounts = new HashMap<String, ArrayList<Double>>();

        for(int i=0; i<this.numberOfClasses;i++){
            classIndexToTotalCounts.put(Integer.toString(i), new ArrayList<Double>(Arrays.asList(0.0, 0.0)));
        }

        return classIndexToTotalCounts;
    }

    public ArrayList<Double> getZeroWordClassCounts(){
        ArrayList<Double> wordClassCounts = new ArrayList<Double>();

        for (int i=0; i< this.numberOfClasses; i++){
            wordClassCounts.add(0.0D);
        }

        return wordClassCounts;
    }

    //TODO: Do you need to print every m nodes the info?
    /**
     * Method to print the whole time tree
     */
    public void printTimeTree(){
        System.out.println("--- TimeTree ---");
        for(DateTime observedTime : this.timeTree4WordStatistics.keySet()){
            System.out.println("Time: " + observedTime.toString());
            System.out.println("Word Statistics: ");
            for(String observedWord : this.timeTree4WordStatistics.get(observedTime).get(0).keySet() ){
                System.out.println("Word: " + observedWord);
                for(int classIndex=0; classIndex< this.numberOfClasses; classIndex++) {
                    System.out.println("Class " + classIndex + " -> documentLength * wordOccurence = " + this.timeTree4WordStatistics.get(observedTime).get(0).get(observedWord).get(classIndex));
                }
            }
            System.out.println("Document Statistics: ");
            System.out.println("Class Priors: ");
            for(int classIndex=0; classIndex < this.numberOfClasses; classIndex++){
                System.out.println("Class " + classIndex + " -> " + this.timeTree4WordStatistics.get(observedTime).get(1).get(Integer.toString(classIndex)).get(0));
            }
            System.out.println("Class Totals: ");
            for(int classIndex=0; classIndex < this.numberOfClasses; classIndex++){
                System.out.println("Class " + classIndex + " -> " + this.timeTree4WordStatistics.get(observedTime).get(1).get(Integer.toString(classIndex)).get(1));
            }
            System.out.println("===");
        }
        System.out.println("--- TimeTree ---");
    }

    /**
     * Method to print the wordToTime inverted index
     */
    public void printWordToTimeInvertedIndex(){
        int countWordsForNewLine = 0;
        System.out.println("--- Word2Time Inverted Index ---");
        for(String observedWord : this.wordToTimesInvertedIndex.keySet()){
            System.out.println("Word: " + observedWord);
            System.out.println("Observed in times: ");
            for(DateTime timeObservedWord : this.wordToTimesInvertedIndex.get(observedWord)){
                countWordsForNewLine++;
                if(countWordsForNewLine % 10 == 0){
                    System.out.println("");
                    countWordsForNewLine = 0;
                }
                System.out.print(timeObservedWord.toString() + " ");
            }
            System.out.println("\n==="); //new word
        }
        System.out.println("--- Word2Time Inverted Index ---");
    }

    /**
     * For memory efficiency, when a word is removed from the filter
     * remove it also from the trajectory data
     * */
    public void removeWords(List<String> wordsToRemove){
        for (String wordToRemove : wordsToRemove){
            if(this.wordsTimeTrees.containsKey(wordToRemove)){
//                System.out.println("Removing time series of word " + wordToRemove);
                this.wordsTimeTrees.remove(wordToRemove);
            }
        }
    }

    public int getSizeOfInvertedIndex(){
        return this.wordToTimesInvertedIndex.keySet().size();
    }

    /**
     * Light-weight version
     * */
    public void addWordInLightWeightTime(DateTime documentTime, String word, double wordCount, int documentClassIndex){
//        System.out.println("add " + word);
        ArrayList<Double> wordConditionals = populateZeroListWithNonZeroOnIndex(wordCount, documentClassIndex);
        //update only the time series of the word
        addWordInLightWeightTrajectory(word, documentTime, wordConditionals);
    }

    /**
     * Add word to TimeTree and inverted index
     * @param documentTime          DateTime: arrival time of document
     * @param word                  String: word of document
     * @param wordCount             double: count of word in the document
     * @param totalClassCount            double: total class count for the document
     * @param documentClassIndex    int: 0 -> first class, 1 -> second class, ..
     * @param firstWordInDocument   boolean: is the word the first inside the document
     */
    public void addWordInTime(DateTime documentTime, String word, double wordCount, double totalClassCount, int documentClassIndex, boolean firstWordInDocument){
//        System.out.println("adding word " + word);
        addWordInTimeTree(documentTime, word, wordCount, totalClassCount, documentClassIndex, firstWordInDocument);
        addWordInWordTimeTrees(word, documentTime);
        //word models
        addWordForPoissonStats(word,documentTime, wordCount, documentClassIndex);
        addWordForSeasonalPoissonStats(word, documentTime, wordCount, documentClassIndex);
        addWordForARIMAStats(word, documentTime, wordCount, documentClassIndex);
        addWordInTimeIndex(word,documentTime);
    }

    public ArrayList<Double> populateZeroListWithNonZeroOnIndex(double wordCount, int documentClassIndex){
        ArrayList<Double> wordCounts = getZeroWordClassCounts();
        wordCounts.set(documentClassIndex, wordCount);
        return wordCounts;
    }
    /**
     * Simpler version
     * */
    public void addWordForPoissonStats(String word, DateTime documentTime, ArrayList<Double> conditionalCounts){
//        System.out.println("in Poisson");
        this.wordsTimeTrees.get(word).addToWordStatisticsForPoisson(documentTime, conditionalCounts);
    }

    /**
     * Updating the statistics for the Poisson model
     * */
    public void addWordForPoissonStats(String word, DateTime documentTime, double wordCount, int documentClassIndex){
        this.wordsTimeTrees.get(word).addToWordStatisticsForPoisson(documentTime, populateZeroListWithNonZeroOnIndex(wordCount, documentClassIndex));
    }

    /**
     * Simpler version
     * */
    public void addWordForSeasonalPoissonStats(String word, DateTime documentTime, ArrayList<Double> conditionalCounts){
//        System.out.println("in Seasonal ");
        this.wordsTimeTrees.get(word).addToWordStatisticsForSeasonalPoisson(documentTime, conditionalCounts);
    }

    /**
     * Updating the statistics for the seasonal Poisson model
     * */
    public void addWordForSeasonalPoissonStats(String word, DateTime documentTime, double wordCount, int documentClassIndex){
        this.wordsTimeTrees.get(word).addToWordStatisticsForSeasonalPoisson(documentTime, populateZeroListWithNonZeroOnIndex(wordCount, documentClassIndex));
    }

    public void addWordForARIMAStats(String word, DateTime documentTime, double wordCount, int documentClassIndex){
        this.wordsTimeTrees.get(word).addToWordStatisticsForARIMA(documentTime, populateZeroListWithNonZeroOnIndex(wordCount, documentClassIndex));
    }

    /**
     * reporting prediction values
     * */
    public TimeSeriesPredPerformance getTimeSeriesPerf(String word){

        if(this.wordsTimeTrees.containsKey(word) == false){
            System.out.println("This word is not tracked.");
            return null;
        }
        else{
            return this.wordsTimeTrees.get(word).getTimeSeriesPerf();
        }
    }

    public boolean attributeEqualsEntity(String attribute){
        boolean attributeEqualsEntity = false;
        for (String entityToTrack : this.trackedWords){
            if(entityToTrack.equalsIgnoreCase(attribute)){
                attributeEqualsEntity = true;
                break;
            }
        }
        return attributeEqualsEntity;
    }

    public void addWordInLightWeightTrajectory(String word, DateTime documentTime, ArrayList<Double> wordConditionals){
        boolean resizeAfterCollecting = false;
        boolean trackWord = false;
        //if word is not registered before
        if( this.wordsTimeTrees.containsKey(word) == false){
            if(attributeEqualsEntity(word)){
                trackWord = true;
            }
            WordTrajectoryData wordTrajectoryData = new WordTrajectoryData(word, this.numberOfClasses, this.aggregationPeriod, this.aggregationGranularity, resizeAfterCollecting, trackWord);
            this.wordsTimeTrees.put(word, wordTrajectoryData);
            if(trackWord){
                this.wordsTimeTrees.get(word).initializePredPerformance(this.outputDirectory);
            }
        }
        //add counts for the word
        this.wordsTimeTrees.get(word).addToWordLightWeightTrajectory(documentTime, wordConditionals); //data set with time stamp
//        this.wordsTimeTrees.get(word).addToWordLightWeightTrajectoryInstBased(documentTime,wordConditionals); //data set without time stamp
    }

    @Deprecated
    public void addWordInWordTimeTrees(String word, DateTime documentTime){
//        boolean resizeAfterCollecting = true;
        boolean resizeAfterCollecting = false;
        boolean trackWord = false;
        /**
         * add word to the hash of WordTrajectoryData
         * */
        /**
         * if word is not registered before
         * */
        if(this.wordsTimeTrees.containsKey(word) == false) {
//            System.out.println("creating wordsTimeTrees for " + word);

            WordTrajectoryData wordTrajectoryData = new WordTrajectoryData(word, this.numberOfClasses, this.aggregationPeriod, this.aggregationGranularity, resizeAfterCollecting, trackWord);
            wordTrajectoryData.setFirstTimeOfWord(documentTime);
            this.wordsTimeTrees.put(word, wordTrajectoryData);
        }
        this.wordsTimeTrees.get(word).addToWordTrajectory(documentTime, this.timeTree4WordStatistics.get(documentTime).get(0).get(word));
    }

    /**
     * Add word to TimeTree
     * @param documentTime              DateTime: arrival time of document
     * @param word                      String: word of document
     * @param wordCount                 double: count of word in the document
     * @param totalClassCount           double: class count of class for the document
     * @param documentClassIndex        int: 0 -> first class, 1 -> second class, ..
     * @param firstWordInDocument       boolean: is the word the first inside the document
     */
    public void addWordInTimeTree(DateTime documentTime, String word, double wordCount, double totalClassCount, int documentClassIndex, boolean firstWordInDocument){

        ArrayList<Double> wordClassCounts = new ArrayList<Double>();

        /**
         * 1) if time has been registered already:
         */
         if(this.timeTree4WordStatistics.containsKey(documentTime)){
             /**
              *1a) if word has been seen already
              */
             if(this.timeTree4WordStatistics.get(documentTime).get(0).containsKey(word)){
                this.timeTree4WordStatistics.get(documentTime).get(0).get(word).set( documentClassIndex, this.timeTree4WordStatistics.get(documentTime).get(0).get(word).get(documentClassIndex) + wordCount);
             }
             /**
              * 1b) if word has not been used before
              */
             else{
                this.timeTree4WordStatistics.get(documentTime).get(0).put(word, getZeroWordClassCounts());
                this.timeTree4WordStatistics.get(documentTime).get(0).get(word).set( documentClassIndex, this.timeTree4WordStatistics.get(documentTime).get(0).get(word).get(documentClassIndex) + wordCount);

                //                this.timeTree4WordStatistics.get(documentTime).get(1).get(Integer.toString(documentClassIndex)).set(0, this.timeTree4WordStatistics.get(documentTime).get(1).get( documentClassIndex).get(0) + 1);
            }
            /**
             * update the priors only for the first word in tweet
             * 1) prior of class
             * 2) total count of class
             * */
            if(firstWordInDocument) {
//                System.out.println("first word in tweet");
                this.timeTree4WordStatistics.get(documentTime).get(1).get(Integer.toString(documentClassIndex)).set(0, this.timeTree4WordStatistics.get(documentTime).get(1).get( Integer.toString(documentClassIndex)).get(0) + 1.0);
                this.timeTree4WordStatistics.get(documentTime).get(1).get(Integer.toString(documentClassIndex)).set(1, this.timeTree4WordStatistics.get(documentTime).get(1).get( Integer.toString(documentClassIndex)).get(1) + totalClassCount);
            }
        }
        /**
         * 2)  if time not registered before:
         */
        else{
             /**
              * create the hash table containing the word and its statistics
              */
             /**
              * create the hash table containing the word and its time occurrences
              */
             //initialize
            HashMap<String, ArrayList<Double>> wordToClassCounts = new HashMap<String, ArrayList<Double>>();
            ArrayList<Double> currentWordClassCounts = getZeroWordClassCounts();
            //set
            currentWordClassCounts.set(documentClassIndex, currentWordClassCounts.get(documentClassIndex) + wordCount);
            wordToClassCounts.put(word, currentWordClassCounts);

            /**
             * initialize the class priors and total counts for the current time
             * */
            //initialize
            HashMap<String, ArrayList<Double>> classIndexToClassCounts = getZeroClassCountsHash();
            //set
            classIndexToClassCounts.get(Integer.toString(documentClassIndex)).set(0, 1.0);
            classIndexToClassCounts.get(Integer.toString(documentClassIndex)).set(1, totalClassCount);

            /**
              * add these hash tables to the time tree
              */
            ArrayList<HashMap<String, ArrayList<Double>>> wordStatisticsForDocumentTime = new ArrayList<HashMap<String, ArrayList<Double>>>();
            wordStatisticsForDocumentTime.add(wordToClassCounts);
            wordStatisticsForDocumentTime.add(classIndexToClassCounts);
            this.timeTree4WordStatistics.put(documentTime, wordStatisticsForDocumentTime);

            /**
             * set the time of the very first instance
             * */
            if(this.veryFirstInstanceInTime != null){
                this.veryFirstInstanceInTime = documentTime;
            }
        }
    }

    /**
     * Add word in the inverted index of time
     * @param word              String: word of document
     * @param documentTime      DateTime: arrival of the document
     */
    public void addWordInTimeIndex(String word, DateTime documentTime) {
        /**
         * 1) if word was registered in the index
         */
        if(this.wordToTimesInvertedIndex.containsKey(word)){
            this.wordToTimesInvertedIndex.get(word).add(documentTime);
        }
        /**
         * 2) if word not registered before
         */
        else{
            ArrayList<DateTime> dateTimesForWord = new ArrayList<DateTime>();
            dateTimesForWord.add(documentTime);
            this.wordToTimesInvertedIndex.put(word, dateTimesForWord);
        }
    }


    public ArrayList<double []> addClassTrajectoryFromTimeTree( DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName){

        /**
         * bound the starting point by the given window
         * */
        if(windowTimeSize != 0){
            if (windowTimeName.equalsIgnoreCase("day")){
                timeRangeStart = timeRangeStop.minusDays(windowTimeSize);
            }
            else if (windowTimeName.equalsIgnoreCase("week")){
                timeRangeStart = timeRangeStop.minusWeeks(windowTimeSize);
            }
            else if (windowTimeName.equalsIgnoreCase("month")){
                timeRangeStart = timeRangeStop.minusMonths(windowTimeSize);
            }
        }


        /**
         * add up the values for each document
         * */
        SortedMap<DateTime, ArrayList<HashMap<String, ArrayList<Double>>>> windowedTimeTree = this.timeTree4WordStatistics.tailMap(timeRangeStart);

        /**
         * initialize arrays with sums
         * */
        double [] sumPriorClassCounts = new double[this.getNumberOfClasses()];
        Arrays.fill(sumPriorClassCounts, 1.0D);

        double [] sumTotalClassCounts = new double[this.getNumberOfClasses()];
        Arrays.fill(sumTotalClassCounts, 1.0D);

        ArrayList<double []> sumClassCounts = new ArrayList<double[]>();
        sumClassCounts.add(sumPriorClassCounts);
        sumClassCounts.add(sumTotalClassCounts);
        /**
         * select prior or total count for classes
         * *//*
        int countsIndex = 0;
        if(priorOrTotalClassCounts.equalsIgnoreCase("prior")){
            countsIndex = 0;
        }
        else if(priorOrTotalClassCounts.equalsIgnoreCase("totalCount")){
            countsIndex = 1;
        }*/
        for(Map.Entry<DateTime,ArrayList<HashMap<String, ArrayList<Double>>>> documentEntry : windowedTimeTree.entrySet()){
            for(int classIndex = 0; classIndex < this.getNumberOfClasses(); classIndex++){
                //get the prior
                sumClassCounts.get(0)[classIndex] += documentEntry.getValue().get(1).get(Integer.toString(classIndex)).get(0);
//                System.out.println("for class " + classIndex + " prior: " + sumClassCounts.get(0)[classIndex]);
                //get the total count
                sumClassCounts.get(1)[classIndex] += documentEntry.getValue().get(1).get(Integer.toString(classIndex)).get(1);
//                System.out.println("for class " + classIndex + " totalCount: " + sumClassCounts.get(1)[classIndex]);
            }
        }



        return sumClassCounts;
    }

    @Deprecated
    /**
     * Collect statistics for given word and time range
     * @param word                      String: word to get statistics for
     * @param timeRangeStart            DateTime: start of time range for statistics
     * @param timeRangeStop             DateTime: stop of time range for statistics
     * @param allTimesOfWord            ArrayList<DateTime>: list of times that the word was observed
     * @return wordCountsInTimeRange    HashMap<DateTime, ArrayList<Double>>: per time of word occurrences collect the word statistics
     */
    public HashMap<DateTime, ArrayList<Double>> collectWordTrajectoryHashFromTimeTree(String word, DateTime timeRangeStart, DateTime timeRangeStop, ArrayList<DateTime> allTimesOfWord){
        HashMap<DateTime, ArrayList<Double>> wordCountsInTimeRange = new HashMap<DateTime, ArrayList<Double>>();

        for(DateTime timeOfWord : allTimesOfWord){
            if((timeOfWord.isEqual(timeRangeStart) || timeOfWord.isAfter(timeRangeStart)) || (timeOfWord.isEqual(timeRangeStop) || timeOfWord.isBefore(timeRangeStop))){
                assert (this.timeTree4WordStatistics.get(timeOfWord).get(0).containsKey(word) == true) : "Error: for current specific time TimeTree does not have the word";
                wordCountsInTimeRange.put(timeOfWord, this.timeTree4WordStatistics.get(timeOfWord).get(0).get(word));
            }
        }

        return wordCountsInTimeRange;
    }

    @Deprecated
    /**
     *
     * @param word                      String: word to get statistics for
     * @param timeRangeStart            DateTime: start of time range for statistics
     * @param timeRangeStop             DateTime: stop of time range for statistics
     * @return wordTrajectoryHash     HashMap<DateTime, ArrayList<Double>>: per time of word occurrences get the word statistics
     */
    public HashMap<DateTime, ArrayList<Double>> getWordTrajectoryHash(String word, DateTime timeRangeStart, DateTime timeRangeStop){
        ArrayList<DateTime> allTimesOfWord;
        /**
         * if we had observed for this word
         * */
        if(this.wordToTimesInvertedIndex.containsKey(word)){
            allTimesOfWord = this.wordToTimesInvertedIndex.get(word);
            return collectWordTrajectoryHashFromTimeTree(word, timeRangeStart, timeRangeStop, allTimesOfWord);
//            findOverLappingTimeRange(allTimesOfWord, DateTime timeRangeStart, )
        }
        /**
         * otherwise,
         * */
        else{
            return getZeroWordCountsHash(timeRangeStop);
        }
    }

    public HashMap<Integer, ArrayList<double []>> getZeroWordTrajectoryArray(DateTime timeRangeStop){
        HashMap<Integer, ArrayList<double []>> zeroWordTrajectoryArray = new HashMap<Integer, ArrayList<double []>>();
        ArrayList<double []> timeStopZeroWordCount = new ArrayList<double []>();
        timeStopZeroWordCount.add(new double[]{(double) timeRangeStop.getMillis(), 0.0});
        for(int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            zeroWordTrajectoryArray.put(classIndex, timeStopZeroWordCount);
        }
        return zeroWordTrajectoryArray;
    }

    public HashMap<Integer, ArrayList<double []>> initializeWordTrajectoryHashArray(){
        HashMap<Integer, ArrayList<double []>> wordTrajectoryHashArray = new HashMap<Integer, ArrayList<double []>>();
        for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
            wordTrajectoryHashArray.put(classIndex, new ArrayList<double[]>());
        }
        return wordTrajectoryHashArray;
    }

    @Deprecated
    public HashMap<Integer, ArrayList<double []>> collectWordTrajectoryArrayFromTimeTree(String word, DateTime timeRangeStart, DateTime timeRangeStop, ArrayList<DateTime> allTimesOfWord){
        HashMap<Integer, ArrayList<double []>> wordTrajectoryHashArray = initializeWordTrajectoryHashArray(); //new HashMap<Integer, ArrayList<Double>>();

        double timeOfWordInMillis;
        double wordConditionalCount;

        for(DateTime timeOfWord : allTimesOfWord){
            if((timeOfWord.isEqual(timeRangeStart) || timeOfWord.isAfter(timeRangeStart)) || (timeOfWord.isEqual(timeRangeStop) || timeOfWord.isBefore(timeRangeStop))){
                assert (this.timeTree4WordStatistics.get(timeOfWord).get(0).containsKey(word) == true) : "Error: for current specific time TimeTree does not have the word";
                for (int classIndex = 0; classIndex < this.numberOfClasses; classIndex++){
                      timeOfWordInMillis = (double) timeOfWord.getMillis();
                      wordConditionalCount = this.timeTree4WordStatistics.get(timeOfWord).get(0).get(word).get(classIndex);
                      wordTrajectoryHashArray.get(classIndex).add(new double[]{timeOfWordInMillis, wordConditionalCount});
                }
            }
        }

        return wordTrajectoryHashArray;
    }

    @Deprecated
    public HashMap<Integer, ArrayList<double []>> getWordTrajectoryArray(String word, DateTime timeRangeStart, DateTime timeRangeStop){
        ArrayList<DateTime> allTimesOfWord;
        /**
         * if we had observed for this word
         * */
        if(this.wordToTimesInvertedIndex.containsKey(word)){
            allTimesOfWord = this.wordToTimesInvertedIndex.get(word);
            return collectWordTrajectoryArrayFromTimeTree(word, timeRangeStart, timeRangeStop, allTimesOfWord);
        }
        else{
            return getZeroWordTrajectoryArray(timeRangeStop);
        }
    }

    /**
     * TODO: to be implemented
     * */
    public void deleteCountsForTimeRange(){

    }
}
