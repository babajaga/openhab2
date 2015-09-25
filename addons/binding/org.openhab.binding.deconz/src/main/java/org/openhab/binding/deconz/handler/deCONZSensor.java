package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

public class deCONZSensor {

	private String sensorID = new String();
	private deCONZSensorState state = new deCONZSensorState();
	private String softwareVersion = new String("Unknown version");
	private String name = new String("ZLL sensor");
	private String model = new String("Unknown model");
	private String manufacturer = new String("");
	private String uniqueId = new String("");
	private String deviceType = new String("Unknonw sensor type");
	private String thingType = new String("");

	public deCONZSensor(String id, String name, String model, String type, String version, String maker, String uniqueId, 
			boolean on, boolean online) {
		sensorID = id;
		this.name = name;
		this.model = model;
		this.uniqueId = uniqueId;
		manufacturer = maker;
		softwareVersion = version;
		this.deviceType = type;
		state.setReachable(online);
	}
	
	public String getId() {
		return sensorID;
	}
	
	public deCONZSensorState getState() {
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
	
	public boolean isState(deCONZSensorState other) {
        try {
        	if (state.isReachable() == other.isReachable()) {
        		return true;
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return false;
	}
	
	public boolean convertToThingType() {
		if (deviceType != null) {
			if (deviceType.compareTo("ZHASwitch") == 0) {
				thingType = THING_TYPE_ONOFFSWITCH.getId();
				return true;
			} 
			if (deviceType.compareTo("ZHADimSwitch") == 0) {
				thingType = THING_TYPE_DIMSWITCH.getId();
				return true;
			} 
		}
		return false;
	}
	
}
