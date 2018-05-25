package com.shangdao.phoenix.util;

import java.util.ArrayList;
import java.util.HashMap;

public class HTTPListResponse extends HTTPResponse{
	
	private Object data;
	private long total;
	private long pageSize;
	private long pageNumber;
	
	public HTTPListResponse(Object data , long total, long pageSize,long pageNumber) {
		this.data = data;
		this.total = total;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public long getPageSize() {
		return pageSize;
	}

	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}

	public long getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(long pageNumber) {
		this.pageNumber = pageNumber;
	}

	
}
