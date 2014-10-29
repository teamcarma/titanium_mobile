package ti.modules.titanium.ui.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wei.ding
 */
public class Interval {

	private double min;
	private double max;

	public Interval(float min, float max) {
		super();
		this.min = min;
		this.max = max;
	}

	public Interval(double min, double max) {
		super();
		this.min = min;
		this.max = max;
	}

	public Interval getModeInterval() {
		double min = this.min % 360;
		double max = this.max % 360;
		return new Interval(min <= max ? min : min - 360, max);
	}

	public Interval getSinInterval() {
		Interval modeInterval = this.getModeInterval();

		if (this.isSingleSinInterval(modeInterval.min, modeInterval.max)) {
			return this.getSingleGrowSinInterval(modeInterval.min, modeInterval.max);
		}

		List<Interval> intervals = new ArrayList<Interval>();
		intervals.add(this.getSingleGrowSinInterval(modeInterval.min, -90));
		intervals.add(this.getSingleGrowSinInterval(-90, Math.min(modeInterval.max, 90)));
		intervals.add(this.getSingleGrowSinInterval(90, modeInterval.max));

		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		for (Interval item : intervals) {
			if (item != null) {
				min = Math.min(min, item.min);
				max = Math.max(max, item.max);
			}
		}

		return new Interval(min, max);
	}

	public Interval getCosInterval() {
		Interval modeInterval = this.getModeInterval();

		if (this.isSingleCosInterval(modeInterval.min, modeInterval.max)) {
			return this.getSingleGrowCosInterval(modeInterval.min, modeInterval.max);
		}

		List<Interval> intervals = new ArrayList<Interval>();
		intervals.add(this.getSingleGrowCosInterval(min, 0));
		intervals.add(this.getSingleGrowCosInterval(0, max));

		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		for (Interval item : intervals) {
			if (item != null) {
				min = Math.min(min, item.min);
				max = Math.max(max, item.max);
			}
		}

		return new Interval(min, max);
	}

	private boolean isSingleCosInterval(double min, double max) {
		if (min <= 0 && max <= 0) {
			return true;
		}
		if (min >= 0 && max >= 0) {
			return true;
		}
		return false;
	}

	private boolean isSingleSinInterval(double min, double max) {
		if (min <= -90 && max <= -90) {
			return true;
		}
		if (min >= 90 && max >= 90) {
			return true;
		}
		if (min >= -90 && max <= 90) {
			return true;
		}
		return false;
	}

	private Interval getSingleGrowSinInterval(double min, double max) {
		if (min > max) {
			return null;
		}
		double sinMin = Math.sin(Math.toRadians(min));
		double sinMax = Math.sin(Math.toRadians(max));
		return new Interval(Math.min(sinMin, sinMax), Math.max(sinMin, sinMax));
	}

	private Interval getSingleGrowCosInterval(double min, double max) {
		if (min > max) {
			return null;
		}
		double cosMin = Math.cos(Math.toRadians(min));
		double cosMax = Math.cos(Math.toRadians(max));
		return new Interval(Math.min(cosMin, cosMax), Math.max(cosMin, cosMax));
	}

	public double getMinimum() {
		return this.min;
	}

	public double getMaximum() {
		return this.max;
	}
}
