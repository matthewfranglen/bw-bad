package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static com.google.common.base.Preconditions.checkState;

import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This predicts the values that java.util.Random objects will produce based on
 * observed values produced by those Random objects.
 * 
 * This will be used to predict the values that CustomerService and Supplier
 * will generate. This will allow the prediction of the next sale and the next
 * purchase price (and ones following that...).
 * 
 * It is not feasible to observe the Random objects that cause the
 * CustomerService and Supplier to fire. They ARE observable when the value
 * produced is below the threshold, but they ARE NOT observable when it is at or
 * above the threshold. This means that any attempt to generate the seeds for
 * them would have to rank the seeds instead of filtering them correctly.
 * 
 * (as I write this I realise that the negative status can be inferred from a
 * tick that does not produce the given event, but this demonstrates the
 * technique I want to show without having to cover that as well).
 *
 * Fundamentally this works because Java Random objects are predictable. They
 * are designed to produce the same output when started with the same seed. The
 * source code for java.util.Random creates randoms like so:
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

	/**
	 * The Random object seed could be one of many billions of values. It is
	 * infeasible to store that many values and reduce them on each method call.
	 * It is possible to estimate the number of seeds that remain which match
	 * the current sequence of calls. This variable stores the limit where the
	 * Oracle will switch from estimating the remaining space to tracking
	 * individual seeds.
	 */
	public static final int SIZE_TRANSITION_LIMIT = 100;

	/**
	 * This holds the creation time of the Oracle. <strong>It is assumed that
	 * the Random object has been created at or before this time.</strong>
	 */
	private final long startingTime;

	/**
	 * This holds the seed generator, which is used to generate the initial set
	 * of seeds for the reduceSeeds method.
	 */
	private final SeedGenerator generator;

	/**
	 * This holds the list of calls to nextInt, in order.
	 */
	private final SeedTest calls;

	/**
	 * When seed resolution is attempted passing seeds are stored in this set.
	 * When this has only a single value the Random object can be generated.
	 */
	private Set<Long> seeds;

	/**
	 * When seed resolution has completed the passing seed is stored in this
	 * variable.
	 */
	private long fixedSeed;

	/**
	 * This is the current state of the Oracle. The Oracle transitions from wild
	 * guesstimates to a limited set of seeds before finally settling on a
	 * single seed.
	 */
	private STATE state;

	public Oracle() {
		startingTime = System.nanoTime();
		generator = new SeedGenerator(startingTime);
		calls = new SeedTest();
		state = STATE.OPEN;
	}

	/**
	 * This should be called when the random object has experienced a
	 * nextInt(bound) call.
	 *
	 * @param value
	 * @param bound
	 */
	public void calledNextInt(int value, int bound) {
		calls.add(r -> r.nextInt(bound) == value, bound);
		state.calledNextInt(this);
	}

	/**
	 * This should be called when the random object has experienced a
	 * method call which cannot be limited to a single value.
	 *
	 * @param call
	 * @param bound
	 */
	public void calledNextInt(Function<Random, Boolean> call, int bound) {
		calls.add(call, bound);
		state.calledNextInt(this);
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
	 * Transition the state of the Oracle.
	 * @param state
	 */
	private void setState(STATE state) {
		this.state = state;
	}

	/**
	 * This calculates the seeds from the starting range and uniquifiers and
	 * filters them against the existing calls. The surviving seeds are stored.
	 */
	private void calculateSeeds() {
		seeds = generator.stream().parallel().filter(calls::test).mapToObj(seed -> seed).collect(Collectors.toSet());
		if (seeds.size() == 1) {
			fixedSeed = seeds.iterator().next();
		}

		checkState(!seeds.isEmpty(), "All available seeds eliminated, cannot continue");
	}

	/**
	 * This takes the available seeds and re-applies the calls to them. If more
	 * calls are available then the number of valid seeds should drop.
	 */
	private void reduceSeeds() {
		seeds = seeds.stream().filter(calls::test).collect(Collectors.toSet());
		if (seeds.size() == 1) {
			fixedSeed = seeds.iterator().next();
		}

		checkState(!seeds.isEmpty(), "All available seeds eliminated, cannot continue");
	}

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
	private static enum STATE {
		/**
		 * The OPEN state is when no attempt to filter the seeds has been
		 * performed. At this point the size value is an estimate. When a new
		 * call comes in which reduces the estimate below a transition
		 * threshold, the seeds will be filtered and the Oracle will transition
		 * into the next state.
		 * 
		 * The next state is likely to be LIMITED, but can be FIXED if only a
		 * single seed passed the filter.
		 */
		OPEN {

			@Override
			public long size(Oracle oracle) {
				return oracle.calls.estimatedSize(SeedGenerator.size());
			}

			@Override
			public void calledNextInt(Oracle oracle) {
				if (oracle.size() < SIZE_TRANSITION_LIMIT) {
					// this checks for an empty seed set
					oracle.calculateSeeds();

					if (oracle.seeds.size() > 1) {
						oracle.setState(LIMITED);
					}
					else {
						oracle.setState(FIXED);
					}
				}
			}
		},
		/**
		 * The LIMITED state is when the seeds have been filtered and a limited
		 * number remain. The set of seeds needs to be reduced to a single valid
		 * seed. Each time a call comes in the set will be filtered. When a
		 * final seed remains the Oracle will transition to the FIXED state.
		 */
		LIMITED {

			@Override
			public long size(Oracle oracle) {
				return oracle.seeds.size();
			}

			@Override
			public void calledNextInt(Oracle oracle) {
				oracle.reduceSeeds();

				if (oracle.seeds.size() == 1) {
					oracle.setState(FIXED);
				}
			}
		},
		/**
		 * The FIXED state is when the seeds have been filtered to a single
		 * value. At this point the Random object can be requested.
		 * 
		 * This is the only state that will return a size of 1.
		 */
		FIXED {

			@Override
			public long size(Oracle oracle) {
				return 1;
			}

			@Override
			public Random getRandom(Oracle oracle) {
				Random result = new Random(oracle.fixedSeed);
				checkState(oracle.calls.test(result), "Oracle fixed seed fails known tests");

				return result;
			}
		};

		/**
		 * Get the number of seeds that are still valid.
		 * 
		 * If this is below SIZE_TRANSITION_LIMIT then this is not an estimate.
		 * If this is above or equal to that then it is probable it is an
		 * estimate.
		 * 
		 * When this returns 1 getRandom can be called without error.
		 * 
		 * @param oracle
		 * @return
		 */
		abstract public long size(Oracle oracle);

		/**
		 * This provides the result of a call to nextInt on the Random object
		 * under study. All seeds that the Oracle considers must match this
		 * result.
		 * 
		 * This MUST be called in the correct order. This MUST be called once
		 * for every call to nextInt made on the Random object.
		 * 
		 * @param oracle
		 * @param value
		 * @param bound
		 */
		public void calledNextInt(Oracle oracle) {
		}

		/**
		 * This will return the Random object from a fixated Oracle. An Oracle
		 * has fixated if the size of it is 1.
		 * 
		 * The Random object provided will match the one under study if EVERY
		 * call to nextInt has been recorded in the Oracle. This means you MUST
		 * continue to call nextInt when new observed values are available.
		 * 
		 * @param oracle
		 * @return
		 */
		public Random getRandom(Oracle oracle) {
			throw new IllegalStateException("Oracle has not fixated");
		}
	}
}
