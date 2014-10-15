package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import static com.google.common.base.Preconditions.checkState;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;
import xyz.rjs.brandwatch.supermarkets.logistics.plugins.ugly.OrderTracker;
import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;
import xyz.rjs.brandwatch.supermarkets.model.events.Order;
import xyz.rjs.brandwatch.supermarkets.model.events.PriceList;
import xyz.rjs.brandwatch.supermarkets.sim.Shop;
import xyz.rjs.brandwatch.supermarkets.sim.Warehouse;

import com.google.common.eventbus.Subscribe;

/**
 * The bad plugin inspects the size of sales and the variations in prices to
 * determine the underlying Random generator responsible for the outcomes. This
 * then uses advance knowledge of sales to stock the shop correctly, and advance
 * knowledge of prices to order stock to cover any price increases.
 * 
 * The bad plugin inspects the value of the sales and the price changes, but
 * does not track when they occur. This is the most complicated plugin to get
 * right, and so the decision was made to limit the complexity of the problem.
 * To estimate the period between sales and price changes the Stats class from
 * the good plugin will be used.
 * 
 * The bad plugin will gather data until duplicates of the target Random objects
 * are calculated. These will then be used to track sales and price rises. The
 * sale tracker will stock the shop to meet the next two sales. The price
 * tracker will buy stock to cover any increase in the price (this would be
 * limited to the balance but the Balance class does not provide access, instead
 * it is limited to 100 changes).
 * 
 * It is quite a bit slower to calculate the Supplier Random. The chance of a
 * price event is lower than the chance of a sale. Furthermore when the price is
 * 1/unit the price cannot decrease further (making -1 and 0 indistinguishable).
 * The saving grace is that every single price event is reported, so a fixed set
 * of transitions is still possible.
 * 
 * @author matthew
 *
 */
