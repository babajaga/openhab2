package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.library.types.HSBType;
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
 * {@link deCONZLightHandler} is the handler for a ZLL light. It uses the {@link deCONZBridgeHandler} to execute the actual
 * command.
 *
 * @author Mike Ludwig - Initial contribution of deconz binding
 */
public class deCONZLightHandler extends BaseThingHandler implements deCONZLightStatusListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_RGBLIGHT, 
    		THING_TYPE_DIMLIGHT, THING_TYPE_ONOFFLIGHT);

    private String lightId;
    private Integer lastSentColorTemp;
    private Integer lastSentBrightness;
    private Logger logger = LoggerFactory.getLogger(deCONZLightHandler.class);
    private deCONZBridgeHandler bridgeHandler;

    public deCONZLightHandler(Thing deconzLight) {
        super(deconzLight);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing deconz light handler.");
        final String configLightId = (String) getConfig().get(DECONZ_LIGHT_ID);
        if (configLightId != null) {
            lightId = configLightId;
            // Access the bridge and register a light state listener to get  informed about light state changes.
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
        logger.debug("Disposing deconz light handler.");
        disposeBridgeHandler();        
        lightId = null;
    }

    private deCONZLight getLight() {
        deCONZBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            return bridgeHandler.getLightById(lightId);
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

        deCONZLight light = getLight();
        if (light == null) {
            logger.debug("light not known on bridge. Cannot handle command.");
            return;
        }

        deCONZLightStateUpdate lightState = null;

        switch (channelUID.getId()) {
            case DECONZ_CHANNEL_COLORTEMP:
                if (command instanceof PercentType) {
                    lightState = deCONZStateConverter.toColorTemperatureLightState((PercentType) command);
                } else if (command instanceof OnOffType) {
                    lightState = deCONZStateConverter.toOnOffLightState((OnOffType) command);
                } else if (command instanceof IncreaseDecreaseType) {
                    lightState = convertColorTempChangeToStateUpdate((IncreaseDecreaseType) command, light);
                }
                break;
            case DECONZ_CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                    lightState = deCONZStateConverter.toBrightnessLightState((PercentType) command);
                } else if (command instanceof OnOffType) {
                    lightState = deCONZStateConverter.toOnOffLightState((OnOffType) command);
                } else if (command instanceof IncreaseDecreaseType) {
                    lightState = convertBrightnessChangeToStateUpdate((IncreaseDecreaseType) command, light);
                }
                break;
            case DECONZ_CHANNEL_COLOR:
                if (command instanceof HSBType) {
                    HSBType hsbCommand = (HSBType) command;
                    if (hsbCommand.getBrightness().intValue() == 0) {
                        lightState = deCONZStateConverter.toOnOffLightState(OnOffType.OFF);
                    } else {
                        lightState = deCONZStateConverter.toColorLightState(hsbCommand);
                    }
                } else if (command instanceof PercentType) {
                    lightState = deCONZStateConverter.toBrightnessLightState((PercentType) command);
                } else if (command instanceof OnOffType) {
                    lightState = deCONZStateConverter.toOnOffLightState((OnOffType) command);
                } else if (command instanceof IncreaseDecreaseType) {
                    lightState = convertBrightnessChangeToStateUpdate((IncreaseDecreaseType) command, light);
                }
                break;
            case DECONZ_CHANNEL_ONOFF:
                if (command instanceof OnOffType) {
                    lightState = deCONZStateConverter.toOnOffLightState((OnOffType) command);
                }
                break;
        }
        if (lightState != null) {
            bridgeHandler.updateLightState(light, lightState);
        } else {
            logger.warn("Command send to an unknown channel id: " + channelUID);
        }
    }

    private deCONZLightStateUpdate convertColorTempChangeToStateUpdate(IncreaseDecreaseType command, deCONZLight light) {
        deCONZLightStateUpdate update = null;
        Integer currentColorTemp = getCurrentColorTemp(light.getState());
        if (currentColorTemp != null) {
            int newColorTemp = deCONZStateConverter.toAdjustedColorTemp(command, currentColorTemp);
            update = new deCONZLightStateUpdate();
            update.setColorTemperature(newColorTemp);
            lastSentColorTemp = newColorTemp;
        }
        return update;
    }

    private Integer getCurrentColorTemp(deCONZLightState lightState) {
        Integer colorTemp = lastSentColorTemp;
        if (colorTemp == null && lightState != null) {
            colorTemp = lightState.getColorTemperature();
        }
        return colorTemp;
    }

    private deCONZLightStateUpdate convertBrightnessChangeToStateUpdate(IncreaseDecreaseType command, deCONZLight light) {
        deCONZLightStateUpdate update = null;
        Integer currentBrightness = getCurrentBrightness(light.getState());
        if (currentBrightness != null) {
            int newBrightness = deCONZStateConverter.toAdjustedBrightness(command, currentBrightness);
            update = createBrightnessStateUpdate(currentBrightness, newBrightness);
            lastSentBrightness = newBrightness;
        }
        return update;
    }

    private Integer getCurrentBrightness(deCONZLightState lightState) {
        Integer brightness = lastSentBrightness;
        if (brightness == null && lightState != null) {
            if (!lightState.isOn()) {
                brightness = 0;
            } else {
                brightness = lightState.getBrightness();
            }
        }
        return brightness;
    }

    private deCONZLightStateUpdate createBrightnessStateUpdate(int currentBrightness, int newBrightness) {
        deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        if (newBrightness == 0) {
            update.setOn(false);
        } else {
            update.setBrightness(newBrightness);
            if (currentBrightness == 0){
                update.setOn(true);
            }
        }
        return update;
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
                bridgeHandler.registerLightStatusListener(this);
            } else {
                return null;
            }
        }
        return bridgeHandler;
    }

    private void disposeBridgeHandler() {
        if (bridgeHandler != null) {
        	bridgeHandler.unregisterLightStatusListener(this);
        }
    }
    
	@Override
	public void onLightStateChanged(deCONZLight light) {
        if (light.getId().equals(lightId)) {
            lastSentColorTemp = null;
            lastSentBrightness = null;

            // update status (ONLINE, OFFLINE)
            if (light.getState().isReachable()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Bridge reports light as not reachable");
            }

            HSBType hsbType = deCONZStateConverter.toHSBType(light.getState());
            if (!light.getState().isOn()) {
                hsbType = new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(0));
            }
            updateState(new ChannelUID(getThing().getUID(), DECONZ_CHANNEL_COLOR), hsbType);

            PercentType percentType = deCONZStateConverter.toColorTemperaturePercentType(light.getState());
            updateState(new ChannelUID(getThing().getUID(), DECONZ_CHANNEL_COLORTEMP), percentType);

            percentType = deCONZStateConverter.toBrightnessPercentType(light.getState());
            if (!light.getState().isOn()) {
                percentType = new PercentType(0);
            }
            updateState(new ChannelUID(getThing().getUID(), DECONZ_CHANNEL_BRIGHTNESS), percentType);
        }
	}

	@Override
	public void onLightRemoved(deCONZLight light) {
        if (light.getId().equals(lightId)) {
            updateStatus(ThingStatus.OFFLINE);
        }
	}

	@Override
	public void onLightAdded(deCONZLight light) {
        if (light.getId().equals(lightId)) {
            updateStatus(ThingStatus.ONLINE);
            onLightStateChanged(light);
        }
	}
}