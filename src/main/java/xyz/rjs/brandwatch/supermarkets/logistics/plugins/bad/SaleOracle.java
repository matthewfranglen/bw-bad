package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.Random;

import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.Sale;

import com.google.common.eventbus.Subscribe;

/**
 * Determines the Random object backing the sale amount based on Sale events.
 * 
 * @author matthew
 */
@Component
public class SaleOracle extends AbstractPlugin {

	private static final int SALE_BOUND = 6;

	private final Oracle oracle;

	public SaleOracle() {
		oracle = new Oracle();
	}

	public boolean isFixed() {
		return oracle.size() == 1;
	}

	public Random getRandom() {
		return oracle.getRandom();
	}

	@Subscribe
	public void saleListener(Sale sale) {
		// Get the Random.nextInt result by subtracting one
		final int value = sale.getAmountSold() - 1;

		oracle.calledNextInt(value, SALE_BOUND);
	}
}
