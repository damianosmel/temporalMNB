package de.l3s.oscar.Evaluation;


import de.l3s.oscar.Preprocess.FilterTfIdf;
import moa.core.Example;
import moa.options.AbstractOptionHandler;

import de.l3s.oscar.Preprocess.ExtendedTweet;
import moa.MOAObject;
import moa.streams.InstanceStream;

import java.util.*;

//samoa
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.SparseInstance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

/**
 * Class generates a training stream from an array of tweets, so that the classifiers can be trained.
 */
public class BayesianStreamBig implements InstanceStream {

	/**
	 * trainingstream for the models - updated over time, instances are deleted
	 * after training
	 */

	public Instances trainingSetSamoa;
	public InstancesHeader trainingSetHeaderSamoa;

	/**
	 * variables needed to re-change the stream
	 * */
	public Instances newTrainingSetSamoa;
	public InstancesHeader newTrainingSetHeaderSamoa;

	Map<String, Integer> token2DataSetIndex = new HashMap<String, Integer>();

	//Samoa
	protected Sketch frequentItemMiner = new SpaceSaving();
//	protected Sketch frequentItemMiner = new SpaceSavingExpDecay();
//	protected Sketch frequentItemMiner = new SpaceSavingAdwin();

	protected static FilterTfIdf filterTfIdf; //Only one for all streams

	protected int dateAttributeIndex = 0; //int to store the index of attribute saving the date time stamp of tweet
	protected int startIndex4Attr = 2; //data with timeStamp
//	protected int startIndex4Attr = 1; //data without timeStamp
	protected ArrayList<ExtendedTweet> tweets;

	/**
	 * number of training instances left in the training stream
	 */
	int numInstances;

	public BayesianStreamBig(ArrayList<ExtendedTweet> tweets) throws Exception {

		/**
		 * Samoa instances
		 * 1) create class and date time stamp attributes
		 * 2) create training set and header of the training set
		 * */
		ArrayList<String> classValues = new ArrayList<String>();
		classValues.add("Positive");
		classValues.add("Negative");
		Attribute classAttribute = new Attribute("class",classValues);
		Attribute dateAttribute = new Attribute("dateTimeStamp"); //data set with time stamp
		ArrayList<Attribute> samoaAttributes = new ArrayList<com.yahoo.labs.samoa.instances.Attribute>();
		samoaAttributes.add(classAttribute);
		samoaAttributes.add(dateAttribute); //data set with time stamp

		this.trainingSetSamoa = new Instances("tweetTrainingSet",samoaAttributes,0);
		this.trainingSetHeaderSamoa = new InstancesHeader(this.trainingSetSamoa);
//		System.out.println("At start, data set has attributes: " + this.trainingSetHeaderSamoa.numAttributes());

		this.trainingSetHeaderSamoa.setClassIndex(0);
		this.dateAttributeIndex = 1;

		/**
		 * initialize the sketch for words frequencies
		 * */
		int numOfFreqWords = 5000; //for sts1.6 10000 5000 3000 //for email and spam 1000
		((SpaceSaving)this.frequentItemMiner).capacityOption.setValue(numOfFreqWords);
//		((SpaceSavingExpDecay) this.frequentItemMiner).capacityOption.setValue(numOfFreqWords);
//		((SpaceSavingAdwin) this.frequentItemMiner).capacityOption.setValue(numOfFreqWords);

		TaskMonitor arg0 = null;
		ObjectRepository arg1 = null;
		((AbstractOptionHandler) this.frequentItemMiner).prepareForUse(arg0,arg1);
		this.filterTfIdf = new FilterTfIdf(this.frequentItemMiner);

		this.tweets = tweets;

		this.token2DataSetIndex = new HashMap<String, Integer>();
	}

	/**
	 * Read next instance (if there is one), process it and save it to training set and the header
	 * @return
	 */
	public boolean processNextDocument(){
		boolean noMoreTweets = false;
		if(this.tweets.size() > 0){

			ExtendedTweet tweet = this.tweets.get(this.numInstances);
			/**
			 * Sketch use
			 * */
			filterAndAddInstanceToDataSet(tweet.getText(), tweet.getDate(), tweet.getType(), this.trainingSetHeaderSamoa);
			/**
			 * All data set use
			 * */
//			addInstanceToDataSet(tweet.getText(), tweet.getDate(), tweet.getType());

			this.tweets.remove(this.numInstances);

			this.numInstances++;
			return noMoreTweets;
		}
		else{
			noMoreTweets = true;
			return noMoreTweets;
		}
	}

	public int getDateAttributeIndex(){
		return this.dateAttributeIndex;
	}
	/**
	 * Prints each attribute of each instance in the data set.
	 */
	public void printDataSet(){
		System.out.println("=== Data Set ===");
		for(int instanceIndex = 0; instanceIndex < this.trainingSetSamoa.numInstances(); instanceIndex++){
			System.out.println("--- Instance " + instanceIndex + " ---");
			com.yahoo.labs.samoa.instances.Instance currentInstance = this.trainingSetSamoa.get(instanceIndex);
			for(int i=0; i < currentInstance.numValues(); i++){
				int index = currentInstance.index(i);
				double wordCount = currentInstance.valueSparse(i);
				String attributeName = currentInstance.attribute(index).name();
//				if(index != this.trainingSetSamoa.classIndex()){
				if(index != currentInstance.classIndex()){
						System.out.println(attributeName + ": " + wordCount);
				}
				else{
					System.out.println("Class: " + this.trainingSetHeaderSamoa.classAttribute().value((int) currentInstance.classValue()));
				}
			}
			System.out.println("Weight: " + currentInstance.weight());
			System.out.println("--- ---");
		}
		System.out.println("=== ===");
	}

	public void printDataSetHeader(){
		System.out.println("=== Data set header ===");
		this.trainingSetHeaderSamoa.toString();
		System.out.println("=== ===");
	}

	public List<String> getRemovedWordsFromFilter(){
		return this.filterTfIdf.getRemovedWords();
	}
	/**
	 * Updated Samoa data set with the given training message
	 *
	 * @param message
	 * @param date
	 * @param classValue
	 */

	/**
	 * Pass instance from sketch then add it to data set
	 * */
	public void filterAndAddInstanceToDataSet(String message, String date, String classValue, InstancesHeader samoaDataSet){ //
		SparseInstance newInstance = this.filterTfIdf.filter(message, date, classValue, samoaDataSet, this.startIndex4Attr);
		this.trainingSetSamoa.add(newInstance);
	}

	/**
	 * Add instance to data set (no filtering)
	 * */
	public void addInstanceToDataSet(String message, String date, String classValue){
		SparseInstance newInstance = createInstance(message, date, classValue, this.startIndex4Attr);
		this.trainingSetSamoa.add(newInstance);
	}

	public SparseInstance createInstance(String message, String date, String classValue, int startingNumAttributes){

		/**
		 * Get tokens and their frequencies in the document
		 * */
		String[] tokens = message.split(" ");	//Get the individual tokens.
		Map<String, Integer> tokensInDocument = new HashMap<String, Integer>();
		for (String token : tokens) {
			//For each token in the document
			if (!token.equals(" ") && !token.equals("")) {
				Integer freq = tokensInDocument.get(token.toLowerCase()); //Compute freq for each token
				tokensInDocument.put(token.toLowerCase(), (freq == null) ? 1 : freq + 1);
			}
		}

		/**
		 * Update feature set
		 * */
		int currentAttIndex = this.trainingSetHeaderSamoa.numAttributes() - 1;
		int newAttIndex = currentAttIndex;

		for(Map.Entry<String, Integer> e : tokensInDocument.entrySet()) {
			String word = e.getKey();
			if(!this.token2DataSetIndex.containsKey(word)){
				newAttIndex++;
				com.yahoo.labs.samoa.instances.Attribute newAtt =  new com.yahoo.labs.samoa.instances.Attribute(e.getKey());
				this.trainingSetHeaderSamoa.insertAttributeAt(newAtt, newAttIndex);
				this.token2DataSetIndex.put(word, newAttIndex);
			}
		}
		/**
		 * Create the mapping from attribute indices to sparse instance indices
		 * */
		int numTokens = (int) tokensInDocument.size();
		double[] attValues = new double[numTokens + startingNumAttributes];
		int[] indices = new int[numTokens + startingNumAttributes];

		int tokenCounter = startingNumAttributes;
		for (Map.Entry<String, Integer> e : tokensInDocument.entrySet()) { 		//For each token in the document
			String token = e.getKey();
			double numInDoc = e.getValue(); 									//Number of occurrences of a token in the specific document.

			int attIndex = this.token2DataSetIndex.get(token);
			indices[tokenCounter] = attIndex;
			attValues[tokenCounter] = numInDoc;                          		//numInDoc
			tokenCounter++;
		}

		/**
		 * Create this new instance
		 * */
		//add 1st attribute -> class
		int attIndex = 0;
		indices[0] = attIndex;
		attValues[0] = 0.0;
		//add 2nd attribute -> timestamp
		attIndex = 1;
		indices[1] = attIndex;
		attValues[1] = Evaluation.parseDateToDouble(date);

		SparseInstance inst = new SparseInstance(1.0, attValues, indices, this.trainingSetHeaderSamoa.numAttributes());
		inst.setDataset(this.trainingSetHeaderSamoa);

		if (classValue.equals("0") || classValue.equals("4")) {
			/**
			 * | class in data set | class meaning | class used encoding (index)    |
			 * |        4          |    Happy      |        Positive (0.0)          |
			 * |        0          |    Sad        |        Negative (1.0)          |
			 * */
			inst.setClassValue(classValue.equals("4") ? 0.0D : 1.0D);
			/**
			 * Test Classes
			 * */
		}

		return inst;
	}

	public int numberOfClasses(){
//		return trainingSet.numClasses(); //moa
		return this.trainingSetSamoa.numClasses();
	}

