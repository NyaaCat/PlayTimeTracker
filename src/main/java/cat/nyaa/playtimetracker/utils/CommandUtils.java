package cat.nyaa.playtimetracker.utils;

import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class CommandUtils {

    public static @Nullable UUID getPlayerUUIDByStr(String string, CommandSender sender) {
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException ignored) {
            var player = getPlayerByStr(string, sender);
            return player != null ? player.getUniqueId() : null;
        }
    }

    public static @Nullable OfflinePlayer getPlayerByStr(String string, CommandSender sender) {
        if (string.startsWith("@")) {
            List<Entity> entities = Bukkit.selectEntities(sender, string);
            if (entities.size() != 1) return null;
            return entities.getFirst() instanceof Player player ? player : null;
        }
        Player onlinePlayer = Bukkit.getPlayer(string);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        return OfflinePlayerUtils.lookupPlayer(string);
    }
}
