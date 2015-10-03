package org.openhab.binding.deconz.rest.bridge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.codec.binary.Base64;
import org.openhab.binding.deconz.handler.deCONZBridge;
import org.openhab.binding.deconz.handler.deCONZDevice;
import org.openhab.binding.deconz.handler.deCONZGroup;
import org.openhab.binding.deconz.handler.deCONZLightState;
import org.openhab.binding.deconz.handler.deCONZSensorState;
import org.openhab.binding.deconz.handler.deCONZTouchlinkState;
import org.openhab.binding.deconz.json.JSONObject;
import org.openhab.binding.deconz.rest.RestResult;
import org.openhab.binding.deconz.rest.bridge.deCONZRestReader.RestBridgeReader;
import org.openhab.binding.deconz.rest.bridge.deCONZRestReader.RestDeviceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.binding.deconz.rest.RestJson;

public class deCONZRestBridgeService extends deCONZRestWorker {

    private Logger logger = LoggerFactory.getLogger(deCONZRestBridgeService.class);
	
	private Boolean authenticated = false;
	private String apiKey = null;
	private deCONZRestReader notify = null;
	
	public Boolean isAuthenticated() {
		return authenticated;
	}

	@Override
	public String getBaseURL() {
		return baseURL;
	}
	
	public void addReader(RestBridgeReader bridge, RestDeviceReader device) {
		notify = new deCONZRestReader(bridge, device);
	}

	@Override
	protected void updateStatus(int status, String message) {
		if ((notify != null) && (notify.getBridgeReader() != null)) {
			notify.getBridgeReader().onStatusInfo(status, message);
		}
	}
	
	@Override
	public void setApiKey(String key) {
		if (key != null) {
			apiKey = key;
		} else {
			apiKey = new String();
		}			
	}

	@Override
	public String getApiKey() {
		return apiKey;
	}

	@Override
	protected boolean beginTouchlink() {
		RestResult ret = checkPreRequisite(true, true, true, "Cannot start touchlink");
		if (ret == null) {
			String url = baseURL + "/api/" + apiKey + "/touchlink/scan";
			logger.debug("Start touchlink for {}", baseURL);
			ret = executePost(url, null);
			if (ret.getResult() == RestResult.REST_OK) {
				addFake = 2;
				return true;
			}
			logger.error("Starting touchlink to {} failed.", baseURL);
		}
		return false;
	}

	private static Integer addFake = 0;
	private static Lock addLock = new ReentrantLock();
	
	@Override
	protected boolean getTouchlinkStatus() {
		boolean goOn = false;
		String url = baseURL + "/api/" + apiKey + "/touchlink/scan";
		RestResult ret = executeGet(url, null);
		if (ret.getResult() == RestResult.REST_OK) {
			JSONObject result = RestJson.parseTouchlinkResponse(ret.getData());
			if (result != null) {
				goOn = RestJson.getTouchlinkActive(result);
				if ((notify != null) && (notify.getBridgeReader() != null)) {
					List<deCONZDevice> devices = RestJson.parseTouchlinkDevices(result);
					if (devices != null) {
						for (deCONZDevice d : devices) {
							System.out.println("Discover device " + d.getRestId() + " (" + d.getUniqueId() + ")");
							notify.getDeviceReader().onDeviceInfo(d);
						}
					} else {
						// TODO - remove (add fake device)
						if (addFake > 0) {
							addLock.lock();
							deCONZDevice d = new deCONZDevice(addFake.toString(), addFake.toString() + "2:34:56:78:90:AB:CD:EF", "", 
									"", "", "", addFake.toString() + "2:34:56:78:90:AB:CD:EF", 
									new deCONZTouchlinkState(false, false, true));
							addFake--;
							addLock.unlock();
							d.makeTouchlink();
							System.out.println("Discover fake device " + d.getRestId() + " (" + d.getUniqueId() + ")");
							notify.getDeviceReader().onDeviceInfo(d);
						}
					}
				}
			}
		} else {
			logger.error("Reading touchlink results from {} failed.", baseURL);
		}
		return goOn;
	}

	@Override
	protected void endTouchlink() {
		logger.debug("Touchlink to {} finished.", baseURL);
	}
	
	@Override
	protected RestResult authenticateUnlocked() {
		// clear the API key
		apiKey = null;
		// clear authenticated
		authenticated = false;
		
		RestResult ret = checkPreRequisite(true, false, false, "Cannot start unlock authentication");
		if (ret == null) {
			String url = baseURL + "/api";
			logger.debug("Start unlock authentication to {}", baseURL);
			ret = executePost(url, RestJson.createAPIKeyRequest("smarthome deconz binding"));
			handleAuthenticateResult(ret, notify);
		}
		return ret;
	}
	
