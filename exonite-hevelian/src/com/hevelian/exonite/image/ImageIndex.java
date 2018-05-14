package com.hevelian.exonite.image;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.CRC32;

import com.hevelian.exonite.core.Configuration;

/**
 * (C) Hevelian -2014
 * This class manages the indexes for the cached image files and their thumbnails+variants.
 * original file in data/images/<cache>/files
 * index key files in data/images/<cache>/keys
 * thumb files in data/images/<cache>/thumbs
 * 
 * key file format is:
 * <orig_filename>@@<key>@@<options>@@<thumb_filename>
 * 
 * @author cb
 *
 */
public class ImageIndex {

	private static final String FOLDER_ROOT			= "data/images/";
	private static final String FOLDER_THUMBS		= "thumbs/";
	private static final String FOLDER_FILES		= "files/";
	private static final String FOLDER_KEYS			= "keys/";

	private String cache							= "";
	private Configuration config					= new Configuration();
	private String imageName						= null;	
	private String srcImageKey						= null;
	private String srcImageKeyPath					= null;

	private String dstImagePath						= null;
	private String dstImageKey						= null;
	private String dstImageOptions					= null;
	
	// the 3 important folders
	private String srcImageKeyFolder				= null;
	private String srcImageFolder					= null;
	private String dstImageFolder					= null;
	
	private ArrayList<ImageIndexKey> index			= new ArrayList<ImageIndexKey>();
	
	public ImageIndex(String image, String _cache) throws IOException {
		cache				= _cache;
		imageName 			= image;
		srcImageKey 		= createKey(imageName);
		
		if(cache.length()>0) {
			srcImageKeyFolder 	= config.getProperty("folder_home") + FOLDER_ROOT + cache + "/" + FOLDER_KEYS;
			srcImageFolder		= config.getProperty("folder_home") + FOLDER_ROOT + cache + "/" + FOLDER_FILES;
			dstImageFolder		= config.getProperty("folder_home") + FOLDER_ROOT + cache + "/" + FOLDER_THUMBS;
		} else {
			srcImageKeyFolder 	= config.getProperty("folder_home") + FOLDER_ROOT + FOLDER_KEYS;
			srcImageFolder 		= config.getProperty("folder_home") + FOLDER_ROOT + FOLDER_FILES;
			dstImageFolder 		= config.getProperty("folder_home") + FOLDER_ROOT + FOLDER_THUMBS;
		}
		
		// make sure all the folders exist
		File fsk = new File(srcImageKeyFolder);
		fsk.mkdirs();
		
		File fsi = new File(srcImageFolder);
		fsi.mkdirs();
		
		File fdi = new File(dstImageFolder);
		fdi.mkdirs();
		
		srcImageKeyPath 	= srcImageKeyFolder + srcImageKey;
		
		loadIndexKeyFile();
	}
	
	/**
	 * check if the source file already exists in the cache
	 * @return
	 */
	public boolean srcExists() {
		File file = new File(srcImageKeyPath);
		return file.exists();
	}
	
	/**
	 * check if tha specific variant exists of the src image (i.e. the formatted result) in the key file
	 * @return
	 */
	public boolean dstExists(ArrayList<FormatCommand> keyOptions) {
		String key = "";
		
		// build the key string
		for(int f=0; f<keyOptions.size(); f++) {
			key += keyOptions.get(f).CommandName;
			for(int o=0; o<keyOptions.get(f).CommandParams.length; o++) {
				key += ":" + keyOptions.get(f).CommandParams[o];
			}
		}
		
		dstImageOptions 	= key;
		dstImageKey 		= createKey(dstImageOptions);
		
		// now find the key in the index
		for(int k=0; k<index.size(); k++) {
			if(index.get(k).Key.equalsIgnoreCase(dstImageKey)) {
				dstImagePath = index.get(k).ThumbFilename;
				return true;
			}
		}
		return false;
	}
	
	public String getSrcFilename() {
		String f = srcImageFolder + srcImageKey;
		System.out.println("SRCIMAGE: " + f);
		return f;
	}
	
	public String getDstFilename() {
		String f = dstImageFolder + dstImageKey;
		System.out.println("DSTIMAGE: " + f);
		return f;
	}
	
	public void fetchToIndex() throws URISyntaxException, MalformedURLException {
		if(srcExists()) return;			// dont fetch it if it already exists
		
		// we need to figure out if this is a URL, URI or local file path
		if(imageName.startsWith("http:") || imageName.startsWith("https:")) {
			URL url = new URL(imageName);
			makeInputFileLocal(url);
			return;
		}
		
		if(imageName.startsWith("file:")) {
			URI uri = new URI(imageName);
			makeInputFileLocal(uri);
			return;
		}
		
		File src = new File(imageName);
		makeInputFileLocal(src);
		
		
	}
	
	private void makeInputFileLocal(File file) {
	}
	
	private void makeInputFileLocal(URL url) {
	}
	
	private void makeInputFileLocal(URI uri) {
	}

	private void loadIndexKeyFile() throws IOException {
		File file = new File(srcImageKeyPath);
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		for(String line; (line = br.readLine()) != null;) {
			String[] parts = line.split("@@");
			if(parts.length!=4) continue;
			
			ImageIndexKey idx = new ImageIndexKey(parts[0], parts[1], parts[2], parts[3]);
			index.add(idx);
		}
		
		br.close();
	}
	
	private String createKey(String from) {
		CRC32 crc = new CRC32();
		try {
			crc.update(from.getBytes("UTF-8"));
			return Long.toHexString(crc.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
}
