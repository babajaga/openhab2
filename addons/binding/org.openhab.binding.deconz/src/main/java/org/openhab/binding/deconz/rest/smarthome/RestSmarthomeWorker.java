package org.openhab.binding.deconz.rest.smarthome;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openhab.binding.deconz.rest.RestHandler;

public abstract class RestSmarthomeWorker extends RestHandler {
	
	protected class CacheTimeout {
		
		private int ticks = 0;
		private int id = 0;
		
		public CacheTimeout(int seconds, int identifier) {
			ticks = seconds;
			id = identifier;
		}
		
		public boolean tick() {
			if (ticks > 0) {
				ticks--;
			}
			if (ticks > 0) {
				return false;
			}
			return true;
		}

		public int getIdentifier() {
			return id;
		}
	};

    private ScheduledFuture<?> job = null;
	private final Lock lock = new ReentrantLock();
    private List<CacheTimeout> timeouts = new ArrayList<CacheTimeout>();
    
    protected abstract void timeoutExpired(int identifier);
    
    public void timeoutAdd(int seconds, int identifier) {
    	lock.lock();
    	timeouts.add(new CacheTimeout(seconds, identifier));
    	lock.unlock();
    }

    public void timeoutRemove(int identifier) {
    	lock.lock();
		for (Iterator<CacheTimeout> i = timeouts.iterator(); i.hasNext(); ) {
            CacheTimeout t = i.next();
			if (t.getIdentifier() == identifier) {
				i.remove();
			}
        }		
    	lock.unlock();
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
	
	        job = scheduler.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
    	}
    }
    
    public void stopWork() {
    	if (job != null) {
    		job.cancel(true);
    		job = null;
    	}
    }
    
	private void doWork() {
		lock.lock();
		List<CacheTimeout> expired = new ArrayList<CacheTimeout>();		
		for (Iterator<CacheTimeout> i = timeouts.iterator(); i.hasNext(); ) {
            CacheTimeout t = i.next();
			if (t.tick()) {
				expired.add(t);
				i.remove();
			}
        }
		lock.unlock();
		for (Iterator<CacheTimeout> i = expired.iterator(); i.hasNext(); ) {
			timeoutExpired(i.next().getIdentifier());
		}
	}
}
