/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiColorHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;
import org.appcelerator.titanium.view.TiUIView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

public class TiUIScrollView extends TiUIView {

	public static final int TYPE_VERTICAL = 0;
	public static final int TYPE_HORIZONTAL = 1;

	private static final String TAG = "TiUIScrollView";
	private int offsetX = 0, offsetY = 0;
	private boolean setInitialOffset = false;
	private boolean mScrollingEnabled = true;

	private ViewGroup scrollView;

	public class TiScrollViewLayout extends TiCompositeLayout {

		private static final int AUTO = Integer.MAX_VALUE;
		private int parentWidth = 0;
		private int parentHeight = 0;
		private boolean canCancelEvents = true;

		public TiScrollViewLayout(Context context, LayoutArrangement arrangement) {
			super(context, arrangement, proxy);
		}

		public void setParentWidth(int width) {
			parentWidth = width;
		}

		public void setParentHeight(int height) {
			parentHeight = height;
		}

		public void setCanCancelEvents(boolean value) {
			canCancelEvents = value;
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			// If canCancelEvents is false, then we want to prevent the scroll view from canceling the touch
			// events of the child view
			if (!canCancelEvents) {
				requestDisallowInterceptTouchEvent(true);
			}

			return super.dispatchTouchEvent(ev);
		}

		private int getContentProperty(String property) {
			Object value = getProxy().getProperty(property);
			if (value != null) {
				if (value.equals(TiC.SIZE_AUTO)) {
					return AUTO;
				} else if (value instanceof Number) {
					return ((Number) value).intValue();
				} else {
					int type = 0;
					TiDimension dimension;
					if (TiC.PROPERTY_CONTENT_HEIGHT.equals(property)) {
						type = TiDimension.TYPE_HEIGHT;
					} else if (TiC.PROPERTY_CONTENT_WIDTH.equals(property)) {
						type = TiDimension.TYPE_WIDTH;
					}
					dimension = new TiDimension(value.toString(), type);
					return dimension.getUnits() == TiDimension.COMPLEX_UNIT_AUTO ? AUTO : dimension.getIntValue();
				}
			}
			return AUTO;
		}

		@Override
		protected int getWidthMeasureSpec(View child) {
			int contentWidth = getContentProperty(TiC.PROPERTY_CONTENT_WIDTH);
			if (contentWidth == AUTO) {
				return MeasureSpec.UNSPECIFIED;
			} else {
				return super.getWidthMeasureSpec(child);
			}
		}

		@Override
		protected int getHeightMeasureSpec(View child) {
			int contentHeight = getContentProperty(TiC.PROPERTY_CONTENT_HEIGHT);
			if (contentHeight == AUTO) {
				return MeasureSpec.UNSPECIFIED;
			} else {
				return super.getHeightMeasureSpec(child);
			}
		}

		@Override
		protected int getMeasuredWidth(int maxWidth, int widthSpec) {
			int contentWidth = getContentProperty(TiC.PROPERTY_CONTENT_WIDTH);
			if (contentWidth == AUTO) {
				contentWidth = maxWidth; // measuredWidth;
			}

			// Returns the content's width when it's greater than the scrollview's width
			if (contentWidth > parentWidth) {
				return contentWidth;
			} else {
				return resolveSize(maxWidth, widthSpec);
			}
		}

		@Override
		protected int getMeasuredHeight(int maxHeight, int heightSpec) {
			int contentHeight = getContentProperty(TiC.PROPERTY_CONTENT_HEIGHT);
			if (contentHeight == AUTO) {
				contentHeight = maxHeight; // measuredHeight;
			}

			// Returns the content's height when it's greater than the scrollview's height
			if (contentHeight > parentHeight) {
				return contentHeight;
			} else {
				return resolveSize(maxHeight, heightSpec);
			}
		}
	}

	private class ScrollViewWrapper extends SwipeRefreshLayout {

		private boolean viewFocused = false;

		private final ScrollView nativeScrollView;

