package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.model.events.PriceList;

import com.google.common.eventbus.Subscribe;

/**
 * Determines the Random object backing the price based on PriceList events.
 * 
 * @author matthew
 */
@Component
public class PriceOracle extends OracleWrapper {

	private static final int PRICE_BOUND = 3;

	private int price;

	public PriceOracle() {
		super(PRICE_BOUND);
		price = 1;
	}

	@Subscribe
	public void priceListListener(PriceList list) {
		// Get the Random.nextInt result by adding one
		final int value = 1 + list.getCurrentPrice() - price;

		if (price == 1 && value == 1) {
			// When the price is 1 the price cannot go down. This means that the
			// value of 1 is ambiguous as it could be a 0 that was fixed.
			calledNextInt(r -> r.nextInt(3) < 2);
		}
		else {
			calledNextInt(value);
		}
		price = list.getCurrentPrice();
	}
}
