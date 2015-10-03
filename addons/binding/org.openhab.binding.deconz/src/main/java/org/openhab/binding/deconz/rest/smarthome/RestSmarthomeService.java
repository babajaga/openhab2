package org.openhab.binding.deconz.rest.smarthome;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openhab.binding.deconz.rest.RestJson;
import org.openhab.binding.deconz.rest.RestResult;
import org.openhab.binding.deconz.rest.smarthome.RestSmarthomeReader.RestBridgeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestSmarthomeService extends RestSmarthomeWorker {

	private final static int CACHE_GROUPS = 0;
	
    private Logger logger = LoggerFactory.getLogger(RestSmarthomeService.class);
	
	@SuppressWarnings("unused")
	private RestSmarthomeReader notify = null;
	// the groups cache
	private final Lock groupsLock = new ReentrantLock();
	private Map<String, String> groups = null;
	
	public void addReader(RestBridgeReader bridge) {
		notify = new RestSmarthomeReader(bridge);
	}

	public String getBaseURL() {
		return baseURL;
	}
	
	public void checkAndCreateGroup(String name) {
		if ((name != null) && !name.isEmpty()) {
			groupsLock.lock();
			if (groups == null) {
				String url = baseURL + "/rest/items"; 
				RestResult ret = executeGet(url, null);
				if (ret.getResult() == RestResult.REST_OK) {
					try {
						groups = RestJson.parseItemResponseForGroups(ret.getData());
						timeoutAdd(10, CACHE_GROUPS);
					} catch (Exception e) {
					}
				}
			}

			boolean found = false;
			if ((groups != null) && (groups.size() > 0)) {
				for (Map.Entry<String, String> e : groups.entrySet()) {
					if (e.getKey().compareTo(name) == 0) {
						found = true;
						break;
					}
				}
				if (!found) {
					// we need a unique name
					String id = createUniqueName(groups);
					String url = baseURL + "/rest/items/" + id; 
					logger.debug("Create group {} as {}.", id, name);
					executePut(url, RestJson.createNewGroupRequest(id, name));
					// update our cache
//					timeoutRemove(CACHE_GROUPS);
//					groups = null;
				}
			}
			groupsLock.unlock();
		}
	}

	@Override
	protected void timeoutExpired(int identifier) {
		switch (identifier) {
		case CACHE_GROUPS:
			groupsLock.lock();
			groups = null;
			groupsLock.unlock();
			break;
		}
	}
	
	private String createUniqueName(Map<String, String> names) {
		int count = 0;
		while (count < 100000) {
			boolean found = false;
			String id = UUID.randomUUID().toString();
			// replace what we cannot use
			id = id.replaceAll(":", "");
			id = id.replaceAll("-", "");
			for (Map.Entry<String, String> e : groups.entrySet()) {
				if (e.getValue().compareTo(id) == 0) {
					found = true;
					break;
				}
			}
			if (!found) {
				return id;
			}
		}
		return new String("notunique");
	}
}
