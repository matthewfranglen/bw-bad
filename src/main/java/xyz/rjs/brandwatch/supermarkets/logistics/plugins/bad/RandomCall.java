package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.Random;

/**
 * This is a data object which wraps up the calls to nextInt(bound).
 * 
 * @author matthew
 */
class RandomCall {

	/**
	 * This is the value that was returned by the call.
	 */
	private final int value;
	/**
	 * This is the exclusive limit that was placed on the produced value.
	 */
	final int bound;

	public RandomCall(int value, int bound) {
		this.value = value;
		this.bound = bound;
	}

	/**
	 * This checks the provided Random object to see if the same call produces
	 * the same value.
	 * 
	 * @param random
	 * @return
	 */
	public boolean test(Random random) {
		return random.nextInt(bound) == value;
	}
}