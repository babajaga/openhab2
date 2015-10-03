package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.deconz.rest.bridge.deCONZRestBridgeService;
import org.openhab.binding.deconz.rest.bridge.deCONZRestReader;
import org.openhab.binding.deconz.rest.smarthome.RestSmarthomeService;
import org.openhab.binding.deconz.rest.smarthome.RestSmarthomeReader;
import org.openhab.binding.deconz.internal.deCONZDiscoveryService;
import org.openhab.binding.deconz.internal.deCONZEventPublisher;
import org.openhab.binding.deconz.rest.RestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link deCONZBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZBridgeHandler extends BaseThingHandler implements deCONZRestReader.RestBridgeReader, 
	deCONZRestReader.RestDeviceReader, RestSmarthomeReader.RestBridgeReader {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);

    private Logger logger = LoggerFactory.getLogger(deCONZBridgeHandler.class);
    
    private deCONZRestBridgeService bridge = new deCONZRestBridgeService();
    private RestSmarthomeService smarthome = new RestSmarthomeService();
    private Map<String, deCONZDevice> devices = new HashMap<>();
    private Map<String, deCONZDevice> devicesBackup = null;
    private List<deCONZDeviceStatusListener> statusListeners = new CopyOnWriteArrayList<>();
    private deCONZDiscoveryService discovery = null;
    @SuppressWarnings("unused")
	private deCONZEventPublisher publisher = new deCONZEventPublisher();
    
	public deCONZBridgeHandler(Thing thing) {
		super(thing);
	}

    @Override
    public void initialize() {
        logger.debug("Initializing deconz handler");
        super.initialize();
        Configuration config = getThing().getConfiguration();

        // We want to apply the new configuration to the rest service. If it has not yet gone further that initialized, 
        // it could have connection or authentication problems. In this case we would just sit and wait until the service 
        // runs tries again. So instead of waiting, we restart the service and try this new configuration right away.
        if (bridge.isInitialized()) {
        	bridge.restart();
        } else {
        	// This is bad. We already have discovered things and the URL of the bridge got changed. As this is a 
        	// different bridge we do loose all devices as well. This although means we should remove all our devices but
        	// on the other hand - should we really do this? Currently we leave the devices with the platform what means 
        	// we only have to clear our internal caches. Otherwise the devices would be considered 'removed' if we read 
        	// the new bridge devices and hence removed from the platform.
            String s = (String) config.get(DECONZ_BRIDGE_LOCATION);
            if ((s != null) && (bridge.getBaseURL() != null) && (s.compareTo(bridge.getBaseURL()) != 0)) {
	        	devices.clear();
	        	bridge.restart();
            }
        }
                
        try { bridge.setBaseURL((String) config.get(DECONZ_BRIDGE_LOCATION)); } catch (Exception e) { }
        try { bridge.setUsername((String) config.get(DECONZ_BRIDGE_USERNAME)); } catch (Exception e) { }
        try { bridge.setPassword((String) config.get(DECONZ_BRIDGE_PASSWORD)); } catch (Exception e) { }
        try { bridge.setApiKey((String) config.get(DECONZ_BRIDGE_APIKEY)); } catch (Exception e) { }
        
        smarthome.setBaseURL("http://localhost:8080");
        
        // create the notifiers        
        bridge.addReader(this, this);
        smarthome.addReader(this);
        
        if (bridge.isConfigurationValid()) {
        	// start the connect and update process
            updateStatus(ThingStatus.INITIALIZING);
            bridge.startWork(scheduler);
            smarthome.startWork(scheduler);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, null);
            logger.debug("deconz has invalid configuration - thing diabled");
        }
    }

    @Override
    public void dispose() {
    	bridge.stopWork();
    	smarthome.stopWork();
    }
	
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// nothing to do
	}

	public void setDiscoveryService(deCONZDiscoveryService service) {
		discovery = service;
	}
	
