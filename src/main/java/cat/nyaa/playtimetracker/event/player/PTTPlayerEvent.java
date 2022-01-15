package cat.nyaa.playtimetracker.event.player;

import cat.nyaa.playtimetracker.event.PTTEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class PTTPlayerEvent extends PTTEvent {
    private final UUID playerId;

    public PTTPlayerEvent(Player player) {
        this.playerId = player.getUniqueId();
    }

    public PTTPlayerEvent(UUID playerId) {
        this.playerId = playerId;
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public UUID getPlayerId() {
        return playerId;
    }
}
