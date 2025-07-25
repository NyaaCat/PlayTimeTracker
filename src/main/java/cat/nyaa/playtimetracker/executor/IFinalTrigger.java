package cat.nyaa.playtimetracker.executor;

public interface IFinalTrigger {

    /**
     * Mark the reference count to increase by one.
     */
    void retain();

    /**
     * Mark the reference count to decrease by one.
     * If the reference count reaches zero, the trigger will be executed.
     */
    void release();
}
