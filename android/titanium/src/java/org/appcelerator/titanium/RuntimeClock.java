/**
 * 
 */
package org.appcelerator.titanium;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;

/**
 * @author wei.ding
 */
public class RuntimeClock {

	private ConcurrentHashMap<String, Long> tagMarkedTimeMap;

	public static final String APP_START = "__appStart";

	/**
	 * 
	 */
	public RuntimeClock() {
		long appStart = SystemClock.elapsedRealtime();
		this.tagMarkedTimeMap = new ConcurrentHashMap<String, Long>();
		this.tagMarkedTimeMap.put(APP_START, appStart);
	}

	/**
	 * Gets elapsed real time in millisecond.
	 * @return
	 */
	public long getElapsedRealTime(String tag) {
		if (tag == null) {
			throw new NullPointerException();
		}
		long now = SystemClock.elapsedRealtime();
		Long time = this.tagMarkedTimeMap.get(tag);
		return time == null ? -1 : now - time.longValue();
	}

	/**
	 * Gets elapsed real time in millisecond from the application start up.
	 * @return
	 */
	public long getElapsedRealTimeAtStartUp() {
		return this.getElapsedRealTime(APP_START);
	}

	/**
	 * Gets elapsed real time in the given unit.
	 * @param unit
	 * @return
	 * @exception NullPointerException if the unit is null.
	 */
	public long getElapsedRealTime(String tag, TimeUnit unit) {
		long time = getElapsedRealTime(tag);
		return time < 0 ? -1 : unit.convert(time, TimeUnit.MILLISECONDS);
	}

	/**
	 * Mark current system time as the start time for this tag.
	 * @param tag the tag string, which can not be {@link #APP_START}.
	 * @return true if tag is not {@link #APP_START}.
	 * @exception NullPointerException if tag is null
	 */
	public boolean markTag(String tag) {
		if (tag == null) {
			throw new NullPointerException();
		}
		if (APP_START.equals(tag)) {
			return false;
		}
		long time = SystemClock.elapsedRealtime();
		this.tagMarkedTimeMap.put(tag, time);
		return true;
	}
}
