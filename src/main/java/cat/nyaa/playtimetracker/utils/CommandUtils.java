package cat.nyaa.playtimetracker.utils;

import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class CommandUtils {

    public static @Nullable UUID getPlayerUUIDByStr(String s, CommandSender sender) {
        var data = getPlayerByStr(s, sender);
        return switch (data) {
            case Player player -> player.getUniqueId();
            case UUID uuid -> uuid;
            case null, default -> null; // not found
        };
    }

    /**
     * Get player by string.
     * @param s the string to search for, can be a player name or a selector starting with '@' or uuid
     * @param sender the command sender, used for selectors
     * @return Player (online), UUID, or null if not found.
     */
    public static @Nullable Object getPlayerByStr(String s, CommandSender sender) {
        if (s.startsWith("@")) {
            List<Entity> entities = Bukkit.selectEntities(sender, s);
            if (entities.size() != 1) {
                return null;
            }
            if (entities.getFirst() instanceof Player player) {
                return player.isOnline() ? player : player.getUniqueId();
            } else {
                return null;
            }
        }

        Player onlinePlayer = Bukkit.getPlayer(s);
        if (onlinePlayer != null) {
            return onlinePlayer.isOnline() ? onlinePlayer : onlinePlayer.getUniqueId();
        }

        UUID uuid = null;
        try {
            uuid = UUID.fromString(s);
        } catch (IllegalArgumentException ignored) {

        }
        if (uuid != null) {
            return uuid;
        }

        var offlinePlayer = OfflinePlayerUtils.lookupPlayer(s);
        if (offlinePlayer != null) {
            return offlinePlayer.getUniqueId();
        }
        return null; // not found
    }
}
