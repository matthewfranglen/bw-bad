package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;


/**
 * This wraps up the tests that are applied to seeds to determine if they are valid.
 * 
 * @author matthew
 */
public class SeedTest {

	private final List<Function<Random, Boolean>> operations;
	private long reduction;

	public SeedTest() {
		operations = new ArrayList<>();
		reduction = 1;
	}

	public void add(Function<Random, Boolean> operation, long reduction) {
		checkArgument(operation != null, "Operation must be provided");
		checkArgument(reduction > 0, "Reduction must be greater than zero");

		operations.add(operation);
		this.reduction *= reduction;
	}

	public long estimatedSize(long range) {
		return range / reduction;
	}

	public int operations() {
		return operations.size();
	}

	public boolean test(long seed) {
		return test(new Random(seed));
	}

	public boolean test(Random random) {
		return operations.stream().allMatch(o -> o.apply(random));
	}
}
