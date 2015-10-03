package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.THING_TYPE_DIMLIGHT;
import static org.openhab.binding.deconz.deCONZBindingConstants.THING_TYPE_DIMSWITCH;
import static org.openhab.binding.deconz.deCONZBindingConstants.THING_TYPE_ONOFFLIGHT;
import static org.openhab.binding.deconz.deCONZBindingConstants.THING_TYPE_ONOFFSWITCH;
import static org.openhab.binding.deconz.deCONZBindingConstants.THING_TYPE_RGBLIGHT;
import static org.openhab.binding.deconz.deCONZBindingConstants.THING_TYPE_TOUCHLINKDEVICE;

import java.util.Arrays;
import java.util.List;

public class deCONZDevice {
	
	private final static List<String> rgbLights = Arrays.asList("Color temperature light", "RGBW light", "Color light", "Extended color light");
	private final static List<String> dimLights = Arrays.asList("Dimmabele light");
	private final static List<String> onoffLights = Arrays.asList("On/Off light");
	private final static List<String> dimSwitches = Arrays.asList("ZHADimSwitch");
	private final static List<String> onoffSwitches = Arrays.asList("ZHASwitch", "ZGPSwitch", "ZHAPresence");
	
	private final static int PREFIX_NONE = 0;
	private final static String PREFIX_LIGHT_NAME = "L";
	private final static int PREFIX_LIGHT = 1;
	private final static String PREFIX_SENSOR_NAME = "S";
	private final static int PREFIX_SENSOR = 2;
	private final static String PREFIX_TOUCHLINK_NAME = "T";
	private final static int PREFIX_TOUCHLINK = 3;
	
	private String internalId = new String();
	private String restId = new String();
	private deCONZDeviceState state = null;
	private String softwareVersion = new String("unknown");
	private String name = new String("");
	private String model = new String("");
	private String manufacturer = new String("");
	private String uniqueId = new String("");
	private String deviceType = new String("");
	private String thingType = new String("");
	private int prefixType = PREFIX_NONE;
	
	public deCONZDevice(String id, String name, String model, String type, String version, String maker, String uniqueId, 
			deCONZDeviceState state) {
		restId = id;
		this.name = name;
		this.model = model;
		this.uniqueId = uniqueId;
		manufacturer = maker;
		softwareVersion = version;
		deviceType = type;
		this.state = state;
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
			if (dimSwitches.contains(deviceType)) {
				thingType = THING_TYPE_DIMSWITCH.getId();
				return true;
			} 
			if (onoffSwitches.contains(deviceType)) {
				thingType = THING_TYPE_ONOFFSWITCH.getId();
				return true;
			}
			if (deviceType.compareTo("InternTouchlinkDevice") == 0) {
				thingType = THING_TYPE_TOUCHLINKDEVICE.getId();
				return true;
			}
		}
		return false;
	}
	
	public void makeLight() {
		if (prefixType == PREFIX_NONE) {
			internalId = PREFIX_LIGHT_NAME + restId;
			prefixType = PREFIX_LIGHT;
		}
	}

	public void makeSensor() {
		if (prefixType == PREFIX_NONE) {
			internalId = PREFIX_SENSOR_NAME + restId;
			prefixType = PREFIX_SENSOR;
		}
	}

	public void makeTouchlink() {
		if (prefixType == PREFIX_NONE) {
			internalId = PREFIX_TOUCHLINK_NAME + restId;
			prefixType = PREFIX_TOUCHLINK;
			deviceType = "InternTouchlinkDevice";
		}
	}
	
	public String getRestId() {
		return restId;
	}

	public String getInternalId() {
		if (prefixType != PREFIX_NONE) {
			return internalId;
		}
		return restId;
	}

	public deCONZDeviceState getState() {
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
	
	public boolean isState(deCONZDeviceState other) {
		if (state != null) {
			return state.isEqual(other);
		}
		return false;
	}

	public void setState(deCONZDeviceState other) {
		if (state != null) {
			state.assign(other);
		} else {
			state = other;
		}
	}

	public static boolean isLightPrefix(String id) {
		if ((id != null) && (id.startsWith(PREFIX_LIGHT_NAME))) {
			return true;
		}
		return false;
	}

	public static boolean isSensorPrefix(String id) {
		if ((id != null) && (id.startsWith(PREFIX_SENSOR_NAME))) {
			return true;
		}
		return false;
	}

	public static boolean isTouchlinkPrefix(String id) {
		if ((id != null) && (id.startsWith(PREFIX_TOUCHLINK_NAME))) {
			return true;
		}
		return false;
	}

	public static String removePrefix(String id) {
		if (isLightPrefix(id)) {
			return id.replaceFirst(PREFIX_LIGHT_NAME, ""); 
		}
		if (isSensorPrefix(id)) {
			return id.replaceFirst(PREFIX_SENSOR_NAME, ""); 
		}
		if (isTouchlinkPrefix(id)) {
			return id.replaceFirst(PREFIX_TOUCHLINK_NAME, ""); 
		}
		return id;
	}
}