@Component
public class BadPlugin extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(BadPlugin.class);

	/**
	 * Get stock to cover initial demand while in GATHER state
	 */
	private static final int DESIRED_TOTAL_STOCK = 85;
	/**
	 * Stock limit to force STABLE state
	 */
	private static final int CRITICAL_TOTAL_STOCK = 30;
	/**
	 * Have the shop hold two maximum sales of stock while in GATHER state
	 */
	private static final int DESIRED_SHOP_STOCK = 2 * 6;
	/**
	 * This is the buy price at which it becomes unprofitable to buy any stock.
	 */
	private static final int PRICE_LIMIT = 10;

	/**
	 * The warehouse is required to create orders.
	 */
	@Autowired
	private Warehouse warehouse;

	/**
	 * The shop is required to stock from the warehouse.
	 */
	@Autowired
	private Shop shop;

	/**
	 * Tracks the time between price fluctuations
	 */
	@Autowired
	private PriceStats prices;

	/**
	 * Tracks the Random object underlying the price fluctuation directions.
	 */
	@Autowired
	private PriceOracle priceOracle;

	/**
	 * Tracks time between sales
	 */
	@Autowired
	private SaleStats sales;

	/**
	 * Tracks the Random object underlying the quantity sold.
	 */
	@Autowired
	private SaleOracle saleOracle;

	/**
	 * Keeps track of orders that have not yet arrived.
	 */
	@Autowired
	private OrderTracker orders;

	/**
	 * Keeps track of the current price and the price pre-rise.
	 */
	private int price, stablePrice;

	/**
	 * The current state of the plugin.
	 */
	private STATE state;

	public BadPlugin() {
		state = STATE.START;
		price = stablePrice = 1;
	}

	@Subscribe
	public void priceListener(PriceList price) {
		this.price = price.getCurrentPrice();

		// The stable price is kept up to date like this because it is
		// explicitly reset when it needs to grow.
		if (this.price < stablePrice) {
			stablePrice = this.price;
		}

		try {
			state.priceListener(this);
		}
		catch (Exception e) {
			logger.error("Failed to execute priceListener", e);
		}
		catch (Throwable e) { // OutOfMemoryError etc
			logger.error("Critical failure to execute priceListener", e);
			System.exit(1);
		}
	}

	@Subscribe
	public void tickListener(ClockTick tick) {
		try {
			state.tickListener(this);
		}
		catch (Exception e) {
			logger.error("Failed to execute tickListener", e);
		}
	}

	/**
	 * Creates the order.
	 * 
	 * @param volume
	 */
	private void placeOrder(int volume) {
		Order order = new Order();
		order.setWarehouse(warehouse);
		order.setVolume(volume);
		eventBus.post(order);
	}

	/**
	 * Indicates if the next price change is a rise.
	 * 
	 * @return
	 */
	private boolean isPriceRising() {
		final Random pricesRandom = priceOracle.getRandom();

		return pricesRandom.nextInt(3) == PriceOracle.RISING_PRICE_VALUE;
	}

	/**
	 * Gets the total stock available if all orders arrived.
	 * 
	 * @return
	 */
	private int getTotalStock() {
		return warehouse.getStock() + shop.getStock() + orders.size();
	}

	/**
	 * Transitions the state of the plugin.
	 * 
	 * @param state
	 */
	private void setState(STATE state) {
		logger.info(String.format("STATE TRANSITION: %s to %s", this.state, state));

		// Whenever the STABLE state is reached the current price is the stable price
		if (state == STATE.STABLE) {
			stablePrice = price;
		}
		this.state = state;
	}

	/**
	 * Restocks the shop that makes the sales.
	 */
	private void stockShop() {
		stockShop(DESIRED_SHOP_STOCK);
	}

	/**
	 * Restocks the shop that makes the sales.
	 */
	private void stockShop(final int stock) {
		final int shopStock = shop.getStock(), warehouseStock = warehouse.getStock();

		if (shopStock < stock && warehouseStock > 0) {
			int volume = Math.min(stock - shopStock, warehouseStock);
			shop.addStock(volume);
			warehouse.setStock(warehouseStock - volume);
		}
	}

	/**
	 * Restocks the shop for two future sales.
	 */
	private void predictShop() {
		final Random sales = saleOracle.getRandom();
		final int stock = 2 + sales.nextInt(6) + sales.nextInt(6);

		stockShop(stock);
	}

	/**
	 * Issues any orders required to bring the total stock to the desired
	 * amount.
	 */
	private void stockWarehouse() {
		final int currentStock = getTotalStock();
		if (currentStock < DESIRED_TOTAL_STOCK) {
			placeOrder(DESIRED_TOTAL_STOCK - currentStock);
		}
	}

	/**
	 * Issues any orders required to cover the time when the prices will rise
	 * until they come back to their current level.
	 */
	private void predictWarehouse() {
		final Random salesRandom = saleOracle.getRandom(), pricesRandom = priceOracle.getRandom();

		// The price will drop to the current level (or below) after a number of
		// transitions. That number can be turned into a duration thanks to the
		// PriceStats. That duration can then be turned into a number of sales
		// with SaleStats. Finally that number of sales can be turned into a
		// quantity with the SaleOracle.
		int transitions = 1, currentPrice = price + pricesRandom.nextInt(3) - 1;

		// The state is wrong if this limit is breached
		checkState(price > 0, "Price will go down");

		for (int i = 0; i < 100; i++, transitions++) {
			if (currentPrice <= price) {
				break;
			}
			currentPrice += 1 - pricesRandom.nextInt(3);
		}

		// If the saleCount was calculated with the confidence then the duration
		// between sales would be much too high, leading to an underestimate.
		final double priceChangeDuration = transitions * prices.confidence();
		final int saleCount = (int) Math.ceil(priceChangeDuration / sales.average());
		final int currentStock = getTotalStock();

		int desiredStock = 0;
		for (int i = 0; i < saleCount; i++) {
			desiredStock += 1 + salesRandom.nextInt(6);
		}

		if (currentStock < desiredStock) {
			placeOrder(desiredStock - currentStock);
		}
	}

	private enum STATE {
		/**
		 * The START state waits for the price to become available. Once the
		 * price is available trading can begin.
		 */
		START() {

			@Override
			void priceListener(BadPlugin plugin) {
				plugin.setState(GATHER_DATA);
			}
		},
		/**
		 * The GATHER_DATA state maintains the BASE_STOCK and DESIRED_SHOP_STOCK
		 * until the oracles have fixated.
		 */
		GATHER_DATA() {

			@Override
			void tickListener(BadPlugin plugin) {
				plugin.stockShop();
				plugin.stockWarehouse();

				if (plugin.priceOracle.isFixed() && plugin.saleOracle.isFixed()) {
					plugin.setState(plugin.price < PRICE_LIMIT ? STABLE : OVERPRICED);
				}
			}
		},
		/**
		 * The STABLE state will maintain the BASE_STOCK and two sales of stock
		 * in the shop. The STABLE state ends when the price will rise.
		 */
		STABLE() {

			@Override
			void priceListener(BadPlugin plugin) {
				if (plugin.price >= PRICE_LIMIT) {
					plugin.setState(OVERPRICED);
				}
			}

			@Override
			void tickListener(BadPlugin plugin) {
				plugin.predictShop();
				plugin.stockWarehouse();

				if (plugin.price + 1 == PRICE_LIMIT && plugin.isPriceRising()) {
					plugin.setState(PRE_OVERPRICE);
				}
				else if (plugin.isPriceRising()) {
					plugin.setState(PRE_RISE);
				}
			}
		},
		/**
		 * The PRE_RISE state accumulates stock to cover the projected rise in
		 * price (to a maximum of 100 changes). The PRE_RISE state will end when
		 * the next price change occurs.
		 */
		PRE_RISE() {

			@Override
			void priceListener(BadPlugin plugin) {
				plugin.setState(RISEN);
			}

			@Override
			void tickListener(BadPlugin plugin) {
				plugin.predictShop();
				plugin.predictWarehouse();
			}
		},
		/**
		 * The RISEN state avoids buying stock. The RISEN state will end either
		 * when the price starts to transition to overpriced, or when the price
		 * returns to the original level, or when stock is low.
		 * 
		 * If the available stock reaches critical levels then the current price
		 * will become the new stable price, ending the RISEN state.
		 */
		RISEN() {

			@Override
			void priceListener(BadPlugin plugin) {
				if (plugin.price >= PRICE_LIMIT) {
					plugin.setState(OVERPRICED);
				}
				else if (plugin.price <= plugin.stablePrice) {
					plugin.setState(STABLE);
				}
			}

			@Override
			void tickListener(BadPlugin plugin) {
				plugin.predictShop();

				if (plugin.price + 1 == PRICE_LIMIT && plugin.isPriceRising()) {
					plugin.setState(PRE_OVERPRICE);
				}
				else if (plugin.getTotalStock() < CRITICAL_TOTAL_STOCK) {
					plugin.setState(STABLE);
				}
			}
		},
		/**
		 * The PRE_OVERPRICE state accumulates stock to cover the projected
		 * overpriced duration (to a maximum of 100 changes). The PRE_OVERPRICE
		 * state will end when the next price change occurs.
		 */
		PRE_OVERPRICE() {

			@Override
			void priceListener(BadPlugin plugin) {
				plugin.setState(OVERPRICED);
			}

			@Override
			void tickListener(BadPlugin plugin) {
				plugin.predictShop();
				plugin.predictWarehouse();
			}
		},
		/**
		 * The OVERPRICED state never buys stock. The OVERPRICED state will end
		 * when the price drops enough to permit profit.
		 * 
		 * When the OVERPRICED period ends the price will be the new stable
		 * price.
		 */
		OVERPRICED() {

			@Override
			void priceListener(BadPlugin plugin) {
				if (plugin.price < PRICE_LIMIT) {
					plugin.setState(STABLE);
				}
			}

			@Override
			void tickListener(BadPlugin plugin) {
				plugin.predictShop();
			}
		};

		void priceListener(BadPlugin plugin) {
		}

		void tickListener(BadPlugin plugin) {
		}
	}
}
