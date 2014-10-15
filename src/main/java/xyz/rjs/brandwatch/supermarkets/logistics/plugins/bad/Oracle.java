package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static com.google.common.base.Preconditions.checkState;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(Oracle.class);

	private static final DecimalFormat formatter = new DecimalFormat("#,###");

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
	 * The number of additional calls the seed must pass before the Oracle
	 * fixates on it.
	 */
	private static final int VALIDATION_ROUNDS = 25;

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
	 * This holds the current evaluation round. The seeds are generated in
	 * batches which are then tested. Each time a complete batch fails the next
	 * batch of seeds is generated, offset from the current time by a larger
	 * amount.
	 */
	private int round;

	/**
	 * When seed resolution is attempted passing seeds are stored in this set.
	 * When this has only a single value the Random object can be generated.
	 */
	private Set<Long> seeds;

	/**
	 * This tracks the current round of validation for the single seed that
	 * remains. When a single seed out of a batch remains the validation starts.
	 * The validation tests the remaining seed with additional calls to ensure
	 * that it is accurate.
	 */
	private int validationRound;

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
		seeds = Collections.emptySet();
		round = 0;
		validationRound = 0;
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
		calledNextInt(r -> r.nextInt(bound) == value, bound);
	}

	/**
	 * This should be called when the random object has experienced a method
	 * call which cannot be limited to a single value.
	 *
	 * @param call
	 * @param bound
	 */
	public void calledNextInt(Function<Random, Boolean> call, int bound) {
		try {
			calls.add(call, bound);
			if (bound > 1) {
				state.calledNextInt(this);
			}
		}
		catch (Exception e) {
			logger.error("Failed to add call", e);
		}
		catch (Throwable e) {
			logger.error("Catastrophic error when adding call", e);
			System.exit(1);
		}
	}

	/**
	 * A method which can be called to allow the Oracle to search the available
	 * seed space.
	 */
	public void tick() {
		state.tick(this);
	}

	/**
	 * @return - the number of valid seeds left.
	 */
	public long size() {
		return state.size(this);
	}

	/**
	 * @return - if the oracle has fixated on a single seed.
	 */
	public boolean isFixed() {
		return state == STATE.FIXED;
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
	 * 
	 * @param state
	 */
	private void setState(STATE state) {
		logger.info(String.format("STATE TRANSITION: %s to %s", this.state, state));

		this.state = state;
	}

	/**
	 * Performs work to determine the matching seed.
	 */
	private void processSeeds() {
		if (seeds.isEmpty()) {
			calculateSeeds();
		}
		else {
			reduceSeeds();
		}

		if (seeds.size() == 1) {
			validationRound = 0;
			setState(STATE.VALIDATING);
		}
	}

	/**
	 * This calculates the seeds from the starting range and uniquifiers and
	 * filters them against the existing calls. The surviving seeds are stored.
	 */
	private void calculateSeeds() {
		try {
			logger.info(String.format("Performing round %s filter of %s seeds", formatter.format(round), formatter.format(SeedGenerator.size())));
			long startTime = System.currentTimeMillis();

			seeds = generator.stream(round).parallel().filter(calls::test).mapToObj(seed -> seed).collect(Collectors.toSet());
			round++;

			logger.info(String.format("Filtering completed in %s ms, %s seeds remain", formatter.format(System.currentTimeMillis() - startTime),
					formatter.format(seeds.size())));
		}
		catch (Exception e) {
			logger.error("Failed to calculate seeds", e);
		}
		catch (Throwable e) {
			logger.error("Catastrophic error when calculating seeds", e);
			System.exit(1);
		}
	}

	/**
	 * This takes the available seeds and re-applies the calls to them. If more
	 * calls are available then the number of valid seeds should drop.
	 */
	private void reduceSeeds() {
		try {
			logger.info(String.format("Performing reduction of %s seeds", formatter.format(seeds.size())));
			long startTime = System.currentTimeMillis();

			seeds = seeds.stream().filter(calls::test).collect(Collectors.toSet());

			logger.info(String.format("Reduction completed in %s ms, %s seeds remain", formatter.format(System.currentTimeMillis() - startTime),
					formatter.format(seeds.size())));
		}
		catch (Exception e) {
			logger.error("Failed to reduce seeds", e);
		}
		catch (Throwable e) {
			logger.error("Catastrophic error when reducing seeds", e);
			System.exit(1);
		}
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
					oracle.processSeeds();

					if (oracle.seeds.size() > 1) {
						oracle.setState(LIMITED);
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
				oracle.processSeeds();
			}

			@Override
			public void tick(Oracle oracle) {
				if (oracle.seeds.isEmpty()) {
					oracle.processSeeds();
				}
			}
		},
		/**
		 * The VALIDATING state is when the seeds have been filtered down to 1
		 * and additional values are being used to verify that single seed. If
		 * the seed makes it through the additional tests then the Oracle will
		 * transition to the FIXED state and can produce cloned Random objects.
		 * If the seed fails then the Oracle will return to the LIMITED state.
		 * 
		 * The Oracle does not return to the OPEN state because enough calls
		 * exist to filter the next batch immediately.
		 */
		VALIDATING {

			@Override
			public long size(Oracle oracle) {
				return 1;
			}

			@Override
			public void calledNextInt(Oracle oracle) {
				oracle.reduceSeeds();

				if (oracle.seeds.isEmpty()) {
					oracle.setState(LIMITED);
				}
				else if (oracle.seeds.size() == 1) {
					if (oracle.validationRound >= VALIDATION_ROUNDS) {
						oracle.setState(FIXED);
					}
					else {
						oracle.validationRound++;
					}
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
		 * This provides the ability to perform a unit of work related to
		 * searching for the seed.
		 */
		public void tick(Oracle oracle) {
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
