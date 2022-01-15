package cat.nyaa.playtimetracker.task;

import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import org.bukkit.Bukkit;

public class PTTTaskManager {
    private final PlayTimeTracker plugin;
    private final PTTConfiguration pluginConfiguration;
    private int timeRecordTaskId;
    private int missionCheckTaskId;
    private int notifyAcquireTaskId;

    public PTTTaskManager(PlayTimeTracker plugin, PTTConfiguration configuration) {
        this.pluginConfiguration = configuration;
        this.plugin = plugin;
        this.init();
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }

    private void init() {
        this.timeRecordTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new TimeRecordTask(), 1, 1);
        this.missionCheckTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new MissionCheckTask(), 1, 1);
        this.notifyAcquireTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new MissionCheckTask(), pluginConfiguration.checkAcquireMs, pluginConfiguration.checkAcquireMs);

    }

    public void destructor() {
        Bukkit.getScheduler().cancelTask(timeRecordTaskId);
        Bukkit.getScheduler().cancelTask(missionCheckTaskId);
        Bukkit.getScheduler().cancelTask(notifyAcquireTaskId);
        Bukkit.getScheduler().cancelTasks(getPlugin());
    }
}
