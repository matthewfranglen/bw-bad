package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

/**
 * This creates and inspects random objects to see if they are predictable
 * enough to work.
 * 
 * @author matthew
 */
public class RandomInspectorTest {

	@Test
	@Ignore
	public void testRandom() throws Exception {
		Random random;

		{
			long nanoStart = System.nanoTime();
			random = new Random();
			long nanoEnd = System.nanoTime();
			long creationTime = Math.abs(nanoEnd - nanoStart);

			// my computer sucks
			assertTrue(String.format("Random creation time (%d) reasonably small", creationTime), creationTime < 250_000);
		}

		// This determines if the seed uniquifier is in the set calculated by
		// Oracle
		long seedUniquifier = RandomInspector.getSeedUniquifier(random);
		int index = Oracle.seedUniquifierValues.indexOf(seedUniquifier);

		assertTrue("Random uniquifier in set", index >= 0);

		// This attempts to read the result of System.nanoTime from the seed.
		// This gets complicated because the seed is formed from the time
		// combined with the uniquifier, which is then scrambled. The scrambling
		// limits the valid bits to 48.
		long seed = RandomInspector.initialScramble(RandomInspector.getSeed(random)) ^ (seedUniquifier & RandomInspector.SCRAMBLE_MASK);

		// This is like deducing the random object after a few calls have been
		// made.
		long currentTime = System.nanoTime();
		long apparentDifference = (seed ^ currentTime) & RandomInspector.SCRAMBLE_MASK;

		// Values:
		// 240_616_638, 38_995_075, 112_812_433 ...
		// Quite large for such a small number of statements
		assertTrue(String.format("Random time (%d) in range", apparentDifference), apparentDifference < 1000_000_000);
	}
}
