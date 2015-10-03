package org.openhab.binding.deconz.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.binding.deconz.handler.deCONZBridge;
import org.openhab.binding.deconz.handler.deCONZDevice;
import org.openhab.binding.deconz.handler.deCONZGroup;
import org.openhab.binding.deconz.handler.deCONZLightState;
import org.openhab.binding.deconz.handler.deCONZSensorState;
import org.openhab.binding.deconz.handler.deCONZTouchlinkState;
import org.openhab.binding.deconz.json.JSONArray;
import org.openhab.binding.deconz.json.JSONObject;

public class RestJson {
	
	public static String createAPIKeyRequest(String name) {
		return new JSONObject().put("devicetype", name).toString();
	}

	public static JSONObject toJSONObjectNoArray(String data) {
		return new JSONObject(removeArrayBrackets(data));
	}

	public static String parseAPIKeyResponse(String data) {
		if (data != null) {
			try {
				// Some instances encapsulate this into array identifier - get rid if it as the 
				// correct response should not have those
				data = removeArrayBrackets(data);
				JSONObject response = new JSONObject(data);
				response = response.getJSONObject("success");
				return response.getString("username");
			} catch (Exception e) {
			}
		}
		return new String();
	}

	public static boolean parseConfigResponse(String data, deCONZBridge bridge) {
		if (data != null) {
			try {
				JSONObject response = new JSONObject(data);
				boolean ret = true;
				if (bridge != null) {
					// Read the values
					try {
						bridge.setName(response.getString("name"));
						bridge.setIpAddress(response.getString("ipaddress"));
						bridge.setMacAddress(response.getString("mac"));
						bridge.setSoftwareVersion(response.getString("swversion"));
					} catch (Exception e) {
						ret = false;
					}
				}
				return ret;
			} catch (Exception e) {
			}
		}
		return false;
	}

	public static Map<String, JSONObject> parseFullResponse(String data) {
		if (data != null) {
			try {
				Map<String, JSONObject> ret = new HashMap<String, JSONObject>();
				JSONObject response = new JSONObject(data);
				String[] names = JSONObject.getNames(response);
				for (int i = 0; i < names.length; i++) {
					JSONObject object = response.getJSONObject(names[i]);
					ret.put(names[i], object);
				}
				if (ret.size() > 0) {
					return ret;
				}
			} catch (Exception e) {
			}
		}
		return null;
	}

	public static JSONObject parseTouchlinkResponse(String data) {
		if (data != null) {
			try {
				return  new JSONObject(data);
			} catch (Exception e) {
			}
		}
		return null;
	}	

	public static boolean getTouchlinkActive(JSONObject data) {
		if (data != null) {
			if (data.has("scanstate")) {
				String state = data.getString("scanstate");
				if (state.compareTo("scanning") == 0) {
					return true;
				}
			}
		}
		return false;
	}	

	public static List<deCONZDevice> parseTouchlinkDevices(JSONObject data) {
		if (data != null) {
			if (data.has("results")) {
				JSONObject results = data.getJSONObject("results");
				String[] names = JSONObject.getNames(results);
				List<deCONZDevice> ret = new ArrayList<deCONZDevice>();
				for (int i = 0; i < names.length; i++) {
					try {
						JSONObject device = results.getJSONObject(names[i]);
						String uniqueId = device.getString("address");
						boolean factory = device.getBoolean("factorynew");
						deCONZTouchlinkState state = new deCONZTouchlinkState(false, false, factory);
						deCONZDevice d = new deCONZDevice(names[i], uniqueId, "", "", "", "", uniqueId, state);
						d.makeTouchlink();
						ret.add(d);
					} catch (Exception e) {
					}
				}
				if (ret.size() > 0) {
					return ret;
				}
			}
		}
		return null;		
	}

	public static boolean isLightsName(String name) {
		if (name != null) {
			return (name.compareTo("lights") == 0) ? true : false;
		}
		return false;
	}

