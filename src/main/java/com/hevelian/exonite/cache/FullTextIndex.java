package com.hevelian.exonite.cache;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.CRC32;

import org.w3c.dom.Element;

import com.hevelian.exonite.core.Configuration;
import com.hevelian.exonite.interfaces.Action;

/**
 * The word cloud is created from distinct words in any field of a record, whilst ignoring the stop words if supplied.
 * An index is automatically created which contains counters and stats about each word, and a file for each word which
 * contains a list of identifiers for each record which contained the word.
 * 
 * global counters for all words are kept in the index.dat file, in the folder named for the id of the index definition.
 * indexes/<id>/index.dat contains:
 * <word>@@<isStopWord>@@<cnt>
 * 
 * counters are also kept for each field processed, in a folder named after the NodeName of the field:
 * indexes/<id>/<field>/index.dat contains:
 * <word>@@<isStopWord>@@<cnt>
 * 
 * references to the cache filename are stored for each unique word found:
 * indexes/<id>/<safe word> contains:
 * <identifyBy>@@<filename>
 * 
 */
public class FullTextIndex {

	private Configuration config 						= new Configuration();
	private HashMap<String, Action> objects				= new HashMap<String, Action>();
	private HashSet<String> stopwords					= new HashSet<String>();
	
	private static final String INDEX_FILENAME			= "index.dat";
	
	private String id				= null;
	private String root				= null;
	private String stopWordFile		= null;
	private String identifyBy		= null;
	private String indexOn			= null;
	private String indexFolder		= null;
	
	public FullTextIndex(Element xml, HashMap<String, Action> map) {
		// copy the parent objects to this local object list
		objects.putAll(map);
		
		// get the params for this word cloud
		id				= xml.getAttribute("id");
		root 			= xml.getAttribute("root");
		stopWordFile 	= xml.getAttribute("stopWords");
		identifyBy		= xml.getAttribute("identifyBy");
		indexOn			= xml.getAttribute("indexOn");
		
		loadStopWords(config.getProperty("folder_home") + "data/" + stopWordFile);
		initialiseIndexStore();
		return;
		
	}
	