		public ScrollViewWrapper(Context context, ScrollView view) {
			super(context);
			this.nativeScrollView = view;
		}

		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			// To prevent undesired "focus" and "blur" events during layout
			// caused by ListView temporarily taking focus, we will disable
			// focus events until layout has finished. First check for a quick
			// exit. listView can be null, such as if window closing.
			// Starting with API 18, calling requestFocus() will trigger another
			// layout pass of the listview, resulting in an infinite loop. Here
			// we check if the view already focused, and stop the loop.
			if (nativeScrollView == null || (Build.VERSION.SDK_INT >= 18 && nativeScrollView != null && !changed && viewFocused)) {
				viewFocused = false;
				super.onLayout(changed, left, top, right, bottom);
				return;
			}
			OnFocusChangeListener focusListener = null;
			View focusedView = nativeScrollView.findFocus();
			int cursorPosition = -1;
			if (focusedView != null) {
				OnFocusChangeListener listener = focusedView.getOnFocusChangeListener();
				if (listener != null && listener instanceof TiUIView) {
					// Before unfocus the current editText, store cursor
					// position so
					// we can restore it later
					if (focusedView instanceof EditText) {
						cursorPosition = ((EditText) focusedView).getSelectionStart();
					}
					focusedView.setOnFocusChangeListener(null);
					focusListener = listener;
				}
			}

			// We are temporarily going to block focus to descendants
			// because LinearLayout on layout will try to find a focusable
			// descendant
			if (focusedView != null) {
				nativeScrollView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			}
			super.onLayout(changed, left, top, right, bottom);
			// Now we reset the descendant focusability
			nativeScrollView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

			TiViewProxy viewProxy = proxy;
			if (viewProxy != null && viewProxy.hasListeners(TiC.EVENT_POST_LAYOUT)) {
				viewProxy.fireEvent(TiC.EVENT_POST_LAYOUT, null);
			}