	public static boolean isSensorsName(String name) {
		if (name != null) {
			return (name.compareTo("sensors") == 0) ? true : false;
		}
		return false;
	}

	public static boolean isGroupsName(String name) {
		if (name != null) {
			return (name.compareTo("groups") == 0) ? true : false;
		}
		return false;
	}
	
	public static List<deCONZDevice> parseLights(JSONObject value) {
		if (value != null) {
			List<deCONZDevice> ret = new ArrayList<deCONZDevice>();
			Set<String> keys = value.keySet();
			for (String id : keys) {
				try {
					JSONObject light = value.getJSONObject(id);
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
								deCONZLightState s = new deCONZLightState(online, on, bri, hue, sat, ct);
								deCONZDevice d = new deCONZDevice(id, name, model, type, version, maker, unique, s);
								d.makeLight();
								ret.add(d);
							}
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			}
			return ret;
		}
		return null;
	}

	public static List<deCONZDevice> parseSensors(JSONObject value) {
		if (value != null) {
			List<deCONZDevice> ret = new ArrayList<deCONZDevice>();
			Set<String> keys = value.keySet();
			for (String id : keys) {
				try {
					JSONObject sensor = value.getJSONObject(id);
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
								if (config.has("reachable")) {
									online = config.getBoolean("reachable");
								} else {
									online = true;
								}
								on = config.getBoolean("on");
								deCONZSensorState s = new deCONZSensorState(online, on);
								deCONZDevice d = new deCONZDevice(id, name, model, type, version, maker, unique, s);
								d.makeSensor();
								ret.add(d);
							}
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			}
			return ret;
		}
		return null;
	}

	public static List<deCONZGroup> parseGroups(JSONObject value) {
		if (value != null) {
			List<deCONZGroup> ret = new ArrayList<deCONZGroup>();
			Set<String> keys = value.keySet();
			for (String id : keys) {
				try {
					JSONObject group = value.getJSONObject(id);
					if (group != null) {
						try {
							ret.add(new deCONZGroup(group.getString("name")));
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			}
			return ret;
		}
		return null;
	}

	public static Map<String, String> parseItemResponseForGroups(String data) {
		try {
			JSONArray groups = new JSONArray(data);
			Map<String, String> ret = new HashMap<String, String>();
			for (int i = 0; i < groups.length(); i++) {
				try {
					JSONObject entry = groups.getJSONObject(i);
					String type = entry.getString("type");
					if (type.compareTo("GroupItem") == 0) {
						ret.put(entry.getString("label"), entry.getString("name"));
					}
				} catch (Exception e) {
				}
			}
			return ret;
		} catch (Exception e) {
		}
		return null;
	}

	public static String createNewGroupRequest(String name, String label) {
		JSONArray a = new JSONArray();
		a.put("home-group");
		JSONObject o = new JSONObject();
		o.put("category", "");
		o.put("name", name);
		o.put("label", label);
		o.put("type", "GroupItem");
		o.put("groupNames", new JSONArray());
		o.put("tags", a);
		return o.toString();
	}

	public static String createSetLightStateRequest(deCONZLightState newState) {
		JSONObject o = new JSONObject();
		o.put("on", newState.isOn());
		o.put("bri", newState.getBrightness());
		o.put("hue", newState.getHue());
		o.put("sat", newState.getSaturation());
		o.put("transitiontime", 10);
		return o.toString();
	}
	
	private static String removeArrayBrackets(String data) {
		data = data.trim();
		int openIdx = data.indexOf('[');
		int closeIdx = data.lastIndexOf(']');
		if (openIdx == 0) {
			// well, a opening array identifier is there
			if (closeIdx == (data.length() - 1)) {
				// a closing array identifier is there as well
				data = data.substring(openIdx + 1, closeIdx - 1);
			} else {
				// no closing array identifier?
				data = data.substring(openIdx + 1, data.length());
			}
		}
		return data.trim();
	}
}
