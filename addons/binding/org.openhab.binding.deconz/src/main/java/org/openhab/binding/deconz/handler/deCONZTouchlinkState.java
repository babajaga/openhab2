package org.openhab.binding.deconz.handler;

public class deCONZTouchlinkState extends deCONZDeviceState {

	private boolean identify = false;
	private boolean reset = false;
	private boolean factoryNew = false;
	
	public deCONZTouchlinkState(boolean identify, boolean reset, boolean factoryNew) {
		this.identify = identify;
		this.reset = reset;
		this.factoryNew = factoryNew;
		setReachable(true);
	}

	public boolean isIdentify() {
		return identify;
	}

	public boolean isReset() {
		return reset;
	}

	public boolean isFactoryNew() {
		return factoryNew;
	}
	
	@Override
	public boolean compare(deCONZDeviceState other) {
		if (other instanceof deCONZTouchlinkState) {
			deCONZTouchlinkState state = (deCONZTouchlinkState)other;
			if ((isReset() == state.isReset()) && (isIdentify() == state.isIdentify()) && 
				(isFactoryNew() == state.isFactoryNew())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void copy(deCONZDeviceState other) {
		if (other instanceof deCONZTouchlinkState) {
			deCONZTouchlinkState state = (deCONZTouchlinkState)other;
			identify = state.isIdentify();
			reset = state.isReset();
			factoryNew = state.isFactoryNew();
		}
	}

	@Override
	protected String compareAsString(deCONZDeviceState other) {
		if (other instanceof deCONZTouchlinkState) {
			deCONZTouchlinkState state = (deCONZTouchlinkState)other;
			String ret = new String();
			if (isReset() != state.isReset())  {
				ret += isReset() ? "Reset vs " : "NO reset vs ";
				ret += state.isReset() ? "NO reset" : "Reset";
			}
			if (isIdentify() != state.isIdentify())  {
				ret += isIdentify() ? "Identify vs " : "NO identify vs ";
				ret += state.isIdentify() ? "NO identify" : "Identify";
			}
			if (isFactoryNew() != state.isFactoryNew())  {
				ret += isFactoryNew() ? "Factory vs " : "Used vs ";
				ret += state.isFactoryNew() ? "Used" : "Factory";
			}
			if (ret.length() > 0) {
				return ret;
			}
		}
		return null;
	}
}
