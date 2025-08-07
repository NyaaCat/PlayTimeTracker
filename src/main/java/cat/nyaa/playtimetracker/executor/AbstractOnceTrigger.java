package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractOnceTrigger implements IOnceTrigger {

    private final AtomicBoolean triggered;

    protected AbstractOnceTrigger() {
        this.triggered = new AtomicBoolean(false);
    }

    protected abstract void handle(@Nullable Long tick);

    @Override
    public void trigger(@Nullable Long tick) {
        if (this.triggered.compareAndSet(false, true)) {
            this.handle(tick);
        }
    }
}
