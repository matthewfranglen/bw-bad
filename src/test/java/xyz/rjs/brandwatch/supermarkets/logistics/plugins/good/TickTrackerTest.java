package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;


/**
 * @author matthew
 *
 */
public class TickTrackerTest {

	@Test
	public void testTickTracker() {
		TickTracker tracker = new TickTracker();

		assertEquals("Initial state tick", 0, tracker.getTick());

		tracker.tickListener(new ClockTick(5));
		assertEquals("Updated tick", 5, tracker.getTick());

		tracker.tickListener(new ClockTick(10));
		assertEquals("Updated tick", 10, tracker.getTick());

		try {
			tracker.tickListener(null);
			fail("Null clock tick accepted");
		}
		catch (NullPointerException e) {}
	}
}
