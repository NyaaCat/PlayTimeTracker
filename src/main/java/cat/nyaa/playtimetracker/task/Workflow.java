package cat.nyaa.playtimetracker.task;

import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import io.netty.util.Timeout;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Workflow implements Runnable {
    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final long threadIdGameLoop;
    private AtomicBoolean isNextRunInGameLoop;
    private List<IWorkStep> currentInGameWorkStepList;
    private List<IWorkStep> currentExternalWorkStepList;
    private List<IWorkStep> nextInGameWorkStepList;
    private List<IWorkStep> nextExternalWorkStepList;


    public Workflow(Plugin plugin, DatabaseManager databaseManager, long threadIdGameLoop) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.threadIdGameLoop = threadIdGameLoop;
        this.isNextRunInGameLoop = new AtomicBoolean(false);
        final int workStepListSize = this.plugin.getServer().getMaxPlayers();
        this.currentInGameWorkStepList = new ObjectArrayList<>(workStepListSize);
        this.currentExternalWorkStepList = new ObjectArrayList<>(workStepListSize);
        this.nextInGameWorkStepList = new ObjectArrayList<>(workStepListSize);
        this.nextExternalWorkStepList = new ObjectArrayList<>(workStepListSize);
    }

    public void markNextRunInGameLoop() {
        this.isNextRunInGameLoop.set(true);
    }

    public void addNextWorkStep(IWorkStep workflow, boolean executeInGameLoop) {
        if (executeInGameLoop) {
            this.nextInGameWorkStepList.add(workflow);
        } else {
            this.nextExternalWorkStepList.add(workflow);
        }
    }

    public Timeout scheduleWorkStep(IWorkStep workflow, long time, TimeUnit timeUnit) {
        return null;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return this.databaseManager.getTimeTrackerConnection();
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return this.databaseManager.getCompletedMissionConnection();
    }

    public RewardsConnection getRewardsConnection() {
        if (Thread.currentThread().threadId() == this.threadIdGameLoop) {
            throw new IllegalStateException("cannot access rewards connection in game loop");
        }
        return this.databaseManager.getRewardsConnection();
    }

    protected void runInGameLoopStart() {
        var tmp = this.currentInGameWorkStepList;
        tmp.clear();
        this.currentInGameWorkStepList = this.nextInGameWorkStepList;
        this.nextInGameWorkStepList = tmp;
    }

    protected void runInGameLoopEnd() {

    }

    protected void runExternallyStart() {
        var tmp = this.currentExternalWorkStepList;
        tmp.clear();
        this.currentExternalWorkStepList = this.nextExternalWorkStepList;
        this.nextExternalWorkStepList = tmp;
        this.databaseManager.getTimeTrackerConnection().beginBatchMode();
        this.databaseManager.getCompletedMissionConnection().beginBatchMode();
        this.databaseManager.getRewardsConnection().beginBatchMode();
    }

    protected void runExternallyEnd() {

        this.databaseManager.getTimeTrackerConnection().endBatchMode();
        this.databaseManager.getCompletedMissionConnection().endBatchMode();
        this.databaseManager.getRewardsConnection().endBatchMode();
    }

    @Override
    public void run() {
        long threadId = Thread.currentThread().threadId();
        var logger = this.plugin.getSLF4JLogger();
        if (threadId == this.threadIdGameLoop) {
            runInGameLoopStart();
            for (IWorkStep workStep : this.currentInGameWorkStepList) {
                try {
                    workStep.execute(this, true);
                } catch (Exception e) {
                    logger.error("error in workflow execution in game loop", e);
                }
            }
            runInGameLoopEnd();
        } else {
            runExternallyStart();
            for (IWorkStep workStep : this.currentExternalWorkStepList) {
                try {
                    workStep.execute(this, false);
                } catch (Exception e) {
                    logger.error("error in workflow execution externally", e);
                }
            }
            runExternallyEnd();
        }
    }
}
