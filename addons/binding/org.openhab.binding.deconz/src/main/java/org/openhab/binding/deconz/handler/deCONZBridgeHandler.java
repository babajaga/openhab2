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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.deconz.internal.deCONZConfiguration;
import org.openhab.binding.deconz.rest.DeconzRestService;
import org.openhab.binding.deconz.rest.RestReader;
import org.openhab.binding.deconz.rest.RestReader.RestBridgeReader;
import org.openhab.binding.deconz.rest.RestReader.RestLightReader;
import org.openhab.binding.deconz.rest.RestReader.RestSensorReader;
import org.openhab.binding.deconz.rest.RestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link deCONZBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZBridgeHandler extends BaseThingHandler implements RestBridgeReader, 
	RestLightReader, RestSensorReader {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);

    private final static int STATE_INIT = 1;
	private final static int STATE_READ_CONFIGURATION = 2;
	private final static int STATE_BASIC_AUTHENTICATE = 3;
	private final static int STATE_UNLOCK_AUTHENTICATE = 4;
	private final static int STATE_READ_DEVICES= 5;
	private final static int STATE_UPDATE = 6;
	
    private Logger logger = LoggerFactory.getLogger(deCONZBridgeHandler.class);
    
    private deCONZConfiguration configuration = new deCONZConfiguration();
    private ScheduledFuture<?> job = null;
    private DeconzRestService rest = new DeconzRestService();
    private int state = STATE_INIT;
    private int stateCount = 0;
    private Map<String, deCONZLight> lights = new HashMap<>();
    private Map<String, deCONZLight> lightsBackup = new HashMap<>();
    private List<deCONZLightStatusListener> lightStatusListeners = new CopyOnWriteArrayList<>();
    private Map<String, deCONZSensor> sensors = new HashMap<>();
    private Map<String, deCONZSensor> sensorsBackup = new HashMap<>();
    private List<deCONZSensorStatusListener> sensorStatusListeners = new CopyOnWriteArrayList<>();
    private RestReader reader = null;
    
	@SuppressWarnings("unused")
	private String bridgeName = null;
	@SuppressWarnings("unused")
	private String bridgeMac = null;
	@SuppressWarnings("unused")
	private String bridgeVersion = null;

	public deCONZBridgeHandler(Thing thing) {
		super(thing);
	}

    @Override
    public void initialize() {
        logger.debug("Initializing deconz handler");
        super.initialize();
        Configuration config = getThing().getConfiguration();

        // If the current state is STATE_INIT we want to apply the new configuration. A init could be due to
        // connection problems, authentication problems. In this case we would just sit and wait until the 
        // counter expires. Just set the counter to try this new configuration right away.
        // If the current state is not STATE_INIT we want to use the new configuration, that is, need to
        // switch to init.
        if (state == STATE_INIT) {
        	stateCount = 0;
        } else {
        	// This is bad. If the URL of the bridge got changed we do loose all lights as well, as we
        	// are actually a different bridge. This although means we should remove all our devices but
        	// on the other hand - should we really do this? Currently we leave the devices with the
        	// platform what means we have to clear our internal caches. Otherwise the devices would
        	// be considered 'removed' if we read the new bridge devices and hence removed from the 
        	// platform.
            String s = (String) config.get(DECONZ_BRIDGE_LOCATION);
            if ((s != null) && (configuration.getInstance() != null) && (s.compareTo(configuration.getInstance()) != 0)) {
	        	lights.clear();
	        	sensors.clear();
	        	state = STATE_INIT;
	        	stateCount = 0;
            }
        }
                
        try {
            configuration.setInstance((String) config.get(DECONZ_BRIDGE_LOCATION));
        } catch (Exception e) {
            // ignore the exception
        }

        try {
            configuration.setUserName((String) config.get(DECONZ_BRIDGE_USERNAME));
        } catch (Exception e) {
            // ignore the exception
        }

        try {
            configuration.setPassword((String) config.get(DECONZ_BRIDGE_PASSWORD));
        } catch (Exception e) {
            // ignore the exception
        }

        try {
            configuration.setApiKey((String) config.get(DECONZ_BRIDGE_APIKEY));
        } catch (Exception e) {
            // ignore the exception
        }
        
        // create the notifiers        
        reader = new RestReader(this, this, this);
        
        if (configuration.isValid()) {
        	// start the connect and update process
            updateStatus(ThingStatus.INITIALIZING);
        	startWork();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, null);
            logger.debug("deconz has invalid configuration - thing diabled");
        }
    }

    @Override
    public void dispose() {
    	if (job != null) {
    		job.cancel(true);
    		job = null;
    	}
    }
	
	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
	}
	
	private void doWork() {
		// this basically runs a state machine
		if (stateCount > 0) {
			stateCount--;
			return;
		}
		
		switch (state) {
		case STATE_INIT:
			state = doInit();
			break;
		case STATE_BASIC_AUTHENTICATE:
			state = doBasicAuthentication();
			break;
		case STATE_UNLOCK_AUTHENTICATE:
			state = doUnlockAuthentication();
			break;
		case STATE_READ_CONFIGURATION:
			state = doGetConfiguration();
			break;
		case STATE_READ_DEVICES:
			state = deGetDevices();
			break;
		case STATE_UPDATE:
			state = doUpdate();
			break;
		default:
            updateStatus(ThingStatus.INITIALIZING);
			state = STATE_INIT;
			break;
		}
	}

    private void startWork() {
    	if (job == null) {
	        Runnable runnable = new Runnable() {
	            @Override
	            public void run() {
	                try {
	                	doWork();
	                } catch (Exception e) {
	                    logger.debug("Exception occurred during execution of deconz work: {}", e.getMessage(), e);
	                }
	            }
	        };
	
	        job = scheduler.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
    	}
    }

	private void onUpdate() {
    }

	public void updateLightState(deCONZLight light, deCONZLightStateUpdate lightState) {
		// we only send updates for lights we know
		if  (lights.containsKey(light.getId())) {
	        RestResult result = rest.setLightState(light, lightState);
	        if (result.getResult() == RestResult.REST_OK) {
				lights.put(light.getId(), light);
	            logger.debug("Update status for light {}.", light.getId());
	            for (deCONZLightStatusListener lightStatusListener : lightStatusListeners) {
	                try {
	                    lightStatusListener.onLightStateChanged(light);
	                } catch (Exception e) {
	                    logger.error("An exception occurred while calling the light changed listener", e);
	                }
	            }
	        }
		}
	}

	public void updateSensorState(deCONZSensor sensor, deCONZLightStateUpdate newState) {
		// we only send updates for sensors we know
		if  (sensors.containsKey(sensor.getId())) {
	        RestResult result = rest.setSensorState(sensor, newState);
	        if (result.getResult() == RestResult.REST_OK) {
				sensors.put(sensor.getId(), sensor);
	            logger.debug("Update status for sensor {}.", sensor.getId());
	            for (deCONZSensorStatusListener listener : sensorStatusListeners) {
	                try {
	                    listener.onSensorStateChanged(sensor);
	                } catch (Exception e) {
	                    logger.error("An exception occurred while calling the sensor changed listener", e);
	                }
	            }
	        }
		}
	}
	
	public deCONZLight getLightById(String lightId) {
        return lights.get(lightId);
	}

	public deCONZSensor getSensorById(String sensorId) {
        return sensors.get(sensorId);
	}
	
    public boolean registerLightStatusListener(deCONZLightStatusListener listener) {
        if (listener != null) {
        	if (lightStatusListeners.add(listener)) {
        		onUpdate();
	            // inform the listener initially about all lights and their states
	            for (deCONZLight light : lights.values()) {
	                listener.onLightAdded(light);
	            }
	            return true;
        	}
        }
        return false;
    }

    public boolean unregisterLightStatusListener(deCONZLightStatusListener listener) {
        if (lightStatusListeners.remove(listener)) {
            onUpdate();
            return true;
        }
        return false;
    }

    public boolean registerSensorStatusListener(deCONZSensorStatusListener listener) {
        if (listener != null) {
        	if (sensorStatusListeners.add(listener)) {
        		onUpdate();
	            // inform the listener initially about all sensors and their states
	            for (deCONZSensor sensor : sensors.values()) {
	                listener.onSensorAdded(sensor);
	            }
	            return true;
        	}
        }
        return false;
    }

    public boolean unregisterSensorStatusListener(deCONZSensorStatusListener listener) {
        if (sensorStatusListeners.remove(listener)) {
            onUpdate();
            return true;
        }
        return false;
    }
    
	public List<deCONZLight> getAllKnownLights() {
        List<deCONZLight> ret = new ArrayList<deCONZLight>();
        for (Entry<String, deCONZLight> entry : lights.entrySet()) {
        	ret.add(entry.getValue());
        }
        return ret;
	}

	public List<deCONZSensor> getAllKnownSensors() {
        List<deCONZSensor> ret = new ArrayList<deCONZSensor>();
        for (Entry<String, deCONZSensor> entry : sensors.entrySet()) {
        	ret.add(entry.getValue());
        }
        return ret;
	}

	public void startSearch() {
        if (rest != null) {
//        	rest.searchDevices();
        }
	}

	@Override
	public void setBridgeName(String name) {
		bridgeName = name;
	}

	@Override
	public void setBridgeIpAddress(String address) {
		// well, this should match our already known base URI
	}

	@Override
	public void setBridgeMacAddress(String address) {
		bridgeMac = address;
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_UNIQUEID, address);
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}

	@Override
	public void setBridgeSoftwareVersion(String version) {
		bridgeVersion = version;
		configuration.setSoftwareVersion(version);
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_SOFTWAREVERSION, version);
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}

	@Override
	public void setBridgeManufacturer(String maker) {
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_MANUFACTURER, maker);
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}

	@Override
	public void setBridgeModel(String model) {
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_MODEL, model);
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}
	
	@Override
	public void setBridgeApiKey(String key) {
		configuration.setApiKey(key);
        try {
            Configuration config = editConfiguration();
            config.put(DECONZ_BRIDGE_APIKEY, key);
            updateConfiguration(config);
        } catch (Exception e) {
            // ignore the exception
        }
	}

	@Override
	public void onLightInfo(deCONZLight light) {
		// we get a notification for each light which is know to the gateway - update our internal list
		if (lights.containsKey(light.getId())) {
			// remove it from the backup
            lightsBackup.remove(light.getId());
            final deCONZLight last = lights.get(light.getId());
            if (!last.isState(light.getState())) {
                logger.debug("Status update for light {} detected.", light.getId());
                for (deCONZLightStatusListener lightStatusListener : lightStatusListeners) {
                    try {
                        lightStatusListener.onLightStateChanged(light);
                    } catch (Exception e) {
                        logger.error("An exception occurred while calling the light changed listener", e);
                    }
                }
            }
		} else {
			// Check if we know this light type
			if (light.convertToThingType()) {
	            logger.debug("add new light {} ({}) as {}.", light.getId(), light.getModelID(), 
	            		light.getThingType());
	            // add it to our lights list
	            lights.put(light.getId(), light);
	            // inform all listeners
	            for (deCONZLightStatusListener lightStatusListener : lightStatusListeners) {
	                try {
	                    lightStatusListener.onLightAdded(light);
	                } catch (Exception e) {
	                    logger.error("An exception occurred while calling the light add listener", e);
	                }
	            }
			} else {
	            logger.warn("cannot add new light {} ({}) - unknown type.", light.getId(), light.getModelID());
			}
		}
	}

	@Override
	public void beginLightInfo() {
		// copy the last known lights to a backup list
		lightsBackup = new HashMap<>(lights);
	}

	@Override
	public void endLightInfo() {
        // check for remaining (removed) lights
        for (Entry<String, deCONZLight> entry : lightsBackup.entrySet()) {
        	// remove it from our light list
            lights.remove(entry.getKey());
            logger.debug("remove light {}.", entry.getKey());
            for (deCONZLightStatusListener lightStatusListener : lightStatusListeners) {
                try {
                    lightStatusListener.onLightRemoved(entry.getValue());
                } catch (Exception e) {
                    logger.error("An exception occurred while calling the light removed listener", e);
                }
            }
        }
        lightsBackup = null;
	}

	@Override
	public void onSensorInfo(deCONZSensor sensor) {
		// we get a notification for every sensor which is know to the gateway - update our internal list
		if (sensors.containsKey(sensor.getId())) {
			// remove it from the backup
            sensorsBackup.remove(sensor.getId());
            final deCONZSensor last = sensors.get(sensor.getId());
            if (!last.isState(sensor.getState())) {
                logger.debug("Status update for sensor {} detected.", sensor.getId());
                for (deCONZSensorStatusListener listener : sensorStatusListeners) {
                    try {
                        listener.onSensorStateChanged(sensor);
                    } catch (Exception e) {
                        logger.error("An exception occurred while calling the sensor changed listener", e);
                    }
                }
            }
		} else {
			// Check if we know this sensor type
			if (sensor.convertToThingType()) {
	            logger.debug("add new sensor {} ({}) as {}.", sensor.getId(), sensor.getModelID(), 
	            		sensor.getThingType());
	            // add it to our lights list
	            sensors.put(sensor.getId(), sensor);
	            // inform all listeners
	            for (deCONZSensorStatusListener listener : sensorStatusListeners) {
	                try {
	                    listener.onSensorAdded(sensor);
	                } catch (Exception e) {
	                    logger.error("An exception occurred while calling the sensor add listener", e);
	                }
	            }
			} else {
	            logger.warn("cannot add new sensor {} ({}) - unknown type.", sensor.getId(), sensor.getModelID());
			}
		}
	}

	@Override
	public void beginSensorInfo() {
		// copy the last known sensors to a backup list
		sensorsBackup = new HashMap<>(sensors);
	}

	@Override
	public void endSensorInfo() {
        // check for remaining (removed) sensors
        for (Entry<String, deCONZSensor> entry : sensorsBackup.entrySet()) {
        	// remove it from our sensor list
            sensors.remove(entry.getKey());
            logger.debug("remove sensor {}.", entry.getKey());
            for (deCONZSensorStatusListener listener : sensorStatusListeners) {
                try {
                    listener.onSensorRemoved(entry.getValue());
                } catch (Exception e) {
                    logger.error("An exception occurred while calling the sensor removed listener", e);
                }
            }
        }
        sensorsBackup = null;
	}
	
	private int doInit() {
		rest.setBaseURI(configuration.getInstance());
		// The first step is to authenticate against the bridge. This depends on the configuration data given.
		// If an API key is available we try to use this without authenticating, that is, getting a new API key.
		String s1 = configuration.getApiKey();
		if ((s1 != null) && (s1.length() > 0)) {
			rest.setApiKey(s1);
			return STATE_READ_CONFIGURATION;
		}

		// If no API key is given we have two options to authenticate. With a password and user name we can
		// can authenticate without user interaction. Lets try this first.
		s1 = configuration.getUserName();
		String s2 = configuration.getPassword();
		if ((s1 != null) && (s1.length() > 0) && (s2 != null) && (s2.length() > 0)) {
			return STATE_BASIC_AUTHENTICATE;
		}
		// Now we need the user interaction, that is, the bridge needs to be unlocked.
		return STATE_UNLOCK_AUTHENTICATE;
	}
	
	private int doBasicAuthentication() {
		RestResult error = rest.authenticateBasic(configuration.getUserName(), 
				configuration.getPassword(), reader);
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// update state to ONLINE
			updateStatus(ThingStatus.ONLINE);
			return STATE_READ_CONFIGURATION;
		case RestResult.REST_CONNECT_ERROR:
			// update state to OFFLINE
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "The deCONZ bridge at " + 
            		configuration.getInstance() + " is not reachable");
            break;
		case RestResult.REST_AUTHENTICATION_ERROR:
			// update state to OFFLINE
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "Authentication failed. " + 
            		"Please check the provided user name and password.");
            break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
        stateCount = 60;
		return STATE_INIT;
	}
	
	private int doUnlockAuthentication() {
		RestResult error = rest.authenticateUnlocked(reader);
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// update state to ONLINE
			updateStatus(ThingStatus.ONLINE);
			return STATE_READ_CONFIGURATION;
		case RestResult.REST_CONNECT_ERROR:
			// update state to OFFLINE
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "The deCONZ bridge at " + 
            		configuration.getInstance() +	" is not reachable");
            break;
		case RestResult.REST_AUTHENTICATION_ERROR:
			// update state to OFFLINE
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "Authentication failed. " + 
            		"Please unlock the bridge to allow authentication. To unlock the bridge go to " +
            		"system->unlock. This will unlock the bridge for 60 seconds. It will be closed " + 
            		"automatically after the time is over.");
            break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
        stateCount = 60;
		return STATE_INIT;
	}
	
	private int doGetConfiguration() {
		RestResult error = rest.connect(reader);
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// update state to ONLINE
			updateStatus(ThingStatus.ONLINE);
			return STATE_READ_DEVICES;
		case RestResult.REST_RESPONSE_ERROR:
			// well, we have a wrong response from the rest interface. Stay here and retry
			// after a while
			stateCount = 10;
			return STATE_READ_CONFIGURATION;
		case RestResult.REST_CONNECT_ERROR:
			// update state to OFFLINE
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "The deCONZ bridge at " + 
            		configuration.getInstance() + " is not reachable");
            break;
		case RestResult.REST_AUTHENTICATION_ERROR:
			// update state to OFFLINE
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR, "The deCONZ bridge at " + 
            		configuration.getInstance() + " is reachable but the binding is not authorized to access the API." + 
            		"A reaon might be that the given API key is not or no longer valid. Try removing the API key in " +
            		"order to aquire a new one.");
            break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
        stateCount = 60;
		return STATE_INIT;
	}
	
	private int deGetDevices() {
		RestResult error = rest.getDevices(reader);
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// run an update after some time
			stateCount = 60;
			return STATE_UPDATE;
		case RestResult.REST_RESPONSE_ERROR:
			// well, we have a wrong response from the rest interface. Stay here and retry
			// after a while
			stateCount = 10;
			return STATE_READ_DEVICES;
		default:
			// all other errors are considered connection problems which
			// lead back to the initial state which than in turns updates
			// the thing status. There is no need to do it here
            break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
        stateCount = 60;
		return STATE_INIT;
	}
	
	private int doUpdate() {
		return STATE_READ_DEVICES;
	}
}

