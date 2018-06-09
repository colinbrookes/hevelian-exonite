<%@ page language="java" contentType="text/xml; charset=UTF-8" pageEncoding="UTF-8" session="true"
    import="com.hevelian.exonite.accesscontrol.*, com.hevelian.exonite.core.*, javax.servlet.http.*, java.util.*" %><?xml version="1.0" encoding="UTF-8" ?><%
    
    HttpSession r_session = request.getSession();
	String status = (String) r_session.getAttribute("isLoggedIn");
    Session ex_session = (Session) r_session.getAttribute("session");
    
	String username = "J guest";
	
    if(status!=null) {
    	username = (String) r_session.getAttribute("username");	
    } else {
    	status = "false";
    }
    
%>
<record>
	<isLoggedIn><%=status%></isLoggedIn>
	<username><![CDATA[<%=username%>]]></username><%
	
	if(ex_session!=null) {
    	HashMap<String,String> props = ex_session.getUserProperties(request);
    	
    	for(Map.Entry<String,String> entry : props.entrySet()) {
    		String key = entry.getKey();
    		String value = entry.getValue();
    		
%>
	<<%=key%>><![CDATA[<%=value%>]]></<%=key%>>
<%    		
    	}
	}
	%>
</record>