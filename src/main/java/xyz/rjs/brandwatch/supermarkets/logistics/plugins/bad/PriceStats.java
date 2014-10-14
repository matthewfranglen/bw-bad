package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.model.events.PriceList;

import com.google.common.eventbus.Subscribe;

/**
 * Tracks the time between price changes.
 * 
 * @author matthew
 */
@Component
public class PriceStats extends TimeStats {

	@Subscribe
	public void priceListListener(PriceList list) {
		trigger();
	}
}
