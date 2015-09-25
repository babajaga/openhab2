package org.openhab.binding.deconz.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;

/**
 * The {@link deCONZStateConverter} is responsible for mapping Eclipse SmartHome types to deconz 
 * binding types and vice versa.
 *
 * @author Mike Ludwig - Initial contribution
 */
public class deCONZStateConverter {

    private static final int HUE_FACTOR = 182;
    private static final double SATURATION_FACTOR = 2.54;
    private static final double BRIGHTNESS_FACTOR = 2.54;
    
    private static final double PERCENTAGE_FACTOR = 1.00;

    public static final int MIN_COLOR_TEMPERATURE = 153;
    public static final int MAX_COLOR_TEMPERATURE = 500;
    private static final int COLOR_TEMPERATURE_RANGE = MAX_COLOR_TEMPERATURE - MIN_COLOR_TEMPERATURE;

    private static final int DIM_STEPSIZE = 30;

    /**
     * Transforms the given {@link HSBType} into a light state.
     *
     * @param hsbType HSB type
     * @return light state representing the {@link HSBType}.
     */
    public static deCONZLightStateUpdate toColorLightState(HSBType hsbType) {
        int hue = (int) Math.round(hsbType.getHue().doubleValue() * HUE_FACTOR);
        int saturation = (int) Math.round(hsbType.getSaturation().doubleValue() * SATURATION_FACTOR);
        int brightness = (int) Math.round(hsbType.getBrightness().doubleValue() * BRIGHTNESS_FACTOR);

        deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        update.setHue(hue);
        update.setSaturation(saturation);
        if (brightness > 0) {
            update.setBrightness(brightness);
        }
        return update;
    }

    /**
     * Transforms the given {@link OnOffType} into a light state containing the
     * 'on' value.
     *
     * @param onOffType on or off state
     * @return light state containing the 'on' value
     */
    public static deCONZLightStateUpdate toOnOffLightState(OnOffType onOffType) {
        deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        update.setOn(OnOffType.ON.equals(onOffType));
        return update;
    }

    /**
     * Transforms the given {@link PercentType} into a light state containing
     * the brightness and the 'on' value represented by {@link PercentType}.
     *
     * @param percentType brightness represented as {@link PercentType}
     * @return light state containing the brightness and the 'on' value
     */
    public static deCONZLightStateUpdate toBrightnessLightState(PercentType percentType) {
        boolean on = percentType.equals(PercentType.ZERO) ? false : true;
        final deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        update.setOn(on);
        int brightness = (int) Math.round(percentType.floatValue() * BRIGHTNESS_FACTOR);
        if (brightness > 0) {
            update.setBrightness(brightness);
        }
        return update;
    }

    /**
     * Adjusts the given brightness using the {@link IncreaseDecreaseType} and returns the updated value.
     *
     * @param type The {@link IncreaseDecreaseType} to be used
     * @param currentBrightness The current brightness
     * @return The adjusted brightness value
     */
    public static int toAdjustedBrightness(IncreaseDecreaseType command, int currentBrightness) {
        int newBrightness;
        if (command == IncreaseDecreaseType.DECREASE) {
            newBrightness = Math.max(currentBrightness - DIM_STEPSIZE, 0);
        } else {
            newBrightness = Math.min(currentBrightness + DIM_STEPSIZE, (int) (BRIGHTNESS_FACTOR * 100));
        }
        return newBrightness;
    }

    
    /**
     * Transforms the given {@link PercentType} into a light state containing
     * the color temperature represented by {@link PercentType}.
     *
     * @param percentType color temperature represented as {@link PercentType}
     * @return light state containing the color temperature
     */
    public static deCONZLightStateUpdate toColorTemperatureLightState(PercentType percentType) {
        int colorTemperature = MIN_COLOR_TEMPERATURE
                + Math.round((COLOR_TEMPERATURE_RANGE * percentType.floatValue()) / 100);
        deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        update.setColorTemperature(colorTemperature);
        return update;
    }

