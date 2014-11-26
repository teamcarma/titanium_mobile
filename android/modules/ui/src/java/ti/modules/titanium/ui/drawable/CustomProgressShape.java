/**
 * 
 */
package ti.modules.titanium.ui.drawable;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.RectF;
import android.graphics.drawable.shapes.RectShape;

/**
 * @author wei.ding
 */
public class CustomProgressShape extends RectShape {

	private static final int MAXIMUM = 100;

	private int progress = 0;

	private Path mPath;

	/**
	 * 
	 */
	public CustomProgressShape() {
		this(0);
	}

	public CustomProgressShape(int progress) {
		super();
		this.setProgress(progress);

		this.mPath = new Path();
	}

	/**
	 * @return the progress
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * @param progress
	 *            the progress to set
	 */
	public void setProgress(int progress) {
		if (progress < 0 || progress > MAXIMUM) {
			throw new IllegalArgumentException("Progress can only be an integer between 0 and 100.");
		}
		this.progress = progress;
	}

	/*
	 * (non-Javadoc)
	 * @see android.graphics.drawable.shapes.Shape#draw(android.graphics.Canvas, android.graphics.Paint)
	 */
	@Override
	public void draw(Canvas canvas, Paint paint) {
		canvas.drawPath(buildPath(), paint);
	}

	private Path buildPath() {
		Path path = this.mPath;

		if (!path.isEmpty()) {
			path.reset();
		}

		RectF bounds = new RectF(this.rect());
		float pivotX = bounds.width() / 2.0f, border = pivotX * ((MAXIMUM - this.progress) / (MAXIMUM * 1.0f));
		bounds.inset(border, 0f);
		path.setFillType(FillType.EVEN_ODD);
		path.moveTo(0, 0);
		path.addRect(bounds, Direction.CW);
		return path;
	}

}
