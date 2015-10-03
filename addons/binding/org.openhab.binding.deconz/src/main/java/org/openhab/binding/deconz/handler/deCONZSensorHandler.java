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
                }
                break;
            case DECONZ_CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                    newState = deCONZStateConverter.toPercentageSensorState((PercentType) command, (deCONZSensorState)currentState);
                } else if (command instanceof OnOffType) {
                    newState = deCONZStateConverter.toOnOffSensorState((OnOffType) command, (deCONZSensorState)currentState);
                	if ((((deCONZSensorState)currentState).getPercentage() > 0) && OnOffType.OFF.equals((OnOffType)command)) {
                		// switch off - set percentage to 0
                		lastPercentage = ((deCONZSensorState)currentState).getPercentage();
                		((deCONZSensorState)newState).setPercentage(0);
                	} else if ((((deCONZSensorState)currentState).getPercentage() == 0) && OnOffType.ON.equals((OnOffType)command)) {
                		// switch onn - set percentage to last known value
                		((deCONZSensorState)newState).setPercentage(lastPercentage);
                	}
                } else if (command instanceof IncreaseDecreaseType) {
                    newState = convertUpDownChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZSensorState)currentState);
                }
                break;
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
	        		updateState(channelUID, new PercentType(0));
	        	} else {
	        		updateState(channelUID, deCONZStateConverter.toBrightnessPercentType(state));
	 	       	}
            	break;
            case DECONZ_CHANNEL_ONOFF:
		        updateState(channelUID, state.isOn() ? OnOffType.ON : OnOffType.OFF);
            	break;
			}
        }
	}
    
    private deCONZDeviceState convertUpDownChangeToStateUpdate(IncreaseDecreaseType command, deCONZSensorState state) {
        int percentage = deCONZStateConverter.toAdjustedPercentage(command, state.getPercentage());
        deCONZSensorState update = new deCONZSensorState(true, false);
        update.assign(state);
        update.setPercentage(percentage);
        return update;
    }
}