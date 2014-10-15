package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

/**
 * This can generate a LongStream of potential seeds. This takes a time range to
 * generate them over, and uses an initial set of uniquifier values. The first
 * few Random objects created during an execution have starting seeds which are
 * in this pool of potential seeds.
 * 
 * @author matthew
 */
public class SeedGenerator {

	/**
	 * The Random object seed is based on the current time. This class requires
	 * that the Random object was created in the recent past. This variable
	 * holds the time range that will be searched.
	 */
	// 1s = 10^9ns
	public static final long DEFAULT_SEED_TIME_RANGE_NANOS = 1 * 5L * 1000L * 1000L;

	/**
	 * The Random object seed is based on a numerical value which changes every
	 * time a Random object is created. This many values will be calculated
	 * within which to search for a matching seed.
	 */
	// Testing has found that the Supplier is #9 and Customer Service is #11
	public static final int SEED_UNIQUIFIER_VALUE_COUNT = 20;
	/**
	 * The Random object seed is based on a numerical value which changes every
	 * time a Random object is created. This progression starts with this
	 * constant.
	 */
	private static final long SEED_UNIQUIFIER_INITIAL_VALUE = 8682522807148012L;
	/**
	 * The Random object seed is based on a numerical value which changes every
	 * time a Random object is created. This progression involves multiplying
	 * the current value with this constant.
	 */
	private static final long SEED_UNIQUIFIER_FACTOR = 181783497276652981L;
	/**
	 * The Random object seed is based on a numerical value which changes every
	 * time a Random object is created. This holds the calculated values to use
	 * to search for the seed.
	 */
	public static final List<Long> seedUniquifierValues;

	static {
		long value = SEED_UNIQUIFIER_INITIAL_VALUE;
		seedUniquifierValues = new ArrayList<Long>();

		for (int i = 0; i < SEED_UNIQUIFIER_VALUE_COUNT; i++) {
			value *= SEED_UNIQUIFIER_FACTOR;
			seedUniquifierValues.add(value); // initial value is not used
		}
	}

	/**
	 * This holds the creation time of the Oracle. <strong>It is assumed that
	 * the Random object has been created at or before this time.</strong>
	 */
	private final long startingTime;

	public SeedGenerator(long startingTime) {
		this.startingTime = startingTime;
	}

	/**
	 * This returns the seed search space.
	 * 
	 * @return
	 */
	public static long size() {
		return DEFAULT_SEED_TIME_RANGE_NANOS * SEED_UNIQUIFIER_VALUE_COUNT;
	}

	/**
	 * This will create a stream of potential seeds. This looks back over the
	 * last second.
	 * 
	 * @param batch - the batch of time to stream
	 * @return
	 */
	public LongStream stream(int batch) {
		long offset = (batch / 2) * DEFAULT_SEED_TIME_RANGE_NANOS;
		if (batch % 2 == 0) {
			return backStream(offset);
		}
		else {
			return forwardStream(offset);
		}
	}

	private LongStream backStream(long offset) {
		final long effectiveStartingTime = startingTime - offset;
		return LongStream.range(effectiveStartingTime - DEFAULT_SEED_TIME_RANGE_NANOS, effectiveStartingTime + 1).flatMap(t -> seedUniquifierValues.stream().mapToLong(u -> t ^ u));
	}
	private LongStream forwardStream(long offset) {
		final long effectiveStartingTime = startingTime + offset;
		return LongStream.range(effectiveStartingTime, effectiveStartingTime + DEFAULT_SEED_TIME_RANGE_NANOS + 1).flatMap(t -> seedUniquifierValues.stream().mapToLong(u -> t ^ u));
	}
}
