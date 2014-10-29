package ti.modules.titanium.ui.utils;

import android.animation.FloatEvaluator;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ModuloEvluator extends FloatEvaluator {

	private static final ModuloEvluator DEFAULT = new ModuloEvluator(Direction.LEFT_TO_RIGHT);
	private static final ModuloEvluator DEFAULT_LTR = new ModuloEvluator(Direction.RIGHT_TO_LEFT);

	private final Direction direction;

	public ModuloEvluator() {
		this(Direction.LEFT_TO_RIGHT);
	}

	public ModuloEvluator(Direction direction) {
		super();
		this.direction = direction == null ? Direction.LEFT_TO_RIGHT : direction;
	}

	/**
	 * @param direction
	 * @return
	 */
	public static ModuloEvluator getDefault(Direction direction) {
		return direction != null && direction == Direction.RIGHT_TO_LEFT ? DEFAULT_LTR : DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 * @see android.animation.FloatEvaluator#evaluate(float, java.lang.Number,
	 * java.lang.Number)
	 */
	@Override
	public Float evaluate(float fraction, Number startValue, Number quotient) {
		int q = quotient.intValue();
		float moduloStart = startValue.floatValue() % q;

		if (this.getDirection() == Direction.RIGHT_TO_LEFT) {
			return (q + (moduloStart - fraction * q)) % q;
		}

		return (moduloStart + fraction * q) % q;
	}

	/**
	 * @return the direction
	 */
	public Direction getDirection() {
		return direction;
	}
}
