package cat.nyaa.playtimetracker;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Communicate with DatabaseManager
 * and maintains pre-session online time info.
 */
public class RecordManager {

    private final DatabaseManager db;
    private final Map<UUID, Long> sessionTimeMap = new HashMap<>();
    private final Map<UUID, Set<String>> sessionRewardMap = new HashMap<>();
    public RecordManager(DatabaseManager db) {
        this.db = db;
    }

    public Set<UUID> updateAllOnlinePlayers() {
        Set<UUID> tmp = Bukkit.getServer().getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .map(this::updateAccumulative)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        db.save();
        return tmp;
    }

    public void updateSingle(OfflinePlayer p) {
        updateAccumulative(p.getUniqueId());
        db.save();
    }

    /**
     * Update the time statistic for single uuid
     * database not flushed
     * fallback to updateNonAccumulative if id is in AFK state
     * prerequisite: player is online
     *
     * @param id the uuid to be updated
     * @return id if changed, null if not
     */
    private UUID updateAccumulative(UUID id) {
        if (id == null) return null;
        if (PTT.isAFK(id)) {
            PTT.debug("updateNonAccumulative due player AFK: " + id);
            return updateNonAccumulative(id);
        }
        PTT.debug("updateAccumulative: " + id);
        DatabaseRecord rec = db.getRecord(id);
        if (rec == null) {
            db.createRecord(id, ZonedDateTime.now());
        } else {
            ZonedDateTime currentTime = ZonedDateTime.now();
            ZonedDateTime lastSeen = rec.lastSeen;
            rec.lastSeen = currentTime;
            long duration = Duration.between(lastSeen, currentTime).toMillis();
            if (duration <= 0) return null;
            PTT.debug(String.format("Time duration: %d (%s ~ %s)", duration, lastSeen, currentTime));

            ZonedDateTime startOfToday = currentTime.truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime startOfWeek = currentTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime startOfMonth = currentTime.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);

            if (startOfToday.isAfter(lastSeen)) {
                PTT.debug("Daily time reset: " + id);
                rec.completedDailyMissions = new HashSet<>();
                rec.dailyTime = Duration.between(startOfToday, currentTime).toMillis();
            } else {
                rec.dailyTime += duration;
            }
            if (startOfWeek.isAfter(lastSeen)) {
                PTT.debug("Weekly time reset: " + id);
                rec.completedWeeklyMissions = new HashSet<>();
                rec.weeklyTime = 0;
            } else {
                rec.weeklyTime += duration;
            }
            if (startOfMonth.isAfter(lastSeen)) {
                PTT.debug("Daily time reset: " + id);
                rec.completedMonthlyMissions = new HashSet<>();
                rec.monthlyTime = 0;
            } else {
                rec.monthlyTime += duration;
            }
            rec.totalTime += duration;

            // update recurrence records
            if (db.recurrenceMap.containsKey(id)) {
                Map<String, Long> tmp = db.recurrenceMap.get(id);
                tmp.replaceAll((n, v) -> v + duration);
            }
        }
        return id;
    }

    private UUID updateNonAccumulative(UUID id) {
        if (id == null) return null;
        PTT.debug("updateNonAccumulative: " + id);
        DatabaseRecord rec = db.getRecord(id);
        if (rec == null) {
            db.createRecord(id, ZonedDateTime.now());
        } else {
            ZonedDateTime currentTime = ZonedDateTime.now();
            ZonedDateTime lastSeen = rec.lastSeen;
            rec.lastSeen = currentTime;
            long duration = Duration.between(lastSeen, currentTime).toMillis();
            if (duration <= 0) return null;

            ZonedDateTime startOfToday = currentTime.truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime startOfWeek = currentTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime startOfMonth = currentTime.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);

            if (startOfToday.isAfter(lastSeen)) {
                PTT.debug("Daily time reset: " + id);
                rec.completedDailyMissions = new HashSet<>();
                rec.dailyTime = 0;
            }
            if (startOfWeek.isAfter(lastSeen)) {
                PTT.debug("Weekly time reset: " + id);
                rec.completedWeeklyMissions = new HashSet<>();
                rec.weeklyTime = 0;
            }
            if (startOfMonth.isAfter(lastSeen)) {
                PTT.debug("Monthly time reset: " + id);
                rec.completedMonthlyMissions = new HashSet<>();
                rec.monthlyTime = 0;
            }
        }
        return id;
    }

    public SessionedRecord getFullRecord(UUID id) {
        return new SessionedRecord(id);
    }

    public void resetAllStatistic() {
        Set<UUID> ids = new HashSet<>(db.getAllRecords().keySet());
        for (UUID id : ids) {
            db.createRecord(id, ZonedDateTime.now());
        }
        db.recurrenceMap.clear();
        db.save();
        sessionRewardMap.clear();
        sessionTimeMap.clear();
    }

    public void resetSingleStatistic(UUID id) {
        if (id == null) return;
        PTT.log(String.format("Statistic reset for %s, old record: %s", id, db.getRecord(id)));
        db.createRecord(id, ZonedDateTime.now());
        db.recurrenceMap.remove(id);
        db.save();
        sessionRewardMap.remove(id);
        sessionTimeMap.remove(id);
    }

    public void sessionStart(UUID id) {
        updateNonAccumulative(id);
        sessionTimeMap.put(id, 0L);
        sessionRewardMap.put(id, new HashSet<>());
        db.save();
    }

    public void sessionEnd(UUID id) {
        updateAccumulative(id);
        sessionTimeMap.remove(id);
        sessionRewardMap.remove(id);
        db.save();
    }

    public void markRuleAsApplied(UUID id, Rule rule) {
        if (rule.period == Rule.PeriodType.SESSION) {
            sessionRewardMap.get(id).add(rule.name);
            return;
        }
        DatabaseRecord rec = db.getRecord(id);
        if (rec == null) return;
        switch (rule.period) {
            case DAY -> rec.completedDailyMissions.add(rule.name);
            case WEEK -> rec.completedWeeklyMissions.add(rule.name);
            case MONTH -> rec.completedMonthlyMissions.add(rule.name);
            case DISPOSABLE -> {
                rec.completedLifetimeMissions.add(rule.name);
                if (db.recurrenceMap.containsKey(id)) {
                    db.recurrenceMap.get(id).remove(rule.name);
                }
            }
        }
        db.save();
    }

    public class SessionedRecord {
        public final DatabaseRecord dbRec;
        private final UUID id;
        private SessionedRecord(UUID id) {
            this.id = id;
            this.dbRec = db.getRecord(id);
        }

        public long getSessionTime() {
            if (sessionTimeMap.containsKey(id)) {
                return sessionTimeMap.get(id);
            } else {
                return -1;
            }
        }

        public void setSessionTime(long time) {
            if (sessionTimeMap.containsKey(id)) {
                sessionTimeMap.put(id, time);
            }
        }

        public Set<String> getCompletedSessionMissions() {
            if (sessionRewardMap.containsKey(id)) {
                return sessionRewardMap.get(id);
            } else {
                return sessionRewardMap.put(id, new HashSet<>());
            }
        }

        public void setCompletedSessionMissions(Set<String> set) {
            sessionRewardMap.put(id, set);
        }
    }
}
