package de.l3s.oscar.Preprocess;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * this class saves the informations for an update of the classification models
 * @author Alino
 *
 */
public class History {
	
	/**
	 * topic the tweets of the pdate belong to
	 */
	String topic = "";
	
	/**
	 * number of tweets in the updating set
	 */
	int number = 0;
	
	/**
	 * number of positive tweets in the updating set
	 */
	int posNumber = 0;
	
	/**
	 * number of negative tweets in the updating set
	 */
	int negNumber = 0;
	
	/**
	 * date of the update
	 */
	
	String date;
	
	/**
	 * Constructor
	 */
	public History(){
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date d = new Date();
		date = d.toString();
	}

}
