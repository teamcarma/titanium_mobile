/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.tabgroup;

import java.util.HashMap;
import java.util.Map.Entry;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.OnKeyboardVisibilityChangeListener;
import org.appcelerator.titanium.view.TiCompositeLayout;

import ti.modules.titanium.ui.TabGroupProxy;
import ti.modules.titanium.ui.TabProxy;
import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;

/**
 * Tab group implementation using the TabWidget/TabHost.
 * If the target SDK version and device framework level is
 * bellow 11 we fall back to using the TabWidget for displaying
 * the tabs. Each window provides an activity which the
 * TabHost starts when that window's tab is selected.
 */
public class TiUITabHostGroup extends TiUIAbstractTabGroup implements OnTabChangeListener, TabContentFactory, OnKeyboardVisibilityChangeListener {

	private static final String TAG = "TiUITabHostGroup";

	private static final String SEPARATOR_TAG = "tab_separator";

	private TabHost tabHost;
	private final HashMap<String, TiUITabHostTab> tabViews = new HashMap<String, TiUITabHostTab>();

	public TiUITabHostGroup(TabGroupProxy proxy, TiBaseActivity activity) {
		super(proxy, activity);
		setupTabHost();

		TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
		params.autoFillsHeight = true;
		params.autoFillsWidth = true;

		((TiCompositeLayout) activity.getLayout()).addView(tabHost, params);

		activity.addKeyboardListener(this);
	}

	private void setupTabHost() {
		Context context = TiApplication.getInstance().getApplicationContext();
		LayoutParams params;

		tabHost = new TabHost(context, null);
		tabHost.setId(android.R.id.tabhost);
		params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		tabHost.setLayoutParams(params);

		LinearLayout container = new LinearLayout(context);
		container.setOrientation(LinearLayout.VERTICAL);
		params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		tabHost.addView(container, params);

		FrameLayout tabcontent = new FrameLayout(context);
		tabcontent.setId(android.R.id.tabcontent);
		params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
		container.addView(tabcontent, params);

		View separator = new View(context);
		separator.setTag(SEPARATOR_TAG);
		container.addView(separator, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 0.0f));

