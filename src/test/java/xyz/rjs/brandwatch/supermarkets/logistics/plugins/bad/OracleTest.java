package xyz.rjs.brandwatch.supermarkets.logistics.plugins.cheater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad.Oracle;


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
	public void testOracle() {
		Oracle oracle = new Oracle();

		{
			long oldSize = oracle.size();
			do {
				int value = random.nextInt(6);
				oracle.calledNextInt(value, 6);

				assertTrue("Oracle is less sure about things", oracle.size() <= oldSize);
				oldSize = oracle.size();
				System.out.println("loop " + oldSize);
			} while (oracle.size() > 1);
		}

		Random copy = oracle.getRandom();

		for (int i = 0;i < 100;i++) {
			assertEquals("Oracle is wrong!", random.nextInt(6), copy.nextInt(6));
		}
	}
}
