package ti.modules.titanium.ui.drawable;

import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;

public class CurvedLineDrawable extends ShapeDrawable {

	private final CurvedLineShape shape;

	public static final double DELTA_RATIO = 0.001;

	public CurvedLineDrawable() {
		super();
		this.shape = new CurvedLineShape();
		this.setShape(this.shape);
	}

	/**
	 * @param startAngle
	 * @param sweepAngle
	 * @param innerRadius
	 * @param thickness
	 */
	public CurvedLineDrawable(float startAngle, float sweepAngle, int innerRadius, int thickness) {
		super();
		this.shape = new CurvedLineShape(startAngle, sweepAngle, innerRadius, thickness);
		this.setShape(this.shape);
	}

	public void applyIntrnsicDimensions() {
		RectF bounds = shape.getBounds();
		int width = (int) (bounds.right - bounds.left);
		int height = (int) (bounds.bottom - bounds.top);
		this.setIntrinsicWidth(width + this.getDeltaErrorAxisX());
		this.setIntrinsicHeight(height);
	}

	/**
	 * @param color
	 */
	public void setColor(int color) {
		this.getPaint().setColor(color);
	}

	/*
	 * (non-Javadoc)
	 * @see android.graphics.drawable.ShapeDrawable#setIntrinsicWidth(int)
	 */
	@Override
	public void setIntrinsicWidth(int width) {
		super.setIntrinsicWidth(width);
	}

	/*
	 * (non-Javadoc)
	 * @see android.graphics.drawable.ShapeDrawable#setIntrinsicHeight(int)
	 */
	@Override
	public void setIntrinsicHeight(int height) {
		super.setIntrinsicHeight(height);
	}

	/*
	 * (non-Javadoc)
	 * @see android.graphics.drawable.ShapeDrawable#getIntrinsicWidth()
	 */
	@Override
	public int getIntrinsicWidth() {
		return super.getIntrinsicWidth();
	}

	/*
	 * (non-Javadoc)
	 * @see android.graphics.drawable.ShapeDrawable#getIntrinsicHeight()
	 */
	@Override
	public int getIntrinsicHeight() {
		return super.getIntrinsicHeight();
	}

	/**
	 * @param innerRadius
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#setInnerRadius(int)
	 */
	public void setInnerRadius(int innerRadius) {
		shape.setInnerRadius(innerRadius);
	}

	/**
	 * @param thickness
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#setThickness(int)
	 */
	public void setThickness(int thickness) {
		shape.setThickness(thickness);
	}

	/**
	 * @param mStart
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#setStartAngle(float)
	 */
	public void setStartAngle(float mStart) {
		shape.setStartAngle(mStart);
	}

	/**
	 * @param mSweep
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#setSweepAngle(float)
	 */
	public void setSweepAngle(float mSweep) {
		shape.setSweepAngle(mSweep);
	}

	/**
	 * @return
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#getStartAngle()
	 */
	public float getStartAngle() {
		return shape.getStartAngle();
	}

	/**
	 * @return
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#getSweepAngle()
	 */
	public float getSweepAngle() {
		return shape.getSweepAngle();
	}

	/**
	 * @return
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#getInnerRadius()
	 */
	public int getInnerRadius() {
		return shape.getInnerRadius();
	}

	/**
	 * @return
	 * @see com.blogspot.gavinfeilong.lycium.android.v8.patch.app.ui.CurvedLineShape#getThickness()
	 */
	public int getThickness() {
		return shape.getThickness();
	}

	private int getDeltaErrorAxisX() {
		return (int) ((this.shape.getInnerRadius() + this.shape.getThickness()) * 2 * DELTA_RATIO + 1);
	}
}
