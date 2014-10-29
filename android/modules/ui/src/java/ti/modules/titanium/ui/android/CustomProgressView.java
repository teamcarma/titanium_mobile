package ti.modules.titanium.ui.android;

import ti.modules.titanium.ui.drawable.CustomProgressShape;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author wei.ding
 */
public class CustomProgressView extends View {

	private ColorStateList mTextColor;
	private int mCurrentTextColor;

	private final ShapeDrawable mProgress = new ShapeDrawable(new CustomProgressShape());

	public CustomProgressView(Context context) {
		super(context);
	}

	public CustomProgressView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	/*
	 * (non-Javadoc)
	 * @see android.view.View#draw(android.graphics.Canvas)
	 */
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		canvas.save();
		canvas.translate(0, 0);
		this.mProgress.setIntrinsicWidth(canvas.getWidth());
		this.mProgress.setIntrinsicHeight(canvas.getHeight());
		this.mProgress.getShape().resize(canvas.getWidth(), canvas.getHeight());
		this.mProgress.draw(canvas);
		canvas.restore();
	}

	/**
	 * @return the progress
	 */
	public int getProgress() {
		CustomProgressShape shape = (CustomProgressShape) this.mProgress.getShape();
		return shape.getProgress();
	}

	/**
	 * @param progress
	 *            the progress to set
	 * @exception IllegalArgumentException
	 *                if progress is not an integer between 0 and 100.
	 */
	public void setProgress(int progress) throws IllegalArgumentException {
		CustomProgressShape shape = (CustomProgressShape) this.mProgress.getShape();
		shape.setProgress(progress);
		this.invalidate();
	}

	/**
	 * Set text color
	 * @param color
	 */
	public void setTextColor(int color) {
		this.mTextColor = ColorStateList.valueOf(color);
		updateTextColor();
	}

	/**
	 * This set text color.
	 * @param color
	 */
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
		this.mProgress.getPaint().setColor(this.mCurrentTextColor);
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
}