		TabWidget tabWidget = new TabWidget(context);
		tabWidget.setId(android.R.id.tabs);
		tabWidget.setOrientation(LinearLayout.HORIZONTAL);
		params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0.0f);

		container.addView(tabWidget, params);

		tabHost.setup();

		tabWidget.setDividerDrawable(android.R.color.transparent);

		// For view properties (backgroundColor) and methods (animate())
		// to work correctly we will use the TabHost as the "native" view.
		setNativeView(tabHost);
	}

	@Override
	public void addTab(final TabProxy tab) {
		TabWidget tabWidget = tabHost.getTabWidget();

		final int currentTabIndex = tabHost.getTabWidget().getTabCount();

		TiUITabHostTab tabView = new TiUITabHostTab(tab);
		tabViews.put(tabView.id, tabView);

		TabSpec tabSpec = tabHost.newTabSpec(tabView.id);
		tabView.setupTabSpec(tabSpec);
		tabSpec.setContent(this);
		tabHost.addTab(tabSpec);

		tabView.setIndicatorView(tabWidget.getChildTabViewAt(currentTabIndex));

		// TabHost will automatically select the first tab.
		// We must suppress the tab selection callback when this selection
		// happens to comply with the abstract tab group contract.
		// We will only hook up the tab listener after the first tab is added.
		if (currentTabIndex == 0) {
			tabHost.setOnTabChangedListener(this);
		}

		tabHost.getTabWidget().getChildTabViewAt(currentTabIndex).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				TabProxy previousTab = getSelectedTab();
				// The default click listener for tab views is responsible for changing the selected tabs.
				tabHost.setCurrentTab(currentTabIndex);
				if (previousTab == tab) {
					tab.fireEvent(TiC.EVENT_TAB_RESET, null);
				}
				tab.fireEvent(TiC.EVENT_CLICK, null);
			}
		});
	}

	@Override
	public void removeTab(TabProxy tab) {
		// Not supported.
		Log.w(TAG, "Tab removal not supported by this group.");
	}

	@Override
	public void selectTab(TabProxy tab) {
		TiUITabHostTab tabView = (TiUITabHostTab) tab.peekView();
		tabHost.setCurrentTabByTag(tabView.id);
	}

	@Override
	public TabProxy getSelectedTab() {
		String id = tabHost.getCurrentTabTag();
		TiUITabHostTab tabView = tabViews.get(id);
		return (TabProxy) tabView.getProxy();
	}

	@Override
	public void onTabChanged(String id) {
		TabGroupProxy tabGroupProxy = ((TabGroupProxy) proxy);
		TiUITabHostTab tab = tabViews.get(id);
		tabGroupProxy.onTabSelected((TabProxy) tab.getProxy());
	}

	@Override
	public View createTabContent(String tag) {
		TiUITabHostTab tabView = tabViews.get(tag);
		return tabView.getContentView();
	}

	/**
	 * {@inheritDoc}
	 * @see ti.modules.titanium.ui.widget.tabgroup.TiUIAbstractTabGroup#processProperties(org.appcelerator.kroll.KrollDict)
	 */
	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
		if (d.containsKey(TiC.PROPERTY_SEPARATOR_WIDTH)) {
			TiDimension separatorWidth = TiConvert.toTiDimension(d.get(TiC.PROPERTY_SEPARATOR_WIDTH), TiDimension.TYPE_HEIGHT);
			View separator = this.tabHost.findViewWithTag(SEPARATOR_TAG);
			LayoutParams params = separator.getLayoutParams();
			params.height = separatorWidth.getAsPixels(this.tabHost);
			separator.setLayoutParams(params);
		}
		if (d.containsKey(TiC.PROPERTY_SEPARATOR_COLOR)) {
			this.tabHost.findViewWithTag(SEPARATOR_TAG).setBackgroundColor(TiConvert.toColor(d.get(TiC.PROPERTY_SEPARATOR_COLOR).toString()));
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_TABS_BACKGROUND_COLOR)) {
			String activeTab = tabHost.getCurrentTabTag();
			int color = TiConvert.toColor(newValue.toString());

			// Update each inactive tab to the new background color.
			for (Entry<String, TiUITabHostTab> e : tabViews.entrySet()) {
				if (e.getKey() == activeTab) {
					continue;
				}
				e.getValue().setBackgroundColor(color);
			}

		} else if (key.equals(TiC.PROPERTY_TITLE)) {
			Activity activity = proxy.getActivity();
			if (activity != null) {
				activity.setTitle(TiConvert.toString(newValue));
			}
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	@Override
	public void onFocusChange(final View v, boolean hasFocus) {
		if (hasFocus) {
			TiMessenger.postOnMain(new Runnable() {

				public void run() {
					TiUIHelper.requestSoftInputChange(proxy, v);
				}
			});
		}
		// Don't fire focus/blur events here because the the events will be fired through event bubbling.
	}

	/**
	 * {@inheritDoc}
	 * @see org.appcelerator.titanium.view.TiUIView#release()
	 */
	@Override
	public void release() {
		super.release();
	}

	/**
	 * {@inheritDoc}
	 * @see org.appcelerator.titanium.view.OnKeyboardVisibilityChangeListener#onKeyboardVisibilityChange(boolean)
	 */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public void onKeyboardVisibilityChange(final boolean visible) {
		final TabWidget tabWidget;
		if (this.tabHost == null || (tabWidget = this.tabHost.getTabWidget()) == null) {
			return;
		}
		if (visible || (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1)) {
			tabWidget.setVisibility(visible ? View.GONE : View.VISIBLE);
			return;
		}
		ViewPropertyAnimator animation = tabWidget.animate();
		animation.setListener(new AnimatorListenerAdapter() {

			@Override
			public void onAnimationEnd(Animator animation) {
				tabWidget.setVisibility(visible ? View.GONE : View.VISIBLE);
			}
		});
		animation.y(visible ? tabWidget.getTop() + tabWidget.getHeight() : tabWidget.getTop());
	}

}