	private void initialiseIndexStore() {
		indexFolder = config.getProperty("folder_home") + "data/" + root + "/" + id + "/";
		
		try {
			File iFolder = new File(indexFolder);
			iFolder.mkdirs();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updateIndex(Element record, String filename) {

		String[] elements = indexOn.split(",");
		for(int i=0; i<elements.length; i++) {
			String field		= elements[i].trim();
			String wordString 	= (record.getElementsByTagName(elements[i].trim())).item(0).getTextContent();
			String[] words 		= wordString.split("[\\s|,|!|']");
			
			ArrayList<String> al		= new ArrayList<String>();
			
			for(int a=0; a<words.length; a++) {
				al.add(words[a]);
			}
			
			for(int w=0; w<al.size(); w++) {
				
				String word				= words[w];
				
				// not sure about this:
				if(word.startsWith("ht") || word.length()==0) continue;
				
				word = word.replaceAll(":|[.?+'\"]|;|/|\\(|\\)|:-\\)|…", " ").trim();
				String[] newword = word.split("\\s");
				if(newword.length>1) {
					for(int nw=0; nw<newword.length; nw++) {
						al.add(newword[nw].trim());
					}
					// so we add the new words to the array and then skip the current word completely (because its not a word we want to index).
					continue;
				}
				
				// we have each word, now we need to update the index
				File wordIndexFile 		= new File(indexFolder + createSafeString(word));
				File fieldIndexFile 	= new File(indexFolder + field + "/" + INDEX_FILENAME);
				String key				= record.getElementsByTagName(identifyBy).item(0).getTextContent() + "@@" + filename;
				String stopword			= "false";
				
				// find out if this is a stop word
				if(stopwords.size()>0) {
					if(stopwords.contains(word)) {
						stopword = "true";
					}
				}
				
				// if we find the key in any word file then we can assume we have processed the record before and skip it.
				boolean recordAlreadyKnown = false;
				
				try {
					if(!wordIndexFile.exists() && word.length()>0) {
						// word has never been seen before, so we need to setup a completely new index for it.
						FileOutputStream out = new FileOutputStream(wordIndexFile);
						out.write(key.getBytes("UTF-8"));
						out.write("\n".getBytes("UTF-8"));	// append a newline	
						out.flush();
						out.close();
						
						// we also need to update the index for the field
						File fieldFolder = new File(indexFolder + field + "/");
						fieldFolder.mkdirs();
						
						FileWriter fw = new FileWriter(fieldIndexFile, true);
						BufferedWriter bw = new BufferedWriter(fw);
						bw.append(word + "@@" + stopword + "@@" + "1" + "\n");
						bw.close();
						
						continue;
					} else {
						// the word has been found before, so we need to see if the key exists in the file
						boolean found = false;
						try(BufferedReader br = new BufferedReader(new FileReader(wordIndexFile))) {
						    for(String line; (line = br.readLine()) != null; ) {
						    	if(line.equalsIgnoreCase(key)) {
						    		recordAlreadyKnown = true;							// we have done this record before
						    		found = true;
						    		break;
						    	}
						    }
						    
						    // not found, so we append it
						    if(found==false) {
								FileWriter fw = new FileWriter(wordIndexFile, true);
								BufferedWriter bw = new BufferedWriter(fw);
								bw.append(key + "\n");
								bw.close();
						    }

						} catch(Exception e) {
							return;
						}
					}
					
					// and here we skip the rest of the record and just return
					if(recordAlreadyKnown) return;
					
					// we know the word previously existed.
					// now we check/add/update the fieldIndexFile
					File fieldFolder = new File(indexFolder + field + "/");
					fieldFolder.mkdirs();
					
					File fieldTmp = new File(indexFolder + field + "/" + INDEX_FILENAME + ".tmp");
					BufferedWriter bw = new BufferedWriter(new FileWriter(fieldTmp));
					boolean found = false;
					try(BufferedReader br = new BufferedReader(new FileReader(fieldIndexFile))) {
					    for(String line; (line = br.readLine()) != null; ) {

					    	if(found) {
					    		bw.write(line + "\n");
					    		continue;
					    	}

					    	String[] parts = line.split("@@");
					    	if(parts[0].equalsIgnoreCase(word) && parts[0].length()>0) {
					    		// we have found the word
					    		found = true;
					    		long cnt = Long.parseLong(parts[2]) + 1;
					    		bw.write(parts[0] + "@@" + parts[1] + "@@" + cnt + "\n");		// preserve the word and stopword, update the count
					    	} else {
					    		bw.write(line + "\n");
					    	}
					    }
					    br.close();
					    
					    // we didnt find it, so we add it
					    if(!found && word.length()>0) {
					    	bw.write(word + "@@" + stopword + "@@" + "1" + "\n");
					    }
					    bw.flush();
					    bw.close();
					    
					    // remove original, rename tmp file
					    fieldIndexFile.delete();
					    fieldTmp.renameTo(fieldIndexFile);
					    
					} catch(Exception e) {
					    if(!found && word.length()>0) {
					    	bw.write(word + "@@" + stopword + "@@" + "1" + "\n");
					    }
					    bw.flush();
					    bw.close();
					    fieldTmp.renameTo(fieldIndexFile);
						continue;
					}
					
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * creates a crc of the string so we can create safe filenames
	 * @param from
	 * @return
	 */
	private String createSafeString(String from) {
		CRC32 crc = new CRC32();
		try {
			crc.update(from.getBytes("UTF-8"));
			return Long.toHexString(crc.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			return from;
		}
	}
	
	/**
	 * Loads a stopword file into a set so we can ignore words we don't want to have in the cloud
	 * @param filename
	 */
	private void loadStopWords(String filename) {
		File file = new File(filename);
		
		if(filename==null || !file.exists()) return;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			for(String line; (line = br.readLine()) != null; ) {
				stopwords.add(line);
			}
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