	@Override
	protected RestResult authenticateBasic(String username, String password) {
		// clear the API key
		apiKey = null;
		// clear authenticated
		authenticated = false;

		RestResult ret = checkPreRequisite(true, false, false, "Cannot start basic authentication");
		if (ret == null) {
			if ((username == null) || (username.length() < 1)) {
				ret = new RestResult();
				ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
				logger.warn("Cannot start basic authentication to {} - no passowrd given.", baseURL);
				return ret;
			}
			if ((password == null) || (password.length() < 1)) {
				ret = new RestResult();
				ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
				logger.warn("Cannot start basic authentication to {} - no username given.", baseURL);
				return ret;
			}
			
			String auth = username + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(auth.getBytes());
			String authEnc = new String(authEncBytes);
			String url = baseURL + "/api";
			logger.debug("Start basic authentication to {}", baseURL);
			executeBasicAuthentication(url, authEnc, RestJson.createAPIKeyRequest("smarthome deconz binding"));
			handleAuthenticateResult(ret,  notify);
		}
		return ret;
	}
	
	@Override
	protected RestResult getConfiguration() {
		RestResult ret = checkPreRequisite(true, true, false, "Cannot read configuration");
		if (ret == null) {
			String url = baseURL + "/api/" + apiKey + "/config";
			logger.debug("Read configuration for {}", baseURL);
			ret = executeGet(url, null);
			if (ret.getResult() == RestResult.REST_OK) {
				// the API key did work
				deCONZBridge bridge = new deCONZBridge();
				if (RestJson.parseConfigResponse(ret.getData(), bridge)) {
					logger.debug("Configuration for {} read successfully", baseURL);
					authenticated = true;
					if ((notify != null) && (notify.getBridgeReader() != null)) {
						notify.getBridgeReader().setBridgeDetails(bridge);
					}
				} else {
					logger.error("Received configuration for {} in an unsupported format", baseURL);
					ret.setResult(RestResult.REST_RESPONSE_ERROR);
				}
			} else {
				logger.error("Read configuration for {} failed", baseURL);
			}
		}
		return ret;
	}

	@Override
	protected RestResult getDevices() {
		RestResult ret = checkPreRequisite(true, true, true, "Cannot read devices");
		if (ret == null) {
			String url = baseURL + "/api/" + apiKey;
			logger.debug("Read devices from {}", baseURL);
			ret = executeGet(url, null);
			if (ret.getResult() == RestResult.REST_OK) {
				if (ret.getData() != null) {
					logger.debug("Devices from {} read successfully", baseURL);
					if (notify != null) {
						Map<String, JSONObject> objects = RestJson.parseFullResponse(ret.getData());
						if (notify.getDeviceReader() != null) {
							try {
								notify.getDeviceReader().beginDeviceInfo();
							} catch(Exception e) {
								logger.error("Exception calling beginDeviceInfo notifier.");
							}
						}
						for (Map.Entry<String, JSONObject> entry : objects.entrySet()) {
							if (RestJson.isLightsName(entry.getKey())) {
								parseLights(entry.getValue());
							} else if (RestJson.isSensorsName(entry.getKey())) {
								parseSensors(entry.getValue());
							} else if (RestJson.isGroupsName(entry.getKey())) {
								parseGroups(entry.getValue());
							}
						}
						if (notify.getDeviceReader() != null) {
							try {
								notify.getDeviceReader().endDeviceInfo();
							} catch(Exception e) {
								logger.error("Exception calling endDeviceInfo notifier.");
							}
						}
					}
				} else {
					logger.error("Received devices for {} in an unsupported format", baseURL);
					ret.setResult(RestResult.REST_RESPONSE_ERROR);
				}
			} else {
				logger.error("Read devices from {} failed", baseURL);
			}
		}
		return ret;
	}

	public RestResult setLightState(deCONZDevice device, deCONZLightState newState) {
		RestResult ret = checkPreRequisite(true, true, true, "Cannot set light state");
		if (ret == null) {
			if ((device == null) || (newState == null) || (device.getRestId() == null) || (device.getRestId().length() < 1)) {
				ret = new RestResult();
				ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
				return ret;
			}
			
			String url = baseURL + "/api/" + apiKey + "/lights/" + device.getRestId() + "/state";
			logger.debug("Change state for device {} at {}", device.getInternalId(), baseURL);
			ret = executePut(url, RestJson.createSetLightStateRequest(newState));
			if (ret.getResult() == RestResult.REST_OK) {
				// Check if we received success notification
				try {
					JSONObject response = RestJson.toJSONObjectNoArray(ret.getData());
					if (response.has("error")) {
						ret.setResult(RestResult.REST_NOT_MODIFIED);
						logger.error("Cannot change state for device {} at {} - bridge returns error.", 
								device.getInternalId(), baseURL);
					}
				} catch (Exception e) {
				}
			}
		}
		return ret;
	}

