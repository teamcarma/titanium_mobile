/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiColorHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import ti.modules.titanium.ui.SearchBarProxy;
import ti.modules.titanium.ui.TableViewProxy;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar;
import ti.modules.titanium.ui.widget.searchview.TiUISearchView;
import ti.modules.titanium.ui.widget.tableview.TableViewModel;
import ti.modules.titanium.ui.widget.tableview.TiTableView;
import ti.modules.titanium.ui.widget.tableview.TiTableView.OnItemClickedListener;
import ti.modules.titanium.ui.widget.tableview.TiTableView.OnItemLongClickedListener;
import android.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class TiUITableView extends TiUIView implements OnItemClickedListener, OnItemLongClickedListener, OnScrollListener, OnLifecycleEvent {

	private static final String TAG = "TitaniumTableView";

	protected TiTableView tableView;

	private boolean isRefreshEnabled = false;

	public TiUITableView(TiViewProxy proxy) {
		super(proxy);
		getLayoutParams().autoFillsHeight = true;
		getLayoutParams().autoFillsWidth = true;
	}

	@Override
	public void onClick(KrollDict data)
	{
		proxy.fireEvent(TiC.EVENT_CLICK, data);
	}

	@Override
	public boolean onLongClick(KrollDict data)
	{
		return proxy.fireEvent(TiC.EVENT_LONGCLICK, data);
	}

	public void setModelDirty()
	{
		tableView.getTableViewModel().setDirty();
	}
	
	public TableViewModel getModel()
	{
		return tableView.getTableViewModel();
	}

	public void updateView()
	{
		tableView.dataSetChanged();
	}

	public void scrollToIndex(final int index)
	{
		tableView.getListView().setSelection(index);
	}

	public void scrollToTop(final int index)
	{
		tableView.getListView().setSelectionFromTop(index, 0);
	}
	
	public void selectRow(final int row_id)
	{
		tableView.getListView().setSelection(row_id);
	}

	public TiTableView getTableView()
	{
		return tableView;
	}

	public ListView getListView()
	{
		return tableView.getListView();
	}
	@SuppressLint("NewApi")
	@Override
	public void processProperties(KrollDict d)
	{
		View nativeView;
		// Don't create a new table view if one already exists
		if (tableView == null) {
			tableView = new TiTableView((TableViewProxy) proxy);
		}
		Activity activity = proxy.getActivity();
		if (activity instanceof TiBaseActivity) {
			((TiBaseActivity) activity).addOnLifecycleEventListener(this);
		}

		boolean clickable = true;
		if (d.containsKey(TiC.PROPERTY_TOUCH_ENABLED)) {
			clickable = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_TOUCH_ENABLED), true);
		}
		if (clickable) {
			tableView.setOnItemClickListener(this);
			tableView.setOnItemLongClickListener(this);
		}
		
		ListView list = getListView();
		if (d.containsKey(TiC.PROPERTY_FOOTER_DIVIDERS_ENABLED)) {
			boolean enabled = TiConvert.toBoolean(d, TiC.PROPERTY_FOOTER_DIVIDERS_ENABLED, false);
			list.setFooterDividersEnabled(enabled);
		} else {
			list.setFooterDividersEnabled(false);
		}
		
		if (d.containsKey(TiC.PROPERTY_HEADER_DIVIDERS_ENABLED)) {
			boolean enabled = TiConvert.toBoolean(d, TiC.PROPERTY_HEADER_DIVIDERS_ENABLED, false);
			list.setHeaderDividersEnabled(enabled);
		} else {
			list.setHeaderDividersEnabled(false);
		}
	
		tableView.setOnScrollListener(this);


		if (d.containsKey(TiC.PROPERTY_SEARCH)) {
			TiViewProxy searchView = (TiViewProxy) d.get(TiC.PROPERTY_SEARCH);
			TiUIView search = searchView.getOrCreateView();
			if (searchView instanceof SearchBarProxy) {
				((TiUISearchBar)search).setOnSearchChangeListener(tableView);
			} else {
				((TiUISearchView)search).setOnSearchChangeListener(tableView);
			}
			if (!(d.containsKey(TiC.PROPERTY_SEARCH_AS_CHILD) && !TiConvert.toBoolean(d.get(TiC.PROPERTY_SEARCH_AS_CHILD)))) {


				search.getNativeView().setId(102);

				RelativeLayout layout = new RelativeLayout(proxy.getActivity());
				layout.setGravity(Gravity.NO_GRAVITY);
				layout.setPadding(0, 0, 0, 0);

				RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.FILL_PARENT,
						RelativeLayout.LayoutParams.FILL_PARENT);
				p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

				TiDimension rawHeight;
				if (searchView.hasProperty("height")) {
					rawHeight = TiConvert.toTiDimension(searchView.getProperty("height"), 0);
				} else {
					rawHeight = TiConvert.toTiDimension("52dp", 0);
				}
				p.height = rawHeight.getAsPixels(layout);

				layout.addView(search.getNativeView(), p);

				p = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.FILL_PARENT,
						RelativeLayout.LayoutParams.FILL_PARENT);
				p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				p.addRule(RelativeLayout.BELOW, 102);
				layout.addView(tableView, p);
				nativeView = layout;
			} else {
				nativeView = tableView;
			}
		} else {
			nativeView = tableView;
		}

		if (d.containsKey(TiC.PROPERTY_FILTER_ATTRIBUTE)) {
			tableView.setFilterAttribute(TiConvert.toString(d, TiC.PROPERTY_FILTER_ATTRIBUTE));
		} else {
			// Default to title to match iPhone default.
			proxy.setProperty(TiC.PROPERTY_FILTER_ATTRIBUTE, TiC.PROPERTY_TITLE, false);
			tableView.setFilterAttribute(TiC.PROPERTY_TITLE);
		}

		if (d.containsKey(TiC.PROPERTY_OVER_SCROLL_MODE)) {
			if (Build.VERSION.SDK_INT >= 9) {
				getListView().setOverScrollMode(TiConvert.toInt(d.get(TiC.PROPERTY_OVER_SCROLL_MODE), View.OVER_SCROLL_ALWAYS));
			}
		}
		boolean filterCaseInsensitive = true;
		if (d.containsKey(TiC.PROPERTY_FILTER_CASE_INSENSITIVE)) {
			filterCaseInsensitive = TiConvert.toBoolean(d, TiC.PROPERTY_FILTER_CASE_INSENSITIVE);
		}
		tableView.setFilterCaseInsensitive(filterCaseInsensitive);
		boolean filterAnchored = false;
		if (d.containsKey(TiC.PROPERTY_FILTER_ANCHORED)) {
			filterAnchored = TiConvert.toBoolean(d, TiC.PROPERTY_FILTER_ANCHORED);
		}
		tableView.setFilterAnchored(filterAnchored);
		//super.processProperties(d);

		SwipeRefreshLayout refreshLayout = new SwipeRefreshLayout(proxy.getActivity());
		refreshLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		refreshLayout.addView(nativeView);
		this.setNativeView(refreshLayout);

		super.processProperties(d);

		refreshLayout.setOnRefreshListener(newOnRefreshListener());
		if (d.containsKey(TiC.PROPERTY_REFRESH_PROGRESSBAR_COLOR)) {
			// TODO Custom the progress bar's colors.
		} else {
			// TODO: add back once we know the method exists
			refreshLayout.setColorSchemeColors(TiColorHelper.HOLO_BLUE_BRIGHT, TiColorHelper.HOLO_GREEN_LIGHT, TiColorHelper.HOLO_ORANGE_LIGHT,
					TiColorHelper.HOLO_RED_LIGHT);
		}
		if (d.containsKey(TiC.PROPERTY_REFRESHABLE)) {
			this.isRefreshEnabled = TiConvert.toBoolean(d, TiC.PROPERTY_REFRESHABLE);
		} else if (d.containsKey(TiC.PROPERTY_REFRESHABLE_DEPRECATED)) {
			this.isRefreshEnabled = TiConvert.toBoolean(d, TiC.PROPERTY_REFRESHABLE_DEPRECATED);
		} else {
			this.isRefreshEnabled = false;
		}
		refreshLayout.setEnabled(this.isRefreshEnabled);
		if (d.containsKey(TiC.PROPERTY_ENABLED) && nativeView != null) {
			nativeView.setEnabled(TiConvert.toBoolean(d, TiC.PROPERTY_ENABLED, true));
		}
	}


	private OnRefreshListener newOnRefreshListener() {
		return new SwipeRefreshLayout.OnRefreshListener() {

			@Override
			public void onRefresh() {
				TiUITableView.this.onRefresh();
			}
		};
	}

	protected void onRefresh() {
		this.fireEvent(TiC.EVENT_REFRESHED, KrollDict.EMPTY);
		this.fireEvent(TiC.EVENT_REFRESHED_DEPRECATED, KrollDict.EMPTY);
	}

	@Override
	public void onResume(Activity activity) {
		if (tableView != null) {
			tableView.dataSetChanged();
		}
	}

	@Override public void onStop(Activity activity) {}
	@Override public void onCreate(Activity activity, Bundle savedInstanceState) {}
	@Override public void onStart(Activity activity) {}
	@Override public void onPause(Activity activity) {}
	@Override public void onDestroy(Activity activity) {}

	@Override
	public void release()
	{
		// Release search bar if there is one
		if (nativeView instanceof RelativeLayout) {
			((RelativeLayout) nativeView).removeAllViews();
			TiViewProxy searchView = (TiViewProxy) (proxy.getProperty(TiC.PROPERTY_SEARCH));
			searchView.release();
		}

		if (tableView != null) {
			tableView.release();
			tableView  = null;
		}
		if (proxy != null && proxy.getActivity() != null) {
			((TiBaseActivity)proxy.getActivity()).removeOnLifecycleEventListener(this);
		}
		nativeView  = null;
		super.release();
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Property: " + key + " old: " + oldValue + " new: " + newValue, Log.DEBUG_MODE);
		}

		if (key.equals(TiC.PROPERTY_TOUCH_ENABLED)) {
			boolean clickable = TiConvert.toBoolean(newValue);
			if (clickable) {
				tableView.setOnItemClickListener(this);
				tableView.setOnItemLongClickListener(this);

			} else {
				tableView.setOnItemClickListener(null);
				tableView.setOnItemLongClickListener(null);
			}

		}

		if (key.equals(TiC.PROPERTY_SEPARATOR_COLOR)) {
			tableView.setSeparatorColor(TiConvert.toString(newValue));
		} else if (TiC.PROPERTY_OVER_SCROLL_MODE.equals(key)) {
			if (Build.VERSION.SDK_INT >= 9) {
				getListView().setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
			}
		} else if (TiC.PROPERTY_MIN_ROW_HEIGHT.equals(key)) {
			updateView();
		} else if (TiC.PROPERTY_HEADER_VIEW.equals(key)) {
			if (oldValue != null) {
				tableView.removeHeaderView((TiViewProxy) oldValue);
			}
			tableView.setHeaderView();
		} else if (TiC.PROPERTY_FOOTER_VIEW.equals(key)) {
			if (oldValue != null) {
				tableView.removeFooterView((TiViewProxy) oldValue);
			}
			tableView.setFooterView();
		} else if (key.equals(TiC.PROPERTY_FILTER_ANCHORED)) {
			tableView.setFilterAnchored(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_FILTER_CASE_INSENSITIVE)) {
			tableView.setFilterCaseInsensitive(TiConvert.toBoolean(newValue));
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
		if (TiC.PROPERTY_REFRESHABLE.equals(key) || TiC.PROPERTY_REFRESHABLE_DEPRECATED.equals(key)) {
			SwipeRefreshLayout layout = (SwipeRefreshLayout) this.getNativeView();
			layout.setEnabled(TiConvert.toBoolean(newValue));
		}
		if (TiC.PROPERTY_ENABLED.equals(key)) {
			SwipeRefreshLayout layout = (SwipeRefreshLayout) this.getNativeView();
			if (layout.getChildCount() == 1) {
				layout.getChildAt(0).setEnabled(TiConvert.toBoolean(newValue, true));
			}
		}
	}

	@Override
	public void registerForTouch() {
		registerForTouch(tableView.getListView());
	}

	/**
	 * This
	 */
	public void finishRefresh() {
		View view = this.getNativeView();
		if (view == null || !(view instanceof SwipeRefreshLayout)) {
			return;
		}
		SwipeRefreshLayout layout = (SwipeRefreshLayout) view;
		if (layout.isRefreshing()) {
			layout.setRefreshing(false);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see android.widget.AbsListView.OnScrollListener#onScrollStateChanged(android.widget.AbsListView, int)
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Don't do anything.
	}

	/**
	 * {@inheritDoc}
	 * @see android.widget.AbsListView.OnScrollListener#onScroll(android.widget.AbsListView, int, int, int)
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		View nativeView = this.getNativeView();
		if (nativeView == null) {
			return;
		}
		SwipeRefreshLayout layout = (SwipeRefreshLayout) nativeView;
		int topRowVerticalPosition = (view == null || view.getChildCount() == 0) ? 0 : view.getChildAt(0).getTop();
		layout.setEnabled(this.isRefreshEnabled && topRowVerticalPosition >= 0);
	}

	
}
