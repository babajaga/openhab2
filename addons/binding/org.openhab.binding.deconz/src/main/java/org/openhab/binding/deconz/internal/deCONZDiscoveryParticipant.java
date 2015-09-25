package org.openhab.binding.deconz.internal;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.UpnpDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.binding.deconz.deCONZBindingConstants;

/**
 * The {@link deCONZDiscoveryParticipant} is responsible for discovering new and
 * removed hue bridges. It uses the central {@link UpnpDiscoveryService}.
 *
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZDiscoveryParticipant implements UpnpDiscoveryParticipant {

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(deCONZBindingConstants.THING_TYPE_BRIDGE);
    }

    @Override
    public DiscoveryResult createResult(RemoteDevice device) {
        ThingUID uid = getThingUID(device);
        if (uid != null) {
            Map<String, Object> properties = new HashMap<>(2);
            properties.put(deCONZBindingConstants.DECONZ_BRIDGE_LOCATION, device.getDetails().getBaseURL().getHost());
            properties.put(deCONZBindingConstants.DECONZ_SERIAL_NUMBER, device.getDetails().getSerialNumber());

            DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                    .withLabel(device.getDetails().getFriendlyName())
                    .withRepresentationProperty(deCONZBindingConstants.DECONZ_SERIAL_NUMBER).build();
            return result;
        } else {
            return null;
        }
    }

    @Override
    public ThingUID getThingUID(RemoteDevice device) {
        DeviceDetails details = device.getDetails();
        if (details != null) {
            ModelDetails modelDetails = details.getModelDetails();
            if (modelDetails != null) {
                String description = modelDetails.getModelDescription();
                String modelName = modelDetails.getModelName();
                if ((description != null) && (modelName != null)) {
                	if (description.startsWith("dresden elektronik")) {
	                    if (modelName.contains("Wireless Light Control Gateway")) {
	                    	URL url = details.getBaseURL();
	                    	if (url != null) {
		                       	System.out.println("UPNP discovered deCONZ at " + 
		                       			url.getProtocol() + "://" + url.getPath());
		                        return new ThingUID(deCONZBindingConstants.THING_TYPE_BRIDGE, 
		                        		details.getSerialNumber());
	                    	}
	                    }
                	}
                   	System.out.println("UPNP result '" + modelName + "' ignored");
                }
            }
        }
        return null;
    }
}
