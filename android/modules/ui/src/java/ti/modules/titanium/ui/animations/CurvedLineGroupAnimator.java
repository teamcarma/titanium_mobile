package ti.modules.titanium.ui.animations;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import ti.modules.titanium.ui.broadcastview.BroadcastAnimateView;
import ti.modules.titanium.ui.broadcastview.CurvedLineElement;
import ti.modules.titanium.ui.utils.ModuloEvluator;
import ti.modules.titanium.ui.utils.OpacityByProgressEvluator;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CurvedLineGroupAnimator extends Animator {

	private long mStartDelay;
	private long mUnscaledDuration = 300;
	private TimeInterpolator mInterpolator;
	private int mRepeatCount;
	private int mRepeatMode = ValueAnimator.RESTART;

	private final AnimatorSet delegate;

	private BroadcastAnimateView target;

	public CurvedLineGroupAnimator() {
		this(null);
	}

	public CurvedLineGroupAnimator(BroadcastAnimateView target) {
		super();
		this.target = target;
		this.delegate = new AnimatorSet();
	}

	@Override
	public long getStartDelay() {
		return this.mStartDelay;
	}

	@Override
	public void setStartDelay(long startDelay) {
		if (startDelay < 0) {
			throw new IllegalArgumentException();
		}
		this.mStartDelay = startDelay;
		this.delegate.setStartDelay(startDelay);
	}

	@Override
	public Animator setDuration(long duration) {
		if (duration < 0) {
			throw new IllegalArgumentException();
		}
		this.mUnscaledDuration = duration;
		this.delegate.setDuration(duration);
		return this;
	}

	@Override
	public long getDuration() {
		return this.mUnscaledDuration;
	}

	@Override
	public void setInterpolator(TimeInterpolator value) {
		this.mInterpolator = value != null ? value : new LinearInterpolator();
		this.delegate.setInterpolator(this.mInterpolator);
	}

	@SuppressLint("Override")
	public TimeInterpolator getInterpolator() {
		return this.mInterpolator;
	}

	@Override
	public boolean isRunning() {
		return this.delegate.isRunning();
	}

	/*
	 * (non-Javadoc)
	 * @see android.animation.Animator#start()
	 */
	@Override
	public void start() {
		if (this.target != null && this.target.getDrawable() != null) {
			setupAnimations();
		}
		this.delegate.start();
	}

	/*
	 * (non-Javadoc)
	 * @see android.animation.Animator#cancel()
	 */
	@Override
	public void cancel() {
		this.delegate.cancel();
		if (this.target != null) {
			this.target.invalidate();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.animation.Animator#end()
	 */
	@Override
	public void end() {
		this.delegate.end();
		if (this.target != null) {
			this.target.invalidate();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.animation.Animator#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return this.delegate.isStarted();
	}

	/*
	 * Set target to current animator, and the animator will be cancelled on
	 * previous target if it has been started.
	 * @see android.animation.Animator#setTarget(java.lang.Object)
	 */
	@Override
	public void setTarget(Object target) {
		if (target != null && !(target instanceof BroadcastAnimateView)) {
			String format = "Only {0} instance can not set as the target for {1}";
			throw new IllegalArgumentException(MessageFormat.format(format, BroadcastAnimateView.class.getSimpleName(), getClass().getSimpleName()));
		}

		if (this.isStarted()) {
			this.cancel();
		}
		this.target = target == null ? null : (BroadcastAnimateView) target;
	}

	/**
	 * Set target to current animator, and the animator will be cancelled on
	 * previous target if it has been started.
	 * @param target
	 */
	public void setTarget(BroadcastAnimateView target) {
		if (this.isStarted()) {
			this.cancel();
		}
		this.target = target == null ? null : target;
	}

	/**
	 * @return the repeatCount
	 */
	public int getRepeatCount() {
		return mRepeatCount;
	}

	/**
	 * @param repeatCount
	 *            the repeatCount to set
	 */
	public void setRepeatCount(int repeatCount) {
		this.mRepeatCount = repeatCount;
	}

	/**
	 * @return the repeatMode
	 */
	public int getRepeatMode() {
		return mRepeatMode;
	}

	/**
	 * @param repeatMode
	 *            the repeatMode to set
	 */
	public void setRepeatMode(int repeatMode) {
		this.mRepeatMode = repeatMode;
	}

	private void setupAnimations() {
		if (this.target == null || this.target.getDrawable() == null) {
			return;
		}
		List<Animator> list = new ArrayList<Animator>();
		list.add(this.createViewUpdateAnimation());
		for (CurvedLineElement element : this.target.getDrawable().getElements()) {
			list.add(this.createAxisXAnimation(element));
			list.add(this.createOpacityAnimation(element));
		}
		if (list.size() > 1) {
			this.delegate.playTogether(list);
		}
	}

	private ValueAnimator createViewUpdateAnimation() {
		ValueAnimator animation = ValueAnimator.ofFloat(0f, 1.0f);
		this.applyProperties(animation);
		animation.setInterpolator(new Interpolator() {

			@Override
			public float getInterpolation(float input) {
				if (target != null) {
					target.invalidate();
				}
				return input;
			}

		});

		return animation;
	}

	private ValueAnimator createOpacityAnimation(CurvedLineElement element) {
		ValueAnimator animation = ObjectAnimator.ofFloat(element, "alpha", element.getProgress(), element.getProgress());
		applyProperties(animation);
		animation.setEvaluator(OpacityByProgressEvluator.getDefault(this.target.getDrawable().getDirection()));
		return animation;
	}

	private ValueAnimator createAxisXAnimation(CurvedLineElement element) {
		ValueAnimator animation = ObjectAnimator.ofFloat(element, "x", element.getX(), element.getWidth());
		applyProperties(animation);
		animation.setEvaluator(new ModuloEvluator(this.target.getDrawable().getDirection()));
		return animation;
	}

	private void applyProperties(ValueAnimator animation) {
		animation.setDuration(this.getDuration());
		animation.setRepeatCount(this.getRepeatCount());
		animation.setRepeatMode(this.getRepeatMode());
		animation.setInterpolator(this.getInterpolator());
	}
}
