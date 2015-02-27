/**
 * 
 */
package org.appcelerator.titanium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Bundle;

import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.OnWindowFocusChangedEvent;

import android.app.Activity;

/**
 * Represents that the current application's state. This class give android the ability to simulate the similar app state as ios.
 * @author wei.ding
 */
public class ApplicationState implements OnLifecycleEvent, OnWindowFocusChangedEvent {

	public static final ApplicationState INSTANCE = new ApplicationState();

	public static enum State {
		INACTIVE,
		ACTIVE,
		BACKGROUND,
		SUSPENDED
	}

	public static interface StateListener {

		public void onStateChanged(State newState, State oldState);

	}

	private final Map<Long, State> activityStates;

	private final AtomicReference<State> currentState;

	private final CopyOnWriteArrayList<StateListener> listeners = new CopyOnWriteArrayList<StateListener>();

	private ReentrantLock statesLock = new ReentrantLock();

	public ApplicationState() {
		this(State.SUSPENDED);
	}

	/**
	 * 
	 */
	public ApplicationState(State state) {
		this.activityStates = new ConcurrentHashMap<Long, State>();
		this.currentState = new AtomicReference<State>(state);
	}

	/*
	 * (non-Javadoc) 
	 * @see org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent#onCreate(android.app.Activity)
	 */
	public void onCreate(Activity activity, Bundle savedInstanceState) {
		this.statesLock.lock();
		try {
			long activityHashCode = activity.hashCode();
			this.activityStates.put(activityHashCode, State.INACTIVE);
			this.updateCurrentState();
		} finally {
			this.statesLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent#onStart(android.app.Activity)
	 */
	public void onStart(Activity activity) {
		this.statesLock.lock();
		try {
			long activityHashCode = activity.hashCode();
			this.activityStates.put(activityHashCode, State.INACTIVE);
			this.updateCurrentState();
		} finally {
			this.statesLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent#onResume(android.app.Activity)
	 */
	public void onResume(Activity activity) {
		this.statesLock.lock();
		try {
			long activityHashCode = activity.hashCode();
			this.activityStates.put(activityHashCode, State.ACTIVE);
			this.updateCurrentState();
		} finally {
			this.statesLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent#onPause(android.app.Activity)
	 */
	public void onPause(Activity activity) {
		this.statesLock.lock();
		try {
			long activityHashCode = activity.hashCode();
			this.activityStates.put(activityHashCode, State.ACTIVE);
			this.updateCurrentState();
		} finally {
			this.statesLock.unlock();
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent#onStop(android.app.Activity)
	 */
	public void onStop(Activity activity) {
		this.statesLock.lock();
		try {
			long activityHashCode = activity.hashCode();
			this.activityStates.put(activityHashCode, State.BACKGROUND);
			this.updateCurrentState();
		} finally {
			this.statesLock.unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent#onDestroy(android.app.Activity)
	 */
	public void onDestroy(Activity activity) {
		this.statesLock.lock();
		try {
			long activityHashCode = activity.hashCode();
			this.activityStates.put(activityHashCode, State.SUSPENDED);
			this.updateCurrentState();
		} finally {
			this.statesLock.unlock();
		}
	}

	private void updateCurrentState() {
		if (this.activityStates.isEmpty()) {
			this.currentState.set(State.SUSPENDED);
		}
		State previousState = this.currentState.get();
		Collection<State> states = this.activityStates.values();
		if (states.contains(State.ACTIVE)) {
			this.currentState.set(State.ACTIVE);
		} else if (states.contains(State.INACTIVE)) {
			this.currentState.set(State.INACTIVE);
		} else if (states.contains(State.BACKGROUND)) {
			this.currentState.set(State.BACKGROUND);
		} else if (states.contains(State.SUSPENDED)) {
			this.currentState.set(State.SUSPENDED);
		}
		State currentState = this.currentState.get();
		if (previousState != currentState) {
			for (StateListener listener : this.listeners) {
				listener.onStateChanged(currentState, previousState);
			}
		}
		for (Map.Entry<Long, State> entry : this.activityStates.entrySet()) {
			if (entry.getValue() == State.SUSPENDED) {
				this.activityStates.remove(entry.getKey());
			}
		}
	}

	public State getCurrentState() {
		return this.currentState.get();
	}

	public boolean addStateListener(StateListener listener) {
		if (listener == null) {
			return false;
		}
		return this.listeners.add(listener);
	}

	/**
	 * @param listener
	 * @return
	 */
	public boolean removeStateListener(StateListener listener) {
		if (listener == null) {
			return true;
		}

		return this.listeners.remove(listener);
	}

	/**
	 * Gets state listeners.
	 * @return
	 */
	public List<StateListener> getStateListeners() {
		if (this.listeners.isEmpty()) {
			return Collections.<StateListener> emptyList();
		}
		List<StateListener> list = new ArrayList<StateListener>(this.listeners.size());
		Collections.copy(list, this.listeners);

		return list;
	}

	/*
	 * (non-Javadoc)
	 * @see org.appcelerator.titanium.TiLifecycle.OnWindowFocusChangedEvent#onWindowFocusChanged(boolean)
	 */
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO
	}

	/**
	 * Gets the state change event name from {@link TiC}.
	 * @param newState
	 * @param oldState
	 * @return
	 */
	public static String getStateChangeEventName(State newState, State oldState) {
		switch (newState) {
			case ACTIVE:
				return TiC.EVENT_RESUMED;
			case BACKGROUND:
				return TiC.EVENT_PAUSED;
			case INACTIVE:
				return oldState == State.ACTIVE ? TiC.EVENT_PAUSE : TiC.EVENT_RESUME;
			case SUSPENDED:
				return TiC.EVENT_DESTROY;
			default:
				return null;
		}
	}
}