	public RestResult setSensorState(deCONZDevice device, deCONZSensorState newState) {
		RestResult ret = new RestResult();
		// TODO send the changed sensor state to the bridge
		ret.setResult(RestResult.REST_OK);
		return ret;
	}

	public RestResult setTouchlinkState(deCONZDevice device, deCONZTouchlinkState newState) {
		RestResult ret = checkPreRequisite(true, true, true, "Cannot execute touchlink commands");
		if (ret == null) {
			if ((device == null) || (newState == null) || (device.getUniqueId() == null) || (device.getUniqueId().length() < 1)) {
				ret = new RestResult();
				ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
				return ret;
			}
			
			if (newState.isIdentify()) {
				String url = baseURL + "/api/" + apiKey + "/touchlink/" + device.getRestId() + "/identify";
				logger.debug("Execute identify for device {} at {}", device.getUniqueId(), baseURL);
				ret = executePost(url, null);
				if (ret.getResult() != RestResult.REST_OK) {
					logger.error("Identify command for device {} at {} failed.", device.getUniqueId(), baseURL);
				}
			} else if (newState.isReset()) {
				String url = baseURL + "/api/" + apiKey + "/touchlink/" + device.getRestId() + "/reset";
				logger.debug("Execute reset for device {} at {}", device.getUniqueId(), baseURL);
				ret = executePost(url, null);
				if (ret.getResult() != RestResult.REST_OK) {
					logger.error("Reset command for device {} at {} failed.", device.getUniqueId(), baseURL);
				}
			} else {
				logger.error("Cannot execute touchlink commands for device {} at {} - no commands given.", 
						device.getUniqueId(), baseURL);
				// We still return ok as to reset the command states off the devices
				ret = new RestResult();
				ret.setResult(RestResult.REST_OK);
			}
		}
		return ret;
	}
	
	private void handleAuthenticateResult(RestResult result, deCONZRestReader notify) {
		if (result.getResult() == RestResult.REST_OK) {
			try {
				String key = RestJson.parseAPIKeyResponse(result.getData());
				if (key.length() > 0) {
					apiKey = key;
					authenticated = true;
					logger.error("Authentication to {} succeeded", baseURL);
					if ((notify != null) && (notify.getBridgeReader() != null)) {
						notify.getBridgeReader().setBridgeApiKey(key);
					}
				} else {
					result.setResult(RestResult.REST_RESPONSE_ERROR);
				}
			} catch (Exception e) {
				logger.error("Received authentication response from {} in wrong format", baseURL);
				result.setResult(RestResult.REST_RESPONSE_ERROR);
			}
		}
	}

	private void parseLights(JSONObject data) {
		if ((notify != null) && (notify.getDeviceReader() != null)) {
			logger.debug("Parse 'lights' section from {}", baseURL);
			List<deCONZDevice> lights = RestJson.parseLights(data);
			if (lights != null) {
				for (deCONZDevice l : lights) {
					try {
						notify.getDeviceReader().onDeviceInfo(l);
					} catch(Exception e) {
						logger.error("Exception calling onDeviceInfo notifier.");
					}
				}
			}
		}		
	}

	private void parseSensors(JSONObject data) {
		if ((notify != null) && (notify.getDeviceReader() != null)) {
			logger.debug("Parse 'sensors' section from {}", baseURL);
			List<deCONZDevice> sensors = RestJson.parseSensors(data);
			if (sensors != null) {
				for (deCONZDevice s : sensors) {
					try {
						notify.getDeviceReader().onDeviceInfo(s);
					} catch(Exception e) {
						logger.error("Exception calling onDeviceInfo notifier.");
					}
				}
			}
		}
	}

	private void parseGroups(JSONObject data) {
		if ((notify != null) && (notify.getBridgeReader() != null)) {
			logger.debug("Parse 'groups' section from {}", baseURL);
			List<deCONZGroup> groups = RestJson.parseGroups(data);
			if (groups != null) {
				for (deCONZGroup g : groups) {
					try {
						notify.getBridgeReader().onGroupInfo(g);
					} catch(Exception e) {
						logger.error("Exception calling onGroupInfo notifier.");
					}
				}
			}
		}
	}
	
	private RestResult checkPreRequisite(boolean url, boolean apikey, boolean auth, String details) {
		if (url && (baseURL.length() < 1)) {
			RestResult ret = new RestResult();
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			logger.warn(details + " - no URL given.");
			return ret;
		}
		
		if (apikey && ((apiKey == null) || (apiKey.length() < 1))) {
			RestResult ret = new RestResult();
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			logger.warn(details + " with {} - no API key available.", baseURL);
			return ret;
		}

		if (auth && !isAuthenticated()) {
			RestResult ret = new RestResult();
			ret.setResult(RestResult.REST_CONFIGURATION_ERROR);
			logger.warn(details + " with {} - not authenticated.", baseURL);
			return ret;
		}
		return null;
	}
}
