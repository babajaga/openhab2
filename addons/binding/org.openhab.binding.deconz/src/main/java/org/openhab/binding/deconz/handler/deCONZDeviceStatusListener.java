package org.openhab.binding.deconz.handler;

/**
 * The {@link deCONZDeviceStatusListener} is notified when a sensor status has changed or a sensor 
 * has been removed or added.
 *
 * @author Mike Ludwig - Initial contribution
 */
public interface deCONZDeviceStatusListener {

    /**
     * This method is called whenever the state of a device has changed. The new state can be obtained by
     * {@link deCONZDevice#getState()}.
     * 
     * @param device The device which received the state update.
     */
    public void onDeviceStateChanged(deCONZDevice device);

    /**
     * This method us called whenever a device is removed.
     * 
     * @param device The device which is removed.
     */
    public void onDeviceRemoved(deCONZDevice device);

    /**
     * This method us called whenever a device is added.
     * 
     * @param device The device which is added.
     */
    public void onDeviceAdded(deCONZDevice device);

}
