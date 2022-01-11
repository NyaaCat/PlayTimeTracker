package cat.nyaa.playtimetracker.db.model;

import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;

import java.util.UUID;

@Table("completed")
public class CompletedMissionDbModel {
    @Column(name = "id", primary = true, autoIncrement = true, nullable = true)
    int id;
    @Column(name = "player")
    UUID playerUniqueId;
    @Column(name = "mission")
    String missionName;
    @Column(name = "lastCompleted")
    long lastCompletedTime;

    public void setPlayerUniqueId(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId;
    }

    public void setLastCompletedTime(long lastCompletedTime) {
        this.lastCompletedTime = lastCompletedTime;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public UUID getPlayerUniqueId() {
        return playerUniqueId;
    }

    public long getLastCompletedTime() {
        return lastCompletedTime;
    }

    public String getMissionName() {
        return missionName;
    }

    public int getId() {
        return id;
    }
}
