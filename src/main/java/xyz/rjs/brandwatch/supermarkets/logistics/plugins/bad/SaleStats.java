package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.model.events.Customer;

import com.google.common.eventbus.Subscribe;

/**
 * Tracks the time between sales.
 * 
 * @author matthew
 */
@Component
public class SaleStats extends TimeStats {

	@Subscribe
	public void customerListener(Customer customer) {
		trigger();
	}
}
