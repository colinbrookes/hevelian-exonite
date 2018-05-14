package com.hevelian.exonite.utils;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XLogger {

	public static final void log(String msg) {
		try {
			Date date = new Date();
			SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss.SSS zzz yyyy");
			File file = new File("/tmp/exonite.log");

			FileWriter fw = new FileWriter(file, true);
			fw.append(format.format(date) + " : " + msg + "\n");
			fw.close();
			
		} catch(Exception e) {
			return;
		}
	}
}
