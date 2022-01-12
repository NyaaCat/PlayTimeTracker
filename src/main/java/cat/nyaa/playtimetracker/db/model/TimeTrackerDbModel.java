package cat.nyaa.playtimetracker.db.model;

import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;

import java.util.UUID;

@Table("time")
public class TimeTrackerDbModel {
    @Column(name = "player", primary = true)
    UUID playerUniqueId;
    @Column(name = "lastSeen")
    long lastSeen; //Unix timestamp
    @Column(name = "dailyTime")
    long dailyTime; // millisecond
    @Column(name = "weeklyTime")
    long weeklyTime; // millisecond
    @Column(name = "monthlyTime")
    long monthlyTime; // millisecond
    @Column(name = "totalTime")
    long totalTime; // millisecond

    public UUID getPlayerUniqueId() {
        return playerUniqueId;
    }

    public void setPlayerUniqueId(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getDailyTime() {
        return dailyTime;
    }

    public void setDailyTime(long dailyTime) {
        this.dailyTime = dailyTime;
    }

    public long getWeeklyTime() {
        return weeklyTime;
    }

    public void setWeeklyTime(long weeklyTime) {
        this.weeklyTime = weeklyTime;
    }

    public long getMonthlyTime() {
        return monthlyTime;
    }

    public void setMonthlyTime(long monthlyTime) {
        this.monthlyTime = monthlyTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
}
