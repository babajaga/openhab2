package org.openhab.binding.deconz.rest;

public class DeconzRestJson {
	
	public static String createAPIKeyRequest(String name) {
		return new JSONObject().put("devicetype", name).toString();
	}

	public static String parseAPIKeyResponse(JSONObject data) throws Exception {
		data = data.getJSONObject("success");
		return data.getString("username");
	}

	public static JSONObject parseLightsResponse(JSONObject data) throws Exception {
		return data.getJSONObject("lights");
	}

	public static JSONObject parseSensorsResponse(JSONObject data) throws Exception {
		return data.getJSONObject("sensors");
	}
}
