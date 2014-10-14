package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.logistics.plugins.good.Stats;
import xyz.rjs.brandwatch.supermarkets.logistics.plugins.good.TickTracker;

/**
 * Tracks the time between price changes.
 * 
 * @author matthew
 */
public class TimeStats extends AbstractPlugin {

	@Autowired
	private TickTracker tick;

	/**
	 * This tracks the time differences between events and calculates duration
	 * confidence.
	 */
	private final Stats values;

	/**
	 * This tracks the last price change tick.
	 */
	private int lastChangeTick;

	public TimeStats() {
		lastChangeTick = 0;
		values = new Stats();
	}

	/**
	 * This should be called when a timed event occurs. This will record the
	 * time since the last event.
	 */
	protected void trigger() {
		final int currentTick = tick.getTick();
		values.add(currentTick - lastChangeTick);
		lastChangeTick = currentTick;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	/**
	 * Indicates the average across all durations.
	 * @return
	 */
	public double average() {
		return values.average();
	}

	/**
	 * Indicates the confidence levels across all durations. This is the number
	 * that will cover 95% of durations.
	 * 
	 * @return
	 */
	public double confidence() {
		return values.confidence();
	}
}
