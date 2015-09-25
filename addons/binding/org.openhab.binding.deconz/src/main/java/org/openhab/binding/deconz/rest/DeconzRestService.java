package org.openhab.binding.deconz.rest;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.openhab.binding.deconz.handler.deCONZLight;
import org.openhab.binding.deconz.handler.deCONZLightStateUpdate;
import org.openhab.binding.deconz.handler.deCONZSensor;

public class DeconzRestService {
	
	private Boolean authenticated = false;
	private String apiKey = null;
	private String baseURI = new String();
	
	private final static String USER_AGENT = "Mozilla/5.0";
	
	private static TrustManager[] trustAllCertificates = new TrustManager[] {
        new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Do nothing. Just allow them all.
			}
			@Override
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Do nothing. Just allow them all.
			}
			@Override
			public X509Certificate[] getAcceptedIssuers() {
                return null; // Not relevant.
			}
        }
    };

    private static HostnameVerifier trustAllHostnames = new HostnameVerifier() {
		@Override
		public boolean verify(String arg0, SSLSession arg1) {
            return true; // Just allow them all.
		}
    };
	
	public Boolean isAuthenticated() {
		return authenticated;
	}

	public void setBaseURI(String uri) {
		if (uri != null) {
			baseURI = new String(uri);
		} else {
			baseURI = new String();
		}			
	}

	public void setApiKey(String key) {
		if (key != null) {
			apiKey = key;
		} else {
			apiKey = new String();
		}			
	}
	
	public RestResult authenticateUnlocked(RestReader notify) {
		RestResult ret = new RestResult();
		// clear the API key
		apiKey = null;
		// clear authenticated
		authenticated = false;
		
		String url = baseURI; 
		if (url.length() < 1) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}

		url += "/api";
		ret = executePost(url, DeconzRestJson.createAPIKeyRequest("smarthome deconz binding"));
		handleAuthenticateResult(ret, notify);
		return ret;
	}
	
	public RestResult authenticateBasic(String username, String password, RestReader notify) {
		RestResult ret = new RestResult();
		// clear the API key
		apiKey = null;
		// clear authenticated
		authenticated = false;

		String url = baseURI;
		if (url.length() < 1) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}

		if ((username == null) || (username.length() < 1)) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}
		if ((password == null) || (password.length() < 1)) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}
		String auth = username + ":" + password;
		
		byte[] authEncBytes = Base64.encodeBase64(auth.getBytes());
		String authEnc = new String(authEncBytes);
		url += "/api";
		executeAuthentication(url, authEnc, DeconzRestJson.createAPIKeyRequest("smarthome deconz binding"));
		handleAuthenticateResult(ret,  notify);
		return ret;
	}
	
	public RestResult connect(RestReader notify) {
		RestResult ret = new RestResult();
		// Access the API using a defined API key
		String url = baseURI;
		if (url.length() < 1) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}
		if ((apiKey == null) || (apiKey.length() < 1)) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}

		url += "/api/" + apiKey + "/config";
		ret = executeGet(url, null);
		if (ret.getResult() == RestResult.REST_OK) {
			// the API key did work
			if (ret.getData() != null) {
				JSONObject response = ret.getData();
				authenticated = true;
				if ((notify != null) && (notify.getBridgeReader() != null)) {
					// Read the values
					try {
						String name, ip, mac, version;
						name = response.getString("name");
						ip = response.getString("ipaddress");
						mac = response.getString("mac");
						version = response.getString("swversion");
						notify.getBridgeReader().setBridgeName(name);
						notify.getBridgeReader().setBridgeIpAddress(ip);
						notify.getBridgeReader().setBridgeMacAddress(mac);
						notify.getBridgeReader().setBridgeSoftwareVersion(version);
					} catch (Exception e) {
//						e.printStackTrace();
						ret.setResult(RestResult.REST_RESPONSE_ERROR);
					}
				}
			} else {
				ret.setResult(RestResult.REST_RESPONSE_ERROR);
			}
		}
		return ret;
	}

	public RestResult getDevices(RestReader notify) {
		RestResult ret = new RestResult();
		// Access the API using a defined API key
		String url = baseURI;
		if (url.length() < 1) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}
		if ((apiKey == null) || (apiKey.length() < 1)) {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			return ret;
		}

		url += "/api/" + apiKey;
		ret = executeGet(url, null);
		if (ret.getResult() == RestResult.REST_OK) {
			if (ret.getData() != null) {
				if (notify != null) {
					if (notify.getLightReader() != null) {
						// read the lights
						try {
							JSONObject response = DeconzRestJson.parseLightsResponse(ret.getData());
							readLights(response, notify);
						} catch (Exception e) {
						}
					}
					if (notify.getSensorReader() != null) {
						// read the sensors
						try {
							JSONObject response = DeconzRestJson.parseSensorsResponse(ret.getData());
							readSensors(response, notify);
						} catch (Exception e) {
						}
					}
				}
			} else {
				ret.setResult(RestResult.REST_RESPONSE_ERROR);
			}
		}
		return ret;
	}

	public RestResult setLightState(deCONZLight light, deCONZLightStateUpdate newState) {
		RestResult ret = new RestResult();
		// TODO send the changed light state to the bridge
		ret.setResult(RestResult.REST_OK);
		return ret;
	}

	public RestResult setSensorState(deCONZSensor sensor, deCONZLightStateUpdate newState) {
		RestResult ret = new RestResult();
		// TODO send the changed sensor state to the bridge
		ret.setResult(RestResult.REST_OK);
		return ret;
	}
	
	private RestResult executeAuthentication(String url, String secret, String content) {
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

	private void handleAuthenticateResult(RestResult result, RestReader notify) {
		if (result.getResult() == RestResult.REST_OK) {
			try {
				String key = DeconzRestJson.parseAPIKeyResponse(result.getData());
				if (key.length() > 0) {
					apiKey = key;
					authenticated = true;
					if ((notify != null) && (notify.getBridgeReader() != null)) {
						notify.getBridgeReader().setBridgeApiKey(key);
					}
				} else {
					result.setResult(RestResult.REST_RESPONSE_ERROR);
				}
			} catch (Exception e) {
				e.printStackTrace();
				result.setResult(RestResult.REST_RESPONSE_ERROR);
			}
		}
	}
	
	private RestResult executeGet(String url, String content) {
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

	private RestResult executePost(String url, String content) {
		RestResult ret = new RestResult();
		if ((content != null) && (content.length() > 0)) {
			try {
				URL page = new URL(url);
				HttpURLConnection connection = (HttpURLConnection) page.openConnection();
				//add request header
				connection.setRequestMethod("POST");
				connection.setRequestProperty("User-Agent", USER_AGENT);
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
		} else {
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
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

	private void readLights(JSONObject lights, RestReader notify) {
		System.out.println(lights.toString());
		Set<String> keys = lights.keySet();
		for (String id : keys) {
			JSONObject light = lights.getJSONObject(id);
			if (light != null) {
				try {
					String name, model, version, type, maker, unique;
					name = light.getString("name");
					model = light.getString("modelid");
					type = light.getString("type");
					version = light.getString("swversion");
					maker = light.getString("manufacturer");
					unique = light.getString("uniqueid");
					JSONObject state = light.getJSONObject("state");
					if (state != null) {
						boolean on, online;
						int bri, hue, sat, ct;
						on = state.getBoolean("on");
						online = state.getBoolean("reachable");
						bri = state.getInt("bri");
						hue = state.getInt("hue");
						sat = state.getInt("sat");
						ct = state.getInt("ct");
						deCONZLight l = new deCONZLight(id, name, model, type, version, maker, unique, on, online, bri, hue, sat, ct);
						notify.getLightReader().onLightInfo(l);
					}
				} catch (Exception e) {
				}
			}
		}
	}

	private void readSensors(JSONObject sensors, RestReader notify) {
		System.out.println(sensors.toString());
		Set<String> keys = sensors.keySet();
		for (String id : keys) {
			JSONObject sensor = sensors.getJSONObject(id);
			if (sensor != null) {
				try {
					String name, model, version, type, maker, unique;
					name = sensor.getString("name");
					model = sensor.getString("modelid");
					type = sensor.getString("type");
					version = sensor.getString("swversion");
					maker = sensor.getString("manufacturername");
					unique = sensor.getString("uniqueid");
					JSONObject config = sensor.getJSONObject("config");
					if (config != null) {
						boolean on, online;
						online = config.getBoolean("reachable");
						on = config.getBoolean("on");
						deCONZSensor l = new deCONZSensor(id, name, model, type, version, maker, unique, on, online);
						notify.getSensorReader().onSensorInfo(l);
					}
				} catch (Exception e) {
				}
			}
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
		
	// Sometimes you need to connect a HTTPS URL. In that case, you may likely face a 
	// javax.net.ssl.SSLException: Not trusted server certificate on some HTTPS sites who 
	// doesn't keep their SSL certificates up to date, or a 
	// java.security.cert.CertificateException: No subject alternative DNS name matching [hostname] found or 
	// javax.net.ssl.SSLProtocolException: handshake alert: unrecognized_name on some misconfigured HTTPS sites.
	// The following one-time-run initializer should make HttpsURLConnection more lenient as to those 
	// HTTPS sites and thus not throw those exceptions anymore.
	public static void httpsTrustAllCerticicates() {
	    try {
	        System.setProperty("jsse.enableSNIExtension", "false");
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCertificates, new SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	        HttpsURLConnection.setDefaultHostnameVerifier(trustAllHostnames);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
