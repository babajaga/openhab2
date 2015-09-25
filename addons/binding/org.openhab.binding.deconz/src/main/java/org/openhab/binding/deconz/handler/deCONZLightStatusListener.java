package org.openhab.binding.deconz.handler;

/**
 * The {@link deCONZLightStatusListener} is notified when a light status has changed or a light 
 * has been removed or added.
 *
 * @author Mike Ludwig - Initial contribution
 */
public interface deCONZLightStatusListener {

    /**
     * This method is called whenever the state of a light has changed. The new state can be obtained by
     * {@link deCONZLight#getState()}.
     * 
     * @param light The light which received the state update.
     */
    public void onLightStateChanged(deCONZLight light);

    /**
     * This method us called whenever a light is removed.
     * 
     * @param light The light which is removed.
     */
    public void onLightRemoved(deCONZLight light);

    /**
     * This method us called whenever a light is added.
     * 
     * @param light The light which is added.
     */
    public void onLightAdded(deCONZLight light);
}
