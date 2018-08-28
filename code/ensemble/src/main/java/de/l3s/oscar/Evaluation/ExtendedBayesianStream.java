package de.l3s.oscar.Evaluation;

import com.yahoo.labs.samoa.instances.WekaToSamoaInstanceConverter;
import de.l3s.oscar.Preprocess.ExtendedTweet;
import java.util.ArrayList;
import moa.MOAObject;
import moa.core.Example;
import moa.core.MultilabelInstancesHeader;
import moa.streams.InstanceStream;

import weka.core.Attribute;
import weka.core.DenseInstance;

import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import moa.core.FastVector;
/**
 * Class generates a training stream from an array of tweets, so that the classifiers can be trained.
 */
public class ExtendedBayesianStream implements InstanceStream {

	/**
	 * trainingstream for the models - updated over time, instances are deleted
	 * after training
	 */
	public Instances trainingSet;

	/**
	 * saves temporary new training instances
	 */
	public Instances newInstances;

	/**
	 * contains all unfiltered training instances from the beginning
	 */
	public Instances unfilteredTrainingSet;

	/**
	 * number of training instances left in the training stream
	 */
	int numInstances;

	/**
	 * Filter for the vector tf*idf representation of the instances
	 * 
	 * @param tweets
	 *            - training tweets for the stream
	 */
	StringToWordVector vector;

	protected WekaToSamoaInstanceConverter instanceConverter; //object to convert weka to samoa instance

