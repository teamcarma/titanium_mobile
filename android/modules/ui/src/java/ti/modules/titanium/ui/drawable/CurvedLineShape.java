package ti.modules.titanium.ui.drawable;

import ti.modules.titanium.ui.utils.Interval;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RectShape;

public class CurvedLineShape extends RectShape {

	private float mStart;
	private float mSweep;
	private int innerRadius;
	private int thickness;

	public CurvedLineShape() {
		super();
	}

	/**
	 * ArcShape constructor.
	 * @param startAngle
	 *            the angle (in degrees) where the arc begins
	 * @param sweepAngle
	 *            the sweep angle (in degrees). Anything equal to or greater
	 *            than 360 results in a complete circle/oval.
	 */
	public CurvedLineShape(float startAngle, float sweepAngle, int innerRadius, int thickness) {
		if (innerRadius < 0 || thickness < 0) {
			throw new IllegalArgumentException("inner radius or thickness can not be negative.");
		}
		mStart = startAngle % 360;
		mSweep = sweepAngle % 360;
		this.innerRadius = innerRadius;
		this.thickness = thickness;

		this.resize((this.innerRadius + this.thickness) * 2, (this.innerRadius + this.thickness) * 2);
	}

	/**
	 * @param innerRadius
	 */
	public void setInnerRadius(int innerRadius) {
		if (innerRadius < 0) {
			throw new IllegalArgumentException("Inner radius can not be negative.");
		}
		this.innerRadius = innerRadius;
		this.resize((this.innerRadius + this.thickness) * 2, (this.innerRadius + this.thickness) * 2);
	}

	/**
	 * @param thickness
	 */
	public void setThickness(int thickness) {
		if (thickness < 0) {
			throw new IllegalArgumentException("Thickness can not be negative.");
		}
		this.thickness = thickness;
		this.resize((this.innerRadius + this.thickness) * 2, (this.innerRadius + this.thickness) * 2);
	}

	/**
	 * @param mStart
	 */
	public void setStartAngle(float mStart) {
		this.mStart = mStart % 360;
	}

	/**
	 * @param mSweep
	 */
	public void setSweepAngle(float mSweep) {
		this.mSweep = mSweep % 360;
	}

	/**
	 * Gets start angle
	 * @return
	 */
	public float getStartAngle() {
		return mStart;
	}

	/**
	 * Gets sweep angle.
	 * @return
	 */
	public float getSweepAngle() {
		return mSweep;
	}

	/**
	 * Get inner radius
	 * @return
	 */
	public int getInnerRadius() {
		return innerRadius;
	}

	/**
	 * Thickness
	 * @return
	 */
	public int getThickness() {
		return thickness;
	}

	@Override
	public void draw(Canvas canvas, Paint paint) {
		super.draw(canvas, paint);
		canvas.drawPath(this.buildRing(), paint);
	}

	private Path buildRing() {
		float sweep = this.mSweep;

		RectF bounds = new RectF(this.rect());

		float x = bounds.width() / 2.0f;
		float y = bounds.height() / 2.0f;

		float thickness = this.thickness;
		float radius = this.innerRadius;

		RectF innerBounds = new RectF(bounds);
		innerBounds.inset(x - radius, y - radius);

		bounds = new RectF(innerBounds);
		bounds.inset(-thickness, -thickness);

		final Path ringPath = new Path();
		// arcTo treats the sweep angle mod 360, so check for that, since we
		// think 360 means draw the entire oval
		if (sweep < 360 && sweep > -360) {
			ringPath.setFillType(Path.FillType.EVEN_ODD);
			// inner top
			int originX = (int) (x + radius * Math.cos(Math.toRadians(this.mStart)));
			int originY = (int) (y + radius * Math.sin(Math.toRadians(this.mStart)));
			int destX = (int) (x + (radius + thickness) * Math.cos(Math.toRadians(this.mStart)));
			int destY = (int) (y + (radius + thickness) * Math.sin(Math.toRadians(this.mStart)));

			ringPath.moveTo(originX, originY);
			// outer top
			ringPath.lineTo(destX, destY);
			// outer arc
			ringPath.arcTo(bounds, this.mStart, sweep, false);
			// inner arc
			ringPath.arcTo(innerBounds, this.mStart + sweep, -sweep, false);

			RectF pathBounds = this.getBounds();
			ringPath.offset(-pathBounds.left, -pathBounds.top);

			ringPath.close();
		} else {
			// add the entire ovals
			ringPath.addOval(bounds, Path.Direction.CW);
			ringPath.addOval(innerBounds, Path.Direction.CCW);
		}

		return ringPath;
	}

	RectF getBounds() {
		RectF shapeBounds = new RectF();
		RectF bounds = new RectF(this.rect());

		float radius = this.innerRadius;

		float x = bounds.width() / 2.0f;
		float y = bounds.height() / 2.0f;

		Interval interval = new Interval(this.mStart, this.mStart + this.mSweep);
		Interval cosInterval = interval.getCosInterval();
		Interval sinInterval = interval.getSinInterval();

		shapeBounds.left = (float) Math.min(x + radius * cosInterval.getMinimum(), x + (radius + thickness) * cosInterval.getMinimum());
		shapeBounds.top = (float) Math.min(y + radius * sinInterval.getMinimum(), y + (radius + thickness) * sinInterval.getMinimum());
		shapeBounds.right = (float) Math.max(x + radius * cosInterval.getMaximum(), x + (radius + thickness) * cosInterval.getMaximum());
		shapeBounds.bottom = (float) Math.max(y + radius * sinInterval.getMaximum(), y + (radius + thickness) * sinInterval.getMaximum());

		return shapeBounds;
	}

}
