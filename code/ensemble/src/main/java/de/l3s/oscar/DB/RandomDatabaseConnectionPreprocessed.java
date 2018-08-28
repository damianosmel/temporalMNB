package de.l3s.oscar.DB;

import de.l3s.oscar.Preprocess.Preprocessor;
import de.l3s.oscar.Preprocess.ExtendedTweet;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;


/**
 * This class is the connection between model and database that contains the training data.
 */
public class RandomDatabaseConnectionPreprocessed {


	/**
	 * Connection to the database
	 */
	private Connection connection;


	/**
	 * preprocessor that cleans the tweets
	 */
	private Preprocessor pp;

	/**
	 * ArrayList that contains the training Istances
	 */
	ArrayList<ExtendedTweet> trainingSet = new ArrayList<ExtendedTweet>();


	PreparedStatement statement;
	String start = "";
	//String date = "";

	ResultSet rs;

	PreparedStatement stmt;



	/**
	 * Controller
	 * @throws java.sql.SQLException
	 * @throws ClassNotFoundException
	 * @throws java.io.IOException
	 */
	public RandomDatabaseConnectionPreprocessed(String db, String user, String password) throws SQLException, ClassNotFoundException, IOException{
		Class.forName("com.mysql.jdbc.Driver");

		pp = new Preprocessor();


		// DB connection
	    connection = DriverManager.getConnection(
						"jdbc:mysql://localhost/"+db, user, password);

	    // db query
	    //TODO: Unlimit the amount of tweets retrieved from dataset
        //stmt = connection.prepareStatement("SELECT sentiment, text, date from text order by date");
        //Use right ordering!
        stmt = connection.prepareStatement("SELECT sentiment, minimalText, STR_TO_DATE(date, '%W %M %d %T PDT %Y') AS stamp FROM tweets ORDER BY RAND()"); // LIMIT 100000 OFFSET 0
		rs = stmt.executeQuery();

		//generate dataset from the results
		this.generateDataSet();

	}


/**
 * Methods generates the trainingSet from the result set of te DB
 * @throws java.sql.SQLException
 * @throws java.io.IOException
 */
public void generateDataSet() throws SQLException, IOException {


	while(rs.next()){


		//Sentiment of the tweet
		String type = rs.getString(1);

		//content of the tweet
		//String text = pp.proceed(rs.getString(2));
        String text = rs.getString(2);
		//date of the tweet
		String date = rs.getString(3);
//		System.out.println("Date in generateDataSet() from ExtendedDatabaseConnection" + date);

		//Generate Tweet Instance
		ExtendedTweet tweet = new ExtendedTweet();
		tweet.setText(text);
		tweet.setType(type);
		tweet.setDate(date);
//		System.out.println("Result of getType()" + tweet.getType());
		//Add Tweet to the Trainingset
		trainingSet.add(tweet);



	 }

//   connection.close();
//
//
//	connection = DriverManager.getConnection(
//			"jdbc:mysql://localhost/training2", "root", "ilpplm");
//
//		for(int j = 0; j<trainingSet.size(); j++){
//
//			Tweet t = trainingSet.get(j);
//			Statement stmt2;
//			String sql;
//
//
//
//
//			sql = "INSERT INTO tweets "
//			    + "VALUES "
//			    + "('" + t.getText() + "',"
//			    + "'" + t.getType() + "',"
//			    + "'" + date + "')";
//
//			stmt2 = connection.createStatement();
//			stmt2.executeUpdate(sql);
//			stmt2.close();
//		}
//
//


}

/**
 * Get trainingSet
 * @param
 * @since Jan 2017
 * @return Arraylist<p>ExtendedTweet<p/>
 * @author Damianos Melidis
 */

public ArrayList<ExtendedTweet> getTrainingSet()
{
	return this.trainingSet;
}

//public static void main (String[] args) throws SQLException, ClassNotFoundException, IOException{
//	DatabaseConnection db = new DatabaseConnection();
//
//}

}
