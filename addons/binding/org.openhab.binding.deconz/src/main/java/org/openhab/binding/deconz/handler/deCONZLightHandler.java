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
                	logger.debug("CT percent");
                } else if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	logger.debug("CT on/off");
                	if (((deCONZLightState)currentState).isOn() && !((deCONZLightState)newState).isOn()) {
                		// switch off - set color temperature to 0
                		lastColorTemperature = ((deCONZLightState)currentState).getBrightness();
                    	logger.debug("save last CT");
                	} else if (!((deCONZLightState)currentState).isOn() && ((deCONZLightState)newState).isOn()) {
                		// switch off - set color temperature to last known value
                		((deCONZLightState)newState).setColorTemperature(lastColorTemperature);
                    	logger.debug("restore last CT");
                	}
                } else if (command instanceof IncreaseDecreaseType) {
                	newState = convertColorTempChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZLightState)currentState);
                	logger.debug("CT increase/decrease");
                }
                break;
            case DECONZ_CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                	newState = deCONZStateConverter.toBrightnessLightState((PercentType) command, (deCONZLightState)currentState);
                	logger.debug("BRI percent");
                } else if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	logger.debug("BRI on/off");
                	if (((deCONZLightState)currentState).isOn() && !((deCONZLightState)newState).isOn()) {
                		// switch off - set brightness to 0
                		lastBrightness = ((deCONZLightState)currentState).getBrightness();
                    	logger.debug("save last BRI");
                	} else if (!((deCONZLightState)currentState).isOn() && ((deCONZLightState)newState).isOn()) {
                		// switch off - set brightness to last known value
                		((deCONZLightState)newState).setBrightness(lastBrightness);
                    	logger.debug("restore last BRI");
                	}
                } else if (command instanceof IncreaseDecreaseType) {
                	newState = convertBrightnessChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZLightState)currentState);
                	logger.debug("BRI increase/decrease");
                }
                break;
            case DECONZ_CHANNEL_COLOR:
                if (command instanceof HSBType) {
                    HSBType hsbCommand = (HSBType) command;
                    if (hsbCommand.getBrightness().intValue() == 0) {
                    	newState = deCONZStateConverter.toOnOffLightState(OnOffType.OFF, (deCONZLightState)currentState);
                    } else {
                    	newState = deCONZStateConverter.toColorLightState(hsbCommand, (deCONZLightState)currentState);
                    }
                	logger.debug("COL hsb");
                } else if (command instanceof PercentType) {
                	newState = deCONZStateConverter.toBrightnessLightState((PercentType) command, (deCONZLightState)currentState);
                	logger.debug("COL percent");
                } else if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	logger.debug("COL on/off");
                	if (((deCONZLightState)currentState).isOn() && !((deCONZLightState)newState).isOn()) {
                		// switch off - set brightness to 0
                		lastBrightness = ((deCONZLightState)currentState).getBrightness();
                    	logger.debug("save last BRI");
                	} else if (!((deCONZLightState)currentState).isOn() && ((deCONZLightState)newState).isOn()) {
                		// switch off - set brightness to last known value
                		((deCONZLightState)newState).setBrightness(lastBrightness);
                    	logger.debug("restore last BRI");
                	}
                } else if (command instanceof IncreaseDecreaseType) {
                	newState = convertBrightnessChangeToStateUpdate((IncreaseDecreaseType) command, (deCONZLightState)currentState);
                	logger.debug("COL increase/decrease");
                }
                break;
            case DECONZ_CHANNEL_ONOFF:
                if (command instanceof OnOffType) {
                	newState = deCONZStateConverter.toOnOffLightState((OnOffType) command, (deCONZLightState)currentState);
                	logger.debug("ON/OFF");
                }
                break;
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
		        updateState(channelUID, deCONZStateConverter.toColorTemperaturePercentType(state));
            	break;
            case DECONZ_CHANNEL_BRIGHTNESS:
		        if (!state.isOn()) {
		        	updateState(channelUID, new PercentType(0));
		        } else {
		        	updateState(channelUID, deCONZStateConverter.toBrightnessPercentType(state));
		        }
            	break;
            case DECONZ_CHANNEL_COLOR:
		        hsbType = deCONZStateConverter.toHSBType(state);
        		if (!state.isOn()) {
            		hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(0));
        		}
		        updateState(channelUID, hsbType);
            	break;
            case DECONZ_CHANNEL_ONOFF:
		        updateState(channelUID, state.isOn() ? OnOffType.ON : OnOffType.OFF);
            	break;
			}
        }
	}
    
    private deCONZDeviceState convertColorTempChangeToStateUpdate(IncreaseDecreaseType command, deCONZLightState state) {
        int colorTemperature = deCONZStateConverter.toAdjustedColorTemp(command, state.getColorTemperature());
        deCONZLightState update = new deCONZLightState(true, false, 0, 0, 0, 0);
        update.assign(state);
        update.setColorTemperature(colorTemperature);
        return update;
    }

    private deCONZDeviceState convertBrightnessChangeToStateUpdate(IncreaseDecreaseType command, deCONZLightState state) {
        int brightness = deCONZStateConverter.toAdjustedBrightness(command, state.getBrightness());
        deCONZLightState update = new deCONZLightState(true, false, 0, 0, 0, 0);
        update.assign(state);
        if (brightness == 0) {
            update.setOn(false);
        } else {
            update.setBrightness(brightness);
            update.setOn(true);
        }
        return update;
    }
}
