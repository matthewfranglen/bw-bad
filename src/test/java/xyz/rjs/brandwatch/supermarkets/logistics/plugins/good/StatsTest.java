package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author matthew
 *
 */
public class StatsTest {

	@Test
	public void testStats() {
		Stats stats = new Stats();

		stats.add(5);
		stats.add(10);
		stats.add(15);
		assertEquals("Average calculation", 10.0, stats.average(), 0.1);

		// standard deviation (m = average, N = element, l = length):
		// sqrt( ( sum(square(N - m)) / l )
		double standardDeviation = Math.sqrt(((5.0 * 5.0) + (5.0 * 5.0)) / 3.0);
		assertEquals("Standard deviation calculation", standardDeviation, stats.standardDeviation(), 0.1);
		assertEquals("Two standard deviation confidence calculation", 10 + (standardDeviation * 2), stats.confidence(), 0.1);

		assertEquals("Average calculation", 3.0, stats.average(10), 0.1);
		assertEquals("Standard deviation calculation", 5.1, stats.standardDeviation(10), 0.1);
		assertEquals("Two standard deviation confidence calculation", 13.2, stats.confidence(10), 0.1);

		try {
			stats.standardDeviation(1);
			fail("Stats can operate with more values than time");
		}
		catch (IndexOutOfBoundsException e) {
		}
		try {
			stats.confidence(1);
			fail("Stats can operate with more values than time");
		}
		catch (IndexOutOfBoundsException e) {
		}
	}
}
