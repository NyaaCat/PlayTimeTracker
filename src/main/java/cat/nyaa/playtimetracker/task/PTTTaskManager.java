package cat.nyaa.playtimetracker.task;

import cat.nyaa.playtimetracker.PlayTimeTracker;
import org.bukkit.Bukkit;

public class PTTTaskManager {
    private final PlayTimeTracker plugin;
    private int timeRecordTaskId;

    public PTTTaskManager(PlayTimeTracker plugin) {
        this.plugin = plugin;
        this.init();
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }

    private void init() {
        this.timeRecordTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new TimeRecordTask(), 1, 1);
    }

    public void destructor() {
        Bukkit.getScheduler().cancelTask(timeRecordTaskId);
        Bukkit.getScheduler().cancelTasks(getPlugin());
    }
}
