package ti.modules.titanium.ui.broadcastview;

import ti.modules.titanium.ui.animations.CurvedLineGroupAnimator;
import ti.modules.titanium.ui.drawable.CurvedLineDrawable;
import ti.modules.titanium.ui.utils.Direction;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.os.Build;
import android.view.View;

public class BroadcastAnimateView extends View {

	private CurvedLineGroupAnimator mAnimation;

	private ColorStateList mTextColor;
	int mCurrentTextColor;

	public final CurvedLineGroup drawable;

	public BroadcastAnimateView(Context context) {
		super(context);
		this.drawable = new CurvedLineGroup();
	}

	/**
	 * @param drawable
	 * @param axisX
	 * @param axisY
	 * @param width
	 * @param color
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.views.CurvedLineGroup#addLine(com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineDrawable,
	 *      float, float, float, int)
	 */
	public void addCurvedLine(CurvedLineDrawable drawable, float axisX, float axisY) {
		this.drawable.addLine(drawable, axisX, axisY, this.mCurrentTextColor);
	}

	/**
	 * @param drawable
	 * @param axisX
	 * @param axisY
	 * @param width
	 * @param color
	 * @param onlyAnimating
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.views.CurvedLineGroup#addElement(com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineDrawable,
	 *      float, float, float, int, boolean)
	 */
	public void addCurvedLine(CurvedLineDrawable drawable, float axisX, float axisY, boolean onlyAnimating) {
		this.drawable.addLine(drawable, axisX, axisY, this.mCurrentTextColor, onlyAnimating);
	}

	/**
	 * @return the drawable
	 */
	public CurvedLineGroup getDrawable() {
		return drawable;
	}

	/*
	 * (non-Javadoc)
	 * @see android.view.View#draw(android.graphics.Canvas)
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		boolean isAnimating = this.mAnimation != null && this.mAnimation.isRunning();
		for (CurvedLineElement element : this.drawable.getElements()) {
			if (!isAnimating && element.isOnlyAnimating()) {
				continue;
			}
			canvas.save();
			float x;
			if (this.getDrawable().getDirection() == Direction.LEFT_TO_RIGHT) {
				x = element.getX();
			} else {
				x = element.getX() + (canvas.getWidth() - element.getWidth() - element.getDrawable().getIntrinsicWidth());
			}
			canvas.translate(x, (canvas.getHeight() - element.getDrawable().getIntrinsicHeight()) / 2.0f);
			element.setAlpha(isAnimating ? element.getAlpha() : 1.0f);
			element.getDrawable().draw(canvas);
			canvas.restore();
		}
	}

	public void setTextColor(int color) {
		this.mTextColor = ColorStateList.valueOf(color);
		updateTextColor();
	}

	public void setTextColor(ColorStateList color) {
		if (color == null) {
			throw new IllegalArgumentException("color can not be null.");
		}
		this.mTextColor = color;
		this.updateTextColor();
	}

	public ColorStateList getTextColor() {
		return this.mTextColor;
	}

	public int getCurrentTextColor() {
		return this.mCurrentTextColor;
	}

	private void updateTextColor() {
		if (this.mTextColor == null) {
			return;
		}
		int color = this.mTextColor.getColorForState(this.getDrawableState(), 0);
		if (color == this.mCurrentTextColor) {
			return;
		}
		this.mCurrentTextColor = color;
		drawable.updateColor(this.mCurrentTextColor);
		this.invalidate();
	}

	/*
	 * (non-Javadoc)
	 * @see android.view.View#drawableStateChanged()
	 */
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (this.mTextColor != null && this.mTextColor.isStateful()) {
			this.updateTextColor();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void startAnimation(CurvedLineGroupAnimator animator) {
		this.setAnimation(animator);
		this.invalidate();
		if (this.mAnimation != null) {
			this.mAnimation.start();
		}
	}

	/**
	 * Return a broadcast animator instance.
	 * @return
	 */
	public CurvedLineGroupAnimator broadcastAnimate() {
		if (this.mAnimation == null) {
			this.mAnimation = new CurvedLineGroupAnimator(this);
		}
		return this.mAnimation;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setAnimation(CurvedLineGroupAnimator animator) {
		if (this.mAnimation != null && this.mAnimation.isStarted()) {
			this.mAnimation.cancel();
		}
		this.mAnimation = animator;
	}
}