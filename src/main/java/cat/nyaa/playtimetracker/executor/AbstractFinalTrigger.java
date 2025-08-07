package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractFinalTrigger implements IFinalTrigger {

    private final AtomicInteger refCount;

    protected AbstractFinalTrigger() {
        this.refCount = new AtomicInteger(0);
    }

    @Override
    public void retain(@Nullable Long tick) {
        if (this.refCount.getAndIncrement() < 0) {
            this.refCount.set(-1);
            throw new IllegalStateException("AbstractFinalTrigger retain called after released");
        }
    }

    @Override
    public void release(@Nullable Long tick) {
        if (this.refCount.decrementAndGet() <= 0) {
            this.refCount.set(-1);
            this.handle(tick);
        }
    }

    protected abstract void handle(@Nullable Long tick);
}
