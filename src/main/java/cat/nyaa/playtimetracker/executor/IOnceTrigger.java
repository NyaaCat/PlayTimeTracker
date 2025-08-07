package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

public interface IOnceTrigger {

    /**
     * Trigger the once action.
     * This method will execute only once.
     * @param tick the tick at which the action is triggered in sync-thread; or null if not applicable
     */
    void trigger(@Nullable Long tick);
}
