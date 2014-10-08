package xyz.rjs.brandwatch.supermarkets.logistics.plugins.cheater;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;


/**
 * This predicts the values that java.util.Random objects will produce.
 * 
 * The Oracle can predict the future. Lucky for it, Random objects are predictable.
 *
 * The source code for java.util.Random creates randoms like so:
 *
    public Random() {
        this(seedUniquifier() ^ System.nanoTime());
    }

    private static long seedUniquifier() {
        // L'Ecuyer, "Tables of Linear Congruential Generators of
        // Different Sizes and Good Lattice Structure", 1999
        for (;;) {
            long current = seedUniquifier.get();
            long next = current * 181783497276652981L;
            if (seedUniquifier.compareAndSet(current, next))
                return next;
        }
    }

    private static final AtomicLong seedUniquifier
        = new AtomicLong(8682522807148012L);
 *
 * The important thing here is that:
 * 1) The current time forms part of the initial seed.
 * 2) This is combined with a series of values based on the constants 8682522807148012L and 181783497276652981L
 *
 * Only a small number of Random objects are created in this project, and most
 * of them will be created within the first second that the program runs. It
 * should be possible to deduce the starting seed for each Random object of
 * interest based on constraints that observed values must pass.
 *
 * These constraints are based on knowledge of the source code, so given the code:
 *
    public int getSomeValue() {
        return random.nextInt() % 4;
    }
 *
 * Then any seed which does not produce 32 initial bits which match the
 * observed modulo result cannot be the starting seed. In this example that
 * would reduce the space of valid seeds by 3/4. Given that each known value
 * can reduce the space by a factor it should only take a few to narrow down to
 * the single starting seed.
 *
 * @author matthew
 */
public class Oracle {
	// NOTE: It seems that there are only 5 Random objects created in this (2x in CustomerService, 1x in Supermarket, 2x in Supplier)
	// NOTE: Imports such as Fairy do create their own. The true number must be determined.
	// NOTE: The delivery of an item from the warehouse also creates one.
	// NOTE: (in general) the price starts at 1 which is as low as it can be, so buying everything at the start might be good
	// NOTE: Crazy application of reflection might allow access to the underlying random objects for a really dirty oracle
	// NOTE: Oh! Oh! https://stackoverflow.com/a/12784901 that could totally work

	private static final long SEED_UNIQUIFIER_INITIAL_VALUE = 8682522807148012L;
	private static final long SEED_UNIQUIFIER_FACTOR = 181783497276652981L;

	private static final Set<Long> seedUniquifierValues;
	
	static {
		long value = SEED_UNIQUIFIER_INITIAL_VALUE;
		seedUniquifierValues = new HashSet<Long>();

		for (int i = 0;i < 10;i++) {
			seedUniquifierValues.add(value);
			value *= SEED_UNIQUIFIER_FACTOR;
		}
	}

	/**
	 * This should be called when the random object has experienced a nextDouble call.
	 * The implementation of this varies based on the version of Java. From the documentation:
	 *
	 * The method {@code nextDouble} is implemented by class {@code Random}
	 * as if by:
	 *  <pre> {@code
	 * public double nextDouble() {
	 *   return (((long)next(26) << 27) + next(27))
	 *     / (double)(1L << 53);
	 * }}</pre>
	 *
	 * In early versions of Java, the result was incorrectly calculated as:
	 *  <pre> {@code
	 *   return (((long)next(27) << 27) + next(27))
	 *     / (double)(1L << 54);}</pre>
	 *
	 * This matters because the older implemetation consumes an additional bit of the random stream.
	 * The older implementation can be verified to be before Java 1.5.0, as the API documentation
	 * from that release references the correct implementation.
	 *
	 * @param constraint - a test that the generated value must pass.
	 */
	public void calledNextDouble(Constraint<Double> constraint) {}

	/**
	 * This should be called when the random object has experienced a nextInt(bound) call.
	 * The implementation 
	 *
	 * @param constraint
	 * @param bound
	 */
	public void calledNextInt(Constraint<Integer> constraint, int bound) {}

	/**
	 * @return - the number of valid seeds left.
	 */
	public int size() {
		throw new RuntimeException("Not implemented");
	}

	/**
	 * This will return the predicted random object at the current state.
	 * This requires that the size of the oracle is 1.
	 * If there is more than one valid seed available then this will throw an exception.
	 *
	 * @return
	 */
	public Random getRandom() {
		throw new RuntimeException("Not implemented");
	}

	public static interface Constraint<T> {
		/**
		 * This will accept any number presented.
		 * It should be used when a call to a random method has been made but the outcome has not been observed.
		 */
		public static final Constraint<?> ANY = o -> true;

		/**
		 * This tests the value to see if it passes the constraint.
		 *
		 * @param value
		 * @return
		 */
		public boolean passes(T value);
	}
}
