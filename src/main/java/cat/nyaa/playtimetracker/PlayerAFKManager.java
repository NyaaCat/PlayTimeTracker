package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.listener.SelfHostedAFKActionListener;
import cat.nyaa.playtimetracker.utils.TaskUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerAFKManager {

    private static PlayerAFKManager instance;

    public static boolean isEssAfk(UUID playerId) {
        if (instance == null) return false;
        return instance.getEssAFKState(playerId);
    }

    public static boolean isAFK(UUID playerId) {
        if (instance == null) return false;
        return instance.getAFKState(playerId);
    }

    public static void setInstance(PlayerAFKManager afkManager)   {
        instance = afkManager;
    }

    private Server server;
    private final @Nullable PTTConfiguration configuration;
    private final @Nullable IEssentials essentialsAPI;
    private final Map<UUID, Activity> lastActivityMap;
    private @Nullable IAFKStateChangeCallback afkStateChangeCallback;
    private int taskId = -1; // Task ID for the AFK check task
    private Listener listener = null;

    public PlayerAFKManager(@Nullable PTTConfiguration pttConfiguration, @Nullable IEssentials essentialsAPI) {
        this.server = null;
        this.configuration = pttConfiguration;
        this.essentialsAPI = essentialsAPI;
        this.lastActivityMap = new HashMap<>();
    }

    public void setAFKStateChangeCallback(@Nullable IAFKStateChangeCallback callback) {
        this.afkStateChangeCallback = callback;
    }

    public boolean getEssAFKState(UUID playerId) {
        if (this.essentialsAPI != null) {
            return this.essentialsAPI.getUser(playerId).isAfk();
        }
        return false;
    }

    public boolean getSelfHostedAfkState(UUID playerId) {
        if (this.configuration == null) {
            return false;
        }
        return this.getAFKStateInner(playerId, TimeUtils.getUnixTimeStampNow(), Bukkit.getPlayer(playerId));
    }

    public boolean getAFKState(UUID playerId) {
        if (this.configuration == null) {
            return false;
        }
        if (!this.configuration.checkAfk) {
            return false;
        }
        //ess afk
        if (this.configuration.useEssAfkStatus) {
            if (this.essentialsAPI != null) {
                return this.essentialsAPI.getUser(playerId).isAfk();
            }
        }

        return this.getAFKStateInner(playerId, TimeUtils.getUnixTimeStampNow(), null);
    }

    public long getAfkTime(UUID playerId) {
        var lastActive = this.lastActivityMap.get(playerId);
        if (lastActive == null) {
            return 0L;
        }
        return Math.max(TimeUtils.getUnixTimeStampNow() - lastActive.time, 0L);
    }

    public long getLastActivity(UUID playerId) {
        var lastActive = this.lastActivityMap.get(playerId);
        if (lastActive == null) {
            return TimeUtils.getUnixTimeStampNow();
        }
        return lastActive.time;
    }

    public void playerVisionChange(PlayerMoveEvent event) {
        this.updatePlayerLastActivity(event.getPlayer(), TimeUtils.getUnixTimeStampNow());
    }

    public void removePlayer(Player player) {
        this.lastActivityMap.remove(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        this.updatePlayerLastActivity(player, TimeUtils.getUnixTimeStampNow());
    }

    /// return -1 if scheduling failed
    public void registerCheckTask(Plugin plugin) {
        if (this.configuration == null) {
            return;
        }
        if (!this.configuration.checkAfk) {
            return;
        }
        if (this.configuration.useEssAfkStatus) {
            return;
        }
        if (this.configuration.afkTimeMS <= 0) {
            return;
        }
        this.server = plugin.getServer();
        var interval = CheckAFKTask.interval(this.configuration.afkTimeMS);
        this.taskId = this.server.getScheduler().scheduleSyncRepeatingTask(plugin, new CheckAFKTask(), 1, interval);
        if (this.taskId != -1) {
            this.listener = new SelfHostedAFKActionListener(this);
            this.server.getPluginManager().registerEvents(this.listener, plugin);
        }
    }

    public void unregisterCheckTask() {
        if (this.listener != null) {
            HandlerList.unregisterAll(this.listener);
            this.listener = null;
        }
        if (this.taskId != -1) {
            this.server.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
    }

    private void updatePlayerLastActivity(Player player, long time) {
        var playerId = player.getUniqueId();
        var lastUpdate = this.lastActivityMap.get(playerId);
        if (lastUpdate == null) {
            lastUpdate = new Activity(time, false);
            this.lastActivityMap.put(playerId, lastUpdate);
        } else {
            var afk = lastUpdate.afk;
            lastUpdate.time = time;
            lastUpdate.afk = false;
            if (this.configuration != null && this.configuration.afkTimeMS > 0) {
                if (afk) {
                    if (this.afkStateChangeCallback != null) {
                        this.afkStateChangeCallback.onAFKStateChange(player, false);
                    }
                }
            }
        }
    }

    private boolean getAFKStateInner(UUID playerId, long time, @Nullable Player player) {
        var lastActive = this.lastActivityMap.get(playerId);
        if (lastActive == null) {
            return false;
        }
        if (lastActive.afk) {
            return true;
        }
        var afkTime = time - lastActive.time;
        assert this.configuration != null;
        if (afkTime > this.configuration.afkTimeMS) {
            lastActive.afk = true;
            if (this.afkStateChangeCallback != null) {
                if (player == null && this.server != null) {
                    player = this.server.getPlayer(playerId);
                }
                if (player != null) {
                    this.afkStateChangeCallback.onAFKStateChange(player, true);
                }
            }
        }
        return lastActive.afk;
    }

    private static class Activity {
        long time;
        boolean afk;

        Activity(long time, boolean afk) {
            this.time = time;
            this.afk = afk;
        }
    }

    private class CheckAFKTask implements Runnable {

        int tick = 0;

        @Override
        public void run() {
            for (Player player : PlayerAFKManager.this.server.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                var time = TimeUtils.getUnixTimeStampNow();
                var hash = TaskUtils.getBitId(playerId, 5);
                if (hash == this.tick % 32) {
                    // Check AFK state every 32 ticks
                    PlayerAFKManager.this.getAFKStateInner(playerId, time, player);
                }
            }
            this.tick++;
        }

        public static long interval(long afkTimeMS) {
            // 1. convert ms to ticks (20 ticks per second)
            // 2. get 1/4 as check interval
            // 3. divide by 32 to get the interval for the task; used in TaskUtils.mod32TickToRun
            return Math.max(afkTimeMS / 20L / 4 / 32, 1);
        }
    }

    public interface IAFKStateChangeCallback {
        void onAFKStateChange(Player player, boolean isAfk);
    }
}
