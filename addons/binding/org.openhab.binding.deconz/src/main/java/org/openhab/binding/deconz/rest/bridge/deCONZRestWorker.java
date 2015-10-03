package org.openhab.binding.deconz.rest.bridge;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.deconz.rest.RestHandler;
import org.openhab.binding.deconz.rest.RestResult;

public abstract class deCONZRestWorker extends RestHandler {

    private final static int STATE_INIT = 1;
	private final static int STATE_READ_CONFIGURATION = 2;
	private final static int STATE_BASIC_AUTHENTICATE = 3;
	private final static int STATE_UNLOCK_AUTHENTICATE = 4;
	private final static int STATE_READ_DEVICES= 5;
	private final static int STATE_UPDATE = 6;
	private final static int STATE_TOUCHLINK_INITIALIZE = 7;
	private final static int STATE_TOUCHLINK_UPDATE = 8;
	
	public final static int REST_INITIALIZING = 0;
	public final static int REST_ONLINE = 1;
	public final static int REST_COMMUNICATION_ERROR = 2;
	public final static int REST_CONFIGURATION_ERROR = 3;

    private ScheduledFuture<?> job = null;
    private String username = null;
    private String password = null;
    private int state = STATE_INIT;
    private int savedState = STATE_INIT;
    private int stateCount = 0;
    private int savedStateCount = 0;
    private boolean restart = false;
    private boolean touchlink = false;
    
    abstract protected RestResult authenticateBasic(String username, String password);
    abstract protected RestResult authenticateUnlocked();
    abstract protected RestResult getConfiguration();
    abstract protected RestResult getDevices();
    abstract protected boolean beginTouchlink();
    abstract protected boolean getTouchlinkStatus();
    abstract protected void endTouchlink();
    abstract protected void updateStatus(int status, String message);
    abstract public String getBaseURL();
    abstract public String getApiKey();
    abstract public void setApiKey(String key);

    public boolean isInitialized() {
    	return (state != STATE_INIT) ? true : false;
    }

    public void restart() {
    	// do not manipulate the state and stateCount right away but wait for the executing
    	// thread to reach a safe switching position.
    	restart = true;
    }
    
    public void startTouchlink() {
    	touchlink = true;
    }
    
    public void startWork(ScheduledExecutorService scheduler) {
    	if (job == null) {
	        Runnable runnable = new Runnable() {
	            @Override
	            public void run() {
	                try {
	                	doWork();
	                } catch (Exception e) {
	                }
	            }
	        };

	        restart = false;
	        job = scheduler.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
    	}
    }
    
    public void stopWork() {
    	if (job != null) {
    		job.cancel(true);
    		job = null;
    	}
    }
    
    public void setUsername(String name) {
    	username = name;
    }

    public void setPassword(String word) {
    	password = word;
    }
    
	private void doWork() {
		// apply requested state chnages
		switchState();
    	
    	// Wait for the next state change
    	if (savedStateCount > 0) {
    		savedStateCount--;
    	}
		if (stateCount > 0) {
			stateCount--;
			return;
		}
		
		switch (state) {
		case STATE_INIT:
			state = doInit();
			break;
		case STATE_BASIC_AUTHENTICATE:
			state = doBasicAuthentication();
			break;
		case STATE_UNLOCK_AUTHENTICATE:
			state = doUnlockAuthentication();
			break;
		case STATE_READ_CONFIGURATION:
			state = doGetConfiguration();
			break;
		case STATE_READ_DEVICES:
			state = doGetDevices();
			break;
		case STATE_UPDATE:
			state = doUpdate();
			break;
		case STATE_TOUCHLINK_INITIALIZE:
			state = doStartTouchlink();
			break;
		case STATE_TOUCHLINK_UPDATE:
			state = doUpdateTouchlink();
			break;
		default:
            updateStatus(REST_INITIALIZING, null);
			state = STATE_INIT;
			break;
		}
	}
	
	private int doInit() {
		// The first step is to authenticate against the bridge. This depends on the configuration data given.
		// If an API key is available we try to use this without authenticating, that is, getting a new API key.
		String s1 = getApiKey();
		if ((s1 != null) && (s1.length() > 0)) {
			return STATE_READ_CONFIGURATION;
		}

		// If no API key is given we have two options to authenticate. With a password and user name we can
		// can authenticate without user interaction. Lets try this first.
		if ((username != null) && (username.length() > 0) && (password != null) && (password.length() > 0)) {
			return STATE_BASIC_AUTHENTICATE;
		}
		// Now we need the user interaction, that is, the bridge needs to be unlocked.
		return STATE_UNLOCK_AUTHENTICATE;
	}
	
	private int doBasicAuthentication() {
		RestResult error = authenticateBasic(username, password);
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// update state to ONLINE
			updateStatus(REST_ONLINE, null);
			return STATE_READ_CONFIGURATION;
		case RestResult.REST_CONNECT_ERROR:
			// update state to OFFLINE
            updateStatus(REST_COMMUNICATION_ERROR, "The deCONZ bridge at " + getBaseURL() + " is not reachable");
            stateCount = 60;
            break;
		case RestResult.REST_AUTHENTICATION_ERROR:
			// update state to OFFLINE
            updateStatus(REST_CONFIGURATION_ERROR, "Authentication failed. Please check the provided user name and password.");
            stateCount = 10;
            break;
        default:
            stateCount = 60;
        	break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
		return STATE_INIT;
	}
	
