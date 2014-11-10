/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.TiLifecycle.OnWindowFocusChangedEvent;
import org.appcelerator.titanium.TiLifecycle.interceptOnBackPressedEvent;
import org.appcelerator.titanium.analytics.TiAnalyticsEventFactory;
import org.appcelerator.titanium.proxy.ActionBarProxy;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.IntentProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;
import org.appcelerator.titanium.util.TiActivitySupportHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiMenuSupport;
import org.appcelerator.titanium.util.TiPlatformHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiWeakList;
import org.appcelerator.titanium.view.KeyboardStateMonitor;
import org.appcelerator.titanium.view.OnKeyboardVisibilityChangeListener;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * The base class for all non tab Titanium activities. To learn more about Activities, see the
 * <a href="http://developer.android.com/reference/android/app/Activity.html">Android Activity documentation</a>.
 */
public abstract class TiBaseActivity extends FragmentActivity implements TiActivitySupport/* , ITiWindowHandler */{

	private static final String TAG = "TiBaseActivity";

	private static OrientationChangedListener orientationChangedListener = null;

	private boolean onDestroyFired = false;
	private int originalOrientationMode = -1;
	private boolean inForeground = false; // Indicates whether this activity is in foreground or not.
	private TiWeakList<OnLifecycleEvent> lifecycleListeners = new TiWeakList<OnLifecycleEvent>();
	private TiWeakList<OnWindowFocusChangedEvent> windowFocusChangedListeners = new TiWeakList<OnWindowFocusChangedEvent>();
	private TiWeakList<interceptOnBackPressedEvent> interceptOnBackPressedListeners = new TiWeakList<interceptOnBackPressedEvent>();

	protected View layout;
	protected TiActivitySupportHelper supportHelper;
	protected int supportHelperId = -1;
	protected TiWindowProxy window;
	protected TiViewProxy view;
	protected ActivityProxy activityProxy;
	protected TiWeakList<ConfigurationChangedListener> configChangedListeners = new TiWeakList<ConfigurationChangedListener>();
	protected int orientationDegrees;
	protected TiMenuSupport menuHelper;
	protected Messenger messenger;
	protected int msgActivityCreatedId = -1;
	protected int msgId = -1;
	protected static int previousOrientation = -1;
	// Storing the activity's dialogs and their persistence
	private CopyOnWriteArrayList<DialogWrapper> dialogs = new CopyOnWriteArrayList<DialogWrapper>();
	private Stack<TiWindowProxy> windowStack = new Stack<TiWindowProxy>();

	private ReentrantLock lifecycleListenersLock = new ReentrantLock();

	private KeyboardStateMonitor mKeyboardStateMonitor;

	public TiWindowProxy lwWindow;
	public boolean isResumed = false;

	/**
	 * 
	 */
	public TiBaseActivity() {
		super();
		this.addOnLifecycleEventListener(ApplicationState.INSTANCE);
	}

	public class DialogWrapper {

		boolean isPersistent;
		AlertDialog dialog;
		WeakReference<TiBaseActivity> dialogActivity;

		public DialogWrapper(AlertDialog d, boolean persistent, WeakReference<TiBaseActivity> activity) {
			isPersistent = persistent;
			dialog = d;
			dialogActivity = activity;
		}

		public TiBaseActivity getActivity() {
			if (dialogActivity == null) {
				return null;
			} else {
				return dialogActivity.get();
			}
		}

		public void setActivity(WeakReference<TiBaseActivity> da) {
			dialogActivity = da;
		}

		public AlertDialog getDialog() {
			return dialog;
		}

		public void setDialog(AlertDialog d) {
			dialog = d;
		}

		public void release() {
			dialog = null;
			dialogActivity = null;
		}

		public boolean getPersistent() {
			return isPersistent;
		}

		public void setPersistent(boolean p) {
			isPersistent = p;
		}
	}

	public void addWindowToStack(TiWindowProxy proxy) {
		if (windowStack.contains(proxy)) {
			Log.e(TAG, "Window already exists in stack", Log.DEBUG_MODE);
			return;
		}
		boolean isEmpty = windowStack.empty();
		if (!isEmpty) {
			windowStack.peek().onWindowFocusChange(false);
		}
		windowStack.add(proxy);
		if (!isEmpty) {
			proxy.onWindowFocusChange(true);
		}
	}

	public void removeWindowFromStack(TiWindowProxy proxy) {
		proxy.onWindowFocusChange(false);

		boolean isTopWindow = ((!windowStack.isEmpty()) && (windowStack.peek() == proxy)) ? true : false;
		windowStack.remove(proxy);

		// Fire focus only if activity is not paused and the removed window was topWindow
		if (!windowStack.empty() && isResumed && isTopWindow) {
			TiWindowProxy nextWindow = windowStack.peek();
			nextWindow.onWindowFocusChange(true);
		}
	}

