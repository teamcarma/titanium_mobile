package ti.modules.titanium.ui.broadcastview;

import ti.modules.titanium.ui.drawable.CurvedLineDrawable;
import ti.modules.titanium.ui.utils.Direction;
import ti.modules.titanium.ui.utils.OpacityByProgressEvluator;

public class CurvedLineElement {

	private float x;
	private float y;
	private float width;
	private float height;

	private CurvedLineDrawable drawable;

	private float alpha = 1.0f;
	private float progress = -1;

	private final boolean onlyAnimating;

	public CurvedLineElement() {
		this(false);
	}

	public CurvedLineElement(boolean onlyAnimating) {
		super();
		this.onlyAnimating = onlyAnimating;
	}

	/**
	 * @return the onlyAnimating
	 */
	public boolean isOnlyAnimating() {
		return onlyAnimating;
	}

	/**
	 * @return the x
	 */
	public float getX() {
		return x;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * @return the y
	 */
	public float getY() {
		return y;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	public void setY(float y) {
		this.y = y;
	}

	/**
	 * @return the width
	 */
	public float getWidth() {
		return width;
	}

	/**
	 * @param width
	 *            the width to set
	 */
	public void setWidth(float width) {
		this.width = width;
	}

	/**
	 * @return the height
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * @param height
	 *            the height to set
	 */
	public void setHeight(float height) {
		this.height = height;
	}

	/**
	 * @return the drawable
	 */
	public CurvedLineDrawable getDrawable() {
		return drawable;
	}

	/**
	 * @param drawable
	 *            the drawable to set
	 */
	public void setDrawable(CurvedLineDrawable drawable) {
		this.drawable = drawable;
	}

	/**
	 * @return the alpha
	 */
	public float getAlpha() {
		return alpha;
	}

	/**
	 * @param alpha
	 *            the alpha to set
	 */
	public void setAlpha(float alpha) {
		this.alpha = alpha;
		if (this.drawable != null) {
			this.drawable.setAlpha((int) (alpha * 255 + .5f));
		}
	}

	/**
	 * @return the progress
	 * @exception IllegalArgumentException
	 *                if the width is not specific
	 */
	public float getProgress() {
		if (this.getWidth() <= 0) {
			throw new IllegalArgumentException("The width must be specific first.");
		}
		if (this.progress < 0) {
			this.progress = this.getX() / this.width;
		}
		return this.progress;
	}

	/**
	 * @param progress
	 *            the progress to set
	 */
	public void setProgress(float progress) {
		this.progress = progress;
	}

	public void setupAlpha(Direction direction) {
		this.setAlpha(OpacityByProgressEvluator.getDefault(direction).evaluate(this.getProgress()));
	}
}
