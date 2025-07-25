package cat.nyaa.playtimetracker.workflow;

import cat.nyaa.playtimetracker.utils.LoggerUtils;
import com.earth2me.essentials.User;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

public class PlayerContext {

    public final static Logger logger = LoggerUtils.getPluginLogger();

    private final Plugin plugin;
    private final UUID playerUUID;

    private boolean playerInitialized;
    private @Nullable Player player;
    private boolean essUserInitialized;
    private @Nullable User essUser;
    private long lastTick;

    public PlayerContext(UUID playerUUID, Plugin plugin) {
        this.plugin = plugin;
        this.playerUUID = playerUUID;
    }

    public UUID getUUID() {
        return this.playerUUID;
    }

    /// must be called in game-loop main thread
    public @Nullable Player getPlayer(long tick) {
        if (!this.playerInitialized || this.lastTick != tick) {
            this.playerInitialized = true;
            this.lastTick = tick;
            this.player = this.plugin.getServer().getPlayer(this.playerUUID);
        }
        return this.player;
    }

    /// must be called in game-loop main thread
    public @Nullable User getEssUser(long tick) {
        if (!this.essUserInitialized || this.lastTick != tick) {
            this.essUserInitialized = true;
            this.lastTick = tick;
            if (this.plugin instanceof IEssentialsAPIProvider provider) {
                var ess = provider.getEssentialsAPI();
                if (ess != null) {
                    this.essUser = ess.getUser(this.playerUUID);
                }
            }
        }
        return this.essUser;
    }
}
