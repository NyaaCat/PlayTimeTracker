package cat.nyaa.playtimetracker.listener;

import cat.nyaa.playtimetracker.PlayTimeTracker;
import cat.nyaa.playtimetracker.event.player.time.DailyResetEvent;
import cat.nyaa.playtimetracker.event.player.time.MonthlyResetEvent;
import cat.nyaa.playtimetracker.event.player.time.WeeklyResetEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PTTListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDailyReset(DailyResetEvent dailyResetEvent) {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getMissionManager() != null) {
                PlayTimeTracker.getInstance().getMissionManager().onDailyReset(dailyResetEvent.getPlayerId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeeklyReset(WeeklyResetEvent weeklyResetEvent) {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getMissionManager() != null) {
                PlayTimeTracker.getInstance().getMissionManager().onWeeklyReset(weeklyResetEvent.getPlayerId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonthlyReset(MonthlyResetEvent monthlyResetEvent) {
        if (PlayTimeTracker.getInstance() != null) {
            if (PlayTimeTracker.getInstance().getMissionManager() != null) {
                PlayTimeTracker.getInstance().getMissionManager().onMonthlyReset(monthlyResetEvent.getPlayerId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var plugin = PlayTimeTracker.getInstance();
        if (plugin != null) {
            var missionManager = plugin.getMissionManager();
            if (missionManager != null) {
                missionManager.checkPlayerMission(event.getPlayer());
            }
            var timeRecordManager = plugin.getTimeRecordManager();
            if (timeRecordManager != null) {
                timeRecordManager.addPlayer(event.getPlayer());
            }
            var afkManager = plugin.getAfkManager();
            if (afkManager != null) {
                afkManager.addPlayer(event.getPlayer());
            }
            var rewardManager = plugin.getRewardManager();
            if(rewardManager != null){
                int delay = 10;
                if(missionManager != null){
                    delay = missionManager.getMissionConfig().loginCheckDelayTicks;
                }
                rewardManager.executeRewardsAutoCheckAsync(event.getPlayer(), delay);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
        if (to == null) return;
        if (from.getPitch() != to.getPitch() || from.getYaw() != to.getYaw()) {
            if (PlayTimeTracker.getInstance() != null) {
                if (PlayTimeTracker.getInstance().getAfkManager() != null) {
                    PlayTimeTracker.getInstance().getAfkManager().playerVisionChange(event);
                }
            }
        }
    }

}
