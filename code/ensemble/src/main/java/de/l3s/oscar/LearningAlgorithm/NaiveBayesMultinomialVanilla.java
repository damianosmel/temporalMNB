package de.l3s.oscar.LearningAlgorithm;

/*
 *    NaiveBayesMultinomial.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Bernhard Pfahringer (bernhard@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

import com.yahoo.labs.samoa.instances.Instance;
import de.l3s.oscar.TimeSeries.AveragePredictor;
import de.l3s.oscar.TimeSeries.BagOfWordsInTime;
import de.l3s.oscar.TimeSeries.RegressionPredictor;
import de.l3s.oscar.TimeSeries.SumPredictor;
import moa.core.Measurement;
import moa.core.StringUtils;
import com.github.javacliparser.FloatOption;

import org.joda.time.DateTime;
import weka.core.*;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

import moa.core.DoubleVector;

/**
 * <!-- globalinfo-start --> Class for building and using a multinomial Naive
 * Bayes classifier. Performs text classic bayesian prediction while making
 * naive assumption that all inputs are independent. For more information
 * see,<br/> <br/> Andrew Mccallum, Kamal Nigam: A Comparison of Event Models
 * for Naive Bayes Text Classification. In: AAAI-98 Workshop on 'Learning for
 * Text Categorization', 1998.<br/> <br/> The core equation for this
 * classifier:<br/> <br/> P[Ci|D] = (P[D|Ci] x P[Ci]) / P[D] (Bayes rule)<br/>
 * <br/> where Ci is class i and D is a document.<br/> <br/> Incremental version
 * of the algorithm.
 * <p/>
 * <!-- globalinfo-end -->
 * <!-- technical-bibtex-start --> BibTeX:
 * <pre>
 * &#64;inproceedings{Mccallum1998,
 *    author = {Andrew Mccallum and Kamal Nigam},
 *    booktitle = {AAAI-98 Workshop on 'Learning for Text Categorization'},
 *    title = {A Comparison of Event Models for Naive Bayes Text Classification},
 *    year = {1998}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 */
public class NaiveBayesMultinomialVanilla extends SlidingWindowClassifier {

    public FloatOption laplaceCorrectionOption = new FloatOption("laplaceCorrection",
            'l', "Laplace correction factor.",
            1.0, 0.00, Integer.MAX_VALUE);

    /**
     * for serialization
     */
    private static final long serialVersionUID = -7204398796974263187L;

    @Override
    public String getPurposeString() {
        return "A Multinomial Naive Bayes classifier: performs classic bayesian prediction while making naive assumption that all inputs are independent.";
    }

    /**
     * sum of weight_of_instance * word_count_of_instance for each class
     */
    protected double[] m_classTotals;

    /**
     * copy of header information for use in toString method
     */
//    protected Instances m_headerInfo;
    protected com.yahoo.labs.samoa.instances.InstancesHeader m_headerInfo;

    /**
     * number of class values
     */
    protected int m_numClasses;

    /**
     * the probability of a class (i.e. Pr[H])
     */
    protected double[] m_probOfClass;

    /**
     * probability that a word (w) exists in a class (H) (i.e. Pr[w|H]) The
     * matrix is in the this format: m_wordTotalForClass[wordAttribute][class]
     */
    protected DoubleVector[] m_wordTotalForClass;


    protected boolean reset = false;

    protected static boolean debugMNBVanilla = false;
    protected String debugLog = "=== MNB Vanilla ===" + "\n";


    /**
     * Ephemeral Entities
     * */
//    protected boolean useLoadedSentiment = false;
    protected boolean useLoadedSentiment = true;

//    protected Map<String, List<Integer>> loadedWordsSentiment = new HashMap<String, List<Integer>>();
    /**
     * ----------- Ephemeral Entities -----------
     * */
    /**
     * Ephemeral entities - 2015
     * */
    // hash map -> {"entity_i": [positiveCounts_Month_j,positiveCounts_Month_j+1,positiveCounts_Month_j+2], [negativeCounts_Month_j, negativeCounts_Month_j+1, negativeCounts_Month_j+2]}
    protected Map<String, ArrayList<ArrayList<Double>>> loadedWordsSentiment = new HashMap<String, ArrayList<ArrayList<Double>>>();

    protected int sentimentSlidingWindowSize = 2;
//    protected int sentimentSlidingWindowSize = 3;
    /**
     * Ephemeral Entities - choosing tweets with entities
     * */
//    protected boolean currentTweetHasEntity = false;
    protected boolean currentTweetHasEntity = true; //if ALL tweets have entities
    /**
     * ----------- Ephemeral Entities -----------
     * */

    /**
     * --- Time series ---
     * */
    protected BagOfWordsInTime bagOfWordsInTime;

    /**
     * SentiWordNet
     * */
    protected HashMap<String, Integer> entitiesHash = new HashMap<String, Integer>();
    protected HashMap<Integer, HashMap<String, ArrayList<Double>>> sentiWordNetSent = new HashMap<Integer, HashMap<String, ArrayList<Double>> > ();
    protected ArrayList<String> wordsNotToUse = new ArrayList<String>(Arrays.asList("null", "blktweetpreprocessing", "typostweet", "blkless3chars", "blklessfreqtweet", "blknegationtweet", "blkstopwordtweet", "meaninglesstweet"));
    protected HashMap<String, Integer> usedEntitiesHash = new HashMap<String, Integer>();
    protected int currentTweetIndex = 0;
    /**
     * --- Time series ---
     * */

    /*public void setHeaderInfo(weka.core.Instances trainingSet)
    {
//        System.out.println("hi hi!!");
        m_headerInfo = trainingSet;
    }*/

    /**
     * Samoa instances
     * */
    public void setSamoaHeaderInfo(com.yahoo.labs.samoa.instances.InstancesHeader samoaTrainingSet){
        m_headerInfo = samoaTrainingSet;
    }

    public static void setDebugMNBVanilla(boolean userDebugMNBVanilla){
//        System.out.println("inside setDebugMNBVanilla");
          debugMNBVanilla = userDebugMNBVanilla;
//        System.out.println("debugMNBVanilla: " + Boolean.toString(debugMNBVanilla));
    }

    public String getThenCleanDebugLog(){
        String currentDebugLog = this.debugLog + "=== MNB Vanilla ===" + "\n";
        this.debugLog = "=== MNB Vanilla ===" + "\n";

        return currentDebugLog;
    }


    @Override
    public boolean getResetClassifierFlag(){
        return this.reset;
    }
    @Override
    public double [] getProbOfClass(){
        return this.m_probOfClass;
    }

    @Override
    public moa.core.DoubleVector [] getWordTotalForClass(){
        return this.m_wordTotalForClass;
    }

    @Override
    public double [] getClassTotals(){
        return this.m_classTotals;
    }

    /**
     * Ephemeral entities - month update
     * */
    public void prepareCounts(Instance instance){
        double laplace = this.laplaceCorrectionOption.getValue();
        this.m_numClasses = instance.numClasses();

        this.m_probOfClass = new double[this.m_numClasses];
        this.m_probOfClass[0] = 0.0D;
        for(int classIndex = 1; classIndex < this.m_numClasses; classIndex++){
            this.m_probOfClass[classIndex] = laplace;
        }

        int numAttributes = instance.numAttributes();
        this.m_classTotals = new double[this.m_numClasses];
        Arrays.fill(this.m_classTotals, laplace * numAttributes);

        this.m_wordTotalForClass = new DoubleVector[this.m_numClasses];
        for (int i = 0; i < this.m_numClasses; i++) {
            //Arrays.fill(wordTotal, laplace);
            this.m_wordTotalForClass[i] = new DoubleVector();
        }

        this.reset = false;
    }

