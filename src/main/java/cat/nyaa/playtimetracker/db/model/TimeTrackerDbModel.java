package cat.nyaa.playtimetracker.db.model;

import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;

import java.util.UUID;

@Table("time")
public class TimeTrackerDbModel {

    @Column(name = "player", primary = true)
    public UUID playerUniqueId;
    @Column(name = "lastSeen")
    public long lastSeen; //Unix timestamp
    @Column(name = "dailyTime")
    public long dailyTime; // millisecond
    @Column(name = "weeklyTime")
    public long weeklyTime; // millisecond
    @Column(name = "monthlyTime")
    public long monthlyTime; // millisecond
    @Column(name = "totalTime")
    public long totalTime; // millisecond
    public TimeTrackerDbModel(UUID playerUniqueId, long lastSeen, long dailyTime, long weeklyTime, long monthlyTime, long totalTime) {
        this.playerUniqueId = playerUniqueId;
        this.lastSeen = lastSeen;
        this.dailyTime = dailyTime;
        this.weeklyTime = weeklyTime;
        this.monthlyTime = monthlyTime;
        this.totalTime = totalTime;
    }


    public TimeTrackerDbModel() {

    }


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
