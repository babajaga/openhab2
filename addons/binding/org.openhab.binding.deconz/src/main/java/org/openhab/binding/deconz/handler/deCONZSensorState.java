package org.openhab.binding.deconz.handler;

public class deCONZSensorState extends deCONZDeviceState {

	private boolean on = false;
	private int percentage = 0;
	
	public deCONZSensorState(boolean online, boolean on) {
		this.on = on;
		setReachable(online);
	}

	public boolean isOn() {
		return on;
	}

	public void setOn(boolean on) {
		this.on = on;
	}
	
	public int getPercentage() {
		return percentage;
	}

	public void setPercentage(int value) {
		percentage = value;
	}
	
	@Override
	public boolean compare(deCONZDeviceState other) {
		if (other instanceof deCONZSensorState) {
			deCONZSensorState state = (deCONZSensorState)other;
			if ((isOn() == state.isOn()) && (percentage == state.getPercentage())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void copy(deCONZDeviceState other) {
		if (other instanceof deCONZSensorState) {
			deCONZSensorState state = (deCONZSensorState)other;
			setOn(state.isOn());
			setPercentage(state.getPercentage());
		}
	}

	@Override
	protected String compareAsString(deCONZDeviceState other) {
		if (other instanceof deCONZSensorState) {
			deCONZSensorState state = (deCONZSensorState)other;
			String ret = new String();
			if (isOn() != state.isOn())  {
				ret += isOn() ? "ON vs " : "OFF vs ";
				ret += state.isOn() ? "ON" : "OFF";
			}
			if (percentage != state.getPercentage())  {
				if (ret.length() > 0) {
					ret += ", %%: ";
				} else {
					ret += "%%: ";
				}
				ret += String.format("%d", percentage);
				ret += " vs ";
				ret += String.format("%d", state.getPercentage());
			}
			if (ret.length() > 0) {
				return ret;
			}
		}
		return null;
	}
}
