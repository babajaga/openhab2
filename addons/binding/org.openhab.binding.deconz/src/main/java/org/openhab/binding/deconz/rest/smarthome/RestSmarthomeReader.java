package org.openhab.binding.deconz.rest.smarthome;

public class RestSmarthomeReader {
	
	private RestBridgeReader bridge = null;

	public interface RestBridgeReader {
		public void onResult();
	}
	
	public RestSmarthomeReader(RestBridgeReader bridge) {
		this.bridge = bridge;
	}
	
	public RestBridgeReader getBridgeReader() {
		return bridge;
	}
}