			// Layout is finished, re-enable focus events.
			if (focusListener != null) {
				// If the configuration changed, we manually fire the blur event
				if (changed) {
					focusedView.setOnFocusChangeListener(focusListener);
					focusListener.onFocusChange(focusedView, false);
				} else {
					// Ok right now focus is with listView. So set it back to
					// the focusedView
					viewFocused = true;
					focusedView.requestFocus();
					focusedView.setOnFocusChangeListener(focusListener);
					// Restore cursor position
					if (cursorPosition != -1) {
						((EditText) focusedView).setSelection(cursorPosition);
					}
				}
			}
		}

	}

	// same code, different super-classes
	private class TiVerticalScrollView extends ScrollView {

		private TiScrollViewLayout innerLayout;

		public TiVerticalScrollView(Context context, LayoutArrangement arrangement) {
			super(context);
			setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);

			innerLayout = new TiScrollViewLayout(context, arrangement);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
			innerLayout.setLayoutParams(params);
			super.addView(innerLayout, params);
		}

		public TiScrollViewLayout getLayout() {
			return innerLayout;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE && !mScrollingEnabled) {
				return false;
			}
			// There's a known Android bug (version 3.1 and above) that will throw an exception when we use 3+ fingers to touch the scrollview.
			// Link: http://code.google.com/p/android/issues/detail?id=18990
			try {
				return super.onTouchEvent(event);
			} catch (IllegalArgumentException e) {
				return false;
			}
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent event) {
			if (mScrollingEnabled) {
				return super.onInterceptTouchEvent(event);
			}

			return false;
		}

		@Override
		public void addView(View child, android.view.ViewGroup.LayoutParams params) {
			innerLayout.addView(child, params);
		}

		public void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			// setting offset once when this view is visible
			if (!setInitialOffset) {
				scrollTo(offsetX, offsetY);
				setInitialOffset = true;
			}

		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);

			KrollDict data = new KrollDict();
			data.put(TiC.EVENT_PROPERTY_X, l);
			data.put(TiC.EVENT_PROPERTY_Y, t);
			setContentOffset(l, t);
			getProxy().fireEvent(TiC.EVENT_SCROLL, data);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			innerLayout.setParentHeight(MeasureSpec.getSize(heightMeasureSpec));
			innerLayout.setParentWidth(MeasureSpec.getSize(widthMeasureSpec));
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			// This is essentially doing the same logic as if you did setFillViewPort(true). In native Android, they
			// don't measure the child again if measured height of content view < scrollViewheight. But we want to do
			// this in all cases since we allow the content view height to be greater than the scroll view. We force
			// this to allow fill behavior: TIMOB-8243.
			if (getChildCount() > 0) {
				final View child = getChildAt(0);
				int height = getMeasuredHeight();
				final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();

				int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
				height -= getPaddingTop();
				height -= getPaddingBottom();

				// If we measure the child height to be greater than the parent height, use it in subsequent
				// calculations to make sure the children are measured correctly the second time around.
				height = Math.max(child.getMeasuredHeight(), height);
				int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

			}
		}
	}

	private class TiHorizontalScrollView extends HorizontalScrollView {

		private TiScrollViewLayout layout;

		public TiHorizontalScrollView(Context context, LayoutArrangement arrangement) {
			super(context);
			setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
			setScrollContainer(true);

			layout = new TiScrollViewLayout(context, arrangement);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
			layout.setLayoutParams(params);
			super.addView(layout, params);

		}

		public TiScrollViewLayout getLayout() {
			return layout;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE && !mScrollingEnabled) {
				return false;
			}
			// There's a known Android bug (version 3.1 and above) that will throw an exception when we use 3+ fingers to touch the scrollview.
			// Link: http://code.google.com/p/android/issues/detail?id=18990
			try {
				return super.onTouchEvent(event);
			} catch (IllegalArgumentException e) {
				return false;
			}
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent event) {
			if (mScrollingEnabled) {
				return super.onInterceptTouchEvent(event);
			}

			return false;
		}

		@Override
		public void addView(View child, android.view.ViewGroup.LayoutParams params) {
			layout.addView(child, params);
		}

		public void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			// setting offset once this view is visible
			if (!setInitialOffset) {
				scrollTo(offsetX, offsetY);
				setInitialOffset = true;
			}

		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);

			KrollDict data = new KrollDict();
			data.put(TiC.EVENT_PROPERTY_X, l);
			data.put(TiC.EVENT_PROPERTY_Y, t);
			setContentOffset(l, t);
			getProxy().fireEvent(TiC.EVENT_SCROLL, data);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			layout.setParentHeight(MeasureSpec.getSize(heightMeasureSpec));
			layout.setParentWidth(MeasureSpec.getSize(widthMeasureSpec));
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			// This is essentially doing the same logic as if you did setFillViewPort(true). In native Android, they
			// don't measure the child again if measured width of content view < scroll view width. But we want to do
			// this in all cases since we allow the content view width to be greater than the scroll view. We force this
			// to allow fill behavior: TIMOB-8243.
			if (getChildCount() > 0) {
				final View child = getChildAt(0);
				int width = getMeasuredWidth();
				final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();

				int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
				width -= getPaddingLeft();
				width -= getPaddingRight();

				// If we measure the child width to be greater than the parent width, use it in subsequent
				// calculations to make sure the children are measured correctly the second time around.
				width = Math.max(child.getMeasuredWidth(), width);
				int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);

				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			}

		}
	}

	public TiUIScrollView(TiViewProxy proxy) {
		// we create the view after the properties are processed
		super(proxy);
		getLayoutParams().autoFillsHeight = true;
		getLayoutParams().autoFillsWidth = true;
	}

	public void setContentOffset(int x, int y) {
		KrollDict offset = new KrollDict();
		offsetX = x;
		offsetY = y;
		offset.put(TiC.EVENT_PROPERTY_X, offsetX);
		offset.put(TiC.EVENT_PROPERTY_Y, offsetY);
		getProxy().setProperty(TiC.PROPERTY_CONTENT_OFFSET, offset);
	}

	public void setContentOffset(Object hashMap) {
		if (hashMap instanceof HashMap) {
			HashMap contentOffset = (HashMap) hashMap;
			offsetX = TiConvert.toInt(contentOffset, TiC.PROPERTY_X);
			offsetY = TiConvert.toInt(contentOffset, TiC.PROPERTY_Y);
		} else {
			Log.e(TAG, "ContentOffset must be an instance of HashMap");
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);
		}
		if (key.equals(TiC.PROPERTY_CONTENT_OFFSET)) {
			setContentOffset(newValue);
			scrollTo(offsetX, offsetY, false);
		}
		if (key.equals(TiC.PROPERTY_CAN_CANCEL_EVENTS)) {
			View view = getNativeScrollView();
			boolean canCancelEvents = TiConvert.toBoolean(newValue);
			if (view instanceof TiHorizontalScrollView) {
				((TiHorizontalScrollView) view).getLayout().setCanCancelEvents(canCancelEvents);
			} else if (view instanceof TiVerticalScrollView) {
				((TiVerticalScrollView) view).getLayout().setCanCancelEvents(canCancelEvents);
			}
		}
		if (TiC.PROPERTY_SCROLLING_ENABLED.equals(key)) {
			setScrollingEnabled(newValue);
		}
		if (TiC.PROPERTY_OVER_SCROLL_MODE.equals(key)) {
			if (Build.VERSION.SDK_INT >= 9) {
				getNativeScrollView().setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
			}
		}
		super.propertyChanged(key, oldValue, newValue, proxy);
		if (this.scrollView instanceof TiVerticalScrollView) {
			SwipeRefreshLayout layout = (SwipeRefreshLayout) this.getNativeView();
			if (TiC.PROPERTY_REFRESHABLE.equals(key) || TiC.PROPERTY_REFRESHABLE_DEPRECATED.equals(key)) {
				layout.setEnabled(TiConvert.toBoolean(newValue));
			}
			if (TiC.PROPERTY_ENABLED.equals(key)) {
				this.scrollView.setEnabled(TiConvert.toBoolean(newValue, true));
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void processProperties(KrollDict d) {
		boolean showHorizontalScrollBar = false;
		boolean showVerticalScrollBar = false;

		if (d.containsKey(TiC.PROPERTY_SCROLLING_ENABLED)) {
			setScrollingEnabled(d.get(TiC.PROPERTY_SCROLLING_ENABLED));
		}

		if (d.containsKey(TiC.PROPERTY_SHOW_HORIZONTAL_SCROLL_INDICATOR)) {
			showHorizontalScrollBar = TiConvert.toBoolean(d, TiC.PROPERTY_SHOW_HORIZONTAL_SCROLL_INDICATOR);
		}
		if (d.containsKey(TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR)) {
			showVerticalScrollBar = TiConvert.toBoolean(d, TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR);
		}

		if (showHorizontalScrollBar && showVerticalScrollBar) {
			Log.w(TAG, "Both scroll bars cannot be shown. Defaulting to vertical shown");
			showHorizontalScrollBar = false;
		}

		if (d.containsKey(TiC.PROPERTY_CONTENT_OFFSET)) {
			Object offset = d.get(TiC.PROPERTY_CONTENT_OFFSET);
			setContentOffset(offset);
		}

		int type = TYPE_VERTICAL;
		boolean deduced = false;
		
		if (d.containsKey(TiC.PROPERTY_WIDTH) && d.containsKey(TiC.PROPERTY_CONTENT_WIDTH)) {
			Object width = d.get(TiC.PROPERTY_WIDTH);
			Object contentWidth = d.get(TiC.PROPERTY_CONTENT_WIDTH);
			if (width.equals(contentWidth) || showVerticalScrollBar) {
				type = TYPE_VERTICAL;
				deduced = true;
			}
			
		}

		if (d.containsKey(TiC.PROPERTY_HEIGHT) && d.containsKey(TiC.PROPERTY_CONTENT_HEIGHT)) {
			Object height = d.get(TiC.PROPERTY_HEIGHT);
			Object contentHeight = d.get(TiC.PROPERTY_CONTENT_HEIGHT);
			if (height.equals(contentHeight) || showHorizontalScrollBar) {
				type = TYPE_HORIZONTAL;
				deduced = true;
			}
		}

		// android only property
		if (d.containsKey(TiC.PROPERTY_SCROLL_TYPE)) {
			Object scrollType = d.get(TiC.PROPERTY_SCROLL_TYPE);
			if (scrollType.equals(TiC.LAYOUT_VERTICAL)) {
				type = TYPE_VERTICAL;
			} else if (scrollType.equals(TiC.LAYOUT_HORIZONTAL)) {
				type = TYPE_HORIZONTAL;
			} else {
				Log.w(TAG, "scrollType value '" + TiConvert.toString(scrollType) + "' is invalid. Only 'vertical' and 'horizontal' are supported.");
			}
		} else if (!deduced && type == TYPE_VERTICAL) {
			Log.w(TAG,
					"Scroll direction could not be determined based on the provided view properties. Default VERTICAL scroll direction being used. Use the 'scrollType' property to explicitly set the scrolling direction.");
		}

		// we create the view here since we now know the potential widget type
		View view = null;
		LayoutArrangement arrangement = LayoutArrangement.DEFAULT;
		TiScrollViewLayout scrollViewLayout;
		if (d.containsKey(TiC.PROPERTY_LAYOUT) && d.getString(TiC.PROPERTY_LAYOUT).equals(TiC.LAYOUT_VERTICAL)) {
			arrangement = LayoutArrangement.VERTICAL;
		} else if (d.containsKey(TiC.PROPERTY_LAYOUT) && d.getString(TiC.PROPERTY_LAYOUT).equals(TiC.LAYOUT_HORIZONTAL)) {
			arrangement = LayoutArrangement.HORIZONTAL;
		}

		switch (type) {
			case TYPE_HORIZONTAL:
				Log.d(TAG, "creating horizontal scroll view", Log.DEBUG_MODE);
				this.scrollView = new TiHorizontalScrollView(getProxy().getActivity(), arrangement);
				view = this.scrollView;
				scrollViewLayout = ((TiHorizontalScrollView) view).getLayout();
				break;
			case TYPE_VERTICAL:
			default:
				Log.d(TAG, "creating vertical scroll view", Log.DEBUG_MODE);
				TiVerticalScrollView verticalScrollView = new TiVerticalScrollView(getProxy().getActivity(), arrangement);
				scrollViewLayout = verticalScrollView.getLayout();
				ScrollViewWrapper layout = new ScrollViewWrapper(this.getProxy().getActivity(), verticalScrollView);
				layout.addView(verticalScrollView);
				this.scrollView = verticalScrollView;
				view = layout;
		}

		if (d.containsKey(TiC.PROPERTY_CAN_CANCEL_EVENTS)) {
			((TiScrollViewLayout) scrollViewLayout).setCanCancelEvents(TiConvert.toBoolean(d, TiC.PROPERTY_CAN_CANCEL_EVENTS));
		}

		boolean autoContentWidth = (scrollViewLayout.getContentProperty(TiC.PROPERTY_CONTENT_WIDTH) == scrollViewLayout.AUTO);
		boolean wrap = !autoContentWidth;
		if (d.containsKey(TiC.PROPERTY_HORIZONTAL_WRAP) && wrap) {
			wrap = TiConvert.toBoolean(d, TiC.PROPERTY_HORIZONTAL_WRAP, true);			
		}
		scrollViewLayout.setEnableHorizontalWrap(wrap);
		
		if (d.containsKey(TiC.PROPERTY_OVER_SCROLL_MODE)) {
			if (Build.VERSION.SDK_INT >= 9) {
				view.setOverScrollMode(TiConvert.toInt(d.get(TiC.PROPERTY_OVER_SCROLL_MODE), View.OVER_SCROLL_ALWAYS));
			}
		}

		setNativeView(view);

		nativeView.setHorizontalScrollBarEnabled(showHorizontalScrollBar);
		nativeView.setVerticalScrollBarEnabled(showVerticalScrollBar);

		super.processProperties(d);

		if (this.scrollView instanceof TiVerticalScrollView) {
			configRefreshProperties((ScrollViewWrapper) view, d);
			if (d.containsKey(TiC.PROPERTY_ENABLED)) {
				this.scrollView.setEnabled(TiConvert.toBoolean(d, TiC.PROPERTY_ENABLED, true));
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void configRefreshProperties(SwipeRefreshLayout layout, KrollDict d) {
		layout.setOnRefreshListener(newOnRefreshListener());
		if (d.containsKey(TiC.PROPERTY_REFRESH_PROGRESSBAR_COLOR)) {
			// TODO Custom the progress bar's colors.
		} else {
			//TODO: Add this back in once we know the method exists in the JAR provided 
			// layout.setColorSchemeColors(TiColorHelper.HOLO_BLUE_BRIGHT, TiColorHelper.HOLO_GREEN_LIGHT, TiColorHelper.HOLO_ORANGE_LIGHT,
			// 		TiColorHelper.HOLO_RED_LIGHT);
		}
		if (d.containsKey(TiC.PROPERTY_REFRESHABLE)) {
			layout.setEnabled(d.getBoolean(TiC.PROPERTY_REFRESHABLE));
		} else if (d.containsKey(TiC.PROPERTY_REFRESHABLE_DEPRECATED)) {
			layout.setEnabled(d.getBoolean(TiC.PROPERTY_REFRESHABLE_DEPRECATED));
		} else {
			layout.setEnabled(false);
		}

		Log.d(TAG, "SwipeRefreshLayout Enabled : " + layout.isEnabled());
	}

	private OnRefreshListener newOnRefreshListener() {
		return new SwipeRefreshLayout.OnRefreshListener() {

			@Override
			public void onRefresh() {
				TiUIScrollView.this.onRefresh();
			}
		};
	}

	protected void onRefresh() {
		Log.d(TAG, "VerticalScrollView is refreshing, and view enabled ? " + this.getNativeView().isEnabled());
		this.fireEvent(TiC.EVENT_REFRESHED, KrollDict.EMPTY);
		this.fireEvent(TiC.EVENT_REFRESHED_DEPRECATED, KrollDict.EMPTY);
	}

	public TiScrollViewLayout getLayout() {
		if (this.scrollView instanceof TiVerticalScrollView) {
			return ((TiVerticalScrollView) this.scrollView).innerLayout;
		} else {
			return ((TiHorizontalScrollView) this.scrollView).layout;
		}
	}

	@Override
	protected void setOnClickListener(View view) {
		View targetView = view;
		// Get the layout and attach the listeners to it
		if (view instanceof TiVerticalScrollView) {
			targetView = ((TiVerticalScrollView) nativeView).innerLayout;
		}
		if (view instanceof TiHorizontalScrollView) {
			targetView = ((TiHorizontalScrollView) nativeView).layout;
		}
		super.setOnClickListener(targetView);
	}

	public void setScrollingEnabled(Object value) {
		try {
			mScrollingEnabled = TiConvert.toBoolean(value);
		} catch (IllegalArgumentException e) {
			mScrollingEnabled = true;
		}
	}

	public boolean getScrollingEnabled() {
		return mScrollingEnabled;
	}

	public void scrollTo(final int x, final int y, KrollDict options) {
		options = options == null ? new KrollDict() : options;
		this.scrollTo(x, y, TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true));
	}

	public void scrollTo(final int x, final int y, boolean animated) {
		if (!animated) {
			getNativeScrollView().scrollTo(x, y);
			getNativeScrollView().computeScroll();
			return;
		}

		View view = getNativeScrollView();

		if (view instanceof TiHorizontalScrollView) {
			final TiHorizontalScrollView scrollView = (TiHorizontalScrollView) view;
			scrollView.post(new Runnable() {

				@Override
				public void run() {
					scrollView.smoothScrollTo(x, y);
				}

			});
			return;
		}

		if (view instanceof TiVerticalScrollView) {
			final TiVerticalScrollView scrollView = (TiVerticalScrollView) view;
			scrollView.post(new Runnable() {

				@Override
				public void run() {
					scrollView.smoothScrollTo(x, y);
				}

			});
			return;
		}
	}

	public void scrollToBottom() {
		View view = getNativeScrollView();
		if (view instanceof TiHorizontalScrollView) {
			TiHorizontalScrollView scrollView = (TiHorizontalScrollView) view;
			scrollView.fullScroll(View.FOCUS_RIGHT);
		} else if (view instanceof TiVerticalScrollView) {
			TiVerticalScrollView scrollView = (TiVerticalScrollView) view;
			scrollView.fullScroll(View.FOCUS_DOWN);
		}
	}

	@Override
	public void add(TiUIView child) {
		this.add(child, -1);
		if (getNativeScrollView() != null) {
			getLayout().requestLayout();
			if (child.getNativeView() != null) {
				child.getNativeView().requestLayout();
			}
		}
	}

	private void add(TiUIView child, int childIndex) {
		if (child == null || this.scrollView == null) {
			return;
		}
		View cv = child.getOuterView();
		if (cv == null) {
			return;
		}
		if (cv.getParent() == null) {
			if (childIndex != -1) {
				this.scrollView.addView(cv, childIndex, child.getLayoutParams());
			} else {
				this.scrollView.addView(cv, child.getLayoutParams());
			}
		}
		children.add(child);
		child.setParent(this.getProxy());
	}

	@Override
	public void remove(TiUIView child) {
		if (child != null) {
			View cv = child.getOuterView();
			if (cv != null) {
				View nv = getLayout();
				if (nv instanceof ViewGroup) {
					((ViewGroup) nv).removeView(cv);
					children.remove(child);
					child.setParent(null);
				}
			}
		}
	}

	@Override
	public void resort() {
		View v = getLayout();
		if (v instanceof TiCompositeLayout) {
			((TiCompositeLayout) v).resort();
		}
	}



	public void finishRefresh() {
		View view = this.getNativeView();
		if (view == null || !(view instanceof SwipeRefreshLayout)) {
			return;
		}
		SwipeRefreshLayout layout = (SwipeRefreshLayout) view;
		if (layout.isEnabled() && layout.isRefreshing()) {
			layout.setRefreshing(false);
		}
	}

	private ViewGroup getNativeScrollView() {
		return this.scrollView;
	}
}