	/**
	 * Returns the window at the top of the stack.
	 * @return the top window or null if the stack is empty.
	 */
	public TiWindowProxy topWindowOnStack() {
		return (windowStack.isEmpty()) ? null : windowStack.peek();
	}

	// could use a normal ConfigurationChangedListener but since only orientation changes are
	// forwarded, create a separate interface in order to limit scope and maintain clarity
	public static interface OrientationChangedListener {

		public void onOrientationChanged(int configOrientationMode);
	}

	public static void registerOrientationListener(OrientationChangedListener listener) {
		orientationChangedListener = listener;
	}

	public static void deregisterOrientationListener() {
		orientationChangedListener = null;
	}

	public static interface ConfigurationChangedListener {

		public void onConfigurationChanged(TiBaseActivity activity, Configuration newConfig);
	}

	/**
	 * @return the instance of TiApplication.
	 */
	public TiApplication getTiApp() {
		return (TiApplication) getApplication();
	}

	/**
	 * @return the window proxy associated with this activity.
	 */
	public TiWindowProxy getWindowProxy() {
		return this.window;
	}

	/**
	 * Sets the window proxy.
	 * @param proxy
	 */
	public void setWindowProxy(TiWindowProxy proxy) {
		this.window = proxy;
		updateTitle();
	}

	/**
	 * Sets the proxy for our layout (used for post layout event)
	 * @param proxy
	 */
	public void setLayoutProxy(TiViewProxy proxy) {
		if (layout instanceof TiCompositeLayout) {
			((TiCompositeLayout) layout).setProxy(proxy);
		}
	}

	/**
	 * Sets the view proxy.
	 * @param proxy
	 */
	public void setViewProxy(TiViewProxy proxy) {
		this.view = proxy;
	}

	/**
	 * @return activity proxy associated with this activity.
	 */
	public ActivityProxy getActivityProxy() {
		return activityProxy;
	}

	public void addDialog(DialogWrapper d) {
		if (!dialogs.contains(d)) {
			dialogs.add(d);
		}
	}

	public void removeDialog(Dialog d) {
		for (int i = 0; i < dialogs.size(); i++) {
			DialogWrapper p = dialogs.get(i);
			if (p.getDialog().equals(d)) {
				p.release();
				dialogs.remove(i);
				return;
			}
		}
	}

	public void setActivityProxy(ActivityProxy proxy) {
		this.activityProxy = proxy;
	}

	/**
	 * @return the activity's current layout.
	 */
	public View getLayout() {
		return layout;
	}

	public void setLayout(View layout) {
		this.layout = layout;
	}

	public void addConfigurationChangedListener(ConfigurationChangedListener listener) {
		configChangedListeners.add(new WeakReference<ConfigurationChangedListener>(listener));
	}

	public void removeConfigurationChangedListener(ConfigurationChangedListener listener) {
		configChangedListeners.remove(listener);
	}

	public void registerOrientationChangedListener(OrientationChangedListener listener) {
		orientationChangedListener = listener;
	}

	public void deregisterOrientationChangedListener() {
		orientationChangedListener = null;
	}

