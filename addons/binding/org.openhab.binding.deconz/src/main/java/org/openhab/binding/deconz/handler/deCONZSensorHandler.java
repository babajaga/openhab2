package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;

import com.google.common.collect.Sets;

/**
 * {@link deCONZSensorHandler} is the handler for a ZLL sensor. It uses the {@link deCONZBridgeHandler} to execute the actual
 * command.
 *
 * @author Mike Ludwig - Initial contribution of deconz binding
 */
public class deCONZSensorHandler extends deCONZDeviceHandler {

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_ONOFFSWITCH, 
    		THING_TYPE_DIMSWITCH);
    private final static boolean COMMAND_DEBUG = false;
    private final static boolean UPDATE_DEBUG = false;

	private int lastPercentage = -1;
	
    public deCONZSensorHandler(Thing thing) {
		super(thing);
	}
    
    @Override
    protected void handleInitializeForDevice() {
        setDeviceId((String) getConfig().get(DECONZ_SENSOR_ID));
    }
    
    @Override
    protected void handleDisposeForDevice() {
    	// nothing to do
    }

    @Override
    protected deCONZDeviceState handleCommandForDevice(ChannelUID channelUID, Command command, deCONZDeviceState currentState) {
    	deCONZDeviceState newState = null;
    	if ((currentState != null) && (currentState instanceof deCONZSensorState)) {
	        switch (channelUID.getId()) {
            case DECONZ_CHANNEL_ONOFF:
                if (command instanceof OnOffType) {
                    newState = deCONZStateConverter.toOnOffSensorState((OnOffType) command, (deCONZSensorState)currentState);
	        		cmd_debug("ON/OFF");
                }
                break;
            case DECONZ_CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                    newState = deCONZStateConverter.toPercentageSensorState((PercentType) command, (deCONZSensorState)currentState);
	        		cmd_debug("BRI percent");
                } else if (command instanceof OnOffType) {
                    newState = deCONZStateConverter.toOnOffSensorState((OnOffType) command, (deCONZSensorState)currentState);
	        		cmd_debug("BRI on/off");
                } else if (command instanceof IncreaseDecreaseType) {
                    newState = convertUpDownChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZSensorState)currentState);
	        		cmd_debug("BRI increase/decrease");
                }
                break;
            default:
                logger.warn("Command send to an unknown channel id: " + channelUID);
                break;
	        }
	        
	        if (newState != null) {
            	if (((deCONZSensorState)currentState).isOn() && !((deCONZSensorState)newState).isOn()) {
            		// switch off - set percentage to 0
            		lastPercentage = ((deCONZSensorState)currentState).getPercentage();
            		((deCONZSensorState)newState).setPercentage(0);
	        		cmd_debug("save last PRO");
            	} else if (!((deCONZSensorState)currentState).isOn() && ((deCONZSensorState)newState).isOn()) {
            		// switch on - set percentage to last known value
            		((deCONZSensorState)newState).setPercentage(lastPercentage);
	        		cmd_debug("restore last PRO");
            	}
	        }
    	}
        return newState;
    }

	@Override
	protected void handleStateChangeForDevice(ChannelUID channelUID, deCONZDeviceState currentState) {
        if (currentState instanceof deCONZSensorState) {
        	deCONZSensorState state = (deCONZSensorState)currentState;
			switch (channelUID.getId()) {
            case DECONZ_CHANNEL_BRIGHTNESS:
	 	       	if (!state.isOn()) {
		        	upd_debug("update PRO - off");
	        		updateState(channelUID, new PercentType(0));
	        	} else {
		        	upd_debug("update PRO - on");
	        		updateState(channelUID, deCONZStateConverter.toBrightnessPercentType(state));
	 	       	}
            	break;
            case DECONZ_CHANNEL_ONOFF:
	 	       	if (!state.isOn()) {
		        	upd_debug("update ON/OFF - off");
	        		updateState(channelUID, OnOffType.OFF);
	        	} else {
		        	upd_debug("update ON/OFF - on");
	        		updateState(channelUID, OnOffType.ON);
	 	       	}
            	break;
			}
        }
	}
    
    private deCONZDeviceState convertUpDownChangeToStateUpdate(IncreaseDecreaseType command, deCONZSensorState state) {
    	int percentage = deCONZStateConverter.toAdjustedPercentage(command, state.getPercentage());
        deCONZSensorState update = new deCONZSensorState(state);
        update.setPercentage(percentage);
        update.setOn(percentage <= 0 ? false : true); 
        return update;
    }

    private void cmd_debug(String msg) {
    	if (COMMAND_DEBUG) {
    		logger.debug(msg);
    	}
    }

    private void upd_debug(String msg) {
    	if (UPDATE_DEBUG) {
    		logger.debug(msg);
    	}
    }
}