package cat.nyaa.playtimetracker.task;

import cat.nyaa.playtimetracker.PTT;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public record NotifyAcquireTask(PTT plugin) implements Runnable {
    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.notifyAcquire(p);
        }
    }
}
