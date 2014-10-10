package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.Random;

/**
 * This is a data object which wraps up the calls to nextInt.
 * 
 * @author matthew
 */
class RandomCall {

	private final int value;
	final int bound;

	public RandomCall(int value, int bound) {
		this.value = value;
		this.bound = bound;
	}

	public boolean test(Random random) {
		return random.nextInt(bound) == value;
	}
}