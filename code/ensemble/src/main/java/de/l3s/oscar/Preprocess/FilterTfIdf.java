package de.l3s.oscar.Preprocess;

import com.yahoo.labs.samoa.instances.InstancesHeader;
import com.yahoo.labs.samoa.instances.SparseInstance;


import de.l3s.oscar.Evaluation.Evaluation;
import de.l3s.oscar.Evaluation.Sketch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to filter tweet by tf-idf
 * Following the logic of FilterTfIdf of https://github.com/rohan-viz/moa-tweetreader
 * <p>
 * Created by Damian on 14.09.17.
 */
public class FilterTfIdf {

    protected Sketch frequentItemMiner;
    protected double numOfDocs = 0; //number of total documents
    protected InstancesHeader trainingSetHeader = null;
    protected List<String> removedWords = new ArrayList<String>();

    public FilterTfIdf(Sketch sketch) {
        this.frequentItemMiner = sketch;
    }

    public void setTrainingSetHeader(InstancesHeader trainingSetHeader) {
        this.trainingSetHeader = trainingSetHeader;
    }

    public InstancesHeader getTrainingSetHeader() {
        return this.trainingSetHeader;
    }

    /**
     * For memory efficiency words removed from the filter
     * are going to be removed from the trajectory data.
     */
    protected void cleanRemovedWords() {
        removedWords.clear();
    }

    protected boolean areWordsRemoved() {
        if (removedWords.size() != 0) {
            return true;
        } else {
            return false;
        }
    }

    public List<String> getRemovedWords() {
        return this.removedWords;
    }

