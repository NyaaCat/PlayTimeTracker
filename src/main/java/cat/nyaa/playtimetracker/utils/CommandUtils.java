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
    @Nullable
    public static UUID getPlayerUUIDByStr(String string, CommandSender sender) {
        if (string.startsWith("@")) {
            List<Entity> entities = Bukkit.selectEntities(sender, string);
            if (entities.size() != 1) return null;
            return entities.get(0) instanceof Player ? entities.get(0).getUniqueId() : null;
        }
        Player onlinePlayer = Bukkit.getPlayer(string);
        if (onlinePlayer != null) return onlinePlayer.getUniqueId();

        UUID StrUuid = null;
        try {
            StrUuid = UUID.fromString(string);
        } catch (IllegalArgumentException ignored) {
        }
        if (StrUuid != null) return StrUuid;

        OfflinePlayer offlinePlayer = OfflinePlayerUtils.lookupPlayer(string);
        if (offlinePlayer != null) return offlinePlayer.getUniqueId();
        return null;
    }
}
