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
import org.openhab.binding.deconz.handler.deCONZLight;
import org.openhab.binding.deconz.handler.deCONZLightHandler;
import org.openhab.binding.deconz.handler.deCONZLightStatusListener;
import org.openhab.binding.deconz.handler.deCONZSensor;
import org.openhab.binding.deconz.handler.deCONZSensorHandler;
import org.openhab.binding.deconz.handler.deCONZSensorStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link deCONZDiscoveryService} tracks for devices which are connected
 * to a paired bridge. The default search time for is 60 seconds.
 *
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZDiscoveryService extends AbstractDiscoveryService implements deCONZLightStatusListener,
	deCONZSensorStatusListener {

    private final Logger logger = LoggerFactory.getLogger(deCONZDiscoveryService.class);

    private final static int SEARCH_TIME = 60;

    private deCONZBridgeHandler bridgeHandler;

    public deCONZDiscoveryService(deCONZBridgeHandler handler) {
        super(SEARCH_TIME);
        bridgeHandler = handler;
    }

    public void activate() {
    	bridgeHandler.registerLightStatusListener(this);
    	bridgeHandler.registerSensorStatusListener(this);
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
        bridgeHandler.unregisterSensorStatusListener(this);
        bridgeHandler.unregisterLightStatusListener(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return Sets.union(deCONZLightHandler.SUPPORTED_THING_TYPES, deCONZSensorHandler.SUPPORTED_THING_TYPES);
    }

    @Override
    public void startScan() {
        List<deCONZLight> lights = bridgeHandler.getAllKnownLights();
        if (lights != null) {
            for (deCONZLight l : lights) {
                onLightAddedInternal(l);
            }
        }
        List<deCONZSensor> sensors = bridgeHandler.getAllKnownSensors();
        if (sensors != null) {
            for (deCONZSensor s : sensors) {
                onSensorAddedInternal(s);
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

    private void onLightAddedInternal(deCONZLight light) {
        ThingUID thingUID = getThingUID(light);
        if (thingUID != null) {
            ThingUID bridgeUID = bridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(deCONZBindingConstants.DECONZ_LIGHT_ID, light.getId());
            properties.put(deCONZBindingConstants.DECONZ_UNIQUEID, light.getUniqueId());
            properties.put(deCONZBindingConstants.DECONZ_MANUFACTURER, light.getManufacturer());
            properties.put(deCONZBindingConstants.DECONZ_MODEL, light.getModelID());
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, light.getSoftwareVersion());
            properties.put(Thing.PROPERTY_SERIAL_NUMBER, light.getUniqueId());
            properties.put(Thing.PROPERTY_VENDOR, light.getManufacturer());
            properties.put(Thing.PROPERTY_MODEL_ID, light.getModelID());
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(light.getName()).build();
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("discovered unsupported light of type '{}' with id {}", light.getModelID(), light.getId());
        }
    }

    private void onSensorAddedInternal(deCONZSensor sensor) {
        ThingUID thingUID = getThingUID(sensor);
        if (thingUID != null) {
            ThingUID bridgeUID = bridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(deCONZBindingConstants.DECONZ_SENSOR_ID, sensor.getId());
            properties.put(deCONZBindingConstants.DECONZ_UNIQUEID, sensor.getUniqueId());
            properties.put(deCONZBindingConstants.DECONZ_MANUFACTURER, sensor.getManufacturer());
            properties.put(deCONZBindingConstants.DECONZ_MODEL, sensor.getModelID());
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, sensor.getSoftwareVersion());
            properties.put(Thing.PROPERTY_SERIAL_NUMBER, sensor.getUniqueId());
            properties.put(Thing.PROPERTY_VENDOR, sensor.getManufacturer());
            properties.put(Thing.PROPERTY_MODEL_ID, sensor.getModelID());
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(sensor.getName()).build();
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("discovered unsupported sensor of type '{}' with id {}", sensor.getModelID(), sensor.getId());
        }
    }

    private ThingUID getThingUID(deCONZLight light) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = new ThingTypeUID(deCONZBindingConstants.BINDING_ID, light.getThingType());
        if (getSupportedThingTypes().contains(thingTypeUID)) {
            String thingLightId = light.getId();
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingLightId);
            return thingUID;
        } else {
            return null;
        }
    }

    private ThingUID getThingUID(deCONZSensor sensor) {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        ThingTypeUID thingTypeUID = new ThingTypeUID(deCONZBindingConstants.BINDING_ID, sensor.getThingType());
        if (getSupportedThingTypes().contains(thingTypeUID)) {
            String thingSensorId = sensor.getId();
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingSensorId);
            return thingUID;
        } else {
            return null;
        }
    }
    
	@Override
	public void onLightStateChanged(deCONZLight light) {
	}

	@Override
	public void onLightRemoved(deCONZLight light) {
        ThingUID thingUID = getThingUID(light);
        if (thingUID != null) {
            thingRemoved(thingUID);
        }
	}

	@Override
	public void onLightAdded(deCONZLight light) {
        onLightAddedInternal(light);
	}

	@Override
	public void onSensorStateChanged(deCONZSensor sensor) {
	}

	@Override
	public void onSensorRemoved(deCONZSensor sensor) {
        ThingUID thingUID = getThingUID(sensor);
        if (thingUID != null) {
            thingRemoved(thingUID);
        }
	}

	@Override
	public void onSensorAdded(deCONZSensor sensor) {
        onSensorAddedInternal(sensor);
	}
}
