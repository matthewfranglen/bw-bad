package xyz.rjs.brandwatch.supermarkets.sim;

import java.util.concurrent.TimeUnit;

import xyz.rjs.brandwatch.supermarkets.model.events.ClockTick;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractScheduledService;

public class ClockTickService extends AbstractScheduledService {
    private int ticksPerSecond;
    private final EventBus eventBus;

    private volatile int ticks = 0;

    public ClockTickService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setTicksPerSecond(int ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
    }

    @Override
    protected void runOneIteration() throws Exception {
        eventBus.post(new ClockTick(ticks++));
    }

    @Override
    protected Scheduler scheduler() {
        int millis = 1000/ticksPerSecond;
        return AbstractScheduledService.Scheduler.newFixedRateSchedule(millis, millis, TimeUnit.MILLISECONDS);
    }
}
