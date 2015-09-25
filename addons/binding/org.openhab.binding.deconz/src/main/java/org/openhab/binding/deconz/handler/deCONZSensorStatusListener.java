package org.openhab.binding.deconz.handler;

/**
 * The {@link deCONZSensorStatusListener} is notified when a sensor status has changed or a sensor 
 * has been removed or added.
 *
 * @author Mike Ludwig - Initial contribution
 */
public interface deCONZSensorStatusListener {

    /**
     * This method is called whenever the state of a sensor has changed. The new state can be obtained by
     * {@link deCONZSensor#getState()}.
     * 
     * @param sensor The sensor which received the state update.
     */
    public void onSensorStateChanged(deCONZSensor sensor);

    /**
     * This method us called whenever a sensor is removed.
     * 
     * @param sensor The sensor which is removed.
     */
    public void onSensorRemoved(deCONZSensor sensor);

    /**
     * This method us called whenever a sensor is added.
     * 
     * @param sensor The sensor which is added.
     */
    public void onSensorAdded(deCONZSensor sensor);

}
