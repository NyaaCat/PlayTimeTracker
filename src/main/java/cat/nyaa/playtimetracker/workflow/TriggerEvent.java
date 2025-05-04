package cat.nyaa.playtimetracker.workflow;

import javax.annotation.Nullable;

public record TriggerEvent(TriggerType source, @Nullable String player, @Nullable String mission) {

    public enum TriggerType {
        ENABLE,
        DISABLE,
        LOGIN,
        LOGOUT,
        RESET,
        MISSION,
        ;
    }
}
