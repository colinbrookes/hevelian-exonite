<%@ page language="java" contentType="text/xml; charset=UTF-8" pageEncoding="UTF-8" session="true"
    import="com.hevelian.exonite.accesscontrol.*, com.hevelian.exonite.core.*, javax.servlet.http.*" %><?xml version="1.0" encoding="UTF-8" ?><%
    
//    Session ex_session = new Session(request);
//    Configuration ex_config = new Configuration();

	HttpSession r_session = request.getSession();
	String status = (String) r_session.getAttribute("isLoggedIn");
    Session ex_session = (Session) r_session.getAttribute("session");
    
	String username = "guest";
	
    if(ex_session!=null) {
    	username = (String) request.getSession().getAttribute("username");
    }
	
	if(status==null) status = "false";
	
%><result>
	<status><%= status %></status>
	<username><![CDATA[<%= username %>]]></username>
</result>
