<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true" trimDirectiveWhitespaces="true"
    import="com.hevelian.exonite.core.*, com.hevelian.exonite.image.*, javax.servlet.http.*, java.util.*, java.io.*"%><%
  
    /*
    	GetImage creates different formats of a specified image, for server-side thumbnails etc.
    	Commands include:
    		resize = <w>:<h>
    		crop = <w> : <h> [: <x> : <y> ]
    		square = <n>
    		maximise = <w> : <h>
    		as = <format>
    		
    		Other parameters include:
    			filename = <path or url>
    			cache = <folder in data for thumbs>
    			
    	if the re-formatted image already exists in the thumb cache then this is returned instead of
    	re-processing the original - for performance reasons. The commands are processed in the exact
    	order they appear in the query and the same command may appear multiple times.
     */
     
	String _name 										= request.getParameter("filename");
    String _cache										= request.getParameter("cache");
    String _outputType									= request.getParameter("as");
	com.hevelian.exonite.image.Formatter formatter		= new com.hevelian.exonite.image.Formatter(_name, _cache);
	ArrayList<FormatCommand> formatOptions				= new ArrayList<FormatCommand>();

	formatter.setOutputType(_outputType);
	
	/* first we extract the parameters in order, which is important */
	String query = request.getQueryString();
	String[] params = query.split("&");
	
	for(int i=0; i<params.length; i++) {
		String param 			= params[i];
		if(param.equalsIgnoreCase("filename") || param.equalsIgnoreCase("cache")) continue;		// skip these
		
		String[] paramParts 	= param.split("="); 
		String[] options		= paramParts[1].split(":");
		
		FormatCommand command	= new FormatCommand(paramParts[0], options);
		formatter.addCommand(command);
		formatOptions.add(command);
	}
	
	File outputFile = formatter.run();
	if(outputFile==null) {
		System.out.println("ImageFormatter: failed to produce output file");
	} else {
		// OutputStream o = response.getOutputStream();
		
	}
%><%=outputFile.getAbsolutePath()%>