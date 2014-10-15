package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.model.events.Customer;

import com.google.common.eventbus.Subscribe;

/**
 * Determines the Random object backing the sale amount based on Sale events.
 * 
 * @author matthew
 */
@Component
public class SaleOracle extends OracleWrapper {

	private static final int SALE_BOUND = 6;

	public SaleOracle() {
		super(SALE_BOUND);
	}

	@Subscribe
	public void customerListener(Customer sale) {
		// Get the Random.nextInt result by subtracting one
		final int value = sale.getStuffNeeded() - 1;

		calledNextInt(value);
	}
}
