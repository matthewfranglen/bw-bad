package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;


/**
 * This tests the powers of the oracle!
 *
 * It is expected that the oracle will not produce accurate numbers immediately.
 * It is expected that the number of available seeds does not increase for every call.
 * It is expected that the oracle will produce accurate numbers when only one seed remains.
 *
 * @author matthew
 */
public class OracleTest {

	private static final Random random = new Random();

	@Test
	@Ignore // this test takes some time to run
	public void testOracle() throws Exception {
		Oracle oracle = new Oracle();

		long seed = RandomInspector.getSeed(random);
		long seedUniquifier = RandomInspector.getSeedUniquifier(random);

		assertTrue("Random uniquifier in set", Oracle.seedUniquifierValues.indexOf(seedUniquifier) >= 0);
		assertTrue("Time within range", RandomInspector.extractTimeDifference(seed, System.nanoTime(), seedUniquifier) < Oracle.SEED_TIME_RANGE_NANOS);

		// 10B seed calculation = 650.324s
		// 500M seed calculation = 36.779s
		{
			long oldSize = oracle.size();
			do {
				int value = random.nextInt(6);
				oracle.calledNextInt(value, 6);

				assertTrue("Oracle is less sure about things", oracle.size() <= oldSize);
				oldSize = oracle.size();
			} while (oracle.size() > 1);
		}

		Random copy = oracle.getRandom();

		assertEquals("Oracle is wrong!", RandomInspector.getSeed(random), RandomInspector.getSeed(copy));

		for (int i = 0;i < 100;i++) {
			assertEquals("Oracle is wrong!", random.nextInt(6), copy.nextInt(6));
		}
	}
}
