package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * This predicts the values that java.util.Random objects will produce.
 * 
 * The Oracle can predict the future. Lucky for it, Random objects are
 * predictable.
 *
 * The source code for java.util.Random creates randoms like so:
 *
 * <pre>
 * <code>
 *     public Random() {
 *         this(seedUniquifier() ^ System.nanoTime());
 *     }
 * 
 *     private static long seedUniquifier() {
 *         // L'Ecuyer, "Tables of Linear Congruential Generators of
 *         // Different Sizes and Good Lattice Structure", 1999
 *         for (;;) {
 *             long current = seedUniquifier.get();
 *             long next = current * 181783497276652981L;
 *             if (seedUniquifier.compareAndSet(current, next))
 *                 return next;
 *         }
 *     }
 * 
 *     private static final AtomicLong seedUniquifier
 *         = new AtomicLong(8682522807148012L);
 * </code>
 * </pre>
 * 
 * The important thing here is that:
 * <ol>
 * <li>The current time forms part of the initial seed.</li>
 * <li>This is combined with a series of values based on the constants
 * 8682522807148012L and 181783497276652981L</li>
 * </ol>
 *
 * Only a small number of Random objects are created in this project, and most
 * of them will be created within the first second that the program runs. It
 * should be possible to deduce the starting seed for each Random object of
 * interest based on constraints that observed values must pass.
 *
 * These constraints are based on knowledge of the source code, so given the
 * code:
 *
 * <pre>
 * </code>
 *     public int getSomeValue() {
 *         return random.nextInt() % 4;
 *     }
 * </code>
 * </pre>
 *
 * Then any seed which does not produce 32 initial bits which match the observed
 * modulo result cannot be the starting seed. In this example that would reduce
 * the space of valid seeds by 3/4. Given that each known value can reduce the
 * space by a factor it should only take a few to narrow down to the single
 * starting seed.
 *
 * @author matthew
 */
public class Oracle {

	// NOTE: It seems that there are only 5 Random objects created in this (2x
	// in CustomerService, 1x in Supermarket, 2x in Supplier)
	// NOTE: Imports such as Fairy do create their own. The true number must be
	// determined.
	// NOTE: The delivery of an item from the warehouse also creates one.
	// NOTE: (in general) the price starts at 1 which is as low as it can be, so
	// buying everything at the start might be good
	// NOTE: Crazy application of reflection might allow access to the
	// underlying random objects for a really dirty oracle
	// NOTE: Oh! Oh! https://stackoverflow.com/a/12784901 that could totally
	// work

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
	private static final List<Long> seedUniquifierValues;

	static {
		long value = SEED_UNIQUIFIER_INITIAL_VALUE;
		seedUniquifierValues = new ArrayList<Long>();

		for (int i = 0; i < SEED_UNIQUIFIER_VALUE_COUNT; i++) {
			seedUniquifierValues.add(value);
			value *= SEED_UNIQUIFIER_FACTOR;
		}
	}

	/**
	 * The Random object seed could be one of many billions of values. It is
	 * infeasible to store that many values and reduce them on each method call.
	 * It is possible to estimate the number of seeds that remain which match
	 * the current sequence of calls. This variable stores the limit where the
	 * Oracle will switch from estimating the remaining space to tracking
	 * individual seeds.
	 */
	private static final int SIZE_TRANSITION_LIMIT = 100;

