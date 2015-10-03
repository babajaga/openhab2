package org.openhab.binding.deconz.handler;

public class deCONZBridge {
	
	private String name = null;
	private String ip = null;
	private String mac = null;
	private String version = null;
	private String manufacturer = null;
	private String model = null;
	private String apiKey = null;

	public void setName(String string) {
		name = string;
	}

	public String getName() {
		return name;
	}

	public void setApiKey(String string) {
		apiKey = string;
	}

	public String getApi() {
		return apiKey;
	}
	
	public void setIpAddress(String string) {
		ip = string;
	}

	public String getIpAddress() {
		return ip;
	}
	
	public void setMacAddress(String string) {
		mac = string;
	}

	public String getMacAddress() {
		return mac;
	}

	public void setSoftwareVersion(String string) {
		version = string;
	}

	public String getSoftwareVersion() {
		return version;
	}
	
	public void setManufacturer(String string) {
		manufacturer = string;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setModel(String string) {
		model = string;
	}

	public String getModel() {
		return model;
	}
}
