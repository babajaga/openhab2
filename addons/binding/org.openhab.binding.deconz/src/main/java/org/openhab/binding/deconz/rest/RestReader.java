package org.openhab.binding.deconz.rest;

import org.openhab.binding.deconz.handler.deCONZLight;
import org.openhab.binding.deconz.handler.deCONZSensor;

public class RestReader {
	
	private RestBridgeReader bridge = null;
	private RestLightReader lights = null;
	private RestSensorReader sensors = null;

	public interface RestLightReader {
		public void onLightInfo(deCONZLight light);
		public void beginLightInfo();
		public void endLightInfo();
	}

	public interface RestSensorReader {
		public void onSensorInfo(deCONZSensor sensor);
		public void beginSensorInfo();
		public void endSensorInfo();
	}
	
	public interface RestBridgeReader {
		public void setBridgeName(String name);
		public void setBridgeIpAddress(String address);
		public void setBridgeMacAddress(String address);
		public void setBridgeSoftwareVersion(String version);
		public void setBridgeApiKey(String key);
		void setBridgeModel(String model);
		void setBridgeManufacturer(String maker);
	}
	
	public RestReader(RestBridgeReader bridge, RestLightReader lights, RestSensorReader sensors) {
		this.bridge = bridge;
		this.lights = lights;
		this.sensors = sensors;
	}
	
	public RestBridgeReader getBridgeReader() {
		return bridge;
	}

	public RestLightReader getLightReader() {
		return lights;
	}

	public RestSensorReader getSensorReader() {
		return sensors;
	}
}
