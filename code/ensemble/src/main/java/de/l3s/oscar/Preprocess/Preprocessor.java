package de.l3s.oscar.Preprocess;
/**
 * The Preprocessing class deals with:
 * Stopwords removal
 * Smileys taggen
 * Laughing taggen
 * special charackters Removal
 * 
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import weka.core.Stopwords;
import weka.core.stemmers.SnowballStemmer;
import weka.core.stemmers.Stemmer;

public class Preprocessor {

	/**
	 * Server usage
	 * */
	public boolean runningOnServer = false;
//	public boolean runningOnServer = true;


	// ######################### ATTRIBUTES ###########################
	public int verbs = 0;
	public int adj3 = 0;
	public int colloq = 0;
	public int rep;
	public int colTweets = 0;
	public int adj2 = 0;
	public int size = 0;
	public int pos = 0;
	public int neg = 0;
	public int smile = 0;
	public int tweets = 0;
	public int tweetsRep = 0;
	public int love = 0;
	public boolean col = true;
	public boolean rev = true;
	public int anzahl = 0;
	
	public int stopw = 0;
	public int tw = 0;
	public int sc = 0;
	FileReader fr;
	BufferedReader br;
	
	ArrayList <String> unstemmed = new ArrayList <String> (); 
	ArrayList <String> stemmed = new ArrayList <String> (); 
	//ArrayListConverter listConverter= new ArrayListConverter();
	HashMapConverter mapConverter = new HashMapConverter();
	HashMap values = new HashMap();
	
	
	/**
	 * removes stopwords from textfile
	 */
	Stopwords st = new Stopwords();
	/**
	 * stemmer for the textfile
	 */
	Stemmer stemmer = new SnowballStemmer();
	
	HashMap verbList;
	HashMap colloqDictionary;
	
	
	public Preprocessor() throws IOException{

		if(runningOnServer){
			//Deken

			/*this.verbList = this.mapConverter.createKeyMap("/media/fs/data-central/home/melidis/oscar/preprocessingLists/verbs.txt");
			this.colloqDictionary = this.mapConverter.creaTeMap("/media/fs/data-central/home/melidis/oscar/preprocessingLists/colloquial.txt");*/
			//Prometheus
			this.verbList = this.mapConverter.createKeyMap("/home/melidis/oscar/preprocessingLists/verbs.txt");
			this.colloqDictionary = this.mapConverter.creaTeMap("/home/melidis/oscar/preprocessingLists/colloquial.txt");
		}
		else {
			/*this.verbList = this.mapConverter.createKeyMap("/home/damian/Desktop/Implementation/OSCAR/preprocessingLists/verbs.txt");
			this.colloqDictionary = this.mapConverter.creaTeMap("/home/damian/Desktop/Implementation/OSCAR/preprocessingLists/colloquial.txt");*/
			this.verbList = this.mapConverter.createKeyMap("/home/damian/Desktop/OSCAR/Ensemble_WP/ensemble/preprocessingLists/verbs.txt");
			this.colloqDictionary = this.mapConverter.creaTeMap("/home/damian/Desktop/OSCAR/Ensemble_WP/ensemble/preprocessingLists/colloquial.txt");
		}
	}

	// ########################## METHODS ################################
	/**
	 * This methos prepares a tweet for data mining processes it includes:
	 * - stopwords removal
	 * - hashTag removal
	 * - HyperLink removal
	 * - special character removal
	 * - emoticons tagging
	 * - colloquial language convertion
	 * - negations tagging with verbs
	 * - negations tagging eith adjectives
	 * - stemming
	 * @param tweetText
	 * @return
	 * @throws IOException
	 */

	
	public String proceed(String tweetText) throws IOException {
		
		col = false;
		rev = false;
		// build a string[] of the tweettext
		String[] strings = tweetText.split(" ");
		
		StringBuilder builder = new StringBuilder();
		ArrayList <String> fin = new ArrayList<String>(); 
		//Start preprocessing
		for (int i = 0; i < strings.length; i++) {
			
			
		if (strings[i] != null) {
				

  			   // step 2: convert all letter to lower case
				strings[i] = strings[i].toLowerCase();


				// step 3: converting emoticons to words
			
			//pos
			if (strings[i].startsWith(":") && strings[i].endsWith(")")|| strings[i].startsWith(";")&& strings[i].endsWith(")")
					|| strings[i].startsWith("(")
					&& strings[i].endsWith(":") || strings[i].startsWith("(")
					&& strings[i].endsWith(";")|| strings[i].startsWith("(")
					&& strings[i].endsWith("=")|| strings[i].startsWith("=")
					&& strings[i].endsWith(")")) {
				//System.out.println(strings[i]);
				strings[i] = "positive";
				//System.out.println(strings[i] + " Proceeded positive: " + pos);
				pos++;

			}

			// neg
			if (strings[i].startsWith(":") && strings[i].endsWith("(")|| strings[i].startsWith(";") && strings[i].endsWith("(")
					|| strings[i].startsWith(")") && strings[i].endsWith(":") || strings[i].startsWith(")") && strings[i].endsWith(";")
					|| strings[i].startsWith("=") && strings[i].endsWith("(") || strings[i].startsWith(")") && strings[i].endsWith("=")) {
				//System.out.println(strings[i]);
				strings[i] = "negative";
				//System.out.println(strings[i] + " Proceeded negative: " + neg);
			    //System.out.println(strings[i]);
				neg++;
			}			
			// smile
			if (strings[i].startsWith(":") && strings[i].endsWith("D")
					|| strings[i].startsWith(";") && strings[i].endsWith("D")
					|| strings[i].startsWith("haha")
					|| strings[i].startsWith("HAHA") || strings[i] == "lol"
					|| strings[i].startsWith("*g")
					|| strings[i].startsWith("=")
					&& strings[i].endsWith("D")) {
				//System.out.println(strings[i] + " Proceeded smile: " + smile);
				strings[i] = "smile";
				//System.out.println(strings[i]);
				smile++;
				
			}

			//love
		if ((strings[i].startsWith("&lt;") && strings[i].endsWith("3"))) {
				
				//System.out.println(strings[i] + " Proceeded love: " + love);
				strings[i] = "love";
				love ++;
				//System.out.println(strings[i]);
		
			}

				// step 4: negations tagging with verbs

				if (strings[i] != null && (strings[i].endsWith("n't") || strings[i].endsWith("not")||  strings[i].equals("nor") || strings[i].equals("neither"))) {
					if(i+1 < strings.length){
					String[] temp =  this.tagNegationsVerbs(strings[i], strings[i + 1]);
					strings[i] = temp[0];
					strings[i + 1] = temp[1];
					
					
					}
					}

				// step 5: taggig negations with adjectives

				if (strings[i] != null &&(strings[i].endsWith("n't") ||strings[i].endsWith("not") || strings[i].equals("not") ||  strings[i].equals("nor") || strings[i].equals("neither"))) {
					if(i+1 < strings.length && (strings[i+1].equals("so") || strings[i+1].equals("as")|| strings[i+1].equals("very") || strings[i+1].equals("such") || strings[i+1].equals("that") || strings[i+1].equals("much")|| strings[i+1].equals("the") || strings[i+1].equals("a"))&& i+2 <strings.length){
					String[] temp = this.tagNegationsAdjective(strings[i], strings[i+2], "   TYPE : 3");
					strings[i]  = temp[0];
					strings[i+1] = null;
					strings[i + 2] = temp[1];
					
					}
					else if(i+1 < strings.length){
						String[] temp = this.tagNegationsAdjective(strings[i], strings[i+1], "   TYPE : 2");
						strings[i]  = temp[0];
						strings[i + 1] = temp[1];
						
						
					}

				}

			// step 5: convert colloqial language
			strings[i] = this.removeRepetitions(strings[i]);
			String[] result = this.transformColloquialLanguage(strings[i]);
			
			int length = 0;
			String word = strings[i];
			
			while(length < result.length){
				word = result[length];
				length++;
			

				// step 1:  remove the hashtag
				if (word.startsWith("#")) {
					word = word.substring(1);
					
				}
				
				// step 6:  remove all the special characters from the string
				
				if (!word.matches("[a-zA-Z]")) {
					word = word.replaceAll("[^a-zA-Z]", "");
				}
				if(word.matches("[0-9]")){
					word = "";
					sc = sc+1;	
				}
								
				// step 7: remove stopp words, numbers, hyperlinks
				
				//Stopwords removal
				if(st.is(word)){
					word = "";
					stopw = stopw+1;
					
				}
				
				if(word.startsWith("@")|| word.startsWith("http") ){
					word = "";
					tw = tw+1;
				}
				
				
				word = word.replaceAll(" ", "");
				
				if (word != null && !word.startsWith(" ")
						&& !word.equals("")) {
					// //System.out.println(st.is(strings[i]));

				// step 8 : stemming
					
					if (!unstemmed.contains(word)){
						unstemmed.add(word);
					}
					
					Stemmer stemmer = new SnowballStemmer();
				    word = stemmer.stem(word);
				    
				    
				    if (!stemmed.contains(word)){
						stemmed.add(word);
					}

					builder.append(word + " ");
					anzahl++;

				}
			}
		}
		}
		
//
//		if (result != null && !result.startsWith(" ") && !result.equals("")) {
//			result = result.substring(0, result.length() - 1);
//			Tweet tweet = new Tweet();
//
//			tweet.setText(result);
//			tweet.setType(type);
//			return tweet;
				
	
			
			
		
		String result = builder.toString();
		////System.out.println("Tweet:  " + result);
		//this.finish();
		return result;
	}
		
	


	
	/**
	 * 
	 */
	public String[] tagNegationsAdjective(String s1, String s2, String type) throws IOException {
		String[] result = new String[2];
		

		fr = new FileReader("opposites.txt");
		br = new BufferedReader(fr);
		
		String vorgaenger = br.readLine();
		String aktuell;
		while (vorgaenger != null) {

			aktuell = br.readLine();
			// end of file
			if (s2.equals(vorgaenger)) {
				s1 = aktuell;
				s2= null;
				// //System.out.println(strings[i]);
				 //System.out.println(s1 + "\n");
				if(type.equals("   TYPE : 3")){
					//System.out.println(type + " proceeded :  " + adj3);
					adj3++;
				}
				else{
					
				 //System.out.println(type + " proceeded :  " + adj2);
					adj2++;
				}
				
				break;

			}

			if (s2.equals(aktuell)) {
				s1 =  vorgaenger;
				s2 = null;
				//System.out.println(s1 + "\n");
				if(type.equals("   TYPE : 3")){
					//System.out.println(type + " proceeded :  " + adj3);
					adj3++;
				}
				else{
					
					//System.out.println(type + " proceeded :  " + adj2);
					adj2++;
				}
				// //System.out.println(strings[i]);
				break;

			}
			vorgaenger = br.readLine();
		}
		result[0] = s1;
		result[1] = s2;
		return result;
		
	}
	/**
	 * this method tags negations with verbs
	 * @param s1 			keyword - signal word for negation
	 * @param s2 			next - word after the keyword
	 * @throws IOException
	 */
	public String[] tagNegationsVerbs(String s1, String s2) throws IOException {
		String[] result = new String[2];
		s1 = s1.replace("n't", "not");

		if (isVerb(s2)) {
			s1 = "NOT";
			s1 = s1 + s2;
			s2 = null;
			//System.out.println("Negations Tagged with verbs: " + verbs );
			//System.out.println(s1);
		    verbs = verbs+1;
		}
		result[0] = s1;
		result[1] = s2;
		// //System.out.println(s1);
		// //System.out.println(s2);
		return result;

	}
	
	/**
	 * this prooves whether a word is a verb
	 * @param s word
	 * @throws IOException
	 */
	public boolean isVerb(String s) throws IOException {
		if(verbList.containsKey(s)){
			return true;
		}
		return false;
	}


	public String removeRepetitions(String s) {
            String moin = s;
		// dealing with colloqial language
			s = s.replaceAll("a{2,}+", "a");
			s = s.replaceAll("o{3,}+", "o");
			s = s.replaceAll("u{2,}+", "u");
			s = s.replaceAll("i{2,}+", "i");
			s = s.replaceAll("e{3,}+", "e");
			if(!moin.equals(s)){
			////System.out.println("  --->  " + s);
			//System.out.println("Repetitions removed: " + rep);
			rep++;
			
			if(rev == false){
				rev = true;
				tweetsRep++;
			}
			}

		return s;

	}
	

	public String[] transformColloquialLanguage(String s) throws IOException{
		
		String r = (String)colloqDictionary.get(s);
		if(r != null && !r.startsWith(" ")){
			if (!values.containsKey(s)){
				values.put(s, Integer.valueOf(1));
				////System.out.println(values.get(s));
			}
			else{
				int wert = (Integer) values.get(s)+1;
				values.put(s, Integer.valueOf(wert));
				////System.out.println(values.get(s));
			}
			////System.out.println("key: " + s);
			s = (String) colloqDictionary.get(s);
			//System.out.println("Value: " + s);
			//System.out.println("Colloq Transformations made: " + colloq);
			colloq++;
			
			if(col == false){
				col = true;
				tweets++;
			}
			
		}

		String[] result = s.split(" ");
		return result;
		
	}
	public void finish(){
		
//		//System.out.println("Stopwords removed: " + stopw);
//		//System.out.println("Specials removed: " + sc);
//		//System.out.println("TwitterWords removed: " + tw);
		int summe = tw+stopw+sc;
//	//System.out.println("Summe: " + summe); 
		//System.out.println(unstemmed.size());
		//System.out.println(stemmed.size());
//		//System.out.println("Words after modifying: " + size);
		Iterator iterator = values.keySet().iterator();
		while(iterator.hasNext()) {
			String key = (String) iterator.next();
		    int wert = (Integer) values.get(key);
		    if(wert > 1000){
		    	//values.remove(key);
		    	//System.out.println(key + " " + wert);
		    }
		}
		
		
		
		//System.out.println(values.toString());
//		//System.out.println("Transformtaion: " + tweets);
//		//System.out.println("Revsionen: " + tweetsRep);
		
	
	
}}
