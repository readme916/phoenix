package com.shangdao.phoenix.util;

public class InsideRuntimeException extends RuntimeException {
	
	private int code;
	private Object error;
	
	public InsideRuntimeException(int code,Object error){
		super(error.toString());
		this.code = code;
		this.error = error;
	}
	public InsideRuntimeException(Object error){
		super(error.toString());
		this.code = 500;
		this.error = error;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public Object getError() {
		return error;
	}
	public void setError(Object error) {
		this.error = error;
	}


}
