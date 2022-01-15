package cat.nyaa.playtimetracker.event.player.time;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WeeklyResetEvent extends PTTPlayerTimeResetEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public WeeklyResetEvent(UUID playerId, long beforeReset) {
        super(playerId, beforeReset);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
