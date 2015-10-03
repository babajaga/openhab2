package org.openhab.binding.deconz.internal;

import static org.openhab.binding.deconz.deCONZBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.openhab.binding.deconz.handler.deCONZBridgeHandler;
import org.openhab.binding.deconz.handler.deCONZLightHandler;
import org.openhab.binding.deconz.handler.deCONZSensorHandler;
import org.openhab.binding.deconz.handler.deCONZTouchlinkHandler;

import com.google.common.collect.Sets;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

import org.osgi.framework.ServiceRegistration;

/**
 * The {@link deCONZHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZHandlerFactory extends BaseThingHandlerFactory {
    
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.union(Sets.union(Sets.union(deCONZBridgeHandler.SUPPORTED_THING_TYPES,
            deCONZLightHandler.SUPPORTED_THING_TYPES), deCONZSensorHandler.SUPPORTED_THING_TYPES), deCONZTouchlinkHandler.SUPPORTED_THING_TYPES);
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        if (deCONZBridgeHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
        	deCONZBridgeHandler handler = new deCONZBridgeHandler((Bridge) thing);
            registerDiscoveryService(handler);
            return handler;
        }
        if (deCONZLightHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            return new deCONZLightHandler(thing);
        }
        if (deCONZSensorHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            return new deCONZSensorHandler(thing);
        }
        if (deCONZTouchlinkHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            return new deCONZTouchlinkHandler(thing);
        }
        return null;
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        if (deCONZBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID bridge = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, bridge, null);
        }
        if (deCONZLightHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID light = getLightUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, light, bridgeUID);
        }
        if (deCONZSensorHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID sensor = getSensorUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, sensor, bridgeUID);
        }
        if (deCONZTouchlinkHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID sensor = getTouchlinkUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, sensor, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the deconz binding.");
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
        	String uuid = UUID.randomUUID().toString();
        	uuid.replaceAll("-", "");
            thingUID = new ThingUID(thingTypeUID, uuid);
        }
        return thingUID;
    }

    private ThingUID getLightUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String lightId = (String) configuration.get(DECONZ_LIGHT_ID);

        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, lightId, bridgeUID.getId());
        }
        return thingUID;
    }

    private ThingUID getSensorUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String sensorId = (String) configuration.get(DECONZ_SENSOR_ID);

        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, sensorId, bridgeUID.getId());
        }
        return thingUID;
    }

    private ThingUID getTouchlinkUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String id = (String) configuration.get(DECONZ_TOUCHLINK_ID);

        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, id, bridgeUID.getId());
        }
        return thingUID;
    }
    
    private synchronized void registerDiscoveryService(ThingHandler thingHandler) {
        if (thingHandler instanceof deCONZBridgeHandler) {
            deCONZDiscoveryService service = new deCONZDiscoveryService((deCONZBridgeHandler)thingHandler);
            service.activate();
            this.discoveryServiceRegs.put(thingHandler.getThing().getUID(), bundleContext.registerService(
                    DiscoveryService.class.getName(), service, new Hashtable<String, Object>()));
            ((deCONZBridgeHandler)thingHandler).setDiscoveryService(service);
        }
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof deCONZBridgeHandler) {
        	((deCONZBridgeHandler)thingHandler).setDiscoveryService(null);
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
            	deCONZDiscoveryService service = (deCONZDiscoveryService) bundleContext.getService(serviceReg
                        .getReference());
                service.deactivate();
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }
}

