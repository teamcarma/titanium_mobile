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
import android.R.attr;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class TiUITabHostTab extends TiUIAbstractTab {

	final String id = Integer.toHexString(hashCode());

	private View indicatorView;
	private Drawable defaultTabBackground;

	private static final String TAG = "TiUITabHostTab";

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

		Object activeIcon = properties.get(TiC.PROPERTY_ACTIVE_ICON);
		spec.setIndicator(this.createTabView(title, icon, activeIcon));
	}

	private View createTabView(String title, Object iconPath, Object activeIconPath) {
		Context context = this.getProxy().getActivity();

		LinearLayout layout = new LinearLayout(context);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, getAsPixel(48), 1.0f);
		layout.setLayoutParams(params);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(getAsPixel(4), getAsPixel(6), getAsPixel(4), 0);

		ImageView icon = new ImageView(context);
		icon.setId(android.R.id.icon);
		LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(this.getAsPixel(24), this.getAsPixel(24));
		iconParams.gravity = Gravity.CENTER;
		icon.setLayoutParams(iconParams);
		icon.setImageDrawable(this.getImageDrawable(TiUIHelper.getResourceDrawable(iconPath), TiUIHelper.getResourceDrawable(activeIconPath)));
		layout.addView(icon);

		TextView text = new TextView(context);
		text.setId(android.R.id.title);
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		textParams.gravity = Gravity.CENTER;
		text.setLayoutParams(textParams);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		text.setText(title);
		layout.addView(text);

		return layout;
	}

	private StateListDrawable getImageDrawable(Drawable src, Drawable activeSource) {
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

	private int getAsPixel(int dip) {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, metrics);
	}

	private Resources getResources() {
		if (this.getProxy() == null || this.getProxy().getActivity() == null) {
			return Resources.getSystem();
		}
		return this.getProxy().getActivity().getResources();
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
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_TITLE)) {
			View titleView = indicatorView.findViewById(android.R.id.title);
			if (titleView instanceof TextView) {
				if (newValue != null) {
					((TextView) titleView).setText(TiConvert.toString(newValue));
				} else {
					((TextView) titleView).setText("");
				}
			} else {
				Log.d(TAG, "Did not find a title View inside indicatorView to update ", Log.DEBUG_MODE);
			}
		}
		if (key.equals(TiC.PROPERTY_ICON)) {
			View iconView = indicatorView.findViewById(android.R.id.icon);
			if (iconView instanceof ImageView) {
				Drawable icon = null;
				if (newValue != null) {
					icon = TiUIHelper.getResourceDrawable(newValue);
				}
				((ImageView) iconView).setImageDrawable(icon);
			} else {
				Log.d(TAG, "Did not find a image View inside indicatorView to update ", Log.DEBUG_MODE);
			}
		}
	}

}
