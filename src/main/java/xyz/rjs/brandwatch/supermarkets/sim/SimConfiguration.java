package xyz.rjs.brandwatch.supermarkets.sim;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import xyz.rjs.brandwatch.supermarkets.logistics.LogisticsConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ServiceManager;

@Configuration
@Import(LogisticsConfiguration.class)
public class SimConfiguration {
    @Bean
    public Main main() {
        return new Main();
    }

    private ClockTickService clockTickService(EventBus eventBus) {
        ClockTickService clockTickService = new ClockTickService(eventBus);
        clockTickService.setTicksPerSecond(1);
        return clockTickService;
    }

    @Bean
    @Autowired
    public Warehouse warehouse(EventBus eventBus) {
        Warehouse warehouse = new Warehouse(eventBus);
        return warehouse;
    }

    @Bean
    @Autowired
    public Shop shop(EventBus eventBus) {
        return new Shop(eventBus);
    }

    @Bean
    @Autowired
    public ServiceManager simulationServiceManager(EventBus eventBus, Shop shop, Warehouse warehouse) {
        return new ServiceManager(ImmutableList.of(
                clockTickService(eventBus),
                new Supermarket(eventBus),
                new Supplier(eventBus),
                new CustomerService(eventBus),
                shop,
                new WarehouseManagementService(eventBus, warehouse, shop)));
    }
}
