package org.openhab.binding.deconz.handler;

public class deCONZLightState extends deCONZDeviceState {

	private int colorTemperature = deCONZStateConverter.MIN_COLOR_TEMPERATURE;
	private int brightness = 0;
	private int saturation = 0;
	private int hue = 0;
	private boolean on = false;
	
	public deCONZLightState(boolean online, boolean on, int bri, int hue, int sat, int ct) {
		this.on = on;
		setReachable(online);
		brightness = bri;
		saturation = sat;
		this.hue = hue;
		colorTemperature = ct;
	}

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

	public void setOn(boolean on) {
		this.on = on;
	}
	
	public boolean isOn() {
		return on;
	}

	@Override
	protected boolean compare(deCONZDeviceState other) {
		if (other instanceof deCONZLightState) {
			deCONZLightState state = (deCONZLightState)other;
			if ((isOn() == state.isOn()) && (colorTemperature == state.getColorTemperature()) &&
				(brightness == state.getBrightness()) && (hue == state.getHue()) && 
				(saturation == state.getSaturation())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void copy(deCONZDeviceState other) {
		if (other instanceof deCONZLightState) {
			deCONZLightState state = (deCONZLightState)other;
			setColorTemperature(state.getColorTemperature());
			setBrightness(state.getBrightness());
			setHue(state.getHue());
			setSaturation(state.getSaturation());
			// do Brightness before On as the setBrightness handler changes the On state 
			setOn(state.isOn());
		}
	}

	@Override
	protected String compareAsString(deCONZDeviceState other) {
		if (other instanceof deCONZLightState) {
			deCONZLightState state = (deCONZLightState)other;
			String ret = new String();
			if (isOn() != state.isOn())  {
				ret += isOn() ? "ON vs " : "OFF vs ";
				ret += state.isOn() ? "ON" : "OFF";
			}
			if (colorTemperature != state.getColorTemperature())  {
				if (ret.length() > 0) {
					ret += ", CT: ";
				} else {
					ret += "CT: ";
				}
				ret += String.format("%d", colorTemperature);
				ret += " vs ";
				ret += String.format("%d", state.getColorTemperature());
			}
			if (brightness != state.getBrightness())  {
				if (ret.length() > 0) {
					ret += ", BRI: ";
				} else {
					ret += "BRI: ";
				}
				ret += String.format("%d", brightness);
				ret += " vs ";
				ret += String.format("%d", state.getBrightness());
			}
			if (hue != state.getHue())  {
				if (ret.length() > 0) {
					ret += ", HUE: ";
				} else {
					ret += "HUE: ";
				}
				ret += String.format("%d", hue);
				ret += " vs ";
				ret += String.format("%d", state.getHue());
			}
			if (saturation != state.getSaturation())  {
				if (ret.length() > 0) {
					ret += ", SAT: ";
				} else {
					ret += "SAT: ";
				}
				ret += String.format("%d", saturation);
				ret += " vs ";
				ret += String.format("%d", state.getSaturation());
			}
			if (ret.length() > 0) {
				return ret;
			}
		}
		return null;
	}
}