    /**
     * Adjusts the given color temperature using the {@link IncreaseDecreaseType} and returns the updated value.
     *
     * @param type The {@link IncreaseDecreaseType} to be used
     * @param currentColorTemp The current color temperature
     * @return The adjusted color temperature value
     */
    public static int toAdjustedColorTemp(IncreaseDecreaseType type, int currentColorTemp) {
        int newColorTemp;
        if (type == IncreaseDecreaseType.DECREASE) {
            newColorTemp = Math.max(currentColorTemp - DIM_STEPSIZE, MIN_COLOR_TEMPERATURE);
        } else {
            newColorTemp = Math.min(currentColorTemp + DIM_STEPSIZE, MAX_COLOR_TEMPERATURE);
        }
        return newColorTemp;
    }

    /**
     * Transforms {@link HueLightState} into {@link PercentType} representing
     * the color temperature.
     *
     * @param lightState
     *            light state
     * @return percent type representing the color temperature
     */
    public static PercentType toColorTemperaturePercentType(deCONZLightState lightState) {
        int percent = (lightState.getColorTemperature() - MIN_COLOR_TEMPERATURE) / COLOR_TEMPERATURE_RANGE;
        return new PercentType(restrictToBounds(percent));
    }

    /**
     * Transforms {@link HueLightState} into {@link PercentType} representing
     * the brightness.
     *
     * @param lightState
     *            light state
     * @return percent type representing the brightness
     */
    public static PercentType toBrightnessPercentType(deCONZLightState lightState) {
        int percent = (int) (lightState.getBrightness() / BRIGHTNESS_FACTOR);
        return new PercentType(restrictToBounds(percent));
    }

    /**
     * Transforms {@link HueLightState} into {@link HSBType} representing the
     * color.
     *
     * @param lightState
     *            light state
     * @return HSB type representing the color
     */
    public static HSBType toHSBType(deCONZLightState lightState) {
        int hue = lightState.getHue();

        int saturationInPercent = (int) (lightState.getSaturation() / SATURATION_FACTOR);
        int brightnessInPercent = (int) (lightState.getBrightness() / BRIGHTNESS_FACTOR);

        saturationInPercent = restrictToBounds(saturationInPercent);
        brightnessInPercent = restrictToBounds(brightnessInPercent);

        HSBType hsbType = new HSBType(new DecimalType(hue / HUE_FACTOR), new PercentType(saturationInPercent),
                new PercentType(brightnessInPercent));

        return hsbType;
    }

    private static int restrictToBounds(int percentValue) {
        if (percentValue < 0) {
            return 0;
        } else if (percentValue > 100) {
            return 100;
        }
        return percentValue;
    }

    /**
     * Transforms the given {@link OnOffType} into a light state containing the
     * 'on' value.
     *
     * @param onOffType on or off state
     * @return light state containing the 'on' value
     */
    public static deCONZLightStateUpdate toOnOffSensorState(OnOffType onOffType) {
        deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        update.setOn(OnOffType.ON.equals(onOffType));
        return update;
    }

    /**
     * Adjusts the given percentage using the {@link IncreaseDecreaseType} and returns the updated value.
     *
     * @param type The {@link IncreaseDecreaseType} to be used
     * @param currentPercentage The current percentage
     * @return The adjusted percentage value
     */
    public static int toAdjustedPercentage(IncreaseDecreaseType command, int currentPercentage) {
        int newPercentage;
        if (command == IncreaseDecreaseType.DECREASE) {
        	newPercentage = Math.max(currentPercentage - DIM_STEPSIZE, 0);
        } else {
        	newPercentage = Math.min(currentPercentage + DIM_STEPSIZE, (int) (PERCENTAGE_FACTOR * 100));
        }
        return newPercentage;
    }

    /**
     * Transforms the given {@link PercentType} into a sensor state containing
     * the percentage and the 'on' value represented by {@link PercentType}.
     *
     * @param percentType percentage represented as {@link PercentType}
     * @return sensor state containing the percentage and the 'on' value
     */
    public static deCONZLightStateUpdate toPercentageSensorState(PercentType percentType) {
        boolean on = percentType.equals(PercentType.ZERO) ? false : true;
        final deCONZLightStateUpdate update = new deCONZLightStateUpdate();
        update.setOn(on);
        int value = (int) Math.round(percentType.floatValue() * PERCENTAGE_FACTOR);
        if (value > 0) {
            update.setBrightness(value);
        }
        return update;
    }
 }