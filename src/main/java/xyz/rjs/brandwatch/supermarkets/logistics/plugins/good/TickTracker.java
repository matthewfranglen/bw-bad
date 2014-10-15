package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;

import com.google.common.eventbus.Subscribe;


/**
 * Allows access to the last seen tick outside of the tickListener method.
 * 
 * @author matthew
 */
@Component
public class TickTracker extends AbstractPlugin {

	/**
	 * This is the last seen tick.
	 */
	private int tick;

	public TickTracker() {
		tick = 0;
	}

	@Subscribe
	public void tickListener(ClockTick tick) {
		this.tick = tick.getTick();
	}

	public int getTick() {
		return tick;
	}
}
