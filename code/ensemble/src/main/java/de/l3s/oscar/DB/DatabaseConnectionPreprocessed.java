package de.l3s.oscar.DB;
import de.l3s.oscar.Preprocess.ExtendedTweet;
import de.l3s.oscar.Preprocess.Preprocessor;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

/**
 * This class is the connection between model and database that contains the training data. 
 */
public class DatabaseConnectionPreprocessed {

	/**
	 * Flag to run on server
	 *
	 * */
	public boolean runningOnServer = false;
//	public boolean runningOnServer = true;

	/**
	 * Connection to the database
	 */
	private Connection connection;


	/**
	 * preprocessor that cleans the tweets
	 */
	private Preprocessor pp;

	/**
	 * ArrayList that contains the training Instances
	 */
	private ArrayList<ExtendedTweet> trainingSet = new ArrayList<ExtendedTweet>();


	PreparedStatement statement;
	String start = "";
	String date = "";

	ResultSet rs;

	PreparedStatement stmt;



	/**
	 * Controller
	 * @throws java.sql.SQLException
	 * @throws ClassNotFoundException
	 * @throws java.io.IOException
	 */
	public DatabaseConnectionPreprocessed(String db, String user, String password) throws SQLException, ClassNotFoundException, IOException{
		Class.forName("com.mysql.jdbc.Driver");

		pp = new Preprocessor();


		// DB connection
//	    connection = DriverManager.getConnection("jdbc:mysql://localhost/"+db, user, password);

		if (runningOnServer) {
			/**
			 * For Denken
			 */
			connection = DriverManager.getConnection("jdbc:mysql://db2.l3s.uni-hannover.de:3306/" + db, user, password);
		}
		else{
			// DB connection
	    	connection = DriverManager.getConnection("jdbc:mysql://localhost/" + db, user, password);
		}
	    // Sebastian's DB query
        //stmt = connection.prepareStatement("SELECT sentiment, minimalText, STR_TO_DATE(date, '%W %M %d %T PDT %Y') AS stamp FROM tweets ORDER BY stamp ASC LIMIT 5 OFFSET 0");

		/**
		 * Tweets 1.6 million
		 * LIMIT 2500 OFFSET 5000
		 * 2009-04-07 00:50:37 (+5000) -- 2009-04-07 02:08:50 (+7500)
		 */

		/**
		 * For 3 days -> ~70.000 instances
		 *
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 10 OFFSET 0"); // LIMIT 10000 OFFSET 0
		/**
		 * test accuracy updated ensemble
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 1000 OFFSET 0");
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 70000 OFFSET 0");
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 30000 OFFSET 0");
//		stmt = connection.prepareStatement("SELECT sentimetn, minimalText, date AS timeStamp FROM tweets_")
		//ALL data set
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC");

		/**
		 * First gap in days -> 38502
		 * More precisely 6.4: 20671 and 17.4: 17831
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 3 OFFSET 20670"); // LIMIT 10000 OFFSET 0
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 1 OFFSET 0"); // LIMIT 10000 OFFSET 0
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 1000000 OFFSET 0");


		/**
		 * All negative values:
		 * 1334589 - 1.6 Mil
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 265411 OFFSET 1334588");


		/**
		 * change of month -> 6.4: 185989 to 9.5 ..
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 50 OFFSET 185969");


		/**
		 * Importing words sentiment from csv file
		 *
		 * All instances of April: 108513	+ 77476 = 185989
		 * Last instance in April: 100024
		 * First instance in May: 100025

		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 100100 OFFSET 0"); //LIMIT 4 OFFSET 0

		/**
		 * ~~~ Max Preprocessed ~~~
		 * */
		/**
		 * 1.6 Mil ST data set
		 * */
		/**
		 * Tiny Data Set -> first 100
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, textPreprocessed, date AS timeStamp FROM tweets_copy2 ORDER BY timeStamp ASC LIMIT 100 OFFSET 0");

		stmt = connection.prepareStatement("SELECT sentiment, textPreprocessed, date AS timeStamp FROM tweets_copy2 ORDER BY timeStamp ASC LIMIT 1000 OFFSET 0");
		/**
		 * Small Data Set -> first 20K
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, textPreprocessed, date AS timeStamp FROM tweets_copy2 ORDER BY timeStamp ASC LIMIT 20000 OFFSET 0");
		/**
		 * first 3 days -> first 70K
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, textPreprocessed, date AS timeStamp FROM tweets_copy2 ORDER BY timeStamp ASC LIMIT 70000 OFFSET 0");

		/**
		 * all DB
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, textPreprocessed, date AS timeStamp FROM tweets_copy2 ORDER BY timeStamp ASC");

		/**
		 * ~~~ Max Preprocessed ~~~
		 * */

		/**
		 * ======== HSPAM 14 ======== *
		 * */
		//TEST + maxPreprocessed
//		stmt = connection.prepareStatement("SELECT class, text, timestamp AS timeStamp FROM test ORDER BY timeStamp ASC");

		//ALL DB + maxPreprocessed
		//test size: 1000
//		stmt = connection.prepareStatement("SELECT class, text, timestamp AS timeStamp FROM hspam14_max ORDER BY timeStamp ASC LIMIT 5000 OFFSET 0");
//		stmt = connection.prepareStatement("SELECT class, text, timestamp AS timeStamp FROM hspam14_max ORDER BY timeStamp ASC LIMIT 50000 OFFSET 0");
		//whole DB:
//		stmt = connection.prepareStatement("SELECT class, text, timestamp AS timeStamp FROM hspam14_max ORDER BY timeStamp ASC");
//		stmt = connection.prepareStatement("SELECT class, text, timestamp FROM hspam14_max LIMIT 10000 OFFSET 0"); // ORDER BY timestamp 1764488 100000
		/**
		 * ======== HSPAM 14 ======== *
		 * */

		/**
		 * All DB 1.6 million tweets
		 * ASC for tweets_copy
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC");

		/**
		 * Debug - Toy example:
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 10 OFFSET 0");
		/**
		 * Debug - Small: 100 tweets
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy LIMIT 100 OFFSET 0"); //ORDER BY timeStamp ASC

		/**
		 * Debug - Small: 1000 tweets
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 2000 OFFSET 0");

		/**
		 * Debug - Small: 10000 tweets
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 10000 OFFSET 0"); //

		/**
		 * Debug - Small: 50000 tweets
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 50000 OFFSET 0"); //ORDER BY timeStamp ASC

		/**
		 * Debug - Medium: 100000 tweets
		 **/

//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 200000 OFFSET 0");

		/**
		 * Debug - Large: 1 Mil tweets
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 1000000 OFFSET 0");

		/**
		 * For debugging -> Seasonal: java.lang.ArrayIndexOutOfBoundsException: 17
		 * */
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM tweets_copy ORDER BY timeStamp ASC LIMIT 350000 OFFSET 0");

		/**
		 * Debug tweets
		 * DESC for debug
		 **/
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM debugTweets ORDER BY timeStamp DESC");
//		stmt = connection.prepareStatement("SELECT sentiment, minimalText, date AS timeStamp FROM debugTweets ORDER BY timeStamp DESC LIMIT 4 OFFSET 6"); // LIMIT 2 OFFSET 0

		/**
		 * FeatureDriftTextual
		 * */
		/**
		 * email data set
		 * */

		//First 100 instances
//		stmt = connection.prepareStatement("SELECT class, text, id FROM email_data ORDER BY id ASC LIMIT 100 OFFSET 0");

		//Whole data set:
//		stmt = connection.prepareStatement("SELECT class, text, id FROM email_data ORDER BY id ASC");

		/**
		 * spam data set
		 * */
		//First 100 instances
//		stmt = connection.prepareStatement("SELECT class, text, id FROM spam_data ORDER BY id ASC LIMIT 100 OFFSET 0");

		//Whole data set
//		stmt = connection.prepareStatement("SELECT class, text, id FROM spam_data ORDER BY id ASC");

		rs = stmt.executeQuery();
        System.out.println("After SQL query");

		//generate dataset from the results
		this.generateDataSet();
        System.out.println("After dataset generation");
	}


/**
 * Methods generates the trainingSet from the result set of te DB
 * @throws java.sql.SQLException
 * @throws java.io.IOException
 */
public void generateDataSet() throws SQLException, IOException {
	System.out.println("Inside generateDataSet()");
	int i = 0;
	while(rs.next()){
		
//		System.out.println(i);
		//Sentiment of the tweet
		String type = rs.getString(1);
		//content of the tweet
		String text = rs.getString(2);
		//date of the tweet
		String date = rs.getString(3);

		//Generate Tweet Instance

		ExtendedTweet tweet = new ExtendedTweet();
		tweet.setText(text);
		tweet.setType(type);
		tweet.setDate(date);

		//Add Tweet to the Trainingset
		trainingSet.add(tweet);
		i++;

	 }

}	/**
	 * Get trainingSet
	 * @param
	 * @since Jan 2017
	 * @return ArrayList<ExtendedTweet>
	 * @author Damianos Melidis
	 */
	public ArrayList<ExtendedTweet> getTrainingSet()
	{
		return this.trainingSet;

	}

}
