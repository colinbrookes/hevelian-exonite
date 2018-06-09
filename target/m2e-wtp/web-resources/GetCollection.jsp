<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true"
    import="com.hevelian.exonite.core.*, javax.servlet.http.*"%><?xml version="1.0" encoding="UTF-8" ?>
<%
	String _name = request.getParameter("collection");
	Collection _collection = new Collection(_name, request);
%><%= _collection.select() %>