	//@Override
	public int measureByteSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	//@Override
	public MOAObject copy() {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	//@Override
	public InstancesHeader getHeader() {

		return this.trainingSetHeaderSamoa;

	}

	//@Override
	public long estimatedRemainingInstances() {
		// TODO Auto-generated method stub
		return 0;
	}

	//@Override
	public boolean hasMoreInstances() {
//		if (this.numInstances > 0) {
		if(this.tweets.size() > 0){
			return true;
		}
		return false;
	}

	public Instances getSamoaInstances(){
		return this.trainingSetSamoa;
	}



		/*
		 * Extended to include timestamps
		 * Use weight as code for unix timetamp
		 */
	/**
	 * Method to return the next Samoa training instance
	 *
	 * this method is to be compatible with the Samoa extension of moa
	 * @see <a href="https://github.com/Waikato/moa/blob/master/moa/src/main/java/com/yahoo/labs/samoa/instances/WekaToSamoaInstanceConverter.java"> WekaToSamoaInstanceConverter </a>
	 * @see <a href="http://www.programcreek.com/java-api-examples/index.php?source_dir=moa-master/moa/src/main/java/weka/classifiers/meta/MOA.java"> Example how to use WekaToSamoaInstnaceConverter </a>
	 * @return			com.yahoo.labs.samoa.instances.Instance, next training instance
	 *
	 * @since Jan 2017
	 * @author Damianos Melidis
	 */
	public Instance nextSamoaInstance() {
		Instance nextInstance = null;

		if(this.hasMoreInstances()){
			processNextDocument();
			int currentInstanceIndex = this.trainingSetSamoa.size() - 1;
			nextInstance = this.trainingSetSamoa.get(currentInstanceIndex);

			this.trainingSetSamoa.delete(currentInstanceIndex);
			this.numInstances--;
		}
		return nextInstance;
	}


/*
	//moa-weka
	public weka.core.Instance nextWekaInstance(){
		weka.core.Instance nextWekaIntance;
		if (this.hasMoreInstances()) {
			int currentInstanceIndex = trainingSet.size() - 1;
			nextWekaIntance = trainingSet.get(currentInstanceIndex);
			//inst.setValue(2, "2014-test");
			trainingSet.remove(currentInstanceIndex);
			numInstances--;
			return nextWekaIntance;
		}
		return null;


	}
*/

	/*
	//moa
	*/
/**
	 * Method returns a training instance and deletes it from the training
	 * stream
	 */

	//@Override
	public Example nextInstance() {
		 /**
		 * Extended to include timestamps
		 * Use weight as code for unix timetamp
		 */

		if (this.hasMoreInstances()) {
			int currentInstanceIndex = this.trainingSetSamoa.size() - 1;
			Example inst = (Example) this.trainingSetSamoa.get(currentInstanceIndex);
			this.trainingSetSamoa.delete(currentInstanceIndex);
			numInstances--;
			return inst;
		}
		return null;
	}

	//@Override
	public boolean isRestartable() {
		// TODO Auto-generated method stub
		return false;
	}

	//@Override
	public void restart() {
		// TODO Auto-generated method stub

	}
/*
	//moa
	public StringToWordVector getVector() {
		return this.vector;
	}

*/
	public Instances getInstances() {

//		return this.trainingSet;
		return this.trainingSetSamoa;
	}

	/**
	 * Method adds a new instance to the training stream
	 * 
	 * @param tweet
	 *            - new training instance
	 */
	public void addInstanceToStream(ExtendedTweet tweet) {
		filterAndAddInstanceToDataSet(tweet.getText(), tweet.getDate(), tweet.getType(), this.newTrainingSetHeaderSamoa); //
	}

	/**
	 * Method generates a new temporary dataset for storage of new training tweets
	 */
	public void createNewUpdateStream() {
		ArrayList<String> classValues = new ArrayList<String>();
		classValues.add("Positive");
		classValues.add("Negative");
		Attribute classAttribute = new Attribute("class", classValues);
		Attribute dateAttribute = new Attribute("dateTimeStamp");
		ArrayList<Attribute> samoaAttributes = new ArrayList<com.yahoo.labs.samoa.instances.Attribute>();
		samoaAttributes.add(classAttribute);
		samoaAttributes.add(dateAttribute);

		this.newTrainingSetSamoa = new Instances("newTrainingSet",samoaAttributes,0);
		this.newTrainingSetHeaderSamoa = new InstancesHeader(this.newTrainingSetSamoa);
		this.newTrainingSetHeaderSamoa.setClassIndex(0);
	}

	/**
	 * Method updates the training stream with new instances
	 * @throws Exception
	 */
	public void updateTrainingStream() throws Exception {
		this.trainingSetSamoa = this.newTrainingSetSamoa;
		this.trainingSetHeaderSamoa = this.newTrainingSetHeaderSamoa;

		this.numInstances = this.newTrainingSetSamoa.numInstances();
	}

}
