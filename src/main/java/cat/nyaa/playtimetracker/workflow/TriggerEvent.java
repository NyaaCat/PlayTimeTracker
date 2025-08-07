package cat.nyaa.playtimetracker.workflow;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public enum TriggerEvent {
    ENABLE, // when the plugin is enabled
    DISABLE, // when the plugin is disabled
    LOGIN, // when a player logs in
    LOGOUT, // when a player logs out
    AFK_START, // when a player starts being AFK
    AFK_END, // when a player stops being AFK
    UPDATE,
    VIEW, // when a player views their play time
    RESET,
    ;

    public boolean isBeginEvent() {
        return this == ENABLE || this == LOGIN || this == AFK_END;
    }

    public boolean isEndEvent() {
        return this == DISABLE || this == LOGOUT || this == AFK_START;
    }

    public boolean isLastSeenEvent() {
        return this == LOGOUT;
    }
}
