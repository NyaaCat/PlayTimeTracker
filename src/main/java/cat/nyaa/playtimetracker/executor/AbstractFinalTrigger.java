package cat.nyaa.playtimetracker.executor;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractFinalTrigger implements IFinalTrigger {

    private final AtomicInteger refCount;

    protected AbstractFinalTrigger() {
        this.refCount = new AtomicInteger(0);
    }

    @Override
    public void retain() {
        if (this.refCount.getAndIncrement() < 0) {
            this.refCount.set(-1);
            throw new IllegalStateException("AbstractFinalTrigger retain called after released");
        }
    }

    @Override
    public void release() {
        if (this.refCount.decrementAndGet() <= 0) {
            this.refCount.set(-1);
            this.handle();
        }
    }

    protected abstract void handle();
}
