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

	public static final int RISING_PRICE_VALUE = 0;
	private static final int PRICE_BOUND = 3;

	private int price;

	public PriceOracle() {
		super(PRICE_BOUND);
		price = 1;
		calledNextInt(r -> { r.nextInt(10); return true; }); // The random object is called once during the initialization
	}

	@Subscribe
	public void priceListListener(PriceList list) {
		// The change is 1 - Random.nextInt(3).
		// To reverse this we need the oldPrice - newPrice (the change) plus one:
		// new_price = old_price + 1 - random
		// random    = old_price + 1 - new_price

		final int value = price + 1 - list.getCurrentPrice();

		if (price == 1 && list.getCurrentPrice() == 1) {
			// When the price is 1 the price cannot go down. This means that the number can only not be 0.
			calledNextInt(r -> r.nextInt(PRICE_BOUND) != RISING_PRICE_VALUE);
		}
		else {
			calledNextInt(value);
		}
		price = list.getCurrentPrice();
	}
}
