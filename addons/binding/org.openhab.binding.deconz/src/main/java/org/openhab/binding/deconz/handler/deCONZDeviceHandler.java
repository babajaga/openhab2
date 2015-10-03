package org.openhab.binding.deconz.handler;

import java.util.List;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class deCONZDeviceHandler extends BaseThingHandler implements deCONZDeviceStatusListener {

    protected Logger logger = LoggerFactory.getLogger(deCONZDeviceHandler.class);
    protected deCONZBridgeHandler bridgeHandler = null;
    private String deviceId = new String();
    
	public deCONZDeviceHandler(Thing thing) {
		super(thing);
	}
	
    protected abstract deCONZDeviceState handleCommandForDevice(ChannelUID channelUID, Command command, deCONZDeviceState currentState);
    protected abstract void handleStateChangeForDevice(ChannelUID channelUID, deCONZDeviceState currentState);
    protected abstract void handleInitializeForDevice();
    protected abstract void handleDisposeForDevice();
    
    @Override
    public void initialize() {
        logger.debug("Initializing deconz device handler.");
        handleInitializeForDevice();
       	getBridgeHandler();
    }

    @Override
    public void dispose() {
        logger.debug("Disposing deconz device handler.");
        handleDisposeForDevice();
        ungetBridgeHandler();        
        deviceId = new String();
    }
    
    @Override
    public void handleRemoval() {
    	bridgeHandler.removeDeviceById(deviceId, getThing());
    	super.handleRemoval();
    }

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
        getBridgeHandler();
        if (bridgeHandler == null) {
            logger.warn("deconz bridge handler not found. Cannot handle command without bridge.");
            return;
        }

        deCONZDevice device = getDevice();
        if (device == null) {
            logger.debug("device not known on bridge. Cannot handle command.");
            return;
        }
        
        deCONZDeviceState newState = handleCommandForDevice(channelUID, command, device.getState());
        if (newState != null) {
            bridgeHandler.updateDeviceState(device, newState);
        }
	}
	
	protected synchronized void getBridgeHandler() {
        // Access the bridge and register a sensor state listener to get  informed about sensor state changes.
        // This is done in a function as it is essential to get access to the bridge and register the listener
        // add the bridge access might fail right away but succeeds in later calls. 
        if (bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge != null) {
	            ThingHandler handler = bridge.getHandler();
	            if (handler instanceof deCONZBridgeHandler) {
	                bridgeHandler = (deCONZBridgeHandler) handler;
	        		if ((deviceId != null) && (deviceId.length() > 0)) {
	                    bridgeHandler.addDeviceById(deviceId, getThing());
	        		}
	                bridgeHandler.registerStatusListener(this);
	            }
            }
        }
	}
	
	protected void ungetBridgeHandler() {
        if (bridgeHandler != null) {
        	bridgeHandler.unregisterStatusListener(this);
        }
	}
	
	protected void setDeviceId(String id) {
        if (id != null) {
            deviceId = id;
        }
	}

	protected String getDeviceId() {
        return deviceId;
	}
	
    protected deCONZDevice getDevice() {
        getBridgeHandler();
        if (bridgeHandler != null) {
            return bridgeHandler.getDeviceById(deviceId);
        }
        return null;
    }
    
    protected boolean isDevice(deCONZDevice device) {
        if (device.getInternalId().equals(deviceId)) {
        	return true;
        }
        return false;
    }
    
    protected void postDelayedStateUpdate(deCONZDeviceState state, int timeout) {
    	if ((state != null) && (timeout > 0)) {
            getBridgeHandler();
            if (bridgeHandler != null) {
                bridgeHandler.addTimeout(deviceId, state, timeout);
            }
    	}
    }
    
	@Override
    public void onDeviceStateChanged(deCONZDevice device) {
        if (isDevice(device)) {
        	// The supported channels of a device are clearly device dependent
        	List<Channel> channels = thing.getChannels();
        	if (channels != null) {
        		for (Channel c : channels) {
        			if (c.isLinked()) {
        				handleStateChangeForDevice(c.getUID(), device.getState());
        			}
        		}
        	}
        	setDeviceState(device);
        }
    }
    
	@Override
	public void onDeviceRemoved(deCONZDevice device) {
        if (isDevice(device)) {
            updateStatus(ThingStatus.OFFLINE);
        }
	}

	@Override
	public void onDeviceAdded(deCONZDevice device) {
		onDeviceStateChanged(device);
	}

	public void updateDeviceState() {
		if ((deviceId != null) && (deviceId.length() > 0)) {
	        getBridgeHandler();
			if (bridgeHandler != null) {
				setDeviceState(bridgeHandler.getDeviceById(deviceId));
			}
		} else {
			// if we have no device id we cannot be identified by the bridge so we have to be offline
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "The identifier which identifies this device" + 
					" at the bridge is missing.");
		}
	}
    
    private void setDeviceState(deCONZDevice device) {
    	if ((device != null) && (device.getState() != null) && (!device.getState().isReachable() || (thing.getStatus() != ThingStatus.ONLINE))) {
	        Bridge bridge = getBridge();
	        if (bridge != null) {
		        ThingStatusInfo statusInfo = bridge.getStatusInfo();
		        if ((statusInfo.getStatus() == ThingStatus.ONLINE)) {
		        	if (device.getState().isReachable()) {
		            	logger.debug("Set state to ONLINE for {}.", thing.getThingTypeUID());
		                updateStatus(ThingStatus.ONLINE);
		            } else {
		            	logger.debug("Set state to OFFLINE for {}.", thing.getThingTypeUID());
		                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "The bridge reports device as not reachable.");
		            }
		        } else {
	            	logger.debug("Set state to BRIDGE_ONLINE for {}.", thing.getThingTypeUID());
					updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "The bridge is offline.");
		        }
	        }
    	} else if ((device == null) || (device.getState() == null)) {
        	logger.debug("Set state to OFFLINE for {}.", thing.getThingTypeUID());
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "The bridge does not know this device.");
    	}
    }
}
