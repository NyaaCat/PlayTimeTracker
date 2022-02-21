package cat.nyaa.playtimetracker.utils;

import cat.nyaa.playtimetracker.PlayTimeTracker;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIUtils {
    static boolean loadPlugin;

    public static void init() {
        loadPlugin = (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
        if (loadPlugin && PlayTimeTracker.getInstance() != null) {
            PlayTimeTracker.getInstance().getLogger().warning("PlaceholderAPI not exists.");
        }
    }

    public static String setPlaceholders(final Player player, @NotNull String text) {
        if (loadPlugin) {
            try {
                return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception e) {
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                    loadPlugin = false;
                }
                return setPlaceholdersWithoutPapi(player,text);
            }
        }
        return setPlaceholdersWithoutPapi(player,text);
    }
    private static @NotNull String setPlaceholdersWithoutPapi(final @NotNull Player player, @NotNull String text){
        return text.replace("%player_name%",player.getName());
    }
}
