/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.text.MessageFormat;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiView;
import android.app.Activity;

@Kroll.proxy(creatableInModule = UIModule.class)
public class ViewProxy extends TiViewProxy {

	private static final String TAG = "ViewProxy";

	public ViewProxy() {
		super();
	}

	public ViewProxy(TiContext tiContext) {
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		if (this.getActivity() != null) {
			TiUIView view = new TiView(this);
			view.getLayoutParams().autoFillsHeight = true;
			view.getLayoutParams().autoFillsWidth = true;
			Log.v(TAG, MessageFormat.format("A view({0}) on {1} created.", view, this.getActivity()), Log.DEBUG_MODE);
			return view;
		} else {
			Log.v(TAG, "Failed to create view because activity is null in this proxy.", Log.DEBUG_MODE);
			return null;
		}
	}

	@Override
	public String getApiName() {
		return "Ti.UI.View";
	}
}
