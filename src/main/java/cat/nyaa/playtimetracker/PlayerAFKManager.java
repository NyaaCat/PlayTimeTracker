package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.ess3.api.IEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

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

    public static void setInstance(PlayerAFKManager afkManager) {
        instance = afkManager;
    }

    private final @Nullable PTTConfiguration configuration;
    private final @Nullable IEssentials essentialsAPI;
    private final Object2LongMap<UUID> lastActivityMap;

    public PlayerAFKManager(@Nullable PTTConfiguration pttConfiguration, @Nullable IEssentials essentialsAPI) {
        this.configuration = pttConfiguration;
        this.essentialsAPI = essentialsAPI;
        this.lastActivityMap = new Object2LongOpenHashMap<>();
    }

    public boolean getEssAFKState(UUID playerId) {
        if (this.essentialsAPI != null) {
            return this.essentialsAPI.getUser(playerId).isAfk();
        }
        return false;
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

        return this.getAfkTime(playerId) > this.configuration.afkTimeMS;
    }

    public long getAfkTime(UUID playerId) {
        var lastActive = this.lastActivityMap.getOrDefault(playerId, 0);
        if (lastActive == 0L) {
            return 0L;
        }
        return Math.max(TimeUtils.getUnixTimeStampNow() - getLastActivity(playerId), 0L);
    }

    public long getLastActivity(UUID playerId) {
        var lastActive = this.lastActivityMap.getOrDefault(playerId, 0);
        if (lastActive == 0L) {
            return TimeUtils.getUnixTimeStampNow();
        }
        return lastActive;
    }

    public void playerVisionChange(PlayerMoveEvent event) {
        this.updatePlayerLastActivity(event.getPlayer().getUniqueId(), TimeUtils.getUnixTimeStampNow());
    }

    public void removePlayer(Player player) {
        this.lastActivityMap.removeLong(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        this.updatePlayerLastActivity(player.getUniqueId(), TimeUtils.getUnixTimeStampNow());
    }

    private void updatePlayerLastActivity(UUID playerId, long time) {
        this.lastActivityMap.put(playerId, time);
    }
}
