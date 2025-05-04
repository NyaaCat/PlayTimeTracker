package cat.nyaa.playtimetracker.workflow;

public interface ITask {

    /**
     * Execute the workflow task once time
     * @param workflow the workflow instance
     * @param executeInGameLoop whether execute in game loop
     * @return 0 if success, non-zero if something wrong
     * @throws {@code Exception} any exception
     */
    int execute(Workflow workflow, boolean executeInGameLoop) throws Exception;
}
