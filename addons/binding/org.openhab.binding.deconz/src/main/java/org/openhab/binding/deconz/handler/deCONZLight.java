package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.Arrays;
import java.util.List;

public class deCONZLight {
	
	private final static List<String> rgbLights = Arrays.asList("Color temperature light", "RGBW light", "Color light");
	private final static List<String> dimLights = Arrays.asList("Dimmabele light");
	private final static List<String> onoffLights = Arrays.asList("On/Off light");

	private String lightID = new String();
	private deCONZLightState state = new deCONZLightState();
	private String softwareVersion = new String("unknown");
	private String name = new String("ZLL light");
	private String model = new String("FLS pp");
	private String manufacturer = new String("");
	private String uniqueId = new String("FLS pp");
	private String deviceType = new String("Dimmable color light");
	private String thingType = new String("");
	
	public deCONZLight(String id, String name, String model, String type, String version, String maker, String uniqueId, 
			boolean on, boolean online,	int bri, int hue, int sat, int ct) {
		lightID = id;
		this.name = name;
		this.model = model;
		this.uniqueId = uniqueId;
		manufacturer = maker;
		softwareVersion = version;
		deviceType = type;
		state.setOn(on);
		state.setHue(hue);
		state.setBrightness(bri);
		state.setSaturation(sat);
		state.setColorTemperature(ct);
		state.setReachable(online);
	}

	public String getId() {
		return lightID;
	}

	public deCONZLightState getState() {
		return state;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public String getName() {
		return name;
	}

	public String getModelID() {
		return model;
	}

	public String getThingType() {
		return thingType;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getUniqueId() {
		return uniqueId;
	}
	
	public boolean isState(deCONZLightState other) {
        try {
        	if ((state.isOn() == other.isOn()) && (state.getBrightness() == other.getBrightness()) &&
                (state.getColorTemperature() == other.getColorTemperature()) &&
                (state.getHue() == other.getHue()) && (state.getSaturation() == other.getSaturation()) &&
                (state.isReachable() == other.isReachable())) {
        		return true;
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return false;
	}

	public boolean convertToThingType() {
		if (deviceType != null) {
			if (rgbLights.contains(deviceType)) {
				thingType = THING_TYPE_RGBLIGHT.getId();
				return true;
			} 
			if (dimLights.contains(deviceType)) {
				thingType = THING_TYPE_DIMLIGHT.getId();
				return true;
			} 
			if (onoffLights.contains(deviceType)) {
				thingType = THING_TYPE_ONOFFLIGHT.getId();
				return true;
			}
		}
		return false;
	}
}