    /**
     * Takes a String, filters it, and calculates the tf-idf values
     * of each token in the string.
     *
     * @param message               - the String to be filtered
     * @param date                  - the date of the message
     * @param classValue            - the actual class of the message
     * @param startingNumAttributes - the number of attributes of the instance before adding n-grams
     * @return the filtered Instance object
     */
    //@param instancesHeader - the header of the data set
    public SparseInstance filter(String message, String date, String classValue, InstancesHeader instancesHeader, int startingNumAttributes) {
        cleanRemovedWords();
//        public SparseInstance filter(String message, String date, String classValue, int startingNumAttributes){
//        System.out.println("Filter: Hi!");
        this.numOfDocs++;

        String[] tokens = message.split(" ");    //Get the individual tokens.

        double documentSize = tokens.length;        //Number of tokens in the document.

        //The tokens and frequency in this specific document.
        Map<String, Integer> tokensInDocument = new HashMap<String, Integer>();

        for (String token : tokens) {
            //For each token in the document
            if (!token.equals(" ") && !token.equals("")) {
                Integer freq = tokensInDocument.get(token.toLowerCase()); //Compute freq for each token
                tokensInDocument.put(token.toLowerCase(), (freq == null) ? 1 : freq + 1);
            }
        }

//        System.out.println("=== Filter ===");
        for (Map.Entry<String, Integer> e : tokensInDocument.entrySet()) {
            String word = e.getKey();
            int oldAttIndex = this.frequentItemMiner.getAttIndex(e.getKey());
            if (classValue.equals("0") || classValue.equals("4")) {
                /**
                 * ====== Sentiment 140 ====== *
                 * | class in data set | class meaning | class used encoding (index)    |
                 * |        4          |    Happy      |        Positive (0.0)          |
                 * |        0          |    Sad        |        Negative (1.0)          |
                 * */
                this.frequentItemMiner.addToken(e.getKey(), e.getValue(), classValue.equals("4")? 1 : -1);
            }
            else if (classValue.equals("spam") || classValue.equals("ham") || classValue.equals("legitimate")) {
                /**
                 * ====== HSPAM 14 ====== *
                 * ====== EMAIL_DATA ====== *
                 * ====== SPAM_DATA  ====== *
                 * | class in data set | class meaning | class used encoding (index) |
                 * |        spam       |    Spam       |        Spam(0.0)            |
                 * |        ham        |    Ham        |        Ham(1.0)             |
                 * |     legitimate    |    Legitimate |        Legitimate(1.0)
                 * */
                this.frequentItemMiner.addToken(e.getKey(), e.getValue(), classValue.equals("spam")? 1 : -1);
            }

//            this.frequentItemMiner.addToken(e.getKey(), e.getValue(), classValue.equalsIgnoreCase("4") ? 1 : -1);
            int newAttIndex = this.frequentItemMiner.getAttIndex(e.getKey());

            if (oldAttIndex == -1) {
                if (newAttIndex + startingNumAttributes > instancesHeader.numAttributes() - 1) {
//              //add new attribute to the vocabulary
                    com.yahoo.labs.samoa.instances.Attribute newAtt = new com.yahoo.labs.samoa.instances.Attribute(e.getKey());
                    instancesHeader.insertAttributeAt(newAtt, newAttIndex + startingNumAttributes);
                } else {
                    //replace attribute
                    String removedWord;
                    /**
                     * there is no renameAttribute for Samoa data set
                     * so we delete and then insert
                     * */
                    removedWord = instancesHeader.attribute(newAttIndex + startingNumAttributes).name();
                    instancesHeader.deleteAttributeAt(newAttIndex + startingNumAttributes);
//                    System.out.println(word + " replaces " + removedWord);
                    this.removedWords.add(removedWord);
                    com.yahoo.labs.samoa.instances.Attribute newAtt = new com.yahoo.labs.samoa.instances.Attribute(e.getKey());
                    instancesHeader.insertAttributeAt(newAtt, newAttIndex + startingNumAttributes);
                }
            }
        }
//        System.out.println("=== ===");

        this.frequentItemMiner.addDoc(documentSize);
        // Create an sparse instance
        int numTokens = (int) tokensInDocument.size();
        double[] attValues = new double[numTokens + startingNumAttributes];
        int[] indices = new int[numTokens + startingNumAttributes];

        int tokenCounter = startingNumAttributes;
        for (Map.Entry<String, Integer> e : tokensInDocument.entrySet()) { //For each token in the document
            String token = e.getKey();
            double numInDoc = e.getValue();                //Number of occurrences of a token in the specific document.
            double docFreq = this.frequentItemMiner.getCount(token);        //Number of documents that the token appears in.
            double tf = numInDoc / documentSize;                            //Term frequency.
            double idf = Math.log10(this.numOfDocs / (docFreq + 1));    //Inverse document frequency.
            int attIndex = this.frequentItemMiner.getAttIndex(token) + startingNumAttributes;//+ 1;
            indices[tokenCounter] = attIndex;
//            attValues[tokenCounter] = (tf * idf);								//tf*idf
//            attValues[tokenCounter] = tf;                                       //tf
            attValues[tokenCounter] = numInDoc;                                 //numInDoc
            tokenCounter++;
        }

        //add 1st attribute -> class
        int attIndex = 0;
        indices[0] = attIndex;
        attValues[0] = 0.0;
        //add 2nd attribute -> timestamp //data set with time stamp
        attIndex = 1;
        indices[1] = attIndex;
        attValues[1] = Evaluation.parseDateToDouble(date);//3.1415;

        SparseInstance inst = new SparseInstance(1.0, attValues, indices, instancesHeader.numAttributes());
        inst.setDataset(instancesHeader);

        if (classValue.equals("0") || classValue.equals("4")) {
            /**
             * ====== Sentiment 140 ====== *
             * | class in data set | class meaning | class used encoding (index)    |
             * |        4          |    Happy      |        Positive (0.0)          |
             * |        0          |    Sad        |        Negative (1.0)          |
             * */
            inst.setClassValue(classValue.equals("4") ? 0.0D : 1.0D);
        }
        else if (classValue.equals("spam") || classValue.equals("ham") || classValue.equals("legitimate")) {
            /**
             * ====== HSPAM 14 ====== *
             * ====== EMAIL_DATA ====== *
             * ====== SPAM_DATA  ====== *
             * | class in data set | class meaning | class used encoding (index) |
             * |        spam       |    Spam       |        Spam(0.0)            |
             * |        ham        |    Ham        |        Ham(1.0)             |
             * |     legitimate    |    Legitimate |        Legitimate(1.0)
             * */
            inst.setClassValue(classValue.equals("spam") ? 0.0D : 1.0D);
        }

        /**
         * setClassMissing() for samoa instance not implemented
         else {

         inst.setClassMissing();
         }*/
        return inst;
    }

    public void printSketch() {
        this.frequentItemMiner.showNodes();
    }

    public double getFreqWord(String word) {
        return frequentItemMiner.getFreqWord(word);
    }
}
