package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.Random;
import java.util.function.Function;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;

import com.google.common.eventbus.Subscribe;


/**
 * @author matthew
 *
 */
public class OracleWrapper extends AbstractPlugin {

	private final Oracle oracle;

	private final int bound;

	public OracleWrapper(int bound) {
		oracle = new Oracle();
		this.bound = bound;
	}

	public boolean isFixed() {
		return oracle.isFixed();
	}

	public Random getRandom() {
		return oracle.getRandom();
	}

	@Subscribe
	public void tickListener(ClockTick tick) {
		oracle.tick();
	}

	protected void calledNextInt(Function<Random, Boolean> call) {
		oracle.calledNextInt(call, 1);
	}

	protected void calledNextInt(int value) {
		oracle.calledNextInt(value, bound);
	}
}