    //TODO: May be it is printProbOfClass not set .. Double-check!!
    public void setProbOfClass(double [] probOfClass){
        System.arraycopy(probOfClass,0,this.m_probOfClass,0,this.m_numClasses);

/*        System.out.println("=== ===");
        System.out.println("probOfClass: " + Arrays.toString(this.m_probOfClass));
        System.out.println("=== ===");*/
    }

    public void setWordTotalForClass(moa.core.DoubleVector [] wordTotalForClass){
        this.m_wordTotalForClass = wordTotalForClass.clone();
        /*System.out.println("=== ===");
        System.out.println("wordTotalForClass: " + this.m_wordTotalForClass.toString());
        System.out.println("=== ===");*/
    }

    public void setClassTotals(double [] classTotals){
        System.arraycopy(classTotals,0,this.m_classTotals,0, this.m_numClasses);
//        this.m_classTotals = classTotals;
        /*System.out.println("=== ===");
        System.out.println("classTotals: " + this.m_classTotals);
        System.out.println("=== ===");*/
    }

    /**
     * --- Time series ---
     * */
    public void initializeWordTimeSeries(){
        int aggregationPeriod = 5;
        String aggregationGranularity = "minute";
        this.bagOfWordsInTime = new BagOfWordsInTime(this.m_numClasses, aggregationPeriod, aggregationGranularity);
    }

    public BagOfWordsInTime getBagOfWordsInTime() {return this.bagOfWordsInTime;}

    public void printWordsTimeSeries(){
        this.bagOfWordsInTime.printTimeTree();
        this.bagOfWordsInTime.printWordToTimeInvertedIndex();
    }

    /**
     * Entities
     * */
    public void createEntitiesHashFromList(ArrayList<String> entitiesList){
        this.entitiesHash = new HashMap<String, Integer>();

        for (String entity : entitiesList){
            this.entitiesHash.putIfAbsent(entity, 1);
        }
    }
    /**
     * --- Time series ---
     * */

    public void createHashWithSentiWordNetFromFile(String sentiWordNetSentimentPath){
        int tweetIndex;

        //read for each tweet the sentiment of ephemeral words from SentiWordNet
        try{
            FileInputStream fileStreamCSV = new FileInputStream(sentiWordNetSentimentPath);
            DataInputStream dataInputCSV = new DataInputStream(fileStreamCSV);
            BufferedReader bufferedReaderCSV = new BufferedReader(new InputStreamReader(dataInputCSV));
            String currentLine;

            double sentimentPositiveCounts = 0.0D;
            double sentimentNegativeCounts = 0.0D;

            while((currentLine = bufferedReaderCSV.readLine()) != null){
//                System.out.println("reading new line ");
                sentimentPositiveCounts = 0.0D;
                sentimentNegativeCounts = 0.0D;
                StringTokenizer splitLineIntoTabs = new StringTokenizer(currentLine, ",");
                tweetIndex = Integer.parseInt(splitLineIntoTabs.nextToken()); //.replaceAll("\\(","")
                String restOfLine = splitLineIntoTabs.nextToken();

                if(!this.sentiWordNetSent.containsKey(tweetIndex)){
                    this.sentiWordNetSent.put(tweetIndex, new HashMap<String, ArrayList<Double>>());
                }
                
                StringTokenizer splitRestIntoTabs = new StringTokenizer(restOfLine, "\t");
                splitRestIntoTabs.nextToken(); //POS not used
                String word = splitRestIntoTabs.nextToken().trim();
                if( !(this.wordsNotToUse.contains(word))){
                    //this word will be found for the tweet only once so no need to check for containsKey()

                    ArrayList<Double> sentimentCounts = new ArrayList<Double>();
                    sentimentPositiveCounts = Double.parseDouble(splitRestIntoTabs.nextToken());
                    sentimentNegativeCounts = Double.parseDouble(splitRestIntoTabs.nextToken()); //.replaceAll("\\)","")

                    sentimentCounts.add(sentimentPositiveCounts);
                    sentimentCounts.add(sentimentNegativeCounts);

//                    this.loadedWordsSentiment.putIfAbsent(loadedWord, sentimentCounts);
                    this.sentiWordNetSent.get(tweetIndex).putIfAbsent(word, sentimentCounts);
                }
            }
            dataInputCSV.close();
        }
        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        /**
         * print SentiWordNet Hash
         * */
        /*for (int key : this.sentiWordNetSent.keySet()){
            System.out.println("key: " + key);

            for (String word : this.sentiWordNetSent.get(key).keySet()){
                System.out.println("word: " + word);
                for (double classCount : this.sentiWordNetSent.get(key).get(word)){
                    System.out.println("classCount: " + classCount);
                }
            }
        }*/
    }
    /**
     * mark all words used from the hash table in the classification
     * */
    public void markWordAsUsedInHash(String usedWord){
        /*if(this.entitiesHash.get(usedWord).intValue() == 0){
            this.entitiesHash.put(usedWord, 1);
        }*/
        if(!this.usedEntitiesHash.containsKey(usedWord)){
            this.usedEntitiesHash.put(usedWord,1);
        }
    }

    /**
     * Get total number of used words from the hash
     * */
    public int getTotalNumberOfUsedWordsFromHash(){
        int totalNumberOfUsedWordsFromHash = 0;
        /*for (String key : this.entitiesHash.keySet()){
            if (entitiesHash.get(key).intValue() == 1){
                totalNumberOfUsedWordsFromHash++;
            }
        }
        return totalNumberOfUsedWordsFromHash;
        */
        return this.usedEntitiesHash.size();
    }

    /**
     * Saving each word of entities (not found in the SentiWordNet)
     * in a hash table with (key, value) -> (word, 0)
     * @param entitiesNotInDictPath
     */
    public void createEntitiesHashFromFile(String entitiesNotInDictPath){
        String currentWord = new String();
        //read word per line and save it in the hash
        try{
            FileInputStream fileStreamCSV = new FileInputStream(entitiesNotInDictPath);
            DataInputStream dataInputCSV = new DataInputStream(fileStreamCSV);
            BufferedReader bufferedReaderCSV = new BufferedReader(new InputStreamReader(dataInputCSV));
            String currentLine;

            while((currentLine = bufferedReaderCSV.readLine()) != null){
                StringTokenizer splitLineIntoTabs = new StringTokenizer(currentLine, "\t");
                currentWord = splitLineIntoTabs.nextToken().trim();
                if (!this.entitiesHash.containsKey(currentWord) ){
//                    System.out.println("adding word " + currentWord);
                    this.entitiesHash.put(currentWord, 0);
                }
            }
            dataInputCSV.close();
        }
        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
        }
    }

