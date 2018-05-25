package com.shangdao.phoenix.util;

public class OutsideRuntimeException extends RuntimeException {
	
	private int code;
	private Object error;
	
	public OutsideRuntimeException(int code,Object error){
		super(error.toString());
		this.error = error;
		this.code = code;
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
