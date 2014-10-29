package ti.modules.titanium.ui.utils;

import android.animation.FloatEvaluator;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class OpacityByProgressEvluator extends FloatEvaluator {

	public static final OpacityByProgressEvluator DEFAULT = new OpacityByProgressEvluator();
	public static final OpacityByProgressEvluator DEFAULT_RIGHT_TO_LEFT = new OpacityByProgressEvluator(Direction.RIGHT_TO_LEFT);

	private final Direction direction;

	public OpacityByProgressEvluator() {
		this(Direction.LEFT_TO_RIGHT);
	}

	public OpacityByProgressEvluator(Direction direction) {
		super();
		this.direction = direction == null ? Direction.LEFT_TO_RIGHT : direction;
	}

	public static OpacityByProgressEvluator getDefault(Direction direction) {
		if (direction != null && direction == Direction.RIGHT_TO_LEFT) {
			return DEFAULT_RIGHT_TO_LEFT;
		}
		return DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * @see android.animation.FloatEvaluator#evaluate(float, java.lang.Number,
	 * java.lang.Number)
	 */
	@Override
	public Float evaluate(float fraction, Number startValue, Number uselessNumber) {
		return evaluate(this.direction == Direction.LEFT_TO_RIGHT ? startValue.floatValue() + fraction : (1 + startValue.floatValue() - fraction));
	}

	/**
	 * Evaluate opacity by the position.
	 * @param progress
	 * @return
	 */
	public Float evaluate(float progress) {
		return this.direction == Direction.LEFT_TO_RIGHT ? evaluateLTR(progress) : evaluateRTL(progress);
	}

	private Float evaluateRTL(float progress) {
		progress = progress % 1;
		if (progress >= 0 && progress <= 0.75) {
			double a = 4 / 3.0;
			return (float) (a * progress);
		}
		if (progress > 0.75 && progress < 1) {
			return (float) (3.25 - 3 * progress);
		}
		return 0.0f;
	}

	private Float evaluateLTR(float progress) {
		progress = progress % 1;
		if (progress >= 0 && progress <= 0.25) {
			return (float) (3 * progress + 0.25);
		}
		if (progress > 0.25 && progress < 1) {
			double a = 4 / 3.0;
			return (float) (a * (1 - progress));
		}
		return 0.0f;
	}

	/**
	 * @return the direction
	 */
	public Direction getDirection() {
		return direction;
	}
}
