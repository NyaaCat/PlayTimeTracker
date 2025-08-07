package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

public interface IFinalTrigger {

    /**
     * Mark the reference count to increase by one.
     * @param tick the tick at which the action is triggered in sync-thread; or null if not applicable
     */
    void retain(@Nullable Long tick);

    /**
     * Mark the reference count to decrease by one.
     * If the reference count reaches zero, the trigger will be executed.
     * @param tick the tick at which the action is triggered in sync-thread; or null if not applicable
     */
    void release(@Nullable Long tick);
}
