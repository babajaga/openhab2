package org.openhab.binding.deconz.handler;

public class deCONZLightStateUpdate {

	private int hue = 0;
	private int saturation = 0;
	private int brightness = 0;
	private int colorTemperature = 0;
	private boolean on = false;
	
	public void setHue(int hue) {
		this.hue = hue;
	}

	public int getHue() {
		return this.hue;
	}

	public void setSaturation(int saturation) {
		this.saturation = saturation;
	}

	public int getSaturation() {
		return this.saturation;
	}
	
	public void setBrightness(int brightness) {
		this.brightness = brightness;
	}

	public int getBrightness() {
		return this.brightness;
	}

	public void setOn(boolean on) {
		this.on = on;
	}

	public boolean isOn() {
		return this.on;
	}

	public void setColorTemperature(int colorTemperature) {
		this.colorTemperature = colorTemperature;
	}

	public int setColorTemperature() {
		return this.colorTemperature;
	}
}