	protected boolean getIntentBoolean(String property, boolean defaultValue) {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(property)) {
				return intent.getBooleanExtra(property, defaultValue);
			}
		}

		return defaultValue;
	}

	protected int getIntentInt(String property, int defaultValue) {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(property)) {
				return intent.getIntExtra(property, defaultValue);
			}
		}

		return defaultValue;
	}

	protected String getIntentString(String property, String defaultValue) {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(property)) {
				return intent.getStringExtra(property);
			}
		}

		return defaultValue;
	}

	protected void updateTitle() {
		if (window == null)
			return;

		if (window.hasProperty(TiC.PROPERTY_TITLE)) {
			String oldTitle = (String) getTitle();
			String newTitle = TiConvert.toString(window.getProperty(TiC.PROPERTY_TITLE));

			if (oldTitle == null) {
				oldTitle = "";
			}

			if (newTitle == null) {
				newTitle = "";
			}

			if (!newTitle.equals(oldTitle)) {
				final String fnewTitle = newTitle;
				runOnUiThread(new Runnable() {

					public void run() {
						setTitle(fnewTitle);
					}
				});
			}
		}
	}

	// Subclasses can override to provide a custom layout
	protected View createLayout() {
		LayoutArrangement arrangement = LayoutArrangement.DEFAULT;

		String layoutFromIntent = getIntentString(TiC.INTENT_PROPERTY_LAYOUT, "");
		if (layoutFromIntent.equals(TiC.LAYOUT_HORIZONTAL)) {
			arrangement = LayoutArrangement.HORIZONTAL;

		} else if (layoutFromIntent.equals(TiC.LAYOUT_VERTICAL)) {
			arrangement = LayoutArrangement.VERTICAL;
		}

		// set to null for now, this will get set correctly in setWindowProxy()
		return new TiCompositeLayout(this, arrangement, null);
	}

	protected void setFullscreen(boolean fullscreen) {
		if (fullscreen) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}

	protected void setNavBarHidden(boolean hidden) {
		if (!hidden) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				// Do not enable these features on Honeycomb or later since it will break the action bar.
				this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
				this.requestWindowFeature(Window.FEATURE_RIGHT_ICON);
			}

			this.requestWindowFeature(Window.FEATURE_PROGRESS);
			this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		} else {
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
	}

	// Subclasses can override to handle post-creation (but pre-message fire) logic
	protected void windowCreated() {
		boolean fullscreen = getIntentBoolean(TiC.PROPERTY_FULLSCREEN, false);
		boolean navBarHidden = getIntentBoolean(TiC.PROPERTY_NAV_BAR_HIDDEN, false);
		boolean modal = getIntentBoolean(TiC.PROPERTY_MODAL, false);
		int softInputMode = getIntentInt(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE, -1);
		boolean hasSoftInputMode = softInputMode != -1;

		setFullscreen(fullscreen);
		setNavBarHidden(navBarHidden);

		if (modal) {
			if (Build.VERSION.SDK_INT < TiC.API_LEVEL_ICE_CREAM_SANDWICH) {
				// This flag is deprecated in API 14. On ICS, the background is not blurred but straight black.
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			}
		}

		if (hasSoftInputMode) {
			Log.i(TAG, "windowSoftInputMode: " + softInputMode, Log.DEBUG_MODE);
			getWindow().setSoftInputMode(softInputMode);
		} else {
			this.mKeyboardStateMonitor = new KeyboardStateMonitor(this, new OnKeyboardVisibilityChangeListener() {

				public void onKeyboardVisibilityChange(boolean visible) {
					TiWindowProxy topWindow = topWindowOnStack();
					if (topWindow != null) {
						KrollDict options = new KrollDict();
						options.put(TiC.PROPERTY_VISIBLE, visible);
						topWindow.fireEvent(TiC.EVENT_KEYBOARD, options);
					}

				}

			});
		}

		boolean useActivityWindow = getIntentBoolean(TiC.INTENT_PROPERTY_USE_ACTIVITY_WINDOW, false);
		if (useActivityWindow) {
			int windowId = getIntentInt(TiC.INTENT_PROPERTY_WINDOW_ID, -1);
			TiActivityWindows.windowCreated(this, windowId);
		}
	}

	@Override
	/**
	 * When the activity is created, this method adds it to the activity stack and
	 * fires a javascript 'create' event.
	 * @param savedInstanceState Bundle of saved data.
	 */
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "Activity " + this + " onCreate", Log.DEBUG_MODE);

		inForeground = true;
		TiApplication tiApp = getTiApp();

		if (tiApp.isRestartPending()) {
			super.onCreate(savedInstanceState);
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		// If all the activities has been killed and the runtime has been disposed, we cannot recover one
		// specific activity because the info of the top-most view proxy has been lost (TiActivityWindows.dispose()).
		// In this case, we have to restart the app.
		if (TiBaseActivity.isUnsupportedReLaunch(this, savedInstanceState)) {
			Log.w(TAG, "Runtime has been disposed. Finishing.");
			super.onCreate(savedInstanceState);
			tiApp.scheduleRestart(250);
			finish();
			return;
		}

		TiApplication.addToActivityStack(this);

		// create the activity proxy here so that it is accessible from the activity in all cases
		activityProxy = new ActivityProxy(this);

		// Increment the reference count so we correctly clean up when all of our activities have been destroyed
		KrollRuntime.incrementActivityRefCount();

		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(TiC.INTENT_PROPERTY_MESSENGER)) {
				messenger = (Messenger) intent.getParcelableExtra(TiC.INTENT_PROPERTY_MESSENGER);
				msgActivityCreatedId = intent.getIntExtra(TiC.INTENT_PROPERTY_MSG_ACTIVITY_CREATED_ID, -1);
				msgId = intent.getIntExtra(TiC.INTENT_PROPERTY_MSG_ID, -1);
			}

			if (intent.hasExtra(TiC.PROPERTY_WINDOW_PIXEL_FORMAT)) {
				getWindow().setFormat(intent.getIntExtra(TiC.PROPERTY_WINDOW_PIXEL_FORMAT, PixelFormat.UNKNOWN));
			}
		}

		// Doing this on every create in case the activity is externally created.
		TiPlatformHelper.intializeDisplayMetrics(this);

		if (layout == null) {
			layout = createLayout();
		}
		if (intent != null && intent.hasExtra(TiC.PROPERTY_KEEP_SCREEN_ON)) {
			layout.setKeepScreenOn(intent.getBooleanExtra(TiC.PROPERTY_KEEP_SCREEN_ON, layout.getKeepScreenOn()));
		}

		super.onCreate(savedInstanceState);

		// we only want to set the current activity for good in the resume state but we need it right now.
		// save off the existing current activity, set ourselves to be the new current activity temporarily
		// so we don't run into problems when we give the proxy the event
		Activity tempCurrentActivity = tiApp.getCurrentActivity();
		tiApp.setCurrentActivity(this, this);

		windowCreated();

		if (this.activityProxy != null) {
			// Fire the sync event with a timeout, so the main thread won't be blocked too long to get an ANR. (TIMOB-13253)
			long syncStartTimestamp = SystemClock.elapsedRealtime();

			this.activityProxy.fireSyncEvent(TiC.EVENT_CREATE, null);

			String message = MessageFormat.format("Activity Create Event has been executed in {0}ms.", SystemClock.elapsedRealtime() - syncStartTimestamp);
			Log.d(TAG, message, Log.DEBUG_MODE);
		}

		// set the current activity back to what it was originally
		tiApp.setCurrentActivity(this, tempCurrentActivity);

		setContentView(layout);

		sendMessage(msgActivityCreatedId);
		// for backwards compatibility
		sendMessage(msgId);

		// store off the original orientation for the activity set in the AndroidManifest.xml
		// for later use
		originalOrientationMode = getRequestedOrientation();

		if (window != null) {
			window.onWindowActivityCreated();
		}
	}

	public int getOriginalOrientationMode() {
		return originalOrientationMode;
	}

	public boolean isInForeground() {
		return inForeground;
	}

	protected void sendMessage(final int msgId) {
		if (messenger == null || msgId == -1) {
			return;
		}

		// fire an async message on this thread's queue
		// so we don't block onCreate() from returning
		TiMessenger.postOnMain(new Runnable() {

			public void run() {
				handleSendMessage(msgId);
			}
		});
	}

	protected void handleSendMessage(int messageId) {
		try {
			Message message = TiMessenger.getMainMessenger().getHandler().obtainMessage(messageId, this);
			messenger.send(message);

		} catch (RemoteException e) {
			Log.e(TAG, "Unable to message creator. finishing.", e);
			finish();

		} catch (RuntimeException e) {
			Log.e(TAG, "Unable to message creator. finishing.", e);
			finish();
		}
	}

	protected TiActivitySupportHelper getSupportHelper() {
		if (supportHelper == null) {
			this.supportHelper = new TiActivitySupportHelper(this);
			// Register the supportHelper so we can get it back when the activity is recovered from force-quitting.
			supportHelperId = TiActivitySupportHelpers.addSupportHelper(supportHelper);
		}

		return supportHelper;
	}

	// Activity Support
	public int getUniqueResultCode() {
		return getSupportHelper().getUniqueResultCode();
	}

	/**
	 * See TiActivitySupport.launchActivityForResult for more details.
	 */
	public void launchActivityForResult(Intent intent, int code, TiActivityResultHandler resultHandler) {
		getSupportHelper().launchActivityForResult(intent, code, resultHandler);
	}

	/**
	 * See TiActivitySupport.launchIntentSenderForResult for more details.
	 */
	public void launchIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
			Bundle options, TiActivityResultHandler resultHandler) {
		getSupportHelper().launchIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options, resultHandler);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		getSupportHelper().onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBackPressed() {
		synchronized (interceptOnBackPressedListeners.synchronizedList()) {
			for (interceptOnBackPressedEvent listener : interceptOnBackPressedListeners.nonNull()) {
				try {
					if (listener.interceptOnBackPressed()) {
						return;
					}

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching interceptOnBackPressed event: " + t.getMessage(), t);
				}
			}
		}

		TiWindowProxy topWindow = topWindowOnStack();

		// Prevent default Android behavior for "back" press
		// if the top window has a listener to handle the event.
		if (topWindow != null && topWindow.hasListeners(TiC.EVENT_ANDROID_BACK)) {
			topWindow.fireEvent(TiC.EVENT_ANDROID_BACK, null);

		} else {
			// If event is not handled by any listeners allow default behavior.
			super.onBackPressed();
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		boolean handled = false;

		TiViewProxy window;
		if (this.window != null) {
			window = this.window;
		} else {
			window = this.view;
		}

		if (window == null) {
			return super.dispatchKeyEvent(event);
		}

		switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK: {

				if (event.getAction() == KeyEvent.ACTION_UP) {
					String backEvent = "android:back";
					KrollProxy proxy = null;
					// android:back could be fired from a tabGroup window (activityProxy)
					// or hw window (window).This event is added specifically to the activity
					// proxy of a tab group in window.js
					if (activityProxy.hasListeners(backEvent)) {
						proxy = activityProxy;
					} else if (window.hasListeners(backEvent)) {
						proxy = window;
					}

					if (proxy != null) {
						proxy.fireEvent(backEvent, null);
						handled = true;
					}

				}
				break;
			}
			case KeyEvent.KEYCODE_CAMERA: {
				if (window.hasListeners(TiC.EVENT_ANDROID_CAMERA)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_CAMERA, null);
					}
					handled = true;
				}
				// TODO: Deprecate old event
				if (window.hasListeners("android:camera")) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent("android:camera", null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_FOCUS: {
				if (window.hasListeners(TiC.EVENT_ANDROID_FOCUS)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_FOCUS, null);
					}
					handled = true;
				}
				// TODO: Deprecate old event
				if (window.hasListeners("android:focus")) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent("android:focus", null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_SEARCH: {
				if (window.hasListeners(TiC.EVENT_ANDROID_SEARCH)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_SEARCH, null);
					}
					handled = true;
				}
				// TODO: Deprecate old event
				if (window.hasListeners("android:search")) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent("android:search", null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_VOLUME_UP: {
				if (window.hasListeners(TiC.EVENT_ANDROID_VOLUP)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_VOLUP, null);
					}
					handled = true;
				}
				// TODO: Deprecate old event
				if (window.hasListeners("android:volup")) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent("android:volup", null);
					}
					handled = true;
				}

				break;
			}
			case KeyEvent.KEYCODE_VOLUME_DOWN: {
				if (window.hasListeners(TiC.EVENT_ANDROID_VOLDOWN)) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent(TiC.EVENT_ANDROID_VOLDOWN, null);
					}
					handled = true;
				}
				// TODO: Deprecate old event
				if (window.hasListeners("android:voldown")) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						window.fireEvent("android:voldown", null);
					}
					handled = true;
				}

				break;
			}
		}

		if (!handled) {
			handled = super.dispatchKeyEvent(event);
		}

		return handled;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// If targetSdkVersion is set to 11+, Android will invoke this function
		// to initialize the menu (since it's part of the action bar). Due
		// to the fix for Android bug 2373, activityProxy won't be initialized b/c the
		// activity is expected to restart, so we will ignore it.
		if (activityProxy == null) {
			return false;
		}

		if (menuHelper == null) {
			menuHelper = new TiMenuSupport(activityProxy);
		}

		return menuHelper.onCreateOptionsMenu(super.onCreateOptionsMenu(menu), menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (activityProxy != null) {
					ActionBarProxy actionBarProxy = activityProxy.getActionBar();
					if (actionBarProxy != null) {
						KrollFunction onHomeIconItemSelected = (KrollFunction) actionBarProxy.getProperty(TiC.PROPERTY_ON_HOME_ICON_ITEM_SELECTED);
						KrollDict event = new KrollDict();
						event.put(TiC.EVENT_PROPERTY_SOURCE, actionBarProxy);
						if (onHomeIconItemSelected != null) {
							onHomeIconItemSelected.call(activityProxy.getKrollObject(), new Object[] { event });
						}
					}
				}
				return true;
			default:
				return menuHelper.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return menuHelper.onPrepareOptionsMenu(super.onPrepareOptionsMenu(menu), menu);
	}

	public static void callOrientationChangedListener(Configuration newConfig) {
		if (orientationChangedListener != null && previousOrientation != newConfig.orientation) {
			previousOrientation = newConfig.orientation;
			orientationChangedListener.onOrientationChanged(newConfig.orientation);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		for (WeakReference<ConfigurationChangedListener> listener : configChangedListeners) {
			if (listener.get() != null) {
				listener.get().onConfigurationChanged(this, newConfig);
			}
		}

		callOrientationChangedListener(newConfig);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		Log.d(TAG, "Activity " + this + " onNewIntent", Log.DEBUG_MODE);

		if (activityProxy != null) {
			IntentProxy ip = new IntentProxy(intent);
			KrollDict data = new KrollDict();
			data.put(TiC.PROPERTY_INTENT, ip);
			activityProxy.fireSyncEvent(TiC.EVENT_NEW_INTENT, data);
			// TODO: Deprecate old event
			activityProxy.fireSyncEvent("newIntent", data);
		}
	}

	public void addOnLifecycleEventListener(OnLifecycleEvent listener) {
		lifecycleListeners.add(new WeakReference<OnLifecycleEvent>(listener));
	}

	public void addOnWindowFocusChangedEventListener(OnWindowFocusChangedEvent listener) {
		windowFocusChangedListeners.add(new WeakReference<OnWindowFocusChangedEvent>(listener));
	}

	public void addInterceptOnBackPressedEventListener(interceptOnBackPressedEvent listener) {
		interceptOnBackPressedListeners.add(new WeakReference<interceptOnBackPressedEvent>(listener));
	}

	public void removeOnLifecycleEventListener(OnLifecycleEvent listener) {
		// TODO stub
	}

	private void releaseDialogs(boolean finish) {
		// clean up dialogs when activity is pausing or finishing
		for (Iterator<DialogWrapper> iter = dialogs.iterator(); iter.hasNext();) {
			DialogWrapper p = iter.next();
			Dialog dialog = p.getDialog();
			boolean persistent = p.getPersistent();
			// if the activity is pausing but not finishing, clean up dialogs only if
			// they are non-persistent
			if (finish || !persistent) {
				if (dialog != null && dialog.isShowing()) {
					dialog.dismiss();
				}
				dialogs.remove(p);
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		synchronized (windowFocusChangedListeners.synchronizedList()) {
			for (OnWindowFocusChangedEvent listener : windowFocusChangedListeners.nonNull()) {
				try {
					listener.onWindowFocusChanged(hasFocus);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching onWindowFocusChanged event: " + t.getMessage(), t);
				}
			}
		}
		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	/**
	 * When this activity pauses, this method sets the current activity to null, fires a javascript 'pause' event,
	 * and if the activity is finishing, remove all dialogs associated with it.
	 */
	protected void onPause() {
		inForeground = false;
		super.onPause();
		isResumed = false;

		Log.d(TAG, "Activity " + this + " onPause", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();
		if (tiApp.isRestartPending()) {
			releaseDialogs(true);
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (!windowStack.empty()) {
			windowStack.peek().onWindowFocusChange(false);
		}

		TiApplication.updateActivityTransitionState(true);
		// tiApp.setCurrentActivity(this, null);
		TiUIHelper.showSoftKeyboard(getWindow().getDecorView(), false);

		// release non-persistent dialogs when activity hides
		releaseDialogs(this.isFinishing());

		if (activityProxy != null) {
			activityProxy.fireSyncEvent(TiC.EVENT_PAUSE, null);
		}

		this.fireLifecycleEvent(TiLifecycle.LIFECYCLE_ON_PAUSE);

		// Checkpoint for ti.end event
		if (tiApp != null) {
			tiApp.postAnalyticsEvent(TiAnalyticsEventFactory.createAppEndEvent());
		}
	}

	@Override
	/**
	 * When the activity resumes, this method updates the current activity to this and fires a javascript
	 * 'resume' event.
	 */
	protected void onResume() {
		inForeground = true;
		super.onResume();
		if (isFinishing()) {
			return;
		}

		Log.d(TAG, "Activity " + this + " onResume", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();
		if (tiApp.isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (!windowStack.empty()) {
			windowStack.peek().onWindowFocusChange(true);
		}

		tiApp.setCurrentActivity(this, this);
		TiApplication.updateActivityTransitionState(false);

		if (activityProxy != null) {
			// Fire the sync event with a timeout, so the main thread won't be blocked too long to get an ANR. (TIMOB-13253)
			activityProxy.fireSyncEvent(TiC.EVENT_RESUME, null);
		}

		this.fireLifecycleEvent(TiLifecycle.LIFECYCLE_ON_RESUME);

		isResumed = true;

		// Checkpoint for ti.start event
		String deployType = tiApp.getAppProperties().getString("ti.deploytype", "unknown");
		tiApp.postAnalyticsEvent(TiAnalyticsEventFactory.createAppStartEvent(tiApp, deployType));
	}

	@Override
	/**
	 * When this activity starts, this method updates the current activity to this if necessary and
	 * fire javascript 'start' and 'focus' events. Focus events will only fire if 
	 * the activity is not a tab activity.
	 */
	protected void onStart() {
		inForeground = true;
		super.onStart();
		if (isFinishing()) {
			return;
		}

		// Newer versions of Android appear to turn this on by default.
		// Turn if off until an activity indicator is shown.
		setProgressBarIndeterminateVisibility(false);

		Log.d(TAG, "Activity " + this + " onStart", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();

		if (tiApp.isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		updateTitle();

		if (activityProxy != null) {
			// we only want to set the current activity for good in the resume state but we need it right now.
			// save off the existing current activity, set ourselves to be the new current activity temporarily
			// so we don't run into problems when we give the proxy the event
			Activity tempCurrentActivity = tiApp.getCurrentActivity();
			tiApp.setCurrentActivity(this, this);

			// Fire the sync event with a timeout, so the main thread won't be blocked too long to get an ANR. (TIMOB-13253)
			activityProxy.fireSyncEvent(TiC.EVENT_START, null);

			// set the current activity back to what it was originally
			tiApp.setCurrentActivity(this, tempCurrentActivity);
		}

		this.fireLifecycleEvent(TiLifecycle.LIFECYCLE_ON_START);

		// store current configuration orientation
		// This fixed bug with double orientation chnage firing when activity starts in landscape
		previousOrientation = getResources().getConfiguration().orientation;
	}

	@Override
	/**
	 * When this activity stops, this method fires the javascript 'blur' and 'stop' events. Blur events will only fire
	 * if the activity is not a tab activity.
	 */
	protected void onStop() {
		inForeground = false;
		super.onStop();

		Log.d(TAG, "Activity " + this + " onStop", Log.DEBUG_MODE);

		if (getTiApp().isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (activityProxy != null) {
			activityProxy.fireSyncEvent(TiC.EVENT_STOP, null);
		}

		this.fireLifecycleEvent(TiLifecycle.LIFECYCLE_ON_STOP);

		KrollRuntime.suggestGC();
	}

	private void fireLifecycleEvent(int eventType) {
		lifecycleListenersLock.lock();
		try {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					TiLifecycle.fireLifecycleEvent(this, listener, eventType);

				} catch (Throwable t) {
					Log.e(TAG, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		} finally {
			lifecycleListenersLock.unlock();
		}
	}

	@Override
	/**
	 * When this activity restarts, this method updates the current activity to this and fires javascript 'restart'
	 * event.
	 */
	protected void onRestart() {
		inForeground = true;
		super.onRestart();

		Log.d(TAG, "Activity " + this + " onRestart", Log.DEBUG_MODE);

		TiApplication tiApp = getTiApp();
		if (tiApp.isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}

			return;
		}

		if (activityProxy != null) {
			// we only want to set the current activity for good in the resume state but we need it right now.
			// save off the existing current activity, set ourselves to be the new current activity temporarily
			// so we don't run into problems when we give the proxy the event
			Activity tempCurrentActivity = tiApp.getCurrentActivity();
			tiApp.setCurrentActivity(this, this);

			activityProxy.fireSyncEvent(TiC.EVENT_RESTART, null);

			// set the current activity back to what it was originally
			tiApp.setCurrentActivity(this, tempCurrentActivity);
		}
	}

	@Override
	/**
	 * When the activity is about to go into the background as a result of user choice, this method fires the 
	 * javascript 'userleavehint' event.
	 */
	protected void onUserLeaveHint() {
		Log.d(TAG, "Activity " + this + " onUserLeaveHint", Log.DEBUG_MODE);

		if (getTiApp().isRestartPending()) {
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (activityProxy != null) {
			activityProxy.fireSyncEvent(TiC.EVENT_USER_LEAVE_HINT, null);
		}

		super.onUserLeaveHint();
	}

	@Override
	/**
	 * When this activity is destroyed, this method removes it from the activity stack, performs
	 * clean up, and fires javascript 'destroy' event. 
	 */
	protected void onDestroy() {
		Log.d(TAG, "Activity " + this + " onDestroy", Log.DEBUG_MODE);

		inForeground = false;
		TiApplication tiApp = getTiApp();
		// Clean up dialogs when activity is destroyed.
		releaseDialogs(true);

		if (tiApp.isRestartPending()) {
			super.onDestroy();
			if (!isFinishing()) {
				finish();
			}
			return;
		}

		if (this.mKeyboardStateMonitor != null) {
			this.mKeyboardStateMonitor.stop();
		}

		this.fireLifecycleEvent(TiLifecycle.LIFECYCLE_ON_DESTROY);

		super.onDestroy();

		boolean isFinishing = isFinishing();
		// If the activity is finishing, remove the windowId and supportHelperId so the window and supportHelper can be released.
		// If the activity is forced to destroy by Android OS, keep the windowId and supportHelperId so the activity can be recovered.
		if (isFinishing) {
			int windowId = getIntentInt(TiC.INTENT_PROPERTY_WINDOW_ID, -1);
			TiActivityWindows.removeWindow(windowId);
			TiActivitySupportHelpers.removeSupportHelper(supportHelperId);
		}

		fireOnDestroy();

		if (layout instanceof TiCompositeLayout) {
			Log.d(TAG, "Layout cleanup.", Log.DEBUG_MODE);
			// ((TiCompositeLayout) layout).removeAllViews();
		}
		layout = null;

		// LW windows
		if (window == null && view != null) {
			// view.releaseViews();
			view = null;
		}

		if (window != null) {
			window.closeFromActivity(isFinishing);
			window = null;
		}

		if (menuHelper != null) {
			menuHelper.destroy();
			menuHelper = null;
		}

		if (activityProxy != null) {
			activityProxy.release();
			activityProxy = null;
		}

		// Don't dispose the runtime if the activity is forced to destroy by Android,
		// so we can recover the activity later.
		KrollRuntime.decrementActivityRefCount(isFinishing);
		KrollRuntime.suggestGC();

		if (!isFinishing) {
			System.exit(-1);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// If the activity is forced to destroy by Android, save the supportHelperId so
		// we can get it back when the activity is recovered.
		if (!isFinishing() && supportHelper != null) {
			outState.putInt("supportHelperId", supportHelperId);
		}

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey("supportHelperId")) {
			supportHelperId = savedInstanceState.getInt("supportHelperId");
			supportHelper = TiActivitySupportHelpers.retrieveSupportHelper(this, supportHelperId);
			if (supportHelper == null) {
				Log.e(TAG, "Unable to retrieve the activity support helper.");
			}
		}
	}

	// called in order to ensure that the onDestroy call is only acted upon once.
	// should be called by any subclass
	protected void fireOnDestroy() {
		if (!onDestroyFired) {
			if (activityProxy != null) {
				activityProxy.fireSyncEvent(TiC.EVENT_DESTROY, null);
			}
			onDestroyFired = true;
		}
	}

	protected boolean shouldFinishRootActivity() {
		return getIntentBoolean(TiC.INTENT_PROPERTY_FINISH_ROOT, false);
	}

	@Override
	public void finish() {
		super.finish();

		if (shouldFinishRootActivity()) {
			TiApplication app = getTiApp();
			if (app != null) {
				TiRootActivity rootActivity = app.getRootActivity();
				if (rootActivity != null && !(rootActivity.equals(this)) && !rootActivity.isFinishing()) {
					rootActivity.finish();
				} else if (rootActivity == null && !app.isRestartPending()) {
					// When the root activity has been killed and garbage collected and the app is not scheduled to restart,
					// we need to force finish the root activity while this activity has an intent to finish root.
					// This happens when the "Don't keep activities" option is enabled and the user stays in some activity
					// (eg. heavyweight window, tabgroup) other than the root activity for a while and then he wants to back
					// out the app.
					app.setForceFinishRootActivity(true);
				}
			}
		}
	}

	// These activityOnXxxx are all used by TiLaunchActivity when
	// the android bug 2373 is detected and the app is being re-started.
	// By calling these from inside its on onXxxx handlers, TiLaunchActivity
	// can avoid calling super.onXxxx (super being TiBaseActivity), which would
	// result in a bunch of Titanium-specific code running when we don't need it
	// since we are restarting the app as fast as possible. Calling these methods
	// allows TiLaunchActivity to fulfill the requirement that the Android built-in
	// Activity's onXxxx must be called. (Think of these as something like super.super.onXxxx
	// from inside TiLaunchActivity.)
	protected void activityOnPause() {
		super.onPause();
	}

	protected void activityOnRestart() {
		super.onRestart();
	}

	protected void activityOnResume() {
		super.onResume();
	}

	protected void activityOnStop() {
		super.onStop();
	}

	protected void activityOnStart() {
		super.onStart();
	}

	protected void activityOnDestroy() {
		super.onDestroy();
	}

	public void activityOnCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * Called by the onCreate methods of TiBaseActivity to determine if an unsupported application
	 * re-launch appears to be occurring.
	 * @param activity The Activity getting the onCreate
	 * @param savedInstanceState The argument passed to the onCreate. A non-null value is a "tell" that the system is re-starting a killed application.
	 */
	public static boolean isUnsupportedReLaunch(Activity activity, Bundle savedInstanceState) {
		// If all the activities has been killed and the runtime has been disposed, we have to relaunch the app.
		return KrollRuntime.getInstance().hasDisposed() && savedInstanceState != null && !(activity instanceof TiLaunchActivity);
	}

	public View getRootView() {
		return this.findViewById(android.R.id.content);
	}

	/**
	 * @return
	 * @see org.appcelerator.titanium.view.KeyboardStateMonitor#isKeyboardVisible()
	 */
	public boolean isKeyboardVisible() {
		return mKeyboardStateMonitor != null ? mKeyboardStateMonitor.isKeyboardVisible() : false;
	}

	/**
	 * @param listener
	 * @return
	 * @see org.appcelerator.titanium.view.KeyboardStateMonitor#add(org.appcelerator.titanium.view.OnKeyboardVisibilityChangeListener)
	 */
	public boolean addKeyboardListener(OnKeyboardVisibilityChangeListener listener) {
		return this.mKeyboardStateMonitor != null ? mKeyboardStateMonitor.add(listener) : false;
	}

	/**
	 * @param listener
	 * @return
	 * @see org.appcelerator.titanium.view.KeyboardStateMonitor#contains(org.appcelerator.titanium.view.OnKeyboardVisibilityChangeListener)
	 */
	public boolean containsKeyboardListener(OnKeyboardVisibilityChangeListener listener) {
		return this.mKeyboardStateMonitor != null ? mKeyboardStateMonitor.contains(listener) : false;
	}

	/**
	 * @param listener
	 * @return
	 * @see org.appcelerator.titanium.view.KeyboardStateMonitor#remove(org.appcelerator.titanium.view.OnKeyboardVisibilityChangeListener)
	 */
	public boolean removeKeyboardListener(OnKeyboardVisibilityChangeListener listener) {
		return this.mKeyboardStateMonitor != null ? mKeyboardStateMonitor.remove(listener) : false;
	}
}
