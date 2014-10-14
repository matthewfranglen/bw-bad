package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.model.events.PriceList;

import com.google.common.eventbus.Subscribe;

/**
 * Determines the Random object backing the price based on PriceList events.
 * 
 * @author matthew
 */
@Component
public class PriceOracle extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(PriceOracle.class);

	private static final int PRICE_BOUND = 3;

	private final Oracle oracle;

	private int price;

	public PriceOracle() {
		oracle = new Oracle();
		price = 1;
	}

	public boolean isFixed() {
		return oracle.size() == 1;
	}

	public Random getRandom() {
		return oracle.getRandom();
	}

	@Subscribe
	public void priceListListener(PriceList list) {
		// Get the Random.nextInt result by adding one
		final int value = 1 + list.getCurrentPrice() - price;
		final long originalSize = oracle.size();

		if (price == 1 && value == 1) {
			// When the price is 1 the price cannot go down. This means that the
			// value of 1 is ambiguous as it could be a 0 that was fixed.
			oracle.calledNextInt(r -> r.nextInt(3) < 2, PRICE_BOUND);
		}
		else {
			oracle.calledNextInt(value, PRICE_BOUND);
		}
		price = list.getCurrentPrice();

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
