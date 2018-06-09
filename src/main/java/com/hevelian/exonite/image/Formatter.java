package com.hevelian.exonite.image;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;

public class Formatter {

	private String outputType						= null;
	private ImageIndex index						= null;
	ArrayList<FormatCommand> formatOptions			= new ArrayList<FormatCommand>();
	ArrayList<FormatCommand> keyOptions				= new ArrayList<FormatCommand>();
	
	public Formatter(String filename, String cache) throws IOException {
		index = new ImageIndex(filename, cache);
	}
	
	public void addCommand(FormatCommand command) {
		formatOptions.add(command);
		keyOptions.add(command);
	}
	
	public void setOutputType(String type) {
		outputType = type;
	}
	
	public File run() throws IOException, InterruptedException, IM4JavaException, URISyntaxException {
		
		// first, if the required file already exists in the index, then we just return it ... lets check
		Collections.sort(keyOptions);
		if(index.srcExists()) {
			if(index.dstExists(keyOptions)) {
				return new File(index.getDstFilename());
			}
			
			index.fetchToIndex();
		}
		
		String srcFilename			= index.getSrcFilename();
		String dstFilename			= index.getDstFilename();
		
		ConvertCmd cmd = new ConvertCmd();
		IMOperation op = new IMOperation();
		
		op.addImage();
		
		for(int i=0; i<formatOptions.size(); i++) {
			FormatCommand command = formatOptions.get(i);
			
			int w = 0;
			int h = 0;
			int x = 0;
			int y = 0;
			
			switch(command.CommandName) {
			case "resize":
				w = Integer.parseInt(command.CommandParams[0]);
				h = Integer.parseInt(command.CommandParams[1]);
				op.resize(w, h);
				op.p_repage();
				break;
				
			case "crop":
				w = Integer.parseInt(command.CommandParams[0]);
				h = Integer.parseInt(command.CommandParams[1]);
				if(command.CommandParams.length==4) {
					x = Integer.parseInt(command.CommandParams[2]);
					y = Integer.parseInt(command.CommandParams[3]);
				}
				op.crop(w, h, x, y);
				op.p_repage();
				break;
				
			case "maximise":
			case "maximize":
				w = Integer.parseInt(command.CommandParams[0]);
				h = Integer.parseInt(command.CommandParams[1]);
				op.resize(w, h);
				op.crop(w, h, 0, 0);
				op.p_repage();
				break;
				
			case "square":
				Info info = new Info(srcFilename, true);
				int iw = info.getImageWidth();
				int ih = info.getImageHeight();
				
				// if the image is already a square, then we are good to go
				if(iw!=ih) {
					if(iw>ih) {
						// image is wider than taller (landscape)
						w = ih;
						h = ih;
						y = 0;
						x = (iw - ih) / 2;
					} else {
						// image is taller than wider (portrait)
						w = iw;
						h = iw;
						x = 0;
						y = (ih - iw) / 2;
					}
					op.crop(w, h, x, y);
					op.p_repage();
				}
				
				// if the user specified a size, then we also resize the square after cropping
				if(command.CommandParams.length>0) {
					int n = Integer.parseInt(command.CommandParams[0]);
					if(n > 0) {
						op.resize(n, n);
						op.p_repage();
					}
				}
				break;
			}
		}
		
		// now run it!
		cmd.run(op, srcFilename, dstFilename);
		return new File(dstFilename);
	}

}
