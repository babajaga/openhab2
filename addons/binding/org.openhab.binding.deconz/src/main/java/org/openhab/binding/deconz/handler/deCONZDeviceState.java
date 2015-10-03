package org.openhab.binding.deconz.handler;

public abstract class deCONZDeviceState {

	private boolean reachable = false;
		
	public void setReachable(boolean online) {
		reachable = online;
	}

	public boolean isReachable() {
		return reachable;
	}
	
	public boolean isEqual(deCONZDeviceState other) {
        try {
        	if ((other != null) && (reachable == other.isReachable())) {
        		return compare(other);
        	}
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return false;
	}

	public void assign(deCONZDeviceState other) {
		reachable = other.isReachable();
		copy(other);
	}
	
	public String getComparisonAsString(deCONZDeviceState state) {
		if (reachable != state.isReachable()) {
			String ret = new String(reachable ? "ONLINE vs " : "OFFLINE vs ");
			ret += state.isReachable() ? "ONLINE" : "OFFLINE";
			String other = compareAsString(state);
			if ((other != null) && (other.length() > 0)) {
				ret += ", " + other;
			}
			return ret;
		} else {
			String other = compareAsString(state);
			if ((other != null) && (other.length() > 0)) {
				return other;
			}
		}
		return new String("Equal");
	}
	
	protected abstract boolean compare(deCONZDeviceState other);
	protected abstract void copy(deCONZDeviceState other);
	protected abstract String compareAsString(deCONZDeviceState state);	
}
