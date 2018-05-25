package com.shangdao.phoenix.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HTTPResponse {

	private int errorCode=0;
	private Object data="ok";
	private String  errorMessage="";
	private long id;
	
	public HTTPResponse() {
		super();
	}
	
	public HTTPResponse(long id) {
		super();
		this.id = id;
	}


	public HTTPResponse(int errorCode,String errorMessage ,Object data){
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.data = data;
	}
	
	public HTTPResponse(int errorCode,String errorMessage ,Object data, long id){
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.data = data;
		this.id = id;
	}
	

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	
}
