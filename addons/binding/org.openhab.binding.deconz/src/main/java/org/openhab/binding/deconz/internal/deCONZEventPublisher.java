package org.openhab.binding.deconz.internal;

import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.State;

public class deCONZEventPublisher {

	private EventPublisher eventPublisher = null; 

    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }	
	
	public void publish(Thing thing, State state) {
		if (eventPublisher != null) {
			List<Channel> channels = thing.getChannels();
			for (Channel c : channels) {
				@SuppressWarnings("deprecation")
				Set<Item> items = c.getLinkedItems();
				for (Item i : items) {
					if (i instanceof GenericItem) {
						GenericItem e = (GenericItem)i;
						e.setState(state);
//						TypeParser.parseState(types, s);
				        ItemStateEvent event = ItemEventFactory.createStateEvent(e.getName(), state);
				        eventPublisher.post(event);
					}
				}
			}
		}
	}	
}
