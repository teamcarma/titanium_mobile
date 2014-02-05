/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

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
		Log.v(TAG, "Creating view with an activity " + this.getActivity() + "...", Log.DEBUG_MODE);

		if (this.getActivity() != null) {
			TiUIView view = new TiView(this);
			view.getLayoutParams().autoFillsHeight = true;
			view.getLayoutParams().autoFillsWidth = true;
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
