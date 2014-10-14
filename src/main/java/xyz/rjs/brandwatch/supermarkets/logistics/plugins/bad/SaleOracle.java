package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(SaleOracle.class);

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
		final long originalSize = oracle.size();

		oracle.calledNextInt(value, SALE_BOUND);
		if (originalSize > 1) {
			final long size = oracle.size();

			if (size == 1) {
				logger.info("SaleOracle has fixated!");
			}
			else {
				logger.info(String.format("SaleOracle (%d -> %d)", originalSize, size));
			}
		}
	}
}
