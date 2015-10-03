package org.openhab.binding.deconz.handler;

public class deCONZGroup {
	
	private String name = null;

	public deCONZGroup(String name) {
		this.name = name.trim();
	}

	public void setName(String string) {
		name = string;
	}

	public String getName() {
		return name;
	}
}
