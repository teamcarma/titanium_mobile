/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.tabgroup;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;

import ti.modules.titanium.ui.TabProxy;
import ti.modules.titanium.ui.android.CustomProgressView;
import ti.modules.titanium.ui.animations.CurvedLineGroupAnimator;
import ti.modules.titanium.ui.broadcastview.BroadcastAnimateView;
import ti.modules.titanium.ui.drawable.CurvedLineDrawable;
import ti.modules.titanium.ui.utils.Direction;
import android.R.attr;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class TiUITabHostTab extends TiUIAbstractTab {

	final String id = Integer.toHexString(hashCode());

	private static final String TAG = "TiUITabHostTab";
	private static final String SEPARATOR_TAG = "tab_item_separator";
	private static final String BROADCAST_VIEW_LTR_TAG = "tab_broadcastview_ltr";
	private static final String BROADCAST_VIEW_RTL_TAG = "tab_broadcastview_rtl";

	private View indicatorView;
	private Drawable defaultTabBackground;
	private ValueAnimator mSelectionAnimation;

	public TiUITabHostTab(TabProxy proxy) {
		super(proxy);
		proxy.setModelListener(this);
	}

	public void setBackgroundColor(int color) {
		indicatorView.setBackgroundColor(color);
	}

	private boolean isOnlyIconOrTitle() {
		KrollDict properties = proxy.getProperties();
		String title = properties.optString(TiC.PROPERTY_TITLE, "");
		Object icon = properties.get(TiC.PROPERTY_ICON);
		return title == null || icon == null;
	}

	void setupTabSpec(TabSpec spec) {
		KrollDict properties = proxy.getProperties();

		String title = properties.optString(TiC.PROPERTY_TITLE, "");
		Object icon = properties.get(TiC.PROPERTY_ICON);

		if (title == null || icon == null) {
			spec.setIndicator(title, icon != null ? TiUIHelper.getResourceDrawable(icon) : null);
			return;
		}

		spec.setIndicator(createTabView(title));
	}

	private View createTabView(String title) {
		Context context = this.getProxy().getActivity();

		LinearLayout layout = new LinearLayout(context);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, getAsPixel(48), 1.0f);
		layout.setLayoutParams(params);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(0, 0, 0, 0);

		CustomProgressView separator = new CustomProgressView(context);
		separator.setTag(SEPARATOR_TAG);
		separator.setTextColor(this.createColorStateList(Color.TRANSPARENT, ((TabProxy) this.proxy).getActiveTabTextColor()));
		LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, this.getAsPixel(3));
		separatorParams.bottomMargin = this.getAsPixel(3);
		layout.addView(separator, separatorParams);

		createIconView(context, layout);

		TextView text = new TextView(context);
		text.setId(android.R.id.title);
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		textParams.gravity = Gravity.CENTER;
		text.setLayoutParams(textParams);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		text.setTypeface(Typeface.DEFAULT_BOLD);
		text.setTextColor(createColorStateList());
		text.setText(title);
		layout.addView(text);

		return layout;
	}

	@SuppressWarnings("deprecation")
	private void createIconView(Context context, LinearLayout layout) {
		KrollDict properties = this.getProxy().getProperties();

		LinearLayout innerLayout = new LinearLayout(context);
		LinearLayout.LayoutParams innerLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		innerLayoutParams.gravity = Gravity.CENTER;

		View leftBroadcastView = createBroadcastView(context, Direction.RIGHT_TO_LEFT);
		innerLayout.addView(leftBroadcastView);

		TextView iconView = new TextView(context);
		iconView.setId(android.R.id.icon);
		iconView.setBackgroundDrawable(createIcon());
		iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		iconView.setTypeface(Typeface.DEFAULT_BOLD);
		iconView.setTextColor(createColorStateList(properties.optColor(TiC.PROPERTY_IMAGE_TEXTUAL_COLOR, Color.BLACK),
				properties.optColor(TiC.PROPERTY_IMAGE_TEXTUAL_SELECTED_COLOR, Color.WHITE)));
		iconView.setText(properties.optString(TiC.PROPERTY_TEXT, ""));
		iconView.setLayoutParams(new LayoutParams(this.getAsPixel(24), this.getAsPixel(24)));
		iconView.setGravity(Gravity.CENTER);
		innerLayout.addView(iconView);

		View rightBroadcastView = createBroadcastView(context, Direction.LEFT_TO_RIGHT);
		innerLayout.addView(rightBroadcastView);

		if (!properties.optBoolean(TiC.PROPERTY_ANIMATABLE, false)) {
			leftBroadcastView.setVisibility(View.GONE);
			rightBroadcastView.setVisibility(View.GONE);
		}
		if (properties.optBoolean(TiC.PROPERTY_ANIMATING, false)) {
			this.animate((BroadcastAnimateView) leftBroadcastView, true);
			this.animate((BroadcastAnimateView) rightBroadcastView, true);
		}

		layout.addView(innerLayout, innerLayoutParams);
	}

	private BroadcastAnimateView createBroadcastView(Context context, Direction direction) {
		int viewWidth = this.getAsPixel(24), drawableWidth = this.getAsPixel(18), height = viewWidth;
		direction = direction != null ? direction : Direction.LEFT_TO_RIGHT;

		BroadcastAnimateView view = new BroadcastAnimateView(context);
		view.setTag(direction == Direction.RIGHT_TO_LEFT ? BROADCAST_VIEW_RTL_TAG : BROADCAST_VIEW_LTR_TAG);
		view.setLayoutParams(new LayoutParams(viewWidth, height));
		view.setTextColor(this.createColorStateList());

		view.getDrawable().setWidth(drawableWidth);
		view.getDrawable().setHeight(height);
		view.getDrawable().setDirection(direction);

		int startAngle = direction == Direction.RIGHT_TO_LEFT ? 150 : -30, sweepAngle = 60, innerRadius = this.getAsPixel(18), thickness = this.getAsPixel(2);
		if (direction == Direction.RIGHT_TO_LEFT) {
			view.addCurvedLine(new CurvedLineDrawable(startAngle, sweepAngle, innerRadius, thickness), this.getAsPixel(16), 0);
			view.addCurvedLine(new CurvedLineDrawable(startAngle, sweepAngle, innerRadius, thickness), this.getAsPixel(10), 0);
			view.addCurvedLine(new CurvedLineDrawable(startAngle, sweepAngle, innerRadius, thickness), this.getAsPixel(4), 0, true);
		} else {
			view.addCurvedLine(new CurvedLineDrawable(startAngle, sweepAngle, innerRadius, thickness), this.getAsPixel(2), 0);
			view.addCurvedLine(new CurvedLineDrawable(startAngle, sweepAngle, innerRadius, thickness), this.getAsPixel(8), 0);
			view.addCurvedLine(new CurvedLineDrawable(startAngle, sweepAngle, innerRadius, thickness), this.getAsPixel(14), 0, true);
		}

		return view;
	}

	private int getAsPixel(int dip) {
		DisplayMetrics metrics = getAndroidResources().getDisplayMetrics();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, metrics);
	}

	void setIndicatorView(View indicatorView) {
		this.indicatorView = indicatorView;
		defaultTabBackground = indicatorView.getBackground();

		// Initialize custom background color of tab if provided.
		int tabBackgroundColor = ((TabProxy) proxy).getTabColor();
		if (tabBackgroundColor != 0) {
			setBackgroundColor(tabBackgroundColor);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onSelectionChange(boolean selected) {
		if (this.isOnlyIconOrTitle()) {
			TabProxy tabProxy = (TabProxy) proxy;
			int backgroundColor = selected ? tabProxy.getActiveTabColor() : tabProxy.getTabColor();
			if (backgroundColor == 0) {
				// Restore to default background color.
				indicatorView.setBackgroundDrawable(defaultTabBackground);
			} else {
				setBackgroundColor(backgroundColor);
			}
		} else {
			// Change separator
			CustomProgressView separator = (CustomProgressView) this.indicatorView.findViewWithTag(SEPARATOR_TAG);
			if (!selected) {
				separator.setProgress(0);
			} else {
				startSelectAnimation(separator);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void startSelectAnimation(CustomProgressView separator) {
		ValueAnimator mAnimation = this.getSelectionAnimation();
		if (mAnimation != null && !mAnimation.isRunning()) {
			mAnimation.start();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private ValueAnimator getSelectionAnimation() {
		if (this.mSelectionAnimation == null) {
			View view = this.indicatorView.findViewWithTag(SEPARATOR_TAG);
			if (view == null || !(view instanceof CustomProgressView)) {
				return null;
			}
			CustomProgressView mProgress = (CustomProgressView) view;
			this.mSelectionAnimation = ObjectAnimator.ofInt(mProgress, "progress", mProgress.getProgress(), 100);
		}
		return this.mSelectionAnimation;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_TITLE)) {
			View titleView = indicatorView.findViewById(android.R.id.title);
			if (titleView instanceof TextView) {
				((TextView) titleView).setText(newValue != null ? TiConvert.toString(newValue) : "");
			} else {
				Log.d(TAG, "Did not find a title View inside indicatorView to update ", Log.DEBUG_MODE);
			}
		}
		if (TiC.PROPERTY_ANIMATABLE.equals(key)) {
			int visibility = TiConvert.toBoolean(newValue, false) ? View.VISIBLE : View.GONE;
			setVisibility(BROADCAST_VIEW_RTL_TAG, visibility);
			setVisibility(BROADCAST_VIEW_LTR_TAG, visibility);
		}
		if (TiC.PROPERTY_ANIMATING.equals(key)) {
			boolean animate = TiConvert.toBoolean(newValue, false);
			this.animate(BROADCAST_VIEW_LTR_TAG, animate);
			this.animate(BROADCAST_VIEW_RTL_TAG, animate);
		}

		View view = indicatorView.findViewById(android.R.id.icon);
		if (!(view instanceof TextView)) {
			Log.d(TAG, "Did not find a text View inside indicatorView to update ", Log.DEBUG_MODE);
			return;
		}

		TextView icon = (TextView) view;

		if (key.equals(TiC.PROPERTY_ICON) || key.equals(TiC.PROPERTY_ACTIVE_ICON)) {
			Drawable imageDrawable = ((TextView) icon).getBackground();
			if (imageDrawable != null && imageDrawable instanceof StateListDrawable) {
				Drawable drawable = this.getDrawable(TiC.PROPERTY_ICON, null);
				Drawable activeIcon = this.getDrawable(TiC.PROPERTY_ACTIVE_ICON, null);
				icon.setBackgroundDrawable(this.createIconStateDrawable(drawable, activeIcon));
			} else {
				icon.setBackgroundDrawable(this.getDrawable(TiC.PROPERTY_ICON, null));
			}

		}
		if (key.equals(TiC.PROPERTY_IMAGE_TEXTUAL_COLOR) || key.equals(TiC.PROPERTY_IMAGE_TEXTUAL_SELECTED_COLOR)) {
			int textColor = proxy.getProperties().optColor(TiC.PROPERTY_IMAGE_TEXTUAL_COLOR, Color.BLACK);
			int textSelectedColor = proxy.getProperties().optColor(TiC.PROPERTY_IMAGE_TEXTUAL_SELECTED_COLOR, Color.BLACK);
			icon.setTextColor(this.createColorStateList(textColor, textSelectedColor));
		}
		if (key.equals(TiC.PROPERTY_TEXT)) {
			icon.setText(TiConvert.toString(newValue));
		}
	}

	private void setVisibility(String tag, int visibility) {
		View view = indicatorView.findViewWithTag(tag);
		if (view == null) {
			return;
		}
		view.setVisibility(visibility);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void animate(String tag, boolean willAnimate) {
		View view = indicatorView.findViewWithTag(tag);
		if (view == null || !(view instanceof BroadcastAnimateView)) {
			return;
		}
		animate((BroadcastAnimateView) view, willAnimate);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void animate(BroadcastAnimateView view, boolean willAnimate) {
		CurvedLineGroupAnimator animation = view.broadcastAnimate();
		if (!animation.isRunning() && willAnimate) {
			animation.setDuration(2500);
			animation.setRepeatCount(ValueAnimator.INFINITE);
			animation.start();
		}
		if (animation.isRunning() && !willAnimate) {
			animation.end();
		}
	}

	private Drawable getDrawable(String key, Drawable defaultValue) {
		if (!proxy.hasProperty(key)) {
			return defaultValue;
		}
		return TiUIHelper.getResourceDrawable(proxy.getProperty(key));
	}

	private StateListDrawable createIcon() {
		Object icon = this.proxy.getProperty(TiC.PROPERTY_ICON);
		Object activeIcon = this.proxy.getProperty(TiC.PROPERTY_ACTIVE_ICON);
		return createIconStateDrawable(TiUIHelper.getResourceDrawable(icon != null ? icon : icon),
				TiUIHelper.getResourceDrawable(activeIcon != null ? activeIcon : null));
	}

	private ColorStateList createColorStateList() {
		int color = ((TabProxy) this.proxy).getTabTextColor(), selectedColor = ((TabProxy) this.proxy).getActiveTabTextColor();
		if (color == 0) {
			color = Color.BLACK;
		}
		if (selectedColor == 0) {
			selectedColor = color;
		}
		return this.createColorStateList(color, selectedColor);
	}

	private ColorStateList createColorStateList(int color, int selectedColor) {
		int[][] states = new int[8][];
		int[] colors = new int[8];

		states[0] = new int[] { -attr.state_focused, -attr.state_pressed, -attr.state_selected };
		colors[0] = color;

		states[1] = new int[] { -attr.state_focused, -attr.state_pressed, attr.state_selected };
		colors[1] = selectedColor;

		states[2] = new int[] { attr.state_focused, -attr.state_pressed, -attr.state_selected };
		colors[2] = selectedColor;

		states[3] = new int[] { attr.state_focused, -attr.state_pressed, attr.state_selected };
		colors[3] = selectedColor;

		states[4] = new int[] { -attr.state_focused, attr.state_pressed, -attr.state_selected };
		colors[4] = selectedColor;

		states[5] = new int[] { -attr.state_focused, attr.state_pressed, attr.state_selected };
		colors[5] = selectedColor;

		states[6] = new int[] { attr.state_focused, attr.state_pressed, -attr.state_selected };
		colors[6] = selectedColor;

		states[7] = new int[] { attr.state_focused, attr.state_pressed, -attr.state_selected };
		colors[7] = selectedColor;

		return new ColorStateList(states, colors);
	}

	private StateListDrawable createIconStateDrawable(Drawable src, Drawable activeSource) {
		StateListDrawable drawable = new StateListDrawable();

		drawable.addState(new int[] { -attr.state_focused, -attr.state_pressed, -attr.state_selected }, src);
		drawable.addState(new int[] { -attr.state_focused, -attr.state_pressed, attr.state_selected }, activeSource);

		drawable.addState(new int[] { attr.state_focused, -attr.state_pressed, -attr.state_selected }, activeSource);
		drawable.addState(new int[] { attr.state_focused, -attr.state_pressed, attr.state_selected }, activeSource);

		drawable.addState(new int[] { -attr.state_focused, attr.state_pressed, -attr.state_selected }, activeSource);
		drawable.addState(new int[] { -attr.state_focused, attr.state_pressed, attr.state_selected }, activeSource);

		drawable.addState(new int[] { attr.state_focused, attr.state_pressed, -attr.state_selected }, activeSource);
		drawable.addState(new int[] { attr.state_focused, attr.state_pressed, attr.state_selected }, activeSource);

		return drawable;
	}
}