/*

    protected String currentInstanceTimeStamp;


    public String getCurrentInstanceTimeStamp(){
        return this.currentInstanceTimeStamp;
    }

    public void setCurrentInstanceTimeStamp(String currentInstanceTimeStamp){
        this.currentInstanceTimeStamp = currentInstanceTimeStamp;
    }
*/

    /**
     * Ephemeral entities
     * */

    /**
     * Ephemeral Entities
     * Function to
     * read sentiment scores for each specified word from the csv file
     * and save them into a hash map of {.. , "word_i" -> [numberOfPositives, numberOfNegatives],.. }
     * @param csvAbsolutePath              String, absolute path for the csv file
     *
     * @return void
     * @since May 2017
     * @author Damianos Melidis
     */
    @Override
    public void loadWordSentimentFromCSV(String csvAbsolutePath, boolean isFormat4Average){
        int tokensCounter = 0;
        double positivesCount = 0.0D;
        double negativesCount = 0.0D;
        String loadedWord = new String();
        //load to the data structure
        try{
            FileInputStream fileStreamCSV = new FileInputStream(csvAbsolutePath);
            DataInputStream dataInputCSV = new DataInputStream(fileStreamCSV);
            BufferedReader bufferedReaderCSV = new BufferedReader(new InputStreamReader(dataInputCSV));
            String currentLine;

            while((currentLine = bufferedReaderCSV.readLine()) != null){

             /*   System.out.println("--- ---");
                System.out.println("currentLine: " + currentLine);
*/
                //create a tokenizer to split each read line
                StringTokenizer splitLineIntoTabs = new StringTokenizer(currentLine, "\t");
//                StringTokenizer splitLineIntoTabs = new StringTokenizer(currentLine, ",");
                String currentToken = "";
//                System.out.println("split lines: " + Integer.toString(splitLineIntoTabs.countTokens()));
                /**
                 * add the word positives and negative counts into the dictionary
                 * */
                while(splitLineIntoTabs.hasMoreTokens()){

                    currentToken = splitLineIntoTabs.nextToken();
  /*                  System.out.println("~~~");
                    System.out.println("the token: " + currentToken);
                    System.out.println(" the token counter: " + Integer.toString(tokensCounter));
  */                  if(tokensCounter == 1){
                        loadedWord = currentToken;
//                        System.out.println("the loaded word: " + loadedWord);
                        ArrayList<ArrayList<Double>> sentimentCounts = new ArrayList<ArrayList<Double>>();
                        ArrayList<Double> sentimentPositiveCounts = new ArrayList<Double>();
                        ArrayList<Double> sentimentNegativeCounts = new ArrayList<Double>();
                        sentimentCounts.add(sentimentPositiveCounts);
                        sentimentCounts.add(sentimentNegativeCounts);

                        this.loadedWordsSentiment.putIfAbsent(loadedWord, sentimentCounts);
                    }
                    else if (tokensCounter == 4 && isFormat4Average == true){
                        positivesCount = Double.parseDouble(currentToken);
//                        System.out.println("positive count: " + Double.toString(positivesCount));
//                        this.loadedWordsSentiment.get(loadedWord).get(0).add(0.0D);
                        this.loadedWordsSentiment.get(loadedWord).get(0).add(positivesCount);
                    }
                    else if (tokensCounter == 5 && isFormat4Average == true){
                        negativesCount = Double.parseDouble(currentToken);
//                        System.out.println("negative count: " + Double.toString(negativesCount));
                        this.loadedWordsSentiment.get(loadedWord).get(1).add(negativesCount);
                    }
                    else if (tokensCounter == 3 && isFormat4Average == false){
                        positivesCount = Double.parseDouble(currentToken);
                        this.loadedWordsSentiment.get(loadedWord).get(0).add(positivesCount);
                    }
                    else if (tokensCounter == 4 && isFormat4Average == false){
                        negativesCount = Double.parseDouble(currentToken);
                        this.loadedWordsSentiment.get(loadedWord).get(1).add(negativesCount);
                    }

                    tokensCounter++;
//                    System.out.println("~~~");
                }//close while for each token in the csv line

                /**
                 * slide the window of sentiments for current word
                 * */
                if(this.loadedWordsSentiment.get(loadedWord).get(0).size() == this.sentimentSlidingWindowSize + 1){
                    System.out.println("===");
                    System.out.println("slide the window of positives");
                    this.loadedWordsSentiment.get(loadedWord).get(0).remove(0);
                }
                if(this.loadedWordsSentiment.get(loadedWord).get(1).size() == this.sentimentSlidingWindowSize + 1){
                    System.out.println("slide the window of negatives");
                    System.out.println("===");
                    this.loadedWordsSentiment.get(loadedWord).get(1).remove(0);
                }
                tokensCounter = 0;
                loadedWord = "";
//                System.out.println("--- ---");
            }//close while for each line(word sentiment statistics) in the csv file

            dataInputCSV.close();
        } catch( Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        /**
         * Debug: print the map of sentiments
         * */
        /*System.out.println("=== ===");
        System.out.println("the map: ");
        for(Map.Entry wordEntry : this.loadedWordsSentiment.entrySet()){
            System.out.println(wordEntry.getKey() + ", " + wordEntry.getValue());
        }
        System.out.println("=== ===");
*/
        //for this month the loaded sentiments will be used
        /**
         * De-activate for base line
         * */
        this.useLoadedSentiment = true;
//        this.useLoadedSentiment = false;
    }

    /**
     * Ephemeral entities
     * */
    @Override
    public boolean getCurrentTweetHasEntity(){
        return this.currentTweetHasEntity;
    }

    /**
     * Ephemeral entities
     * */
    @Override
    public void setCurrentTweetHasEntity(boolean currentTweetHasEntity){
        this.currentTweetHasEntity = currentTweetHasEntity;
    }

    @Override
    public void discardInstanceFromModel(Instance instance) {

        if(debugMNBVanilla){
           System.out.println("~~~start of discard instance of model~~~");
           this.debugLog += "\n=== Discard Instance of Model ===" + "\n";
        }

        /**
         * --- Reset model ---
         * */
        if (this.reset == true){
            System.out.println("Reset flag is TRUE for discardInstanceFromModel");
            System.out.println("Following the reset code of trainOnInstance");
            this.debugLog += "Reset flag is TRUE for discardInstanceFromModel" + "\n";
            this.debugLog += "Following the reset code of trainOnInstance" + "\n";

            this.m_numClasses = instance.numClasses();

            double laplace = this.laplaceCorrectionOption.getValue();
            int numAttributes = instance.numClasses();

            //reset the prior probability of the class
            m_probOfClass = new double[m_numClasses];
            Arrays.fill(m_probOfClass, laplace);

            //reset the total support of words for a class
            m_classTotals = new double[m_numClasses];
            Arrays.fill(m_classTotals, laplace * numAttributes);

            //reset the P(word_i|Class_j)
            m_wordTotalForClass = new DoubleVector[m_numClasses];
            for(int i=0; i < m_numClasses; i++){
                m_wordTotalForClass[i] = new DoubleVector();
            }
            this.reset = false;
        }
        /**
         * --- Reset model ---
         * */

        //subtract word counts of the instance from the model
        double laplace = this.laplaceCorrectionOption.getValue();

        int classIndex = instance.classIndex();
        int classValue = (int) instance.value(classIndex);
//        double instanceWeight = 1.0;
        double instanceWeight = instance.weight();
        int numAttributes = instance.numAttributes();
        //subtract the instance weight from current prior of the instance class
        m_probOfClass[classValue] -= instanceWeight;
        if (m_probOfClass[classValue] <= 1.0D){ //1.0D
            /**
             * System.out.println("removing all from class " + classValue);
            System.out.println(this.laplaceCorrectionOption.getValue());*/
            m_probOfClass[classValue] = this.laplaceCorrectionOption.getValue();
//                m_probOfClass[classValue] = 0.0D;
        }

        //subtract the total support of words of the instance for a class
        m_classTotals[classValue] -= instanceWeight * totalSize(instance);
            if (m_classTotals[classValue] <= 0.0D){
//                m_classTotals[classValue] = this.laplaceCorrectionOption.getValue();
                m_classTotals[classValue] = this.laplaceCorrectionOption.getValue() * numAttributes;
            }

            //subtract the counts for the words of the instance from P(word_i|class_j)
            for(int i=0; i< instance.numValues(); i++){
                int index = instance.index(i);

                if(index != classIndex && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")){
                    double laplaceCorrection = 0.0;

                    if(debugMNBVanilla){
                        this.debugLog += "=== Attribute ===" + "\n";
                        /*this.debugLog += "i = " + Integer.toString(i) + "\n";
                        this.debugLog += "index = " + Integer.toString(index) + "\n";*/
                    }

                    if (m_wordTotalForClass[classValue].getValue(index) == 0.0D) {
                        laplaceCorrection = this.laplaceCorrectionOption.getValue();
                    }
                    m_wordTotalForClass[classValue].addToValue(index, (-1.0)* instanceWeight * instance.valueSparse(i)); // + laplaceCorrection

                    if (m_wordTotalForClass[classValue].getValue(index) <= 0.0D){
                        m_wordTotalForClass[classValue].setValue(index,this.laplaceCorrectionOption.getValue());
                    }
                    if(debugMNBVanilla){
                        this.debugLog += "m_wordTotalForClass[" + Integer.toString(classValue) + "][" + instance.attribute(index).name() + "]= " + Double.toString(m_wordTotalForClass[classValue].getValue(index)) + "\n";
                    }
                    if(debugMNBVanilla){
                        this.debugLog += "=== Attribute ===" + "\n";
                    }
                }
            }

        if (debugMNBVanilla){
            System.out.println("~~~end of discard instance of model~~~");
            StringBuilder currentDebugLog = new StringBuilder();
            currentDebugLog.append(currentDebugLog);
//            this.getModelDescription(currentDebugLog, 1); //not for whole data set
            this.debugLog += currentDebugLog.toString();
            this.debugLog += "=== Discard Instance of Model ===" + "\n";
        }
    }

    @Override
    public void resetLearningImpl() {
        this.reset = true;
    }


    public void trainOnInstanceWithWordTimeSeries(Instance instance, DateTime instanceDateTime) {

//        DateTime instanceDateTime = new DateTime( (long) instance.weight() * 1000);
//        System.out.println("train time: " + instanceDateTime.toString());

        double laplaceCorrection = 0.0D;
        this.debugLog += "\n=== TrainOnInstance ===" + "\n";

        if (debugMNBVanilla) {
            System.out.println("== start of train==");
            this.debugLog += "\n=== TrainOnInstance ===" + "\n";
        }

        /**
         * --- Reset model ---
         * */
        if (this.reset == true) {
//            System.out.println("Setting up number of classes " + instance.numClasses());
            this.m_numClasses = instance.numClasses();
            /**
             * set the number of classes for the word time-series
             * */
            this.bagOfWordsInTime.setNumberOfClasses(this.m_numClasses);
            if (debugMNBVanilla) {
                this.debugLog += "Reset mode: ON!\n";

                this.debugLog += "number of classes: " + Integer.toString(this.m_numClasses) + "\n";
            }
            laplaceCorrection = this.laplaceCorrectionOption.getValue();
            int numAttributes = instance.numAttributes();
            if (debugMNBVanilla) {
                this.debugLog += "number of attributes: " + Integer.toString(numAttributes) + "\n";
            }
            /**
             * Samoa instance
             * */
//            m_probOfClass = new double[m_numClasses];
//            Arrays.fill(m_probOfClass, laplace);

            /**
             * weka instance
             */
            m_probOfClass = new double[m_numClasses];
            m_probOfClass[0] = 0.0D;
            for (int classIndex = 1; classIndex < m_numClasses; classIndex++) {
                m_probOfClass[classIndex] = laplaceCorrection;
//                m_probOfClass[classIndex] = 0.0D;
            }

//            Arrays.fill(m_probOfClass, 0.0); //TODO: Damian
            m_classTotals = new double[m_numClasses];
            Arrays.fill(m_classTotals, laplaceCorrection * numAttributes);

            m_wordTotalForClass = new DoubleVector[m_numClasses];
            for (int i = 0; i < m_numClasses; i++) {
                //Arrays.fill(wordTotal, laplace);
                m_wordTotalForClass[i] = new DoubleVector();
            }
            this.reset = false;
            if (debugMNBVanilla) {
                StringBuilder currentDebugLog = new StringBuilder();
                currentDebugLog.append(currentDebugLog);
//                this.getModelDescription(currentDebugLog, 1);
                this.debugLog += currentDebugLog.toString();
                this.debugLog += "=== TrainOnInstance ===" + "\n";
            }
        }
        /**
         * --- Reset model ---
         * */

        // Update classifier
        int classIndex = instance.classIndex();
        int classValue = (int) instance.value(classIndex);
        if (debugMNBVanilla) {
            this.debugLog += "classIndex: " + Integer.toString(classIndex) + "\n";
            this.debugLog += "classValue: " + Integer.toString(classValue) + "\n";
            this.debugLog += "classValue(): " + Double.toString(instance.classValue()) + "\n";
        }
        //double w = inst.weight();
        double w = 1.0;
        m_probOfClass[classValue] += w;

        m_classTotals[classValue] += w * totalSize(instance);
        double total = m_classTotals[classValue];
        double sumSentimentSlidingWindow = 0.0D;
        boolean firstAttributeInInstance = true;
        double attributeValue;
        for (int i = 0; i < instance.numValues(); i++) {

            int index = instance.index(i);

            if (index != classIndex && !instance.isMissing(i)) { //&& !instance.attribute(i).name().equalsIgnoreCase("tweetDate")) {
                if (debugMNBVanilla) {
                    this.debugLog += "=== Attribute ===" + "\n";
                }

                String attributeName = instance.attribute(index).name();
                laplaceCorrection = 0.0;
                if (m_wordTotalForClass[classValue].getValue(index) == 0) {
                    laplaceCorrection = this.laplaceCorrectionOption.getValue();
                }
                m_wordTotalForClass[classValue].addToValue(index, w * instance.valueSparse(i) + laplaceCorrection);

                /**
                 * --- Time series ---
                 * */


                attributeValue = w * instance.valueSparse(i) + laplaceCorrection;
                /**
                 * for specified entities
                 * */
//                System.out.println(this.useLoadedSentiment);
//                System.out.println(attributeName);
                if(this.sentiWordNetSent.get(this.currentTweetIndex).containsKey(attributeName)  && this.useLoadedSentiment == true){ //&& this.entitiesHash.containsKey(attributeName)
//                System.out.println("in training " + attributeName);
                    /**
                 * all words
                 * */
//                if(this.useLoadedSentiment == true){
//                    System.out.println("train: add counts for entity: " + attributeName );
//                    attributeValue = w * instance.valueSparse(i) + laplaceCorrection;
                    /**
                     * get sentiment from the SentiWordNet
                     * */
//                    System.out.println("in training  " + attributeName);
/*                    ArrayList<Double> sentiWordNetSentiments = this.sentiWordNetSent.get(this.currentTweetIndex).get(attributeName);
                    if(sentiWordNetSentiments.get(0) != 0){
                        //positive class
                        this.bagOfWordsInTime.addWordInTime(instanceDateTime, attributeName, sentiWordNetSentiments.get(0),w * totalSize(instance), 1, firstAttributeInInstance);
//                        System.out.println(attributeName + " is " + "in positive class " + sentiWordNetSentiments.get(0));
                    }
                    else if( sentiWordNetSentiments.get(1) != 0){
                        //negative class
                        this.bagOfWordsInTime.addWordInTime(instanceDateTime, attributeName, sentiWordNetSentiments.get(1),w * totalSize(instance), 2, firstAttributeInInstance);
//                        System.out.println(attributeName + " is " + "in negative class " + sentiWordNetSentiments.get(1));
                    }*/
                    /**
                     * get sentiment from distant supervision
                     * */
                  this.bagOfWordsInTime.addWordInTime(instanceDateTime, attributeName, attributeValue,w * totalSize(instance), classValue, firstAttributeInInstance);

                    firstAttributeInInstance = false;
                }
                /**
                 * --- Time series ---
                 * */
            }
        }
        this.currentTweetIndex++;
    }

    /**
     * Trains the classifier with the given instance.
     */
    @Override
    public void trainOnInstanceImpl(Instance instance) {

        double laplaceCorrection = 0.0D;

        /*System.out.println("== start of train==");
        System.out.println("=== ===");
        System.out.println("the map: ");
        for(Map.Entry wordEntry : this.loadedWordsSentiment.entrySet()){
            System.out.println(wordEntry.getKey() + ", " + wordEntry.getValue());
        }
        System.out.println("=== ===");*/

        this.debugLog += "\n=== TrainOnInstance ===" + "\n";

        if(debugMNBVanilla){
            System.out.println("== start of train==");
            this.debugLog += "\n=== TrainOnInstance ===" + "\n";
        }

        /**
         * --- Reset model ---
         * */
        if (this.reset == true) {
//            System.out.println("Setting up number of classes " + instance.numClasses());
            this.m_numClasses = instance.numClasses();
            if(debugMNBVanilla) {
                this.debugLog += "Reset mode: ON!\n";

                this.debugLog += "number of classes: " + Integer.toString(this.m_numClasses) + "\n";
            }
            laplaceCorrection = this.laplaceCorrectionOption.getValue();
            int numAttributes = instance.numAttributes();
            if(debugMNBVanilla) {
                this.debugLog += "number of attributes: " + Integer.toString(numAttributes) + "\n";
            }
            /**
             * Samoa instance
             * */
//            m_probOfClass = new double[m_numClasses];
//            Arrays.fill(m_probOfClass, laplace);

            /**
             * weka instance
             */
            m_probOfClass = new double[m_numClasses];
//            m_probOfClass[0] = 0.0D;
            for(int classIndex = 0; classIndex < m_numClasses; classIndex++){
                m_probOfClass[classIndex] = laplaceCorrection;
//                m_probOfClass[classIndex] = 0.0D;
            }

//            Arrays.fill(m_probOfClass, 0.0); //TODO: Damian
            m_classTotals = new double[m_numClasses];
            Arrays.fill(m_classTotals, laplaceCorrection * numAttributes);

            m_wordTotalForClass = new DoubleVector[m_numClasses];
            for (int i = 0; i < m_numClasses; i++) {
                //Arrays.fill(wordTotal, laplace);
                m_wordTotalForClass[i] = new DoubleVector();
            }
            this.reset = false;
            if(debugMNBVanilla){
                StringBuilder currentDebugLog = new StringBuilder();
                currentDebugLog.append(currentDebugLog);
//                this.getModelDescription(currentDebugLog, 1);
                this.debugLog += currentDebugLog.toString();
                this.debugLog += "=== TrainOnInstance ===" + "\n";
            }
        }
        /**
         * --- Reset model ---
         * */

        // Update classifier
        int classIndex = instance.classIndex();
        int classValue = (int) instance.value(classIndex);
        if(debugMNBVanilla) {
            this.debugLog += "classIndex: " + Integer.toString(classIndex) + "\n";
            this.debugLog += "classValue: " + Integer.toString(classValue) + "\n";
            this.debugLog += "classValue(): " + Double.toString(instance.classValue()) + "\n";
        }
        double w = instance.weight();
//        double w = 1.0;
        m_probOfClass[classValue] += w;

        m_classTotals[classValue] += w * totalSize(instance);
        double total = m_classTotals[classValue];
        double sumSentimentSlidingWindow = 0.0D;
        for (int i = 0; i < instance.numValues(); i++) {

            int index = instance.index(i);

            if (index != classIndex && !instance.isMissing(i) && !instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) {
                if (debugMNBVanilla) {
                    this.debugLog += "=== Attribute ===" + "\n";
                }

                String attributeName = instance.attribute(index).name();
/*

                laplaceCorrection = 0.0;
                if (m_wordTotalForClass[classValue].getValue(index) == 0) {
                    laplaceCorrection = this.laplaceCorrectionOption.getValue();
                }
                m_wordTotalForClass[classValue].addToValue(index, w * instance.valueSparse(i) + laplaceCorrection);

*/
                /**
                 * ---------------- Ephemeral entities ----------------
                 * */

                /*System.out.println("the word of tweet is " + )
                System.out.println("tweet has entity " + Boolean.toString(this.loadedWordsSentiment.containsKey(instance.attribute(i).name())));*/
                if (this.useLoadedSentiment && this.loadedWordsSentiment.containsKey(attributeName)) {

                    /**
                     * Ephemeral entities - 1.6 Mil
                     * */
                    /**
                     * make "dirty" update for words found in the lexicon
                     * *//*
 *//*                   List<Integer> updatedCountsLoadedWords = this.loadedWordsSentiment.get(attributeName);
                    assert classValue <= updatedCountsLoadedWords.size() : "The class value is bigger than the size of the loaded words sentiment";
//                    int increasedValue = (int) (w * instance.valueSparse(i));
                    updatedCountsLoadedWords.set(classValue-1, updatedCountsLoadedWords.get(classValue-1) + (int) (w * instance.valueSparse(i)) );
                    this.loadedWordsSentiment.put(attributeName, updatedCountsLoadedWords);
*//*

                    *//**
                     * vanilla update of the conditional counts of the word from loaded sentiment
                     * *//*
//                    double conditionalCountOfWord4Sentiment = (double) this.loadedWordsSentiment.get(attributeName).get(classValue-1);
                  *//**
                   * Ephemeral entities - 2015
                   * */
                  //sum and average the sliding window of target sentiment
                  /*System.out.println("=== trainOnInstance ===");
                  System.out.println("for the word: " + attributeName);*/
                  assert this.loadedWordsSentiment.get(attributeName).get(classValue-1).size() > 0 : "Current word has less than 1 month sliding window size";
                  for(int slidingWindowMonthIndex = 0; slidingWindowMonthIndex < this.loadedWordsSentiment.get(attributeName).get(classValue-1).size(); slidingWindowMonthIndex++){
                      sumSentimentSlidingWindow +=  this.loadedWordsSentiment.get(attributeName).get(classValue-1).get(slidingWindowMonthIndex);
//                      System.out.println("we use the value: " + Double.toString(this.loadedWordsSentiment.get(attributeName).get(classValue-1).get(slidingWindowMonthIndex)));
                  }
//                  System.out.println("=== trainOnInstance ===");
                  double conditionalCountOfWord4Sentiment = sumSentimentSlidingWindow / this.loadedWordsSentiment.get(attributeName).get(classValue-1).size();
                  System.out.println("=== ===");
                  System.out.println(" for the word: " + instance.attribute(i).name());
                  System.out.println(" MNB had the count: " + Double.toString(m_wordTotalForClass[classValue].getValue(index)));
                  System.out.println(" We will use now: " + Double.toString(conditionalCountOfWord4Sentiment));
                  System.out.println("=== ===");
                  m_wordTotalForClass[classValue].addToValue(index, conditionalCountOfWord4Sentiment);
                  /**
                   * Ephemeral entities - only tweets with entities
                   */
//                    System.out.println("after training, current tweet does not have entity");
                    //ALL tweets have entities
//                    this.currentTweetHasEntity = false;
//                    this.currentTweetHasEntity = true;

                  sumSentimentSlidingWindow = 0.0D; //for next word
                }
                else { //if we don't use loaded sentiments
                    laplaceCorrection = 0.0;
                    if (m_wordTotalForClass[classValue].getValue(index) == 0) {
                        laplaceCorrection = this.laplaceCorrectionOption.getValue();
                    }
                    m_wordTotalForClass[classValue].addToValue(index, w * instance.valueSparse(i) + laplaceCorrection);
                    /*
                     * Ephemeral entities - only tweets with entities - base line
                     */
                    /**
                     * Ephemeral entities - 2015
                     * ALL tweets have entities
                     * */
                    /*if(this.loadedWordsSentiment.containsKey(attributeName)){
//                        System.out.println("now tweet is out..");
                        this.currentTweetHasEntity = false;

                    }*/
                }
                /**
                 * ---------------- Ephemeral entities ----------------
                 * */

                if(debugMNBVanilla){
                    this.debugLog += "m_wordTotalForClass[" + Integer.toString(classValue) + "][" + instance.attribute(index).name() + "]= " + Double.toString(m_wordTotalForClass[classValue].getValue(index)) + "\n";
                    this.debugLog += "=== Attribute ===" + "\n";
                }
            }
        }
        if(debugMNBVanilla){
            StringBuilder currentDebugLog = new StringBuilder();
            currentDebugLog.append(currentDebugLog);
            this.getModelDescription(currentDebugLog, 1);
            this.debugLog += currentDebugLog.toString(); //not for whole data set
            this.debugLog += "=== TrainOnInstance ===" + "\n";
        }
//        System.out.println(" ===  TrainOnInstance ===");
    }


    public double [] predictWordValueFromTimeSeries(String word, String predictorName, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName){
//        System.out.println("predicting from time series");

        if(predictorName.equalsIgnoreCase("averagePredictor")){
//            System.out.println("hoi!");
            AveragePredictor averagePredictor = new AveragePredictor();
            return averagePredictor.loadWordTrajectoryAndPredictValue(this.bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
        }
        else if(predictorName.equalsIgnoreCase("regressionPredictor")){
            RegressionPredictor regressionPredictor = new RegressionPredictor();
            return regressionPredictor.loadWordTrajectoryAndPredictValue(this.bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
        }
        else if (predictorName.equalsIgnoreCase("sumPredictor")){
            SumPredictor sumPredictor = new SumPredictor();
            return sumPredictor.loadWordTrajectoryAndPredictValue(this.bagOfWordsInTime, word, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
        }
        else{
            return new double[m_numClasses];
        }
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance using time series prediction.
     *
     * @param instance the instance to be classified
     * @param predictorName         String, the predictor to be used for the time-series
     * @param timeRangeStart        DateTime, the start of the time range for the prediction
     * @param timeRangeStop         DateTime, the end of the time range for the prediction
     * @param windowTimeSize        int, size of the window for the prediction
     * @param windowTimeName        String, name of the window for the prediction
     * @return predicted class probability distribution
     * */

    public double [] getVotesForInstanceWithWordTimeSeries(Instance instance, String predictorName, DateTime timeRangeStart, DateTime timeRangeStop, int windowTimeSize, String windowTimeName){
        /**
         * tweet has entity
         * */
        boolean documentHasEntity = false;
        if (debugMNBVanilla) {
//            System.out.println("=== start of getVotes ===");
            this.debugLog += "\n=== getVotesForInstance ===" + "\n";
            this.debugLog += "for instance: " + instance.toString() + "\n";
        }

        /**
         * --- Reset model ---
         * */
        if (this.reset == true) {
            if(debugMNBVanilla) {
                this.debugLog += "Reset mode: ON!" +"\n";
                this.debugLog += "=== getVotesForInstance ===" + "\n";
            }
            /**
             * set the number of classes for the word time series
             * */
            this.bagOfWordsInTime.setNumberOfClasses(instance.numClasses());
            /**
             * weka instances
             * */
//            System.out.println("number of classes is " + this.m_numClasses);
            return new double[3];
            /**
             * Samoa instances
             * */
//            return new double[m_numClasses];
        }
        /**
         * --- Reset model ---
         * */

        /**
         * --- Test model with instance
         * */
        double[] probOfClassGivenDoc = new double[m_numClasses];
        //time series initialization
        Arrays.fill(probOfClassGivenDoc, 0.0D);
        double totalSize = totalSize(instance);

        if(debugMNBVanilla){
            this.debugLog += "First step of posterior prob. of class given document: " + "\n";
            this.debugLog += "Probability of class given document: " + "\n";
        }
/*
        *//**
         * TODO: would you predict also the prior and total counts?
         * */
        for (int i = 0; i < m_numClasses; i++) {
            if(debugMNBVanilla) {
                this.debugLog += "=== Class " + Integer.toString(i) + " ===" +"\n";
                this.debugLog += "m_probOfClass[" + Integer.toString(i) + "]= " + Double.toString(m_probOfClass[i]) +"\n";
                this.debugLog += "m_classTotals[" + Integer.toString(i) + "]= " + Double.toString(m_classTotals[i]) +"\n";
            }
            probOfClassGivenDoc[i] = Math.log(m_probOfClass[i]) - totalSize * Math.log(m_classTotals[i]);
            if(debugMNBVanilla) {
                this.debugLog += "probOfClassGivenDoc[" + Integer.toString(i) + "]= " + Double.toString(probOfClassGivenDoc[i]) +"\n";
                this.debugLog += "=== Class " + Integer.toString(i) + " ===" + "\n";
            }
        }

        /**
         * --- Time series ---
         * */
        double [] predictedClassConditionalWordCounts = new double[m_numClasses];
        for (int i = 0; i < instance.numValues(); i++) {
            int index = instance.index(i);
            if (index == instance.classIndex() || instance.isMissing(i)) { //|| instance.attribute(i).name().equalsIgnoreCase("tweetDate")
                //System.out.println("At getVotesForInstance(): skipping class or date at index " + Integer.toString(i));
                continue;
            }
            if (debugMNBVanilla) {
                this.debugLog += "=== Attribute " + Integer.toString(i) + " ===" + "\n";
            }


            double wordCount = instance.valueSparse(i);

            String attributeName = instance.attribute(index).name();

            /**
             * --- Time series ---
             * for specified entities
             * */
            if(this.sentiWordNetSent.get(this.currentTweetIndex).containsKey(attributeName) && this.useLoadedSentiment == true) { //this.entitiesHash.containsKey(attributeName) //this.sentiWordNetSent.get(this.currentTweetIndex).containsKey(attributeName)
//                System.out.println("yes in testing: " + attributeName);

            /**
             * for all words use the time series prediction
             * */
//            if(this.useLoadedSentiment == true){
//                System.out.println("test: Predicting value for entity: " + attributeName);
                predictedClassConditionalWordCounts = predictWordValueFromTimeSeries(attributeName, predictorName, timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
//                documentHasEntity = true;
                markWordAsUsedInHash(attributeName); //mark the word as used
            }


            for (int c = 0; c < m_numClasses; c++) {
                if (debugMNBVanilla) {
                    this.debugLog += "=== Class " + Integer.toString(c) + " ===" + "\n";
                }

                double value;

                /**
                 * TODO: add prediction of value
                 * */
//                if(this.entitiesHash.containsKey(attributeName) && this.useLoadedSentiment == true){
                /**
                 * for all entities
                 * */
                if(this.sentiWordNetSent.get(this.currentTweetIndex).containsKey(attributeName) && this.useLoadedSentiment == true){ //this.entitiesHash.containsKey(attributeName)
                    /**
                     * --- Time series ---
                     * */
                    value = predictedClassConditionalWordCounts[c];

                    if(Double.isNaN(value) || value < 0){
                        //correct when word is not found in the time-series
                        value = 0.0;
                    }
//                System.out.println("class c " + c + " -> predicted value: " + value);
                    /**
                     * --- Time series ---
                     * */
                }
                else {
                    value = m_wordTotalForClass[c].getValue(index);
                }
                probOfClassGivenDoc[c] += wordCount * Math.log(value == 0 ? this.laplaceCorrectionOption.getValue() : value);

                if(debugMNBVanilla){
                    this.debugLog += "m_wordTotalForClass[" + Integer.toString(c) +"][" + attributeName + "]= " + Double.toString(value) +"\n";
                    this.debugLog += "probOfClassGivenDoc[" + Integer.toString(c) + "]=" + Double.toString(probOfClassGivenDoc[c]) +"\n";
                    this.debugLog += "=== Class " + Integer.toString(c) + "===" +"\n";
                }
            }//close loop for each class value
            if(debugMNBVanilla){
                this.debugLog += "=== Attribute " + Integer.toString(i) + "===" + "\n";
            }
        }//close loop for each attribute


        /**
         * Time series
         * Sum of prior and
         * */
        for (int i = 0; i < m_numClasses; i++) {
            if(debugMNBVanilla) {
                this.debugLog += "=== Class " + Integer.toString(i) + " ===" +"\n";
                this.debugLog += "m_probOfClass[" + Integer.toString(i) + "]= " + Double.toString(m_probOfClass[i]) +"\n";
                this.debugLog += "m_classTotals[" + Integer.toString(i) + "]= " + Double.toString(m_classTotals[i]) +"\n";
            }
//            if(documentHasEntity == true && this.useLoadedSentiment == true){
            /**
             * for all words
             * */
            /*if(this.useLoadedSentiment == true){
//                System.out.println("before prob: " + probOfClassGivenDoc[i]);
                ArrayList<double []> sumClassCounts = this.bagOfWordsInTime.addClassTrajectoryFromTimeTree(timeRangeStart, timeRangeStop, windowTimeSize, windowTimeName);
//                probOfClassGivenDoc[i] = Math.log(sumClassCounts.get(0)[i]) - totalSize * Math.log(sumClassCounts.get(1)[i]);
                probOfClassGivenDoc[i] += Math.log(sumClassCounts.get(0)[i]) - totalSize * Math.log(m_classTotals[i]);
//                System.out.println("prob: " + probOfClassGivenDoc[i]);
            }
            else{
                probOfClassGivenDoc[i] += Math.log(m_probOfClass[i]) - totalSize * Math.log(m_classTotals[i]);
            }*/
            if(debugMNBVanilla) {
                this.debugLog += "probOfClassGivenDoc[" + Integer.toString(i) + "]= " + Double.toString(probOfClassGivenDoc[i]) +"\n";
                this.debugLog += "=== Class " + Integer.toString(i) + " ===" + "\n";
            }
        }

        if(debugMNBVanilla) {
            this.debugLog += "\n" + "Final step of posterior prob. of class given document: " + "\n";
            double[] posteriorOfClassGivenDocument = Utils.logs2probs(probOfClassGivenDoc);
            this.debugLog += "Probability of class given document: " + "\n";
            for(int i = 0; i < m_numClasses; i++){
                this.debugLog += "Class " + Integer.toString(i) + ": " + Double.toString(posteriorOfClassGivenDocument[i]) + "\n";
            }
            this.debugLog += "=== getVotesForInstance ===" + "\n";
        }
//        System.out.println("=== end of getVotes ===");
        return Utils.logs2probs(probOfClassGivenDoc);
    }

    /**
     * Calculates the class membership probabilities for the given test
     * instance.
     *
     * @param instance the instance to be classified
     * @return predicted class probability distribution
     */


    //TODO: Change currentTweetHasEntities to true
    @Override
    public double[] getVotesForInstance(Instance instance) {
//        System.out.println("=== start of getVotes ===");
        if (debugMNBVanilla) {
//            System.out.println("=== start of getVotes ===");
            this.debugLog += "\n=== getVotesForInstance ===" + "\n";
            this.debugLog += "for instance: " + instance.toString() + "\n";
        }

        /**
         * --- Reset model ---
         * */
        if (this.reset == true) {
            if(debugMNBVanilla) {
                this.debugLog += "Reset mode: ON!" +"\n";
                this.debugLog += "=== getVotesForInstance ===" + "\n";
            }

            //TODO: remove it if not needed
            /*
            for (int i = 0; i < instance.numValues(); i++) {
                int index = instance.index(i);
                if (index == instance.classIndex() || instance.isMissing(i) ) { //|| instance.attribute(i).name().equalsIgnoreCase("tweetDate")
                    //System.out.println("At getVotesForInstance(): skipping class or date at index " + Integer.toString(i));
                    continue;
                }
                String attributeName = instance.attribute(index).name();

            }*/
            /**
             * weka instances
             * */
//            System.out.println("number of classes is " + this.m_numClasses);

//            return new double[this.m_numClasses];
            return new double[instance.numClasses()];
            /**
             * Samoa instances
             * */
//            return new double[m_numClasses];
        }
        /**
         * --- Reset model ---
         * */

        /**
         * --- Test model with instance
         * */
        double[] probOfClassGivenDoc = new double[m_numClasses];
        double totalSize = totalSize(instance);

        if(debugMNBVanilla){
            this.debugLog += "First step of posterior prob. of class given document: " + "\n";
            this.debugLog += "Probability of class given document: " + "\n";
        }

        for (int i = 0; i < m_numClasses; i++) {
            if(debugMNBVanilla) {
                this.debugLog += "=== Class " + Integer.toString(i) + " ===" +"\n";
                this.debugLog += "m_probOfClass[" + Integer.toString(i) + "]= " + Double.toString(m_probOfClass[i]) +"\n";
                this.debugLog += "m_classTotals[" + Integer.toString(i) + "]= " + Double.toString(m_classTotals[i]) +"\n";
            }
            probOfClassGivenDoc[i] = Math.log(m_probOfClass[i]) - totalSize * Math.log(m_classTotals[i]);
            if(debugMNBVanilla) {
                this.debugLog += "probOfClassGivenDoc[" + Integer.toString(i) + "]= " + Double.toString(probOfClassGivenDoc[i]) +"\n";
                this.debugLog += "=== Class " + Integer.toString(i) + " ===" + "\n";
            }
        }
        /**
         * Ephemeral entities - 2015
         * */
        double sumSentimentSlidingWindow;
        for (int i = 0; i < instance.numValues(); i++) {
            int index = instance.index(i);
            if (index == instance.classIndex() || instance.isMissing(i) || instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp") ) { //|| instance.attribute(i).name().equalsIgnoreCase("tweetDate")
                //System.out.println("At getVotesForInstance(): skipping class or date at index " + Integer.toString(i));
                continue;
            }
            if(debugMNBVanilla){
                this.debugLog += "=== Attribute " + Integer.toString(i) + " ===" +"\n";
            }

            double wordCount = instance.valueSparse(i);
            sumSentimentSlidingWindow = 0.0D;

            for (int c = 0; c < m_numClasses; c++) {
                if(debugMNBVanilla) {
                    this.debugLog += "=== Class " + Integer.toString(c) + " ===" +"\n";
                }

                String attributeName = instance.attribute(index).name();
                double value;

                /**
                 * ---------------- Ephemeral entities ----------------
                 * */
                if(this.useLoadedSentiment && this.loadedWordsSentiment.containsKey(attributeName)){
                    double conditionalCountOfWord4Sentiment;
                    if (c == 0){
                        conditionalCountOfWord4Sentiment = this.laplaceCorrectionOption.getValue();
                    }
                    else{
//                        conditionalCountOfWord4Sentiment = (double) this.loadedWordsSentiment.get(attributeName).get(c-1);
                        /**
                         * Ephemeral entities - 2015
                         */
                        /*System.out.println("=== getVotes ===");
                        System.out.println(" for the word: " + attributeName);*/
                        //sum and average the sliding window of target sentiment
                        assert this.loadedWordsSentiment.get(attributeName).get(c-1).size() > 0 : "Current word has less than 1 month sliding window size";
                        for(int slidingWindowMonthIndex = 0; slidingWindowMonthIndex < this.loadedWordsSentiment.get(attributeName).get(c-1).size(); slidingWindowMonthIndex++){
                            sumSentimentSlidingWindow +=  this.loadedWordsSentiment.get(attributeName).get(c-1).get(slidingWindowMonthIndex);
//                            System.out.println("we use the value: " + Double.toString(this.loadedWordsSentiment.get(attributeName).get(c-1).get(slidingWindowMonthIndex)));
                        }
//                        System.out.println("=== getVotes ===");
                        conditionalCountOfWord4Sentiment = sumSentimentSlidingWindow / this.loadedWordsSentiment.get(attributeName).get(c-1).size();
                    }

                    /*System.out.println("=== ===");
                    System.out.println(" for the word: " + attributeName);
                    System.out.println("for the class: " + Integer.toString(c));
                    System.out.println(" MNB had the count: " + Double.toString(m_wordTotalForClass[c].getValue(index)));
                    System.out.println(" We will use now: " + Double.toString(conditionalCountOfWord4Sentiment));
                    System.out.println("=== ===");*/

                    value = conditionalCountOfWord4Sentiment;
                    /**
                     * Ephemeral entities - only tweets with entities
                     * */
                    this.currentTweetHasEntity = true;
//                    System.out.println("current tweet has the entity: " + attributeName);
                }
                else { //if we don't use the loaded sentiments
//                    System.out.println("for tweet attributeName: " + attributeName);

                    value = m_wordTotalForClass[c].getValue(index);
                    /**
                     * Ephemeral entities - Only tweets with entities, only for base line
                     */
                    /*if(this.loadedWordsSentiment.containsKey(attributeName)){
//                        System.out.println("now tweet is in!");
                        this.currentTweetHasEntity = true;
                    }*/
//                    String attributeName = instance.attribute(index).name();

//                    probOfClassGivenDoc[c] += wordCount * Math.log(value == 0 ? this.laplaceCorrectionOption.getValue() : value);
                }
                /**
                 * ---------------- Ephemeral entities ----------------
                 * */

                probOfClassGivenDoc[c] += wordCount * Math.log(value == 0 ? this.laplaceCorrectionOption.getValue() : value);
                if(debugMNBVanilla){
                    this.debugLog += "m_wordTotalForClass[" + Integer.toString(c) +"][" + attributeName + "]= " + Double.toString(value) +"\n";
                    this.debugLog += "probOfClassGivenDoc[" + Integer.toString(c) + "]=" + Double.toString(probOfClassGivenDoc[c]) +"\n";
                    this.debugLog += "=== Class " + Integer.toString(c) + "===" +"\n";
                }
            }//close for loop for each class value

            if(debugMNBVanilla){
                this.debugLog += "=== Attribute " + Integer.toString(i) + "===" + "\n";
            }
        }//close for loop for each attribute

        if(debugMNBVanilla) {
            this.debugLog += "\n" + "Final step of posterior prob. of class given document: " + "\n";
            double[] posteriorOfClassGivenDocument = Utils.logs2probs(probOfClassGivenDoc);
            this.debugLog += "Probability of class given document: " + "\n";
            for(int i = 0; i < m_numClasses; i++){
                this.debugLog += "Class " + Integer.toString(i) + ": " + Double.toString(posteriorOfClassGivenDocument[i]) + "\n";
            }
            this.debugLog += "=== getVotesForInstance ===" + "\n";
        }
//        System.out.println("=== start of getVotes ===");
        return Utils.logs2probs(probOfClassGivenDoc);
    }

    public double totalSize(Instance instance) {
        if(debugMNBVanilla) {
//            System.out.println("== start of totalSize ==");
            this.debugLog += "\n=== totalSize ===" + "\n";
        }
        int classIndex = instance.classIndex();
        double total = 0.0;
        for (int i = 0; i < instance.numValues(); i++) {
            int index = instance.index(i);
            if (index == classIndex || instance.isMissing(i) || instance.attribute(index).name().equalsIgnoreCase("dateTimeStamp")) { //|| instance.attribute(i).name().equalsIgnoreCase("tweetDate")
//                System.out.println("At totalSize: Skipping class or date at index " + Integer.toString(i));
                continue;
            }
            double count = instance.valueSparse(i);
            if (count >= 0) {
                total += count;
            } else {
                //throw new Exception("Numeric attribute value is not >= 0. " + i + " " + index + " " +
                //		    instance.valueSparse(i) + " " + " " + instance);
            }
        }
        if (debugMNBVanilla) {
//            System.out.println("== end of totalSize ==");
            this.debugLog += "total count: " + Double.toString(total) + "\n";
            StringBuilder currentDebugLog = new StringBuilder();
            currentDebugLog.append(currentDebugLog);
            this.getModelDescription(currentDebugLog, 1);
            this.debugLog += currentDebugLog.toString();
            this.debugLog += "=== totalSize ===" + "\n";
        }

        return total;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return null;
    }

    @Override
    public void getModelDescription(StringBuilder result, int indent) {
        StringUtils.appendIndented(result, indent, "xxx MNB1 xxx\n\n");

        result.append("The independent probability of a class\n");
        result.append("--------------------------------------\n");

        for (int c = 0; c < m_numClasses; c++) {
            result.append(m_headerInfo.classAttribute().value(c)).append("\t").
                    append(Double.toString(m_probOfClass[c])).append("\n");
        }


        result.append("\n m_classTotals: sum of weight_of_instance * word_count_of_instance for each class\n");
        result.append("-----------------------------------------\n");

        for (int c = 0; c < m_numClasses; c++){
            result.append(m_headerInfo.classAttribute().value(c)).append("\t\t").append(Double.toString(m_classTotals[c])).append("\n");
        }

        result.append("\nThe probability of a word given the class\n");
        result.append("-----------------------------------------\n\t");

        for (int c = 0; c < m_numClasses; c++) {
            result.append(m_headerInfo.classAttribute().value(c)).append("\t");
        }

        result.append("\n");

        for (int w = 0; w < m_headerInfo.numAttributes(); w++) {
            if (w == m_headerInfo.classIndex()) {
                continue;
            }
            result.append(m_headerInfo.attribute(w).name()).append("\t");
            for (int c = 0; c < m_numClasses; c++) {
                double value = m_wordTotalForClass[c].getValue(w);
                if (value == 0) {
                    value = this.laplaceCorrectionOption.getValue();
                }
                result.append(value / m_classTotals[c]).append("\t");
            }
            result.append("\n");
        }
        StringUtils.appendNewline(result);
    }

    public boolean isRandomizable() {
        return false;
    }
}
