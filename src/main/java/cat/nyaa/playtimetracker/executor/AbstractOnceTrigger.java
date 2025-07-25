package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractOnceTrigger implements IOnceTrigger {

    private final AtomicBoolean triggered;

    protected AbstractOnceTrigger() {
        this.triggered = new AtomicBoolean(false);
    }

    protected abstract void handle();

    @Override
    public void trigger() {
        if (this.triggered.compareAndSet(false, true)) {
            this.handle();
        }
    }
}
