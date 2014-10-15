package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This provides static methods to inspect the Random object. It is not my
 * intent to use these to duplicate the values, but instead to use them to
 * determine if this is feasible at all.
 * 
 * This is also very useful for testing.
 * 
 * @author matthew
 */
public class RandomInspector {

	/**
	 * A copy of the multiplier field from Random.
	 */
	public static final long SCRAMBLE_MULTIPLIER = 0x5DEECE66DL;
	/**
	 * A copy of the mask field from Random.
	 */
	public static final long SCRAMBLE_MASK = (1L << 48) - 1;
	/**
	 * This is the field on the Random object that holds the seed.
	 */
	public static final Field SEED_ACCESSOR;
	/**
	 * This is the field on the Random object that holds the seedUniquifier.
	 */
	public static final Field SEED_UNIQ_ACCESSOR;

	/**
	 * This initializes the SEED_ACCESSOR. The SecurityManager can prevent this.
	 * If there are any problems here then this class is unusable.
	 */
	static {
		try {
			SEED_ACCESSOR = Random.class.getDeclaredField("seed");
			SEED_UNIQ_ACCESSOR = Random.class.getDeclaredField("seedUniquifier");
			SEED_ACCESSOR.setAccessible(true);
			SEED_UNIQ_ACCESSOR.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the current value of the seed within the provided Random object.
	 * 
	 * @param random
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static long getSeed(Random random) throws IllegalArgumentException, IllegalAccessException {
		AtomicLong field = (AtomicLong) SEED_ACCESSOR.get(random);
		return field.get();
	}

	/**
	 * Returns the current value of the seed uniquifier within the provided
	 * Random object.
	 * 
	 * @param random
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static long getSeedUniquifier(Random random) throws IllegalArgumentException, IllegalAccessException {
		AtomicLong field = (AtomicLong) SEED_UNIQ_ACCESSOR.get(random);
		return field.get();
	}

	/**
	 * This applies the initial scrambling to the seed, which is reversible by
	 * calling this again. The resulting seed is truncated to 48 bytes, but
	 * Random does not use more anyway.
	 * 
	 * @param seed
	 */
	public static long initialScramble(long seed) {
		return (seed ^ SCRAMBLE_MULTIPLIER) & SCRAMBLE_MASK;
	}

	/**
	 * This takes a seed and the uniquifier value that was used to produce it
	 * and returns the original System.nanoTime that was used to produce it.
	 * 
	 * @param seed
	 * @param uniquifier
	 * @return
	 */
	public static long extractTime(long seed, long uniquifier) {
		return (initialScramble(seed) ^ uniquifier) & SCRAMBLE_MASK;
	}

	/**
	 * This takes the seed and uniquifier and uses them to produce a time. That
	 * time is then differenced against the provided time (which is assumed to
	 * be after the creation of the Random object).
	 * 
	 * This method exists because the Random constructor indirectly masks the
	 * long value provided by System.nanoTime. If the mask is not applied to the
	 * comparison time then the difference can be wildly wildly inaccurate.
	 * 
	 * @param seed
	 * @param time
	 * @param uniquifier
	 * @return
	 */
	public static long extractTimeDifference(long seed, long time, long uniquifier) {
		return (time & SCRAMBLE_MASK) - extractTime(seed, uniquifier);
	}
}
