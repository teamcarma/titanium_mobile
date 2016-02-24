/*
 * Copyright (c) 2014 Avego Ltd., All Rights Reserved.
 * For licensing terms please contact Avego LTD.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.appcelerator.titanium.view;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appcelerator.kroll.common.Log;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;

/**
 * The KeyboardStateMonitor represents
 * @version $Id$
 * @author wei.ding
 */
public class KeyboardStateMonitor implements OnGlobalLayoutListener {

	/**
	 * This
	 */
	private static final String TAG = "KeyboardStateMonitor";

	private Activity activity;

	private View activityRootView;
	private View windowRootView;

	private List<OnKeyboardVisibilityChangeListener> listeners;

	public static final int FAULT_TOLERANT = 10;

	private final int faultTolerantInPixes;

	private AtomicBoolean isKeyboardShown = new AtomicBoolean(false);

	/**
	 * This creates a KeyboardStateMonitor
	 * @param activity
	 */
	public KeyboardStateMonitor(Activity activity) {
		this(activity, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	/**
	 * This creates a KeyboardStateMonitor
	 * @param activity
	 */
	public KeyboardStateMonitor(Activity activity, OnKeyboardVisibilityChangeListener listener) {
		this(activity, WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE, listener);
	}

	/**
	 * This creates a KeyboardStateMonitor
	 * @param activity
	 */
	public KeyboardStateMonitor(Activity activity, int windowSoftInputMode) {
		this(activity, windowSoftInputMode, null);
	}

	/**
	 * This creates a KeyboardStateMonitor
	 * @param activity
	 */
	public KeyboardStateMonitor(Activity activity, int windowSoftInputMode, OnKeyboardVisibilityChangeListener listener) {
		super();
		if (activity == null) {
			throw new IllegalArgumentException();
		}

		this.listeners = new CopyOnWriteArrayList<OnKeyboardVisibilityChangeListener>();

		this.activity = activity;
		this.activity.getWindow().setSoftInputMode(windowSoftInputMode);

		this.faultTolerantInPixes = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, FAULT_TOLERANT, this.getDisplayMetrics());

		this.activityRootView = this.activity.findViewById(android.R.id.content);
		this.windowRootView = this.activityRootView.getRootView();

		this.activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);

		if (listener != null) {
			this.add(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see android.view.ViewTreeObserver.OnGlobalLayoutListener#onGlobalLayout()
	 */
	public void onGlobalLayout() {
		int gap = this.windowRootView.getBottom() - this.activityRootView.getBottom();
		boolean _isKeyboardShown = gap > this.getFaultTolerantInPixes();

		Log.d(TAG, MessageFormat.format("Keyboard state: {0} -> {1}.", this.isKeyboardVisible(), _isKeyboardShown), Log.DEBUG_MODE);

		if (!isKeyboardShown.compareAndSet(!_isKeyboardShown, _isKeyboardShown)) {
			return;
		}
		for (OnKeyboardVisibilityChangeListener listener : this.listeners) {
			listener.onKeyboardVisibilityChange(_isKeyboardShown);
		}
	}

	/**
	 * This checks if the keyboard is visible.
	 * @return
	 */
	public boolean isKeyboardVisible() {
		return this.isKeyboardShown.get();
	}

	/**
	 * @param listener
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(OnKeyboardVisibilityChangeListener listener) {
		return listeners.add(listener);
	}

	/**
	 * @see java.util.List#clear()
	 */
	public void clear() {
		listeners.clear();
	}

	/**
	 * @param listener
	 * @return
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(OnKeyboardVisibilityChangeListener listener) {
		return listeners.contains(listener);
	}

	/**
	 * @param listener
	 * @return
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(OnKeyboardVisibilityChangeListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * This gets the faultTolerantInPixes
	 * @return the faultTolerantInPixes
	 */
	public int getFaultTolerantInPixes() {
		return faultTolerantInPixes;
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		this.listeners.clear();
		this.activityRootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	private DisplayMetrics getDisplayMetrics() {
		DisplayMetrics metrics = new DisplayMetrics();
		this.activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics;
	}
}