	public ExtendedBayesianStream(ArrayList<ExtendedTweet> tweets) throws Exception {

		// initialising the trainingSet
		System.out.println("Inside extended Bayesian stream");

		FastVector attributes = new FastVector();
		//add text of tweet as instance attribute
		attributes.addElement(new Attribute("tweetText",(FastVector) null));
		//add class attribute
		FastVector classValues = new FastVector();
		classValues.addElement("default");
		classValues.addElement("positive"); //4: happy, positive
		classValues.addElement("negative"); //0: sad, negative
		attributes.addElement(new Attribute("Class",classValues));
		System.out.println("attributes: " + attributes.toString());

		unfilteredTrainingSet = new Instances("TrainingSet",attributes,0);
		unfilteredTrainingSet.setClassIndex(unfilteredTrainingSet.numAttributes() - 1);

		/* === Sebastian's code for adding attributes to each tweet instance ===
		FastVector atts = new FastVector(2);
        FastVector fvClassVal = new FastVector(2);
		//FastVector atts = new FastVector(3);
		//FastVector fvClassVal = new FastVector(3);

		//fvClassVal.addElement("H");
		//fvClassVal.addElement("S");
        fvClassVal.addElement("4");
        fvClassVal.addElement("0");

		Attribute ClassAttribute = new Attribute("sentimentClass", fvClassVal);

		atts.addElement(ClassAttribute);
		atts.addElement(new Attribute("tweet", (FastVector) null));
//		System.out.println("atts vector length: "+ atts.size());
		//atts.addElement(new Attribute("date", (FastVector) null));
//		System.out.println("atts vector length: "+ atts.size());
//		System.out.println("Dada: " + atts.elementAt(2));
		unfilteredTrainingSet = new Instances("TrainigSet", atts, 0);
//		System.out.println("Kaboom " + unfilteredTrainingSet.attribute(2));
		unfilteredTrainingSet.setClassIndex(0);
=== End of Sebastian's code ===*/

		int i = 0;

		/* TODO: Sebastian's code for time stamps
        boolean firstTimestampSet = false;
        int firstTimestamp = 0; //in our case, the first week we look at*/

		while (i < tweets.size()) {
			numInstances++;
			ExtendedTweet tweet = tweets.get(tweets.size() - 1);
			//Sliding window functionality? Removing the first tweet from dataset
			tweets.remove(tweets.size() - 1);

			addInstanceToDataSet(tweet.getText(),tweet.getType(),unfilteredTrainingSet);
/* === Sebastian code ===
//			System.out.println("Debugging like a pro.");
			numInstances++;
			ExtendedTweet tweet = tweets.get(tweets.size() - 1); //!!!!
			tweets.remove(tweets.size() - 1);
			Instance inst = new DenseInstance(2);
			//Instance inst = new DenseInstance(3);
			inst.setValue(unfilteredTrainingSet.attribute(0), tweet.getType());
			inst.setValue(unfilteredTrainingSet.attribute(1), tweet.getText());
=== ===*/

//			System.out.println("What's the Tweet's date?: " + tweet.getDate());
			/*
			 * attenzione
			 */
//TODO: time stamp code			String dateString = tweet.getDate();
		    //DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
//TODO: time stamp code            String parseString = "YYYY-MM-dd HH:mm:ss.0";
//TODO: time stamp code            LocalDateTime joda = LocalDateTime.parse(dateString, DateTimeFormat.forPattern(parseString));
            //DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
            //DateFormat dateFormat = new SimpleDateFormat(parseString, Locale.US);
		    //Date date = dateFormat.parse(dateString ); //Hier passiert der Fehler!
            //System.out.println("Date String (original) from ExtendedDatabaseConnection: " + dateString);
            //System.out.println("Date in Bayesian Stream: " + date);
            //DateTime joda = new DateTime(date);
            //System.out.println("Joda Date in Bayesian Stream: " + joda);
            /* TODO: time stamp code
			int week = joda.getWeekOfWeekyear();
            int day = joda.getDayOfYear();
            int hour = joda.getHourOfDay();
            int hourOfYear = hour + 24 * day;
            */
            //---Testing! Setting to day of year
            //int week = joda.getDayOfYear();
            //System.out.println("Week Of Weekyear in Bayesian Stream: " + week);
            //System.out.println("Day of Year in Bayesian Stream: " + day);
            /* TODO: time stamp code
			if(firstTimestampSet == false)
            {
                firstTimestamp = week;
                firstTimestampSet = true;
            }*/

            //long unixTime = (long) date.getTime()/1000;
//		    System.out.println("Unix Time: " + unixTime );
            //double unixFloat = unixTime;
            //System.out.println("Flot time: " + unixFloat);
//TODO: time stamp code            inst.setWeight(day);
            //inst.setWeight(week);
            //inst.setWeight(hourOfYear);
            //System.out.println("Instance weight: " + inst.weight());
//		    System.out.println(inst);
//			double instanceValue = inst.value(unfilteredTrainingSet.attribute(1));
			
			/*System.out.println("This instances value attribute 0 " + inst.value(unfilteredTrainingSet.attribute(0)));
			System.out.println("This instances value attribute 1 " + inst.value(unfilteredTrainingSet.attribute(1)));
			System.out.println("This instances value attribute 2 " + inst.value(unfilteredTrainingSet.attribute(2)));
			System.out.println("This instance "+ inst);
			System.out.println(inst.toString());
			System.out.println(inst.toString(2));
			System.out.println(inst.toString(ClassAttribute));
			System.out.println(inst.toString(unfilteredTrainingSet.attribute(2)));*/
//TODO: Sebastian code			unfilteredTrainingSet.add(inst);

		}
		/* === Sebastian's code ===
//		System.out.println("Finished the loop of all tweets");
		vector = new StringToWordVector();
		//attenzione here please
		String[] options = new String[2];
		//String[] options = new String[3];
		options[0] = "-C";
		options[1] = "-I";


		vector.setOptions(options);
		vector.setInputFormat(unfilteredTrainingSet);
		// vector.setOutputWordCounts(true);
		// vector.setIDFTransform(true);
		// vector.setNormalizeDocLength();

		trainingSet = Filter.useFilter(unfilteredTrainingSet, vector);
		//System.out.println("This point is never reached");
		=== Sebastian's code ===*/

		System.out.println("Finished the loop of all tweets");
		vector = new StringToWordVector();
		String rangeofAttributesForFilter = "first"; //damian

		String[] options = new String[1];
		options[0] = "-C"; //Output word counts rather than boolean word presence.
		//options[1] = "-I"; //Transform each word frequency into: fij*log(num of Documents/num of documents containing word i) where fij if frequency of word i in jth document(instance)

		vector.setOptions(options);
		vector.setWordsToKeep(3000); //3000 //set approximately how many words to keep after filtering
		vector.setInputFormat(unfilteredTrainingSet);
		vector.setAttributeIndices(rangeofAttributesForFilter); //damian
		trainingSet = Filter.useFilter(unfilteredTrainingSet, vector);
		trainingSet.setClassIndex(0);

		System.out.println("After using filter");
		System.out.println("===");

	}

	/**
	 * Method that converts a text message into an instance and assigned it to the data set.
	 * @param text			String, text message
	 * @param data			Instances, data set to add this instance
	 * @return instance		Instance, created instance from input text message
	 */
	private Instance makeInstance(String text, Instances data) {

		// Create instance of length two.
		Instance instance = new DenseInstance(2);

		// Set value for message attribute
		Attribute messageAtt = data.attribute("tweetText");
		instance.setValue(messageAtt, messageAtt.addStringValue(text));

		// Give instance access to attribute information from the dataset.
		instance.setDataset(data);
		return instance;
	}

