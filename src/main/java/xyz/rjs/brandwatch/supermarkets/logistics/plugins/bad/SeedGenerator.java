package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

/**
 * This holds tests which can be applied to a range of potential seeds. The
 * tests will exclude the seeds which do not produce the correct values.
 * 
 * This allows the seed that corresponds with the Random object generator to be
 * determined, allowing a duplicate Random object to be produced.
 * 
 * @author matthew
 */
class SeedGenerator {

	/**
	 * The Random object seed is based on the current time. This class requires
	 * that the Random object was created in the recent past. This variable
	 * holds the time range that will be searched.
	 */
	// 1s = 10^9ns
	public static final long DEFAULT_SEED_TIME_RANGE_NANOS = 1 * 1000L * 1000L * 1000L;

	/**
	 * The Random object seed is based on a numerical value which changes every
	 * time a Random object is created. This many values will be calculated
	 * within which to search for a matching seed.
	 */
	public static final int SEED_UNIQUIFIER_VALUE_COUNT = 10;
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
	 * @return
	 */
	public static long size() {
		return DEFAULT_SEED_TIME_RANGE_NANOS * SEED_UNIQUIFIER_VALUE_COUNT;
	}

	/**
	 * This will create a stream of potential seeds. This looks back over the
	 * last second.
	 * 
	 * @return
	 */
	public LongStream stream() {
		return stream(DEFAULT_SEED_TIME_RANGE_NANOS);
	}

	/**
	 * This will create a stream of potential seeds, looking as far back in time
	 * as indicated.
	 * 
	 * One millisecond is one million nanoseconds (which is one billion
	 * nanoseconds to the second).
	 * 
	 * @param timeRange - the time, in nanoseconds, to iterate through
	 * @return
	 */
	public LongStream stream(long timeRange) {
		return LongStream.range(startingTime - timeRange, startingTime + 1).flatMap(t -> seedUniquifierValues.stream().mapToLong(u -> t ^ u));
	}
}
