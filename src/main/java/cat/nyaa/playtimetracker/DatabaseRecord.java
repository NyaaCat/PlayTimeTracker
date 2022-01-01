package cat.nyaa.playtimetracker;

import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DatabaseRecord implements Cloneable {
    public UUID uuid;
    public ZonedDateTime lastSeen; // timestamp millisecond
    public long dailyTime; // millisecond
    public long weeklyTime; // millisecond
    public long monthlyTime; // millisecond
    public long totalTime; // millisecond
    public Set<String> completedDailyMissions;
    public Set<String> completedWeeklyMissions;
    public Set<String> completedMonthlyMissions;
    public Set<String> completedLifetimeMissions;

    public DatabaseRecord() {
        reset();
    }

    public DatabaseRecord(UUID id, ZonedDateTime time) {
        reset();
        uuid = id;
        lastSeen = time;
    }

    public static DatabaseRecord deserialize(UUID id, ConfigurationSection sec) {
        DatabaseRecord rec = new DatabaseRecord();
        rec.uuid = id;
        rec.lastSeen = ZonedDateTime.parse(sec.getString("last_seen"));
        rec.dailyTime = sec.getLong("daily_play_time");
        rec.weeklyTime = sec.getLong("weekly_play_time");
        rec.monthlyTime = sec.getLong("monthly_play_time");
        rec.totalTime = sec.getLong("total_play_time");
        rec.completedDailyMissions = new HashSet<>(sec.getStringList("completed_daily_mission"));
        rec.completedWeeklyMissions = new HashSet<>(sec.getStringList("completed_weekly_mission"));
        rec.completedMonthlyMissions = new HashSet<>(sec.getStringList("completed_monthly_mission"));
        rec.completedLifetimeMissions = new HashSet<>(sec.getStringList("completed_lifetime_mission"));
        return rec;
    }

    private static HashSet<String> _legacy_deserializeSet(String str) {
        if ("{}".equals(str)) return new HashSet<>();
        HashSet<String> r = new HashSet<>(Arrays.asList(str.substring(1, str.length() - 1).split(",")));
        return r;
    }

    public static DatabaseRecord deserialize_legacy(UUID id, String old_format_string) {
        String[] tmp = old_format_string.split(" ");
        DatabaseRecord rec = new DatabaseRecord();
        Instant instantTime = Instant.ofEpochMilli(Long.parseLong(tmp[0]));
        rec.lastSeen = ZonedDateTime.ofInstant(instantTime, ZoneId.systemDefault());
        rec.dailyTime = Long.parseLong(tmp[4]);
        rec.weeklyTime = Long.parseLong(tmp[5]);
        rec.monthlyTime = Long.parseLong(tmp[6]);
        rec.totalTime = Long.parseLong(tmp[7]);
        rec.completedDailyMissions = _legacy_deserializeSet(tmp[8]);
        rec.completedWeeklyMissions = _legacy_deserializeSet(tmp[9]);
        rec.completedMonthlyMissions = _legacy_deserializeSet(tmp[10]);
        rec.completedLifetimeMissions = _legacy_deserializeSet(tmp[11]);
        return rec;
    }

    public void serialize(ConfigurationSection sec) {
        sec.set("last_seen", lastSeen.toString());
        sec.set("daily_play_time", dailyTime);
        sec.set("weekly_play_time", weeklyTime);
        sec.set("monthly_play_time", monthlyTime);
        sec.set("total_play_time", totalTime);
        sec.set("completed_daily_mission", completedDailyMissions.toArray(new String[0]));
        sec.set("completed_weekly_mission", completedWeeklyMissions.toArray(new String[0]));
        sec.set("completed_monthly_mission", completedMonthlyMissions.toArray(new String[0]));
        sec.set("completed_lifetime_mission", completedLifetimeMissions.toArray(new String[0]));
    }

    public void reset() {
        lastSeen = ZonedDateTime.now();
        dailyTime = 0;
        weeklyTime = 0;
        monthlyTime = 0;
        totalTime = 0;
        completedLifetimeMissions = new HashSet<>();
        completedDailyMissions = new HashSet<>();
        completedWeeklyMissions = new HashSet<>();
        completedMonthlyMissions = new HashSet<>();
    }

    @Override
    public String toString() {
        return super.toString() + String.format(
                "{\"last_seen\": \"%s\", \"daily_play_time\": %d, \"weekly_play_time\": %d, \"monthly_play_time\": %d, " +
                        "\"total_play_time\": %d, \"daily_completed\": \"%s\", \"weekly_completed\": \"%s\", " +
                        "\"monthly_completed\": \"%s\", \"lifetime_completed\": \"%s\"}",
                lastSeen.toString(), dailyTime, weeklyTime, monthlyTime, totalTime,
                completedDailyMissions, completedWeeklyMissions, completedMonthlyMissions, completedLifetimeMissions);
    }

    @Override
    public DatabaseRecord clone() {
        try {
            DatabaseRecord r = (DatabaseRecord) super.clone();
            r.completedLifetimeMissions = new HashSet<>(completedLifetimeMissions);
            r.completedDailyMissions = new HashSet<>(completedDailyMissions);
            r.completedMonthlyMissions = new HashSet<>(completedMonthlyMissions);
            r.completedWeeklyMissions = new HashSet<>(completedWeeklyMissions);
            r.lastSeen = ZonedDateTime.from(lastSeen);
            return r;
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
            PTT.log("Failed to clone: " + this);
        }
        return new DatabaseRecord();
    }
}
