package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.Utils.TimeUtils;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import net.ess3.api.IEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerAFKManager {
    private static PlayerAFKManager instance;
    private final PlayTimeTracker plugin;
    private final Map<UUID,Long> lastActivityMap = new HashMap<>();

    public PlayerAFKManager(PlayTimeTracker playTimeTracker) {
        this.plugin = playTimeTracker;
        instance = this;
    }

    public static boolean isAFK(UUID playerId) {
        if (instance == null) return false;
        PTTConfiguration conf = instance.getPlugin().getPttConfiguration();
        if(conf == null )return false;
        if(!conf.checkAfk)return false;
        //ess afk
        if(conf.useEssAfkStatus) {
            Plugin ess = getEssPlugin();
            if (ess != null) {
                if (((IEssentials) ess).getUser(playerId).isAfk()) {
                    return true;
                }
            }
        }

        return instance.getAfkTime(playerId) > conf.afkTimeMS;
    }

    public long getAfkTime(UUID playerId) {
        if(!lastActivityMap.containsKey(playerId))return 0L;
       return Math.max(TimeUtils.getUnixTimeStampNow() - getlastActivity(playerId),0L);
    }
    public long getlastActivity(UUID playerId) {
        if(!lastActivityMap.containsKey(playerId))return TimeUtils.getUnixTimeStampNow();
        return lastActivityMap.get(playerId);
    }

    @Nullable
    private static Plugin getEssPlugin() {
        PlayTimeTracker playTimeTracker = PlayTimeTracker.getInstance();
        if (playTimeTracker == null) return null;
        return playTimeTracker.getEssentialsPlugin();
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }

    public void playerVisionChange(PlayerMoveEvent event) {
        updatePlayerLastActivity(event.getPlayer().getUniqueId(), TimeUtils.getUnixTimeStampNow());
    }

    public void removePlayer(Player player) {
        this.lastActivityMap.remove(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        updatePlayerLastActivity(player.getUniqueId(), TimeUtils.getUnixTimeStampNow());
    }
    private void updatePlayerLastActivity(UUID playerId,long time){
            this.lastActivityMap.put(playerId,time);
    }
}
