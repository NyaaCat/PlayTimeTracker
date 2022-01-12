package cat.nyaa.playtimetracker;

import net.ess3.api.IEssentials;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerAFKManager {
    private static PlayerAFKManager instance;
    private final PlayTimeTracker plugin;

    public PlayerAFKManager(PlayTimeTracker playTimeTracker) {
        this.plugin = playTimeTracker;
        instance = this;
    }

    public static boolean isAFK(UUID playerId) {
        //ess afk
        Plugin ess = getEssPlugin();
        if (ess != null) {
            if (((IEssentials) ess).getUser(playerId).isAfk()) {
                return true;
            }
        }

        if (instance == null)
            return false;
        return false;
        //todo
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
}
