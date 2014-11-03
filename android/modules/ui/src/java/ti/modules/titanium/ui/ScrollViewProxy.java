/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIScrollView;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;

@SuppressWarnings("deprecation")
@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = { TiC.PROPERTY_CONTENT_HEIGHT, TiC.PROPERTY_CONTENT_WIDTH,
		TiC.PROPERTY_SHOW_HORIZONTAL_SCROLL_INDICATOR, TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR, TiC.PROPERTY_SCROLL_TYPE, TiC.PROPERTY_CONTENT_OFFSET,
		TiC.PROPERTY_CAN_CANCEL_EVENTS, TiC.PROPERTY_OVER_SCROLL_MODE, TiC.PROPERTY_REFRESHABLE_DEPRECATED, TiC.PROPERTY_REFRESHABLE })
public class ScrollViewProxy extends TiViewProxy implements Handler.Callback {

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_SCROLL_TO = MSG_FIRST_ID + 100;
	private static final int MSG_SCROLL_TO_BOTTOM = MSG_FIRST_ID + 101;
	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	public ScrollViewProxy() {
		super();
		defaultValues.put(TiC.PROPERTY_OVER_SCROLL_MODE, 0);
	}

	public ScrollViewProxy(TiContext context) {
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		return new TiUIScrollView(this);
	}

	public TiUIScrollView getScrollView() {
		return (TiUIScrollView) getOrCreateView();
	}

	@Kroll.method
	public void scrollTo(int x, int y, @Kroll.argument(optional = true) KrollDict options) {
		if (TiApplication.isUIThread()) {
			handleScrollTo(x, y, options);
		} else {
			KrollDict dict = new KrollDict();
			dict.put("x", x);
			dict.put("y", y);
			dict.put("options", options);
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO), dict);
		}
	}

	@Kroll.setProperty
	@Kroll.method
	public void setScrollingEnabled(Object enabled) {
		getScrollView().setScrollingEnabled(enabled);
	}

	@Kroll.getProperty
	@Kroll.method
	public boolean getScrollingEnabled() {
		return getScrollView().getScrollingEnabled();
	}

	@Kroll.method
	public void scrollToBottom() {
		if (!TiApplication.isUIThread()) {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO_BOTTOM), getActivity());
		} else {
			handleScrollToBottom();
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (this.getActivity() == null || this.getScrollView() == null) {
			return super.handleMessage(msg);
		}

		if (msg.what == MSG_SCROLL_TO) {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict dict = (KrollDict) result.getArg();
			KrollDict options = dict.getKrollDict("options");
			handleScrollTo(dict.optInt("x", 0), dict.optInt("y", 0), options);
			result.setResult(null); // signal scrolled
			return true;
		} else if (msg.what == MSG_SCROLL_TO_BOTTOM) {
			handleScrollToBottom();
			AsyncResult result = (AsyncResult) msg.obj;
			result.setResult(null); // signal scrolled
			return true;
		}
		return super.handleMessage(msg);
	}

	public void handleScrollTo(int x, int y, KrollDict options) {
		getScrollView().scrollTo(x, y, options);
	}

	public void handleScrollToBottom() {
		getScrollView().scrollToBottom();
	}

	@Override
	public String getApiName() {
		return "Ti.UI.ScrollView";
	}

	@Kroll.method
	public void cmMarkRefreshFinished() {
		TiUIScrollView view = (TiUIScrollView) this.peekView();
		view.finishRefresh();
	}

}
