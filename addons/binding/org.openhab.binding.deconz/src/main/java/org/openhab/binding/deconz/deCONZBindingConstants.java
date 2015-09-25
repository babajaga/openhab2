package org.openhab.binding.deconz;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link deCONZBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZBindingConstants {

    public static final String BINDING_ID = "deconz";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "deCONZBridge");
    public final static ThingTypeUID THING_TYPE_RGBLIGHT = new ThingTypeUID(BINDING_ID, "deCONZRGBLight");
    public final static ThingTypeUID THING_TYPE_DIMLIGHT = new ThingTypeUID(BINDING_ID, "deCONZDimmableLight");
    public final static ThingTypeUID THING_TYPE_ONOFFLIGHT = new ThingTypeUID(BINDING_ID, "deCONZLight");
    
    public final static ThingTypeUID THING_TYPE_ONOFFSWITCH = new ThingTypeUID(BINDING_ID, "deCONZSwitch");
    public final static ThingTypeUID THING_TYPE_DIMSWITCH = new ThingTypeUID(BINDING_ID, "deCONZDimmer");
    
    // List of configuration parameter names
    public static final String DECONZ_BRIDGE_LOCATION = "BridgeLocation";
    public static final String DECONZ_BRIDGE_USERNAME = "Username";
    public static final String DECONZ_BRIDGE_PASSWORD = "Password";
    public static final String DECONZ_BRIDGE_APIKEY = "ApiKey";
    public static final String DECONZ_SOFTWAREVERSION = "SoftwareVersion";
    public static final String DECONZ_UNIQUEID = "UniqueId";
    public static final String DECONZ_MANUFACTURER = "Manufacturer";
    public static final String DECONZ_MODEL = "Model";
    public static final String DECONZ_LIGHT_ID = "deconzLightID";
    public static final String DECONZ_SENSOR_ID = "deconzSensorID";

    public static final String DECONZ_SERIAL_NUMBER = "serialNumber";
    
    // List of all Channel ids
    public final static String DECONZ_CHANNEL_COLOR = "deconzColor";
    public final static String DECONZ_CHANNEL_COLORTEMP = "deconzColorTemperature";
    public final static String DECONZ_CHANNEL_BRIGHTNESS = "deconzBrightness";
    public final static String DECONZ_CHANNEL_ONOFF = "deconzOnOff";
}
