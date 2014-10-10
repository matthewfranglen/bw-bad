package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author matthew
 *
 */
class SeedGenerator implements Iterable<Long> {

	/**
	 * The Random object seed is based on the current time. This class requires
	 * that the Random object was created in the recent past. This variable
	 * holds the time range that will be searched.
	 */
	// 1s = 10^9ns
	public static final long SEED_TIME_RANGE_NANOS = 1 * 1000L * 1000L * 1000L;

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
	private static final Long[] seedUniquifierValues;

	static {
		long value = SEED_UNIQUIFIER_INITIAL_VALUE;
		List<Long> values = new ArrayList<Long>();

		for (int i = 0; i < SEED_UNIQUIFIER_VALUE_COUNT; i++) {
			values.add(value);
			value *= SEED_UNIQUIFIER_FACTOR;
		}
		seedUniquifierValues = values.toArray(new Long[SEED_UNIQUIFIER_VALUE_COUNT]);
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
	 * @return
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Long> iterator() {
		return new SeedGeneratorIterator();
	}

	private class SeedGeneratorIterator implements Iterator<Long> {

		private long time;
		private int uniquifier;

		public SeedGeneratorIterator() {
			time = startingTime - SEED_TIME_RANGE_NANOS;
			uniquifier = 0;
		}

		/**
		 * @return
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return time < startingTime || uniquifier < seedUniquifierValues.length;
		}

		/**
		 * @return
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Long next() {
			time++;
			if (time > startingTime) {
				uniquifier++;
				time = startingTime - SEED_TIME_RANGE_NANOS;

				if (uniquifier > seedUniquifierValues.length) {
					throw new IllegalStateException();
				}
			}

			return time * uniquifier;
		}
	}
}
