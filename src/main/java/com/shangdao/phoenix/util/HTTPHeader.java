package com.shangdao.phoenix.util;

public class HTTPHeader {

	private Terminal terminal;
	private String imei;
	private double longitude;
	private double latitude;
	
	
	public Terminal getTerminal() {
		return terminal;
	}

	public void setTerminal(Terminal terminal) {
		this.terminal = terminal;
	}


	public String getImei() {
		return imei;
	}


	public void setImei(String imei) {
		this.imei = imei;
	}


	public double getLongitude() {
		return longitude;
	}


	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}


	public double getLatitude() {
		return latitude;
	}


	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}


	public enum Terminal{
		ANDROID,IOS,WXWORK_DESK,WXWORK_MOBILE,WXPUBLIC,BROWSER,ELECTRON,API
	}
}
