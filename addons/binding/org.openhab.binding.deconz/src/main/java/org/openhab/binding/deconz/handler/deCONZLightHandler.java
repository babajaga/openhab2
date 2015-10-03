package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;

import com.google.common.collect.Sets;

/**
 * {@link deCONZLightHandler} is the handler for a ZLL light. It uses the {@link deCONZBridgeHandler} to execute the actual
 * command.
 *
 * @author Mike Ludwig - Initial contribution of deconz binding
 */
public class deCONZLightHandler extends deCONZDeviceHandler {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_RGBLIGHT, 
    		THING_TYPE_DIMLIGHT, THING_TYPE_ONOFFLIGHT);
    private final static boolean COMMAND_DEBUG = false;
    private final static boolean UPDATE_DEBUG = false;
    
    private int lastBrightness = -1;
    private int lastColorTemperature = -1;

    public deCONZLightHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void handleInitializeForDevice() {
        setDeviceId((String) getConfig().get(DECONZ_LIGHT_ID));
    }
    
    @Override
    protected void handleDisposeForDevice() {
    	// nothing to do
    }
    
    @Override
    protected deCONZDeviceState handleCommandForDevice(ChannelUID channelUID, Command command, deCONZDeviceState currentState) {
    	deCONZDeviceState newState = null;
    	if ((currentState != null) && (currentState instanceof deCONZLightState)) {
	        switch (channelUID.getId()) {
            case DECONZ_CHANNEL_COLORTEMP:
                if (command instanceof PercentType) {
                	newState = deCONZStateConverter.toColorTemperatureLightState((PercentType) command, (deCONZLightState)currentState);
                	cmd_debug("CT percent");
                } else if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	cmd_debug("CT on/off");
                } else if (command instanceof IncreaseDecreaseType) {
                	newState = convertColorTempChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZLightState)currentState);
                	cmd_debug("CT increase/decrease");
                }
                break;
            case DECONZ_CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                	newState = deCONZStateConverter.toBrightnessLightState((PercentType) command, (deCONZLightState)currentState);
                	cmd_debug("BRI percent");
                } else if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	cmd_debug("BRI on/off");
                } else if (command instanceof IncreaseDecreaseType) {
                	newState = convertBrightnessChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZLightState)currentState);
                	cmd_debug("BRI increase/decrease");
                }
                break;
            case DECONZ_CHANNEL_COLOR:
                if (command instanceof HSBType) {
                    HSBType hsbCommand = (HSBType) command;
                    if (hsbCommand.getBrightness().intValue() <= 0) {
                    	newState = deCONZStateConverter.toOnOffLightState(OnOffType.OFF, (deCONZLightState)currentState);
                    } else {
                    	newState = deCONZStateConverter.toColorLightState(hsbCommand, (deCONZLightState)currentState);
                    }
                    cmd_debug("COL hsb");
                } else if (command instanceof PercentType) {
                	newState = deCONZStateConverter.toBrightnessLightState((PercentType) command, (deCONZLightState)currentState);
                	cmd_debug("COL percent");
                } else if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	cmd_debug("COL on/off");
                } else if (command instanceof IncreaseDecreaseType) {
                	newState = convertBrightnessChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZLightState)currentState);
                	cmd_debug("COL increase/decrease");
                }
                break;
            default:
                logger.warn("Command send to an unknown channel id: " + channelUID);
                break;
	        }

	        if (newState != null) {
		        // depending on the changes made we have to adjust some more things
	        	if (((deCONZLightState)currentState).isOn() && !((deCONZLightState)newState).isOn()) {
	        		// switch off - set brightness and color temperature to 0
	        		lastBrightness = ((deCONZLightState)currentState).getBrightness();
	        		lastColorTemperature = ((deCONZLightState)currentState).getColorTemperature();
	        		((deCONZLightState)newState).setColorTemperature(0);
	        		cmd_debug("save last BRI/CT");
	        	} else if (!((deCONZLightState)currentState).isOn() && ((deCONZLightState)newState).isOn()) {
	        		// switch on - set brightness and color temperature to last known value
	        		((deCONZLightState)newState).setBrightness(lastBrightness);
	        		((deCONZLightState)newState).setColorTemperature(lastColorTemperature);
	        		cmd_debug("restore last BRI/CT");
	        	}
	        	
	        	// Send the same state around again after two seconds to ensure that the UI
	        	// is updated correctly
	        	postDelayedStateUpdate(new deCONZLightState(newState), 2);
	        }
    	}
        return newState;
    }

	@Override
	public void handleStateChangeForDevice(ChannelUID channelUID, deCONZDeviceState currentState) {
        if (currentState instanceof deCONZLightState) {
        	deCONZLightState state = (deCONZLightState)currentState;
			HSBType hsbType;
			switch (channelUID.getId()) {
            case DECONZ_CHANNEL_COLORTEMP:
		        if (!state.isOn()) {
	            	upd_debug("update CT - off");
		        	updateState(channelUID, new PercentType(0));
		        } else {
		        	upd_debug("update CT - on");
		        	updateState(channelUID, deCONZStateConverter.toColorTemperaturePercentType(state));
		        }
            	break;
            case DECONZ_CHANNEL_BRIGHTNESS:
		        if (!state.isOn()) {
		        	upd_debug("update BRI - off");
		        	updateState(channelUID, new PercentType(0));
		        } else {
		        	upd_debug("update BRI - on");
		        	updateState(channelUID, deCONZStateConverter.toBrightnessPercentType(state));
		        }
            	break;
            case DECONZ_CHANNEL_COLOR:
		        hsbType = deCONZStateConverter.toHSBType(state);
        		if (!state.isOn()) {
        			upd_debug("update COL - off");
            		hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(0));
        		} else { 
        			upd_debug("update COL - off");
        		}
		        updateState(channelUID, hsbType);
            	break;
			}
        }
	}
    
    private deCONZDeviceState convertColorTempChangeToStateUpdate(IncreaseDecreaseType command, deCONZLightState state) {
        int colorTemperature = deCONZStateConverter.toAdjustedColorTemp(command, state.getColorTemperature());
        deCONZLightState update = new deCONZLightState(state);
        update.setColorTemperature(colorTemperature);
        return update;
    }

    private deCONZDeviceState convertBrightnessChangeToStateUpdate(IncreaseDecreaseType command, deCONZLightState state) {
        int brightness = deCONZStateConverter.toAdjustedBrightness(command, state.getBrightness());
        deCONZLightState update = new deCONZLightState(state);
        if (brightness == 0) {
            update.setOn(false);
        } else {
            update.setBrightness(brightness);
            update.setOn(true);
        }
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
