package org.openhab.binding.deconz.rest;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

public class RestHandler {

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String PROTOCOL_HTTP_NAME = "http://";
	private final static String PROTOCOL_HTTPS_NAME = "https://";
	
	protected String baseURL = new String();
	
	public void setBaseURL(String uri) {
		baseURL = checkURL(uri);
	}

	public boolean isConfigurationValid() {
		if ((baseURL != null) && (baseURL.length() > 0)) {
			return true;
		}
		return false;
	}

	protected RestResult executeGet(String url, String content) {
		RestResult ret = new RestResult();
		try {
			URL page = new URL(url);
			HttpURLConnection connection = (HttpURLConnection)page.openConnection();
			//add request header
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			try {
				if ((content != null) && (content.length() >0)) {
					connection.setDoOutput(true);
					DataOutputStream param = new DataOutputStream(connection.getOutputStream());
					param.writeBytes(content);
					param.flush();
					param.close();
				}
				convertHttpResponseCode(connection.getResponseCode(), ret);
				if (ret.getResult() == RestResult.REST_OK) {
					try {
						// read the response data
						ret.setData(connection.getInputStream());
					} catch (Exception e) {
						ret.setResult(RestResult.REST_RESPONSE_ERROR);
					}
				}
			} catch (Exception e) {
				ret.setResult(RestResult.REST_CONNECT_ERROR);
			}
		} catch (Exception e) {
			ret.setResult(RestResult.REST_CONNECT_ERROR);
		}
		return ret;
	}

	protected RestResult executePost(String url, String content) {
		RestResult ret = new RestResult();
		try {
			URL page = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) page.openConnection();
			//add request header
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setDoOutput(true);
			try {
				if ((content != null) && (content.length() > 0)) {
					DataOutputStream param = new DataOutputStream(connection.getOutputStream());
					param.writeBytes(content);
					param.flush();
					param.close();
				}
				convertHttpResponseCode(connection.getResponseCode(), ret);
				if (ret.getResult() == RestResult.REST_OK) {
					try {
						// read the response data
						ret.setData(connection.getInputStream());
					} catch (Exception e) {
						ret.setResult(RestResult.REST_RESPONSE_ERROR);
					}
				}
			} catch (Exception e) {
				ret.setResult(RestResult.REST_CONNECT_ERROR);
			}
		} catch (Exception e) {
			ret.setResult(RestResult.REST_CONNECT_ERROR);
		}
		return ret;
	}
	
	protected RestResult executePut(String url, String content) {
		RestResult ret = new RestResult();
		try {
			URL page = new URL(url);
			HttpURLConnection connection = (HttpURLConnection)page.openConnection();
			//add request header
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setRequestProperty("Content-Type", "application/json");
			try {
				if ((content != null) && (content.length() >0)) {
					connection.setDoOutput(true);
					DataOutputStream param = new DataOutputStream(connection.getOutputStream());
					param.writeBytes(content);
					param.flush();
					param.close();
				}
				convertHttpResponseCode(connection.getResponseCode(), ret);
				if (ret.getResult() == RestResult.REST_OK) {
					try {
						// read the response data
						ret.setData(connection.getInputStream());
					} catch (Exception e) {
						ret.setResult(RestResult.REST_RESPONSE_ERROR);
					}
				}
			} catch (Exception e) {
				ret.setResult(RestResult.REST_CONNECT_ERROR);
			}
		} catch (Exception e) {
			ret.setResult(RestResult.REST_CONNECT_ERROR);
		}
		return ret;
	}
	
	protected RestResult executeBasicAuthentication(String url, String secret, String content) {
		RestResult ret = new RestResult();
		try {
			URL page = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) page.openConnection();
			//add request header
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setRequestProperty("Authorization", "Basic " + secret);
			connection.setDoOutput(true);
			try {
				DataOutputStream param = new DataOutputStream(connection.getOutputStream());
				param.writeBytes(content);
				param.flush();
				param.close();
				convertHttpResponseCode(connection.getResponseCode(), ret);
				if (ret.getResult() == RestResult.REST_OK) {
					try {
						// read the response data
						ret.setData(connection.getInputStream());
					} catch (Exception e) {
						ret.setResult(RestResult.REST_RESPONSE_ERROR);
					}
				}
			} catch (Exception e) {
				ret.setResult(RestResult.REST_CONNECT_ERROR);
			}
		} catch (Exception e) {
			ret.setResult(RestResult.REST_CONNECT_ERROR);
		}
		return ret;
	}
	
	private void convertHttpResponseCode(int code, RestResult result) {
		switch (code) {
		case HttpURLConnection.HTTP_OK:
		case HttpURLConnection.HTTP_ACCEPTED:
		case HttpURLConnection.HTTP_CREATED:
			// the request was okay
			result.setResult(RestResult.REST_OK);
			break;
		case HttpURLConnection.HTTP_NOT_MODIFIED: 
			// we treat this as if the request was okay as this is the case
			result.setResult(RestResult.REST_NOT_MODIFIED);
			break;
		case HttpURLConnection.HTTP_FORBIDDEN:
		case HttpURLConnection.HTTP_UNAUTHORIZED:
			// not authorized
			result.setResult(RestResult.REST_AUTHENTICATION_ERROR);
			break;
		default:
			// everything else is an error
			result.setResult(RestResult.REST_CONNECT_ERROR);
			break;
		}
	}
	
	@SuppressWarnings("unused")
	private String mapToQueryString(Map<String, Object> map) {
	    StringBuilder string = new StringBuilder();
	    Boolean first = true;
	    String key, value;

	    if(map.size() > 0) {
	        string.append("?");
	    }

	    for(Entry<String, Object> entry : map.entrySet()) {
	    	if (!first) {
		        string.append("&");
	    	}
	        first = false;
	        
	        try {
		        key = entry.getKey();
		        value = entry.getValue().toString();

		        string.append(key);
		        string.append("=");
	        	string.append(value);
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
	    }
	    return string.toString();
	}

	private String checkURL(String url) {
		if ((url != null) && (url.length() > 0)) {
			// kill spaces at front and end
			url = url.trim();
			// check and remove '/' at the end
	        while (url.endsWith("/")) {
	        	url = url.substring(0, url.length() - 1);
	        }
			if ((url.startsWith(PROTOCOL_HTTP_NAME)) && (url.length() > PROTOCOL_HTTP_NAME.length())) {
				return url;
			}
			if ((url.startsWith(PROTOCOL_HTTPS_NAME)) && (url.length() > PROTOCOL_HTTPS_NAME.length())) {
				return url;
			}
		}
		return new String();
	}
}
