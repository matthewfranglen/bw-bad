package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;


/**
 * @author matthew
 *
 */
public class SeedTestTest {

	@Test
	public void testSeedTest() throws Exception {
		SeedTest set = new SeedTest();
		Random random = new Random();
		long scrambledSeed = getSeed(random),
			seed = RandomInspector.initialScramble(scrambledSeed),
			uniquifier = RandomInspector.getSeedUniquifier(random),
			time = RandomInspector.extractTime(scrambledSeed, uniquifier);

		assertEquals("Set starts empty", 0, set.operations());
		assertTrue("All seeds initially valid", set.test(0));
		assertTrue("All seeds initially valid", set.test(Long.MAX_VALUE));
		assertTrue("All seeds initially valid", set.test(Long.MIN_VALUE));
		assertTrue("All seeds initially valid", set.test(seed));

		double value = random.nextDouble();
		set.add(r -> r.nextDouble() == value, Long.MAX_VALUE);
		// nextDouble consumes more than an entire seed of entropy
		assertEquals("Set has an operation", 1, set.operations());

		assertTrue("Valid seed continues to be valid", set.test(seed));
		assertTrue("Other seeds become invalid", !set.test(0));
		assertTrue("Other seeds become invalid", !set.test(Long.MAX_VALUE));
		assertTrue("Other seeds become invalid", !set.test(Long.MIN_VALUE));

		List<Long> valid = LongStream.range(time - 1000, time + 1000)
				.flatMap(v -> SeedGenerator.seedUniquifierValues.stream().mapToLong(u -> v ^ u))
				.filter(set::test)
				.mapToObj(Long::new)
				.collect(Collectors.toList());
		assertEquals("Single valid seed found", 1, valid.size());
	}

	private long getSeed(Random random) {
		try {
			return RandomInspector.getSeed(random);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