//	private void onUpdate() {
//    }

	public void updateDeviceState(deCONZDevice device, deCONZDeviceState newState) {
		// we only send updates for devices we know
		if  (devices.containsKey(device.getInternalId())) {
			RestResult result = null;
        	boolean publish = false;
			if (newState instanceof deCONZLightState) {
		        result = bridge.setLightState(device, (deCONZLightState)newState);
			} else if (newState instanceof deCONZSensorState) {
		        result = bridge.setSensorState(device, (deCONZSensorState)newState);
			} else if (newState instanceof deCONZTouchlinkState) {
		        result = bridge.setTouchlinkState(device, (deCONZTouchlinkState)newState);
		        // We do not care for the result here
		        publish = true;
			}
	        if (result != null) {
	        	deCONZDevice dev = devices.get(device.getInternalId());
	        	if (result.getResult() == RestResult.REST_OK) {
		        	if (!dev.isState(newState)) {
			        	dev.setState(newState);
			            logger.debug("Update status for device {}.", dev.getInternalId());
			            publish = true;
		        	} else {
			            logger.debug("Update status for device {} received with unchanged state.", 
			            		dev.getInternalId());
		        	}
	        	} else if (result.getResult() == RestResult.REST_NOT_MODIFIED) {
	        		// publish the old state
		            logger.debug("Update failed - publish old status for device {}.", device.getInternalId());
		            publish = true;
	        	}
	        	if (publish) {
		            for (deCONZDeviceStatusListener listener : statusListeners) {
		                try {
		                	listener.onDeviceStateChanged(dev);
		                } catch (Exception e) {
		                    logger.error("An exception occurred while calling the device changed listener", e);
		                }
		            }
	        	}
	        }
		}
	}

	public deCONZDevice getDeviceById(String id) {
        return devices.get(id);
	}

	public void addDeviceById(String id, Thing otherThing) {
		if ((id != null) && (!devices.containsKey(id))) {
			// This should not happen, but if someone adds a device manually to this bridge which does not belong to us (yet?) 
			// we will be here. If we add this to our internal cache it would be removed on the next device update. So, currently
			// we do not add this to our cache as not to remove it from the UI. If the user adds this device to the gateway later on,
			// it will be added through the update and updated to online. This however is different for touchlink devices as those 
			// are not removed from our device list.
			if (deCONZDevice.isTouchlinkPrefix(id) && (otherThing != null)) {
				// we need to recreate the deCONZDevice object to store it in our list
				Configuration configuration = otherThing.getConfiguration();
				if (configuration != null) {
					Map<String, Object> properties = configuration.getProperties();
					if ((properties != null) && properties.containsKey(DECONZ_UNIQUEID) && properties.containsKey(DECONZ_FACTORYNEW)) {
						String unique = (String)properties.get(DECONZ_UNIQUEID);
						String factory = (String)properties.get(DECONZ_FACTORYNEW);
						boolean factoryNew = false;
						if (factory.compareTo("Yes") == 0) {
							factoryNew = true;
						}
						deCONZDevice device = new deCONZDevice(deCONZDevice.removePrefix(id), unique, "", "", "", "", 
								unique,	new deCONZTouchlinkState(false, false, factoryNew));
						device.makeTouchlink();
						devices.put(id, device);
					}
				}
			}
		}
	}

	public void removeDeviceById(String id, Thing ting) {
		if ((id != null) && (devices.containsKey(id))) {
			// The light has been removed from the UI so remove it from our cache as well as to add it as new discovered thing back 
			// to the UI.
			devices.remove(id);
		}
	}
	
    public boolean registerStatusListener(deCONZDeviceStatusListener listener) {
        if (listener != null) {
        	if (statusListeners.add(listener)) {
	            // inform the listener initially about all devices and their states
	            for (deCONZDevice device : devices.values()) {
	                listener.onDeviceAdded(device);
	            }
	            return true;
        	}
        }
        return false;
    }

    public boolean unregisterStatusListener(deCONZDeviceStatusListener listener) {
        if (statusListeners.remove(listener)) {
            return true;
        }
        return false;
    }


	public List<deCONZDevice> getAllKnownDevices() {
        List<deCONZDevice> ret = new ArrayList<deCONZDevice>();
        for (Entry<String, deCONZDevice> entry : devices.entrySet()) {
       		ret.add(entry.getValue());
        }
        return ret;
	}
    
	public List<deCONZDevice> getAllKnownLights() {
        List<deCONZDevice> ret = new ArrayList<deCONZDevice>();
        for (Entry<String, deCONZDevice> entry : devices.entrySet()) {
        	if (entry.getValue().getState() instanceof deCONZLightState) {
        		ret.add(entry.getValue());
        	}
        }
        return ret;
	}

	public List<deCONZDevice> getAllKnownSensors() {
        List<deCONZDevice> ret = new ArrayList<deCONZDevice>();
        for (Entry<String, deCONZDevice> entry : devices.entrySet()) {
        	if (entry.getValue().getState() instanceof deCONZSensorState) {
        		ret.add(entry.getValue());
        	}
        }
        return ret;
	}

	public void startSearch() {
        if (bridge != null) {
        	bridge.startTouchlink();
        }
	}

	@Override
	public void onStatusInfo(int status, String message) {
		switch (status) {
		case deCONZRestBridgeService.REST_INITIALIZING:
            updateStatus(ThingStatus.INITIALIZING);
			break;
		case deCONZRestBridgeService.REST_ONLINE:
			updateStatus(ThingStatus.ONLINE);
			// The ThingManager sets all things that belong to this bridge to ONLINE as even if the device
			// itself might already reported to be offline. Run an state update for all known devices.
			updateDeviceStatus();
			break;
		case deCONZRestBridgeService.REST_COMMUNICATION_ERROR:
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, message);
			break;
		case deCONZRestBridgeService.REST_CONFIGURATION_ERROR:
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, message);
            break;
		}
	}
	
	private void updateDeviceStatus() {
		if (thing instanceof Bridge) {
			List<Thing> things = ((Bridge)thing).getThings();
			if (things != null) {
		        for (Thing t : things) {
		        	ThingHandler h = t.getHandler();
		        	if (h != null) {
		        		if (h instanceof deCONZDeviceHandler) {
		        			((deCONZDeviceHandler)h).updateDeviceState();
		        		}
		        	}
				}
			}
		}
	}

	@Override
	public void setBridgeDetails(deCONZBridge bridge) {
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_UNIQUEID, bridge.getMacAddress());
            config.put(DECONZ_SOFTWAREVERSION, bridge.getSoftwareVersion());
            config.put(DECONZ_MANUFACTURER, bridge.getManufacturer());
            config.put(DECONZ_MODEL, bridge.getModel());
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}

	@Override
	public void setBridgeApiKey(String key) {
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_BRIDGE_APIKEY, key);
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}

	public void addTimeout(String id, deCONZDeviceState state, int timeout) {
		if (bridge != null) {
			bridge.addTimout(id, state, timeout);
		}
	}
	
	@Override
	public void onTimeout(String id, deCONZDeviceState state) {
		deCONZDevice d = devices.get(id);
		if (d != null) {
			updateDeviceState(d, state);
		}
	}
	
	@Override
	public void onDeviceInfo(deCONZDevice device) {
		// we get a notification for each device which is know to the gateway - update our internal list
		if (devices.containsKey(device.getInternalId())) {
			// remove it from the backup
			if (devicesBackup != null) devicesBackup.remove(device.getInternalId());
			deCONZDevice dev = devices.get(device.getInternalId());
            if (!dev.getState().isEqual(device.getState())) {
                logger.debug("Status update for device {} detected ({}).", device.getInternalId(), 
                		dev.getState().getComparisonAsString(device.getState()));
                // update the cached state
                dev.setState(device.getState());
                if (!dev.isState(device.getState())) {
                	logger.error("state update did not work!");
                }
                // inform the listeners
                for (deCONZDeviceStatusListener listener : statusListeners) {
                    try {
                        listener.onDeviceStateChanged(dev);
                    } catch (Exception e) {
                        logger.error("An exception occurred while calling the device changed listener", e);
                    }
                }
            }
		} else {
			// Check if we know this device type
			if (device.convertToThingType()) {
	            logger.debug("add new device {} ({}) as {}.", device.getInternalId(), device.getModelID(), 
	            		device.getThingType());
	            // add it to our device list
	            devices.put(device.getInternalId(), device);
	            // inform all listeners
	            for (deCONZDeviceStatusListener listener : statusListeners) {
	                try {
	                    listener.onDeviceAdded(device);
	                } catch (Exception e) {
	                    logger.error("An exception occurred while calling the device add listener", e);
	                }
	            }
	            // make it available as discovery result
	            if (discovery != null) {
	            	discovery.deviceAdded(device);
	            }
			} else {
	            logger.warn("cannot add new device {} ({}, {}) - unknown type.", device.getInternalId(), 
	            		device.getModelID(), device.getDeviceType());
			}
		}
	}

	@Override
	public void beginDeviceInfo() {
		// copy the last known devices to a backup list
		devicesBackup = new HashMap<>(devices);
	}

	@Override
	public void endDeviceInfo() {
        // check for remaining (removed) devices
        for (Entry<String, deCONZDevice> entry : devicesBackup.entrySet()) {
        	// If it is a touchlink device we will not get any updates for it. Therefore we have to
        	// consider touchlink devices as still there until removed via the UI.
        	deCONZDevice d = entry.getValue();
        	if ((d.getState() == null) && !(d.getState() instanceof deCONZTouchlinkState)) {
	        	// remove it from our device list
	            devices.remove(entry.getKey());
	            logger.debug("remove device {}.", entry.getKey());
	            for (deCONZDeviceStatusListener listener : statusListeners) {
	                try {
	                    listener.onDeviceRemoved(entry.getValue());
	                } catch (Exception e) {
	                    logger.error("An exception occurred while calling the device removed listener", e);
	                }
	            }
	            // make it visible to the platform
	            if (discovery != null) {
	            	discovery.deviceRemoved(entry.getValue());
	            }
        	}
        }
        devicesBackup = null;
	}

	@Override
	public void onGroupInfo(deCONZGroup group) {
		if ((group != null) && (smarthome != null)) {
//			smarthome.checkAndCreateGroup(group.getName());
		}
	}

	@Override
	public void onResult() {
	}
}