	/**
	 * Updates data set with the given training message.
	 * @param message			String, instance's text message
	 * @param classValue		String, instance's class value
	 * @param dataSet			Instances, data set to add this instance
	 * @throws Exception
	 */
	public void addInstanceToDataSet(String message, String classValue, Instances dataSet) throws Exception {

		// Make message into instance.
		Instance instance = makeInstance(message, dataSet);

		// Set class value for instance.
		if (classValue.equalsIgnoreCase("4")){
			instance.setClassValue("positive");
		}
		else{
			instance.setClassValue("negative");
		}

		// Add instance to training data.
		dataSet.add(instance);
	}

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
	public com.yahoo.labs.samoa.instances.Instance nextSamoaInstance() {
		/*
		 * Extended to include timestamps
		 * Use weight as code for unix timetamp
		 */
		weka.core.Instance currentWekaInstance;
		this.instanceConverter = new WekaToSamoaInstanceConverter();
		//WekaToSamoaInstanceConverter wekaToSamoaInstanceConverter = new WekaToSamoaInstanceConverter();
		com.yahoo.labs.samoa.instances.Instance currentInstance = null;
		if (this.hasMoreInstances()) {
			int currentInstanceIndex = trainingSet.size() - 1;
			currentWekaInstance = trainingSet.get(currentInstanceIndex);
/*

			try{
				int classIndex = currentWekaInstance.classIndex();
				System.out.println("the class index is " + Integer.toString(classIndex));
				System.out.println("the value at this index is " + currentWekaInstance.attributeSparse(classIndex));
				System.out.println(" class attribute is " + currentWekaInstance.classAttribute());
			}
			catch (UnassignedDatasetException e){
				System.out.println("hi!!!!!!!!!!!!!!!!!!!");
				System.out.println(e.toString());
			}
			System.out.println("Current weka instance's has");
			System.out.println("has missing value? " + Boolean.toString(currentWekaInstance.hasMissingValue()));
			System.out.println("Its' attributes are: " + currentWekaInstance.toString());
			System.out.println("Its' class is: " + currentWekaInstance.classValue());// classAttribute().value(0));

			System.out.println("===");
*/
			currentInstance = instanceConverter.samoaInstance(currentWekaInstance);
			trainingSet.remove(currentInstanceIndex);
			numInstances--;
			return currentInstance;
		}
		return currentInstance;

	}

	public int measureByteSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public MOAObject copy() {
		// TODO Auto-generated method stub
		return null;
	}

	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

	public MultilabelInstancesHeader getHeader() {

		return null;
	}

	public long estimatedRemainingInstances() {
		// TODO Auto-generated method stub
		return 0;
	}


	public boolean hasMoreInstances() {
		if (numInstances > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Method returns a training instance and deletes it from the training
	 * stream
	 */
	public Example<com.yahoo.labs.samoa.instances.Instance> nextInstance() {
		/*
		 * Extended to include timestamps
		 * Use weight as code for unix timetamp
		 */
		if (this.hasMoreInstances()) {
			int moin = trainingSet.size() - 1;
			Instance inst = trainingSet.get(moin);
			//inst.setValue(2, "2014-test");
			trainingSet.remove(moin);
			numInstances--;
			return (Example<com.yahoo.labs.samoa.instances.Instance>) inst;
		}
		return null;
	}

	public boolean isRestartable() {
		// TODO Auto-generated method stub
		return false;
	}

	public void restart() {
		// TODO Auto-generated method stub

	}

	public StringToWordVector getVector() {
		return this.vector;
	}

	public Instances getInstances() {
		return this.trainingSet;
	}

	/**
	 * Method adds a new instance to the training stream
	 * 
	 * @param t
	 *            - new training instance
	 */
	public void addInstanceToStream(ExtendedTweet t) {

		Instance inst = new DenseInstance(2);
		
		//Once for unfiltered training set, once for new Instances

		inst.setValue(unfilteredTrainingSet.attribute(0), t.getType());

		inst.setValue(unfilteredTrainingSet.attribute(1), t.getText());
		
		//inst.setValue(unfilteredTrainingSet.attribute(2), t.getDate());

		unfilteredTrainingSet.add(inst);

		Instance inst2 = new DenseInstance(2);

		inst2.setValue(newInstances.attribute(0), t.getType());

		inst2.setValue(newInstances.attribute(1), t.getText());
		
		//inst2.setValue(newInstances.attribute(2), t.getDate());

		newInstances.add(inst2);

	}

	/**
	 * Method generates a new temporary dataset for storage of new training tweets
	 */
	public void createNewUpdateStream() {
		// initialising the trainingSet

		FastVector atts = new FastVector(); //Sebastian code (2)
		FastVector fvClassVal = new FastVector(); //Sebastian code (2)

		//fvClassVal.addElement("H");
		//fvClassVal.addElement("S");
        fvClassVal.addElement("4");
        fvClassVal.addElement("0");
		// fvClassVal.addElement("neutral");

		Attribute ClassAttribute = new Attribute("sentimentClass", fvClassVal);

		// atts.addElement(new Attribute("tweet", (FastVector) null));
		atts.addElement(ClassAttribute);
		atts.addElement(new Attribute("tweet", (FastVector) null));
		newInstances = new Instances("TrainigSet", atts, 0);
		newInstances.setClassIndex(0);

	}

	/**
	 * Method updates the training stream with new instances
	 * @throws Exception
	 */
	public void updateTrainingStream() throws Exception {
		this.trainingSet = Filter.useFilter(newInstances, vector);
		numInstances = newInstances.size();
	}

}
