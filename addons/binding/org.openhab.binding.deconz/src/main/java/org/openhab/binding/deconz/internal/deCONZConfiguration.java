package org.openhab.binding.deconz.internal;

public class deCONZConfiguration {

	public final static int PROTOCOL_HTTP = 1;
	public final static String PROTOCOL_HTTP_NAME = "http://";
	public final static int PROTOCOL_HTTPS = 2;
	public final static String PROTOCOL_HTTPS_NAME = "https://";
	
    private String deconzInstance = null;
    private String deconzUsername = null;
    private String deconzPassowrd = null;
    private String deconzApiKey = null;
    private String deconzVersion = null;
    @SuppressWarnings("unused")
	private int protocol = PROTOCOL_HTTP;
    
	public void setInstance(String string) {
		deconzInstance = checkInstanceURL(string);
	}

	public String getInstance() {
		return deconzInstance;
	}
	
	public void setUserName(String string) {
		deconzUsername = string;
	}

	public String getUserName() {
		return deconzUsername;
	}

	public void setPassword(String string) {
		deconzPassowrd = string;
	}

	public String getPassword() {
		return deconzPassowrd;
	}

	public void setApiKey(String string) {
		deconzApiKey = string;
	}

	public String getApiKey() {
		return deconzApiKey;
	}

	public void setSoftwareVersion(String version) {
		deconzVersion = version;
	}

	public String getSoftwareVersion() {
		return deconzVersion;
	}
	
	public boolean isValid() {
		if ((deconzInstance != null) && (deconzInstance.length() > 0)) {
			return true;
		}
		return false;
	}

	private String checkInstanceURL(String url) {
		if ((url != null) && (url.length() > 0)) {
			// kill spaces at front and end
			url = url.trim();
			// check and remove '/' at the end
	        while (url.endsWith("/")) {
	        	url = url.substring(0, url.length() - 1);
	        }
			if ((url.startsWith(PROTOCOL_HTTP_NAME)) && (url.length() > PROTOCOL_HTTP_NAME.length())) {
				protocol = PROTOCOL_HTTP;
				return url;
			}
			if ((url.startsWith(PROTOCOL_HTTPS_NAME)) && (url.length() > PROTOCOL_HTTPS_NAME.length())) {
				protocol = PROTOCOL_HTTP;
				return url;
			}
		}
		return new String();
	}
}
