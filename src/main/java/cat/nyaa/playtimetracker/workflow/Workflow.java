package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.config.MissionConfig;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import io.netty.util.Timeout;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Workflow implements Runnable {
    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final long threadIdGameLoop;
    private AtomicBoolean isNextRunInGameLoop;
    private List<ITask> currentInGameWorkStepList;
    private List<ITask> currentExternalWorkStepList;
    private List<ITask> nextInGameWorkStepList;
    private List<ITask> nextExternalWorkStepList;


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

    public void addNextWorkStep(ITask workflow, boolean executeInGameLoop) {
        if (executeInGameLoop) {
            this.nextInGameWorkStepList.add(workflow);
        } else {
            this.nextExternalWorkStepList.add(workflow);
        }
    }

    public Timeout scheduleWorkStep(ITask workflow, long time, TimeUnit timeUnit) {
        return null;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public @Nullable Plugin getEssentialsPlugin() {
        return null;
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return this.databaseManager.getTimeTrackerConnection();
    }

    public CompletedMissionConnection getCompletedMissionConnection() {
        return this.databaseManager.getCompletedMissionConnection();
    }

    public RewardsConnection getRewardsConnection() {
        return this.databaseManager.getRewardsConnection();
    }

    public MissionConfig getMissionConfig() {
        // TODO: implement this
        return null;
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
            for (ITask workStep : this.currentInGameWorkStepList) {
                try {
                    workStep.execute(this, true);
                } catch (Exception e) {
                    logger.error("error in workflow execution in game loop", e);
                }
            }
            runInGameLoopEnd();
        } else {
            runExternallyStart();
            for (ITask workStep : this.currentExternalWorkStepList) {
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
