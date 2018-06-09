package com.hevelian.exonite.api;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.hevelian.exonite.core.Collection;

@Controller
@RequestMapping(path = "/collection.svc")
public class CollectionController {

	@RequestMapping(value="/{CollectionName}", method=RequestMethod.GET)
	public ResponseEntity<byte[]> GetCollection(HttpServletRequest request, @PathVariable String CollectionName) throws UnsupportedEncodingException, ParseException, SQLException {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		
		Collection collection = new Collection(CollectionName, request);
		return new ResponseEntity<byte[]>(collection.select().getBytes("UTF-8"), responseHeaders, HttpStatus.OK);
	}

}