	/**
	 * This holds the different states that the Oracle can move through.
	 *
	 * The Oracle has to deal with a very large potential space. This space also
	 * shrinks rapidly but whill not shrink in a completely predictable way.
	 * Finally the Oracle will fixate on a single value.
	 *
	 * Given that the requirements of each of these states is so different, it
	 * makes sense to implement them as separate states.
	 *
	 * @author matthew
	 */
	private static enum ORACLE_STATE {
		OPEN {

			@Override
			public long size(Oracle oracle) {
				long denominator = oracle.calls.stream().reduce(1L, (accumulated, entry) -> accumulated * entry.bound, (a, b) -> a * b);

				return (SeedGenerator.SEED_UNIQUIFIER_VALUE_COUNT * SeedGenerator.SEED_TIME_RANGE_NANOS) / denominator;
			}

			@Override
			public void calledNextInt(Oracle oracle, int value, int bound) {
				oracle.calls.add(new RandomCall(value, bound));

				if (oracle.size() < SIZE_TRANSITION_LIMIT) {
					oracle.calculateSeeds();
				}
			}
		},
		LIMITED {

			@Override
			public long size(Oracle oracle) {
				return oracle.seeds.size();
			}

			@Override
			public void calledNextInt(Oracle oracle, int value, int bound) {
				oracle.reduceSeeds(new RandomCall(value, bound));
			}
		},
		FIXED {

			@Override
			public long size(Oracle oracle) {
				return 1;
			}

			@Override
			public void calledNextInt(Oracle oracle, int value, int bound) {
				oracle.calls.add(new RandomCall(value, bound));
			}

			@Override
			public Random getRandom(Oracle oracle) {
				Random result = new Random(oracle.fixedSeed);
				oracle.calls.stream().forEachOrdered(e -> result.nextInt(e.bound));

				return result;
			}
		};

		abstract public long size(Oracle oracle);

		abstract public void calledNextInt(Oracle oracle, int value, int bound);

		public Random getRandom(Oracle oracle) {
			throw new IllegalStateException("Oracle has not fixated");
		}
	}

	/**
	 * This holds the creation time of the Oracle. <strong>It is assumed that
	 * the Random object has been created at or before this time.</strong>
	 */
	private final long startingTime;

	/**
	 * This holds the list of calls to nextInt, in order.
	 */
	private final List<RandomCall> calls;

	/**
	 * When seed resolution is attempted passing seeds are stored in this set.
	 * When this has only a single value the Random object can be generated.
	 */
	private Set<Long> seeds;

	/**
	 * When seed resolution has completed the passing seed is stored in this variable.
	 */
	private long fixedSeed;

	private ORACLE_STATE state;

	public Oracle() {
		startingTime = System.nanoTime();
		calls = new ArrayList<RandomCall>();
		state = ORACLE_STATE.OPEN;
	}

	/**
	 * This should be called when the random object has experienced a
	 * nextInt(bound) call. The implementation
	 *
	 * @param constraint
	 * @param bound
	 */
	public void calledNextInt(int value, int bound) {
		state.calledNextInt(this, value, bound);
	}

	/**
	 * @return - the number of valid seeds left.
	 */
	public long size() {
		return state.size(this);
	}

	/**
	 * This will return the predicted random object at the current state. This
	 * requires that the size of the oracle is 1.
	 *
	 * @return
	 * @throws IllegalStateException
	 *             - If there is more than one valid seed available then this
	 *             will throw an exception.
	 */
	public Random getRandom() {
		return state.getRandom(this);
	}

	/**
	 * This calculates the seeds from the starting range and uniquifiers and
	 * filters them against the existing calls. The surviving seeds are stored
	 * and the oracle transitions to the LIMITED state.
	 */
	private void calculateSeeds() {
		seeds =
			LongStream.iterate(startingTime - SEED_TIME_RANGE_NANOS, t -> t + 1)
				.limit(SEED_TIME_RANGE_NANOS)
				.flatMap(this::mapTimeToSeeds)
				.filter(this::filterSeed)
				.mapToObj(seed -> seed)
				.collect(Collectors.toSet());

		if (seeds.size() > 1) {
			state = ORACLE_STATE.LIMITED;
		}
		else {
			state = ORACLE_STATE.FIXED;
		}
	}

	private void reduceSeeds(RandomCall call) {
		calls.add(call);
		seeds = seeds.stream().filter(this::filterSeed).collect(Collectors.toSet());

		if (seeds.size() <= 1) {
			state = ORACLE_STATE.FIXED;
		}
	}

	private boolean filterSeed(long seed) {
		Random r = new Random(seed);
		return calls.stream().allMatch(c -> c.test(r));
	}

	private LongStream mapTimeToSeeds(long time) {
		return seedUniquifierValues.stream().mapToLong(u -> time * u);
	}
}
