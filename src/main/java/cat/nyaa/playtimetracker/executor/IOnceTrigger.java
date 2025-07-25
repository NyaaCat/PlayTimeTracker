package cat.nyaa.playtimetracker.executor;

import org.jetbrains.annotations.Nullable;

public interface IOnceTrigger {

    /**
     * Trigger the once action.
     * This method will execute only once.
     */
    void trigger();
}
