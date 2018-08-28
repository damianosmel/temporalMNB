package de.l3s.oscar.Preprocess;
import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;



public class HashMapConverter {
	
HashMap map = new HashMap();

public HashMap creaTeMap(String filename) throws IOException{
	FileReader fr = new FileReader(filename);
	BufferedReader br = new BufferedReader(fr);
	String entry;
	int i = 0;
	while ((entry = br.readLine()) != null ) {

		i++;
		String[] split = entry.split("::");
		if(split.length>1 && split[1] != null && !split[1].startsWith(" ")){
			map.put(split[0], split[1]);
			//System.out.println(split[0]);
			
		}
	
		}

	return map;
	
}

public HashMap createKeyMap(String filename) throws IOException{
	FileReader fr = new FileReader(filename);
	BufferedReader br = new BufferedReader(fr);
	String entry;
	
	while ((entry = br.readLine()) != null ) {
		
			map.put(entry, " ");
		}
		

	return map;
	
}
}
