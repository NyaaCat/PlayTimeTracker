package cat.nyaa.playtimetracker.event.player.time;

import cat.nyaa.playtimetracker.event.player.PTTPlayerEvent;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class PTTPlayerTimeResetEvent extends PTTPlayerEvent {
    private final long beforeReset;

    public PTTPlayerTimeResetEvent(UUID playerId, long beforeReset) {
        super(playerId);
        this.beforeReset = beforeReset;
    }

    public PTTPlayerTimeResetEvent(Player player, long beforeReset) {
        super(player);
        this.beforeReset = beforeReset;
    }

    public long getBeforeReset() {
        return beforeReset;
    }
}
