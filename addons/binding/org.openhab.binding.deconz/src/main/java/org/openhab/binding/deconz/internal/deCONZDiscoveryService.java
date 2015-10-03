package org.openhab.binding.deconz.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.deconz.deCONZBindingConstants;
import org.openhab.binding.deconz.handler.deCONZBridgeHandler;
import org.openhab.binding.deconz.handler.deCONZDevice;
import org.openhab.binding.deconz.handler.deCONZLightHandler;
import org.openhab.binding.deconz.handler.deCONZLightState;
import org.openhab.binding.deconz.handler.deCONZSensorHandler;
import org.openhab.binding.deconz.handler.deCONZSensorState;
import org.openhab.binding.deconz.handler.deCONZTouchlinkHandler;
import org.openhab.binding.deconz.handler.deCONZTouchlinkState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link deCONZDiscoveryService} tracks for devices which are connected
 * to a paired bridge. The default search time is 60 seconds.
 *
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(deCONZDiscoveryService.class);

    private final static int SEARCH_TIME = 60;

    private deCONZBridgeHandler bridgeHandler = null;

    public deCONZDiscoveryService(deCONZBridgeHandler handler) {
        super(SEARCH_TIME);
        bridgeHandler = handler;
    }

    public void activate() {
    }

	public void deviceRemoved(deCONZDevice device) {
		ThingUID thingUID = getThingUID(device);
		if (thingUID != null) {
			thingRemoved(thingUID);
		}
	}

	public void deviceAdded(deCONZDevice device) {
		if (device != null) {
			ThingUID thingUID = getThingUID(device);
			if (thingUID != null) {
	            ThingUID bridgeUID = bridgeHandler.getThing().getUID();
	            Map<String, Object> properties = new HashMap<>();
	            if (device.getState() instanceof deCONZLightState) {
	            	properties.put(deCONZBindingConstants.DECONZ_LIGHT_ID, device.getInternalId());
	            } else if (device.getState() instanceof deCONZSensorState) {
	                properties.put(deCONZBindingConstants.DECONZ_SENSOR_ID, device.getInternalId());
	            } else if (device.getState() instanceof deCONZTouchlinkState) {
	                properties.put(deCONZBindingConstants.DECONZ_TOUCHLINK_ID, device.getInternalId());
	                if (((deCONZTouchlinkState)device.getState()).isFactoryNew()) {
	                	properties.put(deCONZBindingConstants.DECONZ_FACTORYNEW, "Yes");
	                } else {
	                	properties.put(deCONZBindingConstants.DECONZ_FACTORYNEW, "No");
	                }
	            } else {
	            	return;
	            }
	            properties.put(deCONZBindingConstants.DECONZ_UNIQUEID, device.getUniqueId());
	            properties.put(deCONZBindingConstants.DECONZ_MANUFACTURER, device.getManufacturer());
	            properties.put(deCONZBindingConstants.DECONZ_MODEL, device.getModelID());
	            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, device.getSoftwareVersion());
	            properties.put(Thing.PROPERTY_SERIAL_NUMBER, device.getUniqueId());
	            properties.put(Thing.PROPERTY_VENDOR, device.getManufacturer());
	            properties.put(Thing.PROPERTY_MODEL_ID, device.getModelID());
	            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
	                    .withBridge(bridgeUID).withLabel(device.getName()).build();
	            thingDiscovered(discoveryResult);
	        } else {
	            logger.debug("discovered unsupported device of type '{}' with id {}",
	            		device.getModelID(), device.getInternalId());
	        }
		}
	}
    
    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Sets.union(Sets.union(deCONZLightHandler.SUPPORTED_THING_TYPES, deCONZSensorHandler.SUPPORTED_THING_TYPES), 
        		deCONZTouchlinkHandler.SUPPORTED_THING_TYPES);
    }

    @Override
    public void startScan() {
        List<deCONZDevice> devices = bridgeHandler.getAllKnownDevices();
        if (devices != null) {
            for (deCONZDevice d : devices) {
            	deviceAdded(d);
            }
        }
        // search for unpaired devices
        bridgeHandler.startSearch();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    private ThingUID getThingUID(deCONZDevice device) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = new ThingTypeUID(deCONZBindingConstants.BINDING_ID, device.getThingType());
        if (getSupportedThingTypes().contains(thingTypeUID)) {
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, device.getInternalId());
            return thingUID;
        } else {
            return null;
        }
    }
}
