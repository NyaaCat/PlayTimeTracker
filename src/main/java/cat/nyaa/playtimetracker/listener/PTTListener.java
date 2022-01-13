package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.TimeRecordManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PTTListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getTimeRecordManager() != null) {
                PlayTimeTracker.getInstance().getTimeRecordManager().addPlayer(event.getPlayer());
            }
            if (PlayTimeTracker.getInstance().getAfkManager() != null) {
                PlayTimeTracker.getInstance().getAfkManager().addPlayer(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getTimeRecordManager() != null) {
                PlayTimeTracker.getInstance().getTimeRecordManager().removePlayer(event.getPlayer());
            }
            if (PlayTimeTracker.getInstance().getAfkManager() != null) {
                PlayTimeTracker.getInstance().getAfkManager().removePlayer(event.getPlayer());
            }
        }


    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
         Location from = event.getFrom();
         Location to = event.getTo();
         if(to == null)return;
        if(from.getPitch() != to.getPitch() || from.getYaw() != to.getYaw()){
            if (PlayTimeTracker.getInstance() != null) {
                if (PlayTimeTracker.getInstance().getAfkManager() != null) {
                    PlayTimeTracker.getInstance().getAfkManager().playerVisionChange(event);
                }
            }
        }
    }

}
