/**
 * 
 */
package ti.modules.titanium.ui.broadcastview;

import java.util.ArrayList;
import java.util.List;

import ti.modules.titanium.ui.drawable.CurvedLineDrawable;
import ti.modules.titanium.ui.utils.Direction;

/**
 * @author wei.ding
 */
public class CurvedLineGroup {

	private List<CurvedLineElement> elements = new ArrayList<CurvedLineElement>();

	private Direction direction = Direction.LEFT_TO_RIGHT;

	private float width;
	private float height;

	/**
	 * @param drawable
	 * @param axisX
	 * @param axisY
	 * @param width
	 * @param color
	 */
	public void addLine(CurvedLineDrawable drawable, float axisX, float axisY, int color) {
		this.addLine(drawable, axisX, axisY, color, false);
	}

	/**
	 * @param drawable
	 * @param axisX
	 * @param axisY
	 * @param width
	 * @param color
	 * @param onlyAnimating
	 */
	public void addLine(CurvedLineDrawable drawable, float axisX, float axisY, int color, boolean onlyAnimating) {
		drawable.applyIntrnsicDimensions();
		drawable.setColor(color);

		CurvedLineElement item = new CurvedLineElement(onlyAnimating);
		item.setDrawable(drawable);

		item.setX(axisX);
		item.setY(axisY);

		item.setWidth(this.getWidth());
		item.setHeight(getHeight());

		this.getElements().add(item);
	}

	public List<CurvedLineElement> getElements() {
		return elements;
	}

	public void updateColor(int color) {
		for (CurvedLineElement element : getElements()) {
			element.getDrawable().setColor(color);
		}
	}

	/**
	 * @param waveAnimationView
	 *            TODO
	 * @return the direction
	 */
	public Direction getDirection() {
		return this.direction;
	}

	/**
	 * @param waveAnimationView
	 *            TODO
	 * @param direction
	 *            the direction to set
	 */
	public void setDirection(Direction direction) {
		this.direction = direction == null ? Direction.LEFT_TO_RIGHT : direction;
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
		if (width <= 0) {
			throw new IllegalArgumentException();
		}
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
		if (height <= 0) {
			throw new IllegalArgumentException();
		}
		this.height = height;
	}

}
