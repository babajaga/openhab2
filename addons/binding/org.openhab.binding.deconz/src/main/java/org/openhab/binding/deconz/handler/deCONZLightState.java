package org.openhab.binding.deconz.handler;

public class deCONZLightState {

	private int colorTemperature = deCONZStateConverter.MIN_COLOR_TEMPERATURE;
	private int brightness = 0;
	private int saturation = 0;
	private int hue = 0;
	private boolean reachable = false;
	private boolean on = false;
	
	public void setColorTemperature(int colorTemperature) {
		this.colorTemperature = colorTemperature;
	}

	public int getColorTemperature() {
		return colorTemperature;
	}

	public void setBrightness(int brightness) {
		this.brightness = brightness;
		if (brightness < 1) {
			setOn(false);
		} else {
			setOn(true);
		}
	}

	public int getBrightness() {
		return brightness;
	}
	
	public void setSaturation(int saturation) {
		this.saturation = saturation;
	}

	public int getSaturation() {
		return saturation;
	}
	
	public void setHue(int hue) {
		this.hue = hue;
	}
	
	public int getHue() {
		return hue;
	}

	public void setReachable(boolean online) {
		reachable = online;
	}

	public boolean isReachable() {
		return reachable;
	}

	public void setOn(boolean on) {
		this.on = on;
	}
	
	public boolean isOn() {
		return on;
	}
}
