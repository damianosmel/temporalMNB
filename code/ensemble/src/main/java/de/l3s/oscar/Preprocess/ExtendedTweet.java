package de.l3s.oscar.Preprocess;

import java.util.ArrayList;

/**
 * Class saves all the informations of a single tweet: text, sentiment
 * @author Alino
 *
 */
public class ExtendedTweet {
	
	String tweet;
	String type;
	String id;
	String user;
	String query;
	String date;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	double[] classificationValues;
	
	
	public String getText() {
		
		return tweet;
	}

	public void setText(String tweet) {
		this.tweet = tweet;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	
	
	

}
