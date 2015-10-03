package org.openhab.binding.deconz.handler;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;

import com.google.common.collect.Sets;

/**
 * {@link deCONZTouchlinkHandler} is the handler for a ZigBee device that has been found by touchlinking. It uses the {@link deCONZBridgeHandler} to execute the actual
 * command.
 *
 * @author Mike Ludwig - Initial contribution of deconz binding
 */
public class deCONZTouchlinkHandler extends deCONZDeviceHandler {

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_TOUCHLINKDEVICE);

    public deCONZTouchlinkHandler(Thing thing) {
		super(thing);
	}
    
    @Override
    protected void handleInitializeForDevice() {
        setDeviceId((String) getConfig().get(DECONZ_TOUCHLINK_ID));
    }
    
    @Override
    protected void handleDisposeForDevice() {
    	// nothing to do
    }

    @Override
    protected deCONZDeviceState handleCommandForDevice(ChannelUID channelUID, Command command, deCONZDeviceState currentState) {
    	deCONZDeviceState newState = null;
    	if ((currentState != null) && (currentState instanceof deCONZTouchlinkState)) {
	        switch (channelUID.getId()) {
            case DECONZ_CHANNEL_IDENTIFY:
                if (command instanceof OnOffType) {
                	if (!((deCONZTouchlinkState)currentState).isIdentify() && !((deCONZTouchlinkState)currentState).isReset() &&
                			(((OnOffType)command).compareTo(OnOffType.ON) == 0)) {
                		newState = new deCONZTouchlinkState(true, false, ((deCONZTouchlinkState)currentState).isFactoryNew());
                		// reset the state after two seconds
                		postDelayedStateUpdate(new deCONZTouchlinkState(false, false, 
                				((deCONZTouchlinkState)currentState).isFactoryNew()), 2);
                	} // else ignore the command
                }
                break;
            case DECONZ_CHANNEL_RESET:
                if (command instanceof OnOffType) {
                	if (!((deCONZTouchlinkState)currentState).isIdentify() && !((deCONZTouchlinkState)currentState).isReset() &&
                			(((OnOffType)command).compareTo(OnOffType.ON) == 0)) {
                		newState = new deCONZTouchlinkState(false, true, ((deCONZTouchlinkState)currentState).isFactoryNew());
                		// reset the state after two seconds
                		postDelayedStateUpdate(new deCONZTouchlinkState(false, false, 
                				((deCONZTouchlinkState)currentState).isFactoryNew()), 2);
                	} // else ignore the command
                }
                break;
            default:
                logger.warn("Command send to an unknown channel id: " + channelUID);
                break;
	        }
    	}
        return newState;
    }

	@Override
	protected void handleStateChangeForDevice(ChannelUID channelUID, deCONZDeviceState currentState) {
        if (currentState instanceof deCONZTouchlinkState) {
        	deCONZTouchlinkState state = (deCONZTouchlinkState)currentState;
			switch (channelUID.getId()) {
            case DECONZ_CHANNEL_IDENTIFY:
		        updateState(channelUID, state.isIdentify() ? OnOffType.ON : OnOffType.OFF);
            	break;
            case DECONZ_CHANNEL_RESET:
		        updateState(channelUID, state.isReset() ? OnOffType.ON : OnOffType.OFF);
            	break;
			}
        }
	}
}
