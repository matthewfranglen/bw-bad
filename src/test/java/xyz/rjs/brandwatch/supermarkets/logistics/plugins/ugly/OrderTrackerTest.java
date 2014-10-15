package xyz.rjs.brandwatch.supermarkets.logistics.plugins.ugly;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.ugly.OrderTracker;
import xyz.rjs.brandwatch.supermarkets.model.events.ArrivalNotification;
import xyz.rjs.brandwatch.supermarkets.model.events.Order;
import xyz.rjs.brandwatch.supermarkets.sim.DeliverablePlace;


/**
 * @author matthew
 *
 */
public class OrderTrackerTest {

	@Test
	public void testOrderTracker() {
		OrderTracker tracker;

		tracker = new OrderTracker(); // has a null warehouse
		assertEquals("OrderTracker initially empty", 0, tracker.size());

		Order order = new Order();
		order.setVolume(5);
		tracker.orderListener(order);
		assertEquals("OrderTracker accepts any order", 5, tracker.size());

		tracker.orderListener(order);
		assertEquals("OrderTracker accepts any order", 10, tracker.size());

		tracker.arrivalListener(new ArrivalNotification(null, 5));
		assertEquals("OrderTracker accepts matching arrivals", 5, tracker.size());

		tracker.arrivalListener(new ArrivalNotification(null, 10));
		assertEquals("OrderTracker accepts matching arrivals", 5, tracker.size());

		tracker.arrivalListener(new ArrivalNotification(new DeliverablePlace(null) {}, 5));
		assertEquals("OrderTracker accepts matching arrivals", 5, tracker.size());

		tracker.arrivalListener(new ArrivalNotification(null, 5));
		assertEquals("OrderTracker accepts matching arrivals", 0, tracker.size());
	}
}