	private int doUnlockAuthentication() {
		RestResult error = authenticateUnlocked();
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// update state to ONLINE
			updateStatus(REST_ONLINE, null);
			return STATE_READ_CONFIGURATION;
		case RestResult.REST_CONNECT_ERROR:
			// update state to OFFLINE
            updateStatus(REST_COMMUNICATION_ERROR, "The deCONZ bridge at " + getBaseURL() + " is not reachable");
            stateCount = 60;
            break;
		case RestResult.REST_AUTHENTICATION_ERROR:
			// update state to OFFLINE
            updateStatus(REST_CONFIGURATION_ERROR, "Authentication failed. " + 
            		"Please unlock the bridge to allow authentication. To unlock the bridge go to " +
            		"system->unlock. This will unlock the bridge for 60 seconds. It will be closed " + 
            		"automatically after the time is over.");
            stateCount = 10;
            break;
        default:
            stateCount = 60;
        	break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
		return STATE_INIT;
	}
	
	private int doGetConfiguration() {
		RestResult error = getConfiguration();
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// update state to ONLINE
			updateStatus(REST_ONLINE, null);
			return STATE_READ_DEVICES;
		case RestResult.REST_RESPONSE_ERROR:
			// well, we have a wrong response from the rest interface. Stay here and retry
			// after a while
			stateCount = 10;
			return STATE_READ_CONFIGURATION;
		case RestResult.REST_CONNECT_ERROR:
			// update state to OFFLINE
            updateStatus(REST_COMMUNICATION_ERROR, "The deCONZ bridge at " + getBaseURL() + " is not reachable");
			stateCount = 60;
            break;
		case RestResult.REST_AUTHENTICATION_ERROR:
			// update state to OFFLINE
            updateStatus(REST_CONFIGURATION_ERROR, "The deCONZ bridge at " + getBaseURL() + " is reachable but " + 
            		"the binding is not authorized to access the API. A reaon might be that the given API key is " + 
            		"not or no longer valid. Try removing the API key in order to aquire a new one.");
            // as the API key is obviously wrong we clear the key to acquire a new one
            setApiKey("");
            stateCount = 10;
            break;
        default:
            stateCount = 60;
        	break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
		return STATE_INIT;
	}
	
	private int doGetDevices() {
		RestResult error = getDevices();
		switch (error.getResult()) {
		case RestResult.REST_OK:
			// run an update after some time
			stateCount = 60;
			return STATE_UPDATE;
		case RestResult.REST_RESPONSE_ERROR:
			// well, we have a wrong response from the rest interface. Stay here and retry
			// after a while
			stateCount = 10;
			return STATE_READ_DEVICES;
		default:
			// all other errors are considered connection problems which
			// lead back to the initial state which than in turns updates
			// the thing status. There is no need to do it here
            break;
		}
        // in case of connection, authentication or general errors we want to wait a bit 
		// until the next round
        stateCount = 60;
		return STATE_INIT;
	}
	
	private int doUpdate() {
		return STATE_READ_DEVICES;
	}

	private int doStartTouchlink() {
		if (beginTouchlink()) {
			// we poll the touchlink status every 3 seconds
			stateCount = 3;
			return STATE_TOUCHLINK_UPDATE;
		}
		// starting touchlink failed - no need to wait
		stateCount = savedStateCount;
		return savedState;
	}

	private int doUpdateTouchlink() {
		if (getTouchlinkStatus()) {
			stateCount = 3;
			return state;
		}
		endTouchlink();
		stateCount = savedStateCount;
		return savedState;
	}
	
	private void switchState() {
		if (restart) {
    		if ((state == STATE_TOUCHLINK_INITIALIZE) || (state == STATE_TOUCHLINK_UPDATE)) {
    			// we do not apply restarts while touchlinking
    		} else {
		        state = STATE_INIT;
		        stateCount = 0;
    	    	restart = false;
    	    	if (touchlink) {
        			// it doesn't make sense to touchlink directly after a restart
        			endTouchlink();
    	    		touchlink = false;
    	    		return;
    	    	}
    		}
    	}
    	
    	if (touchlink) {
    		if ((state == STATE_TOUCHLINK_INITIALIZE) || (state == STATE_TOUCHLINK_UPDATE)) {
    			// just clear the request
    		} else if ((state == STATE_UPDATE) || (state == STATE_READ_DEVICES)) {
    			// switch immediately to touchlink
    			savedState = state;
    			savedStateCount = stateCount;
    			state = STATE_TOUCHLINK_INITIALIZE;
    			stateCount = 0;
    		} else {
    			// it doesn't make sense to touchlink
    			endTouchlink();
    		}
			touchlink = false;
    	}
	}
}
