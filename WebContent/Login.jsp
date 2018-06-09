<%@ page language="java" contentType="text/xml; charset=UTF-8" pageEncoding="UTF-8" session="true"
    import="com.hevelian.exonite.accesscontrol.*, com.hevelian.exonite.core.*, javax.servlet.http.*" %><?xml version="1.0" encoding="UTF-8" ?><%
    
    Session ex_session = new Session(request);
    Configuration ex_config = new Configuration();

	String status = "false";
	String message = "invalid username or password";
	
	String username = request.getParameter("frm_username");
	String password = request.getParameter("frm_password");
	boolean result = ex_session.AttemptLogin(username, password);
	
	String url = ex_session.getURL();
	
	if(result) {
		HttpSession r_session = request.getSession();
		r_session.setAttribute("session", ex_session);
		r_session.setAttribute("isLoggedIn", "true");
		r_session.setAttribute("username", username);
		
		status = "true";
		message = "";
	}
		
%><result>
	<status><%=status%></status>
	<url><![CDATA[<%=url%>]]></url>
	<message><![CDATA[<%=message%>]]></message>
</result>