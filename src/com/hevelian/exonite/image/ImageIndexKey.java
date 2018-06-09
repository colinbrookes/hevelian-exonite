package com.hevelian.exonite.image;

public class ImageIndexKey {

	public String OriginalFilename			= null;
	public String Key						= null;
	public String Options					= null;
	public String ThumbFilename				= null;
	
	public ImageIndexKey(String orig, String key, String options, String thumb) {
		this.OriginalFilename 	= orig;
		this.Key 				= key;
		this.Options 			= options;
		this.ThumbFilename 		= thumb;
	}
}
