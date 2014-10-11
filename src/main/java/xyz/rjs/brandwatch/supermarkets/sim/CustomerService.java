package xyz.rjs.brandwatch.supermarkets.sim;

import java.util.Random;

import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;
import xyz.rjs.brandwatch.supermarkets.model.events.Customer;

import com.google.common.eventbus.EventBus;

public class CustomerService extends AbstractProbabalisticTickingService {

    private final Random random = new Random();
    private final int minNeeded = 1;
    private final int maxNeeded = 7;

    public CustomerService(EventBus eventBus) {
        super(eventBus, 0.1);
    }

    @Override
	public void probableTick(ClockTick tick) {
        Customer customer = new Customer();
        customer.setName("Robert Paulson");
        customer.setStuffNeeded(random.nextInt(maxNeeded - minNeeded) + minNeeded);
        eventBus.post(customer);
    }
}
