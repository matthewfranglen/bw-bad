package xyz.rjs.brandwatch.supermarkets.logistics.plugins.ugly;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.ArrivalNotification;
import xyz.rjs.brandwatch.supermarkets.model.events.Order;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

import com.google.common.eventbus.Subscribe;


/**
 * Simple order tracker provides a count of outstanding orders.
 * 
 * @author matthew
 */
@Component
public class OrderTracker extends AbstractPlugin {

	@Autowired
	private Warehouse warehouse;

	private final List<Integer> orders;

	public OrderTracker() {
		orders = new ArrayList<>();
	}

	@Subscribe
	public void arrivalListener(ArrivalNotification arrival) {
		// Only arrivals at the Warehouse need to be tracked.
		// Warehouse to Shop is instant (see WarehouseManagementSystem).
		if (arrival.getPlace() != warehouse) {
			return;
		}

		orders.remove(Integer.valueOf(arrival.getAmount()));
	}

	@Subscribe
	public void orderListener(Order order) {
		orders.add(order.getVolume());
	}

	public int size() {
		return orders.stream().mapToInt(Integer::intValue).sum();
	}
}
