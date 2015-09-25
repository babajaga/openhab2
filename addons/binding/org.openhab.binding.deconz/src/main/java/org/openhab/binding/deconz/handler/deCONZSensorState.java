package org.openhab.binding.deconz.handler;

public class deCONZSensorState {

	private boolean on = false;
	private boolean reachable = false;
	private int percentage = 0;
	
	public void setReachable(boolean online) {
		reachable = online;
	}

	public boolean isReachable() {
		return reachable;
	}

	public boolean isOn() {
		return on;
	}
	
	public int getPercentage() {
		return percentage;
	}
}
