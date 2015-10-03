package org.openhab.binding.deconz.rest.bridge;

import org.openhab.binding.deconz.handler.deCONZBridge;
import org.openhab.binding.deconz.handler.deCONZDevice;
import org.openhab.binding.deconz.handler.deCONZGroup;

public class deCONZRestReader {
	
	private RestBridgeReader bridge = null;
	private RestDeviceReader device = null;

	public interface RestDeviceReader {
		public void onDeviceInfo(deCONZDevice device);
		public void beginDeviceInfo();
		public void endDeviceInfo();
	}

	public interface RestBridgeReader {
		public void onStatusInfo(int status, String message);
		public void setBridgeApiKey(String key);
		public void setBridgeDetails(deCONZBridge bridge);
		public void onGroupInfo(deCONZGroup group);
	}
	
	public deCONZRestReader(RestBridgeReader bridge, RestDeviceReader device) {
		this.bridge = bridge;
		this.device = device;
	}
	
	public RestBridgeReader getBridgeReader() {
		return bridge;
	}

	public RestDeviceReader getDeviceReader() {
		return device;
	}
}
