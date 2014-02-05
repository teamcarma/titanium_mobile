/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIActivityIndicator;
import android.app.Activity;
import android.os.Message;

@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = { TiC.PROPERTY_MESSAGE, TiC.PROPERTY_MESSAGEID, TiC.PROPERTY_COLOR, TiC.PROPERTY_FONT,
		TiC.PROPERTY_STYLE })
@Kroll.dynamicApis(methods = { "hide", "show" })
public class ActivityIndicatorProxy extends TiViewProxy {

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;
	private static final int MSG_SHOW = MSG_FIRST_ID + 100;

	boolean visible = false;

	public ActivityIndicatorProxy() {
		super();
		defaultValues.put(TiC.PROPERTY_VISIBLE, false);
	}

	public ActivityIndicatorProxy(TiContext tiContext) {
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		try {
			TiUIView view = new TiUIActivityIndicator(this);
			if (this.visible) {
				getMainHandler().obtainMessage(MSG_SHOW).sendToTarget();
			}
			return view;
		} catch (NullPointerException e) {
			return null;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_SHOW:
				handleShow(null);
				return true;
		}
		return super.handleMessage(msg);
	}

	@Override
	protected KrollDict getLangConversionTable() {
		KrollDict table = new KrollDict();
		table.put(TiC.PROPERTY_MESSAGE, TiC.PROPERTY_MESSAGEID);
		return table;
	}

	@Override
	protected void handleShow(KrollDict options) {
		visible = true;
		if (view == null) {
			TiUIActivityIndicator ai = (TiUIActivityIndicator) getOrCreateView();
			ai.show();
			return;
		}
		super.handleShow(options);
	}

	@Override
	protected void handleHide(KrollDict options) {
		visible = false;
		if (view == null) {
			TiUIActivityIndicator ai = (TiUIActivityIndicator) getOrCreateView();
			ai.hide();
			return;
		}
		super.handleHide(options);
	}

	@Override
	public String getApiName() {
		return "Ti.UI.ActivityIndicator";
	}
}
