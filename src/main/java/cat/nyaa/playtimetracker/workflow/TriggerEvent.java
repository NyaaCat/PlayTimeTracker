package cat.nyaa.playtimetracker.workflow;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public enum TriggerEvent {
    ENABLE,
    DISABLE,
    LOGIN,
    LOGOUT,
    RESET,
    MISSION,
    AFK_START,
    AFK_END,
    UPDATE,
    ;

    public boolean isBeginEvent() {
        return this == ENABLE || this == LOGIN || this == AFK_END;
    }

    public boolean isEndEvent() {
        return this == DISABLE || this == LOGOUT || this == AFK_START;
    }
}
