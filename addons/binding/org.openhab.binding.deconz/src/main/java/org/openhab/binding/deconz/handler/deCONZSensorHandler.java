package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * {@link deCONZSensorHandler} is the handler for a ZLL sensor. It uses the {@link deCONZBridgeHandler} to execute the actual
 * command.
 *
 * @author Mike Ludwig - Initial contribution of deconz binding
 */
public class deCONZSensorHandler extends BaseThingHandler implements deCONZSensorStatusListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_ONOFFSWITCH, 
    		THING_TYPE_DIMSWITCH);

    private String sensorId;
    Integer lastSentPercentage = null;;
    private Logger logger = LoggerFactory.getLogger(deCONZSensorHandler.class);
    private deCONZBridgeHandler bridgeHandler;
    
    public deCONZSensorHandler(Thing deconzSensor) {
        super(deconzSensor);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing deconz sensor handler.");
        final String configId = (String) getConfig().get(DECONZ_SENSOR_ID);
        if (configId != null) {
            sensorId = configId;
            // Access the bridge and register a sensor state listener to get  informed about sensor state changes.
            // This is done in a function as it is essential to get access to the bridge and register the listener
            // add the bridge access might fail right away but succeeds in later calls. 
        	getBridgeHandler();
            // Get the state from the bridge handler and update the status
        	Bridge bridge = getBridge();
        	if (bridge != null) {
        		ThingStatusInfo statusInfo = bridge.getStatusInfo();
        		updateStatus(statusInfo.getStatus(), statusInfo.getStatusDetail(), statusInfo.getDescription());
        	}
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing deconz sensor handler.");
        disposeBridgeHandler();        
        sensorId = null;
    }

    private deCONZSensor getSensor() {
        deCONZBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            return bridgeHandler.getSensorById(sensorId);
        }
        return null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        deCONZBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            logger.warn("deconz bridge handler not found. Cannot handle command without bridge.");
            return;
        }

        deCONZSensor sensor = getSensor();
        if (sensor == null) {
            logger.debug("sensor not known on bridge. Cannot handle command.");
            return;
        }

        deCONZLightStateUpdate newState = null;

        switch (channelUID.getId()) {
            case DECONZ_CHANNEL_ONOFF:
                if (command instanceof OnOffType) {
                    newState = deCONZStateConverter.toOnOffSensorState((OnOffType) command);
                }
                break;
            case DECONZ_CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                    newState = deCONZStateConverter.toPercentageSensorState((PercentType) command);
                } else if (command instanceof OnOffType) {
                    newState = deCONZStateConverter.toOnOffSensorState((OnOffType) command);
                } else if (command instanceof IncreaseDecreaseType) {
                    newState = convertUpDownChangeToStateUpdate((IncreaseDecreaseType) command, sensor);
                }
                break;
        }
        if (newState != null) {
            bridgeHandler.updateSensorState(sensor, newState);
        } else {
            logger.warn("Command send to an unknown channel id: " + channelUID);
        }
    }

    private deCONZLightStateUpdate convertUpDownChangeToStateUpdate(IncreaseDecreaseType command, deCONZSensor sensor) {
        deCONZLightStateUpdate update = null;
        Integer currentPercentage = getCurrentPercentage(sensor.getState());
        if (currentPercentage != null) {
            int newPercentage = deCONZStateConverter.toAdjustedPercentage(command, currentPercentage);
            update = new deCONZLightStateUpdate();
            update.setBrightness(newPercentage);
            lastSentPercentage = newPercentage;
        }
        return update;
    }

    private Integer getCurrentPercentage(deCONZSensorState state) {
        Integer percentage = lastSentPercentage;
        if ((percentage == null) && (state != null)) {
            percentage = state.getPercentage();
        }
        return percentage;
    }

    private synchronized deCONZBridgeHandler getBridgeHandler() {
        if (bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof deCONZBridgeHandler) {
                bridgeHandler = (deCONZBridgeHandler) handler;
                bridgeHandler.registerSensorStatusListener(this);
            } else {
                return null;
            }
        }
        return bridgeHandler;
    }

    private void disposeBridgeHandler() {
        if (bridgeHandler != null) {
        	bridgeHandler.unregisterSensorStatusListener(this);
        }
    }
    
    @Override
    public void onSensorStateChanged(deCONZSensor sensor) {
        if (sensor.getId().equals(sensorId)) {
            lastSentPercentage = null;

            // update status (ONLINE, OFFLINE)
            if (sensor.getState().isReachable()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Bridge reports sensor as not reachable");
            }

            // TODO
        }
    }
    
	@Override
	public void onSensorRemoved(deCONZSensor sensor) {
        if (sensor.getId().equals(sensorId)) {
            updateStatus(ThingStatus.OFFLINE);
        }
	}

	@Override
	public void onSensorAdded(deCONZSensor sensor) {
        if (sensor.getId().equals(sensorId)) {
            updateStatus(ThingStatus.ONLINE);
            onSensorStateChanged(sensor);
        }
	}
}