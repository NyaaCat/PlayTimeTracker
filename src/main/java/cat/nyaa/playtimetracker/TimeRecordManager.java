package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.Utils.TaskUtils;
import cat.nyaa.playtimetracker.Utils.TimeUtils;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.event.player.time.DailyResetEvent;
import cat.nyaa.playtimetracker.event.player.time.MonthlyResetEvent;
import cat.nyaa.playtimetracker.event.player.time.WeeklyResetEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TimeRecordManager {
    private final PlayTimeTracker plugin;
    private final Set<UUID> holdPlayers = new HashSet<>();
    private final TimeTrackerConnection timeTrackerConnection;
    private long tickNum;

    public TimeRecordManager(PlayTimeTracker playTimeTracker, TimeTrackerConnection timeTrackerConnection) {
        this.plugin = playTimeTracker;
        this.timeTrackerConnection = timeTrackerConnection;
        init();
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }

    private void init() {
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
    }

    public void addPlayer(UUID playerId) {
        holdPlayers.add(playerId);
        insertOrResetPlayer(playerId, TimeUtils.getUnixTimeStampNow());
    }

    public void addPlayer(@NotNull Player player) {
        addPlayer(player.getUniqueId());
    }

    public void removePlayer(UUID playerId) {
        if (!holdPlayers.contains(playerId)) return;
        updatePlayerTime(playerId);
        holdPlayers.remove(playerId);
    }

    public void removePlayer(@NotNull Player player) {
        removePlayer(player.getUniqueId());
    }

    public void insertOrResetPlayer(UUID playerId, long timestamp) {
        TimeTrackerDbModel model = timeTrackerConnection.getPlayerTimeTracker(playerId);
        if (model == null) {
            timeTrackerConnection.insertPlayer(playerId, timestamp);
            return;
        }
        this.updatePlayerTime(playerId, model, timestamp, false);
    }

    public void checkAndUpdateTick() {
        this.tickNum++;
        Set<UUID> onlineSet = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toSet());
        if (!onlineSet.equals(holdPlayers)) {
            checkHoldPlayers(onlineSet);
        }
        holdPlayers.forEach((uuid) -> TaskUtils.mod64TickToRun(tickNum, uuid, () -> updatePlayerTime(uuid)));
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTrackerDbModel(@NotNull Player player) {
        return getPlayerTimeTrackerDbModel(player.getUniqueId());
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTrackerDbModel(@NotNull UUID playerId) {
        return timeTrackerConnection.getPlayerTimeTracker(playerId);
    }

    private void updatePlayerTime(UUID playerId) {
        TimeTrackerDbModel model = timeTrackerConnection.getPlayerTimeTracker(playerId);
        if (model == null) {
            throw new RuntimeException("Unable to update player data : player id " + playerId.toString() + " not found in database.");
        }
        this.updatePlayerTime(playerId, model, TimeUtils.getUnixTimeStampNow(), true);
    }

    private void updatePlayerTime(UUID playerId, @NotNull TimeTrackerDbModel model, long nowTimestamp, boolean accumulative) {
        long pre = System.currentTimeMillis();
        long lastSeenTimestamp = model.getLastSeen();
        long duration = nowTimestamp - lastSeenTimestamp;
        if (duration <= 0) return;
        if (PlayerAFKManager.isAFK(playerId)) {
            accumulative = false; //todo AFK calculation can be more precise
        }
        ZonedDateTime now = TimeUtils.timeStamp2ZonedDateTime(nowTimestamp);
        ZonedDateTime lastSeen = TimeUtils.timeStamp2ZonedDateTime(lastSeenTimestamp);

        ZonedDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
        if (startOfToday.isAfter(lastSeen)) {
            Bukkit.getPluginManager().callEvent(new DailyResetEvent(playerId, model.getDailyTime()));
            model.setDailyTime(0);
        } else if (accumulative) {
            model.setDailyTime(model.getDailyTime() + duration);
        }

        if (startOfWeek.isAfter(lastSeen)) {
            Bukkit.getPluginManager().callEvent(new WeeklyResetEvent(playerId, model.getWeeklyTime()));
            model.setWeeklyTime(0);
        } else if (accumulative) {
            model.setWeeklyTime(model.getWeeklyTime() + duration);
        }

        if (startOfMonth.isAfter(lastSeen)) {
            Bukkit.getPluginManager().callEvent(new MonthlyResetEvent(playerId, model.getMonthlyTime()));
            model.setMonthlyTime(0);
        } else if (accumulative) {
            model.setMonthlyTime(model.getMonthlyTime() + duration);
        }

        if (accumulative)
            model.setTotalTime(model.getMonthlyTime() + duration);
        model.setLastSeen(nowTimestamp);
        timeTrackerConnection.updateDbModel(model);
    }

    public TimeTrackerConnection getTimeTrackerConnection() {
        return timeTrackerConnection;
    }

    private void checkHoldPlayers(Set<UUID> onlineSet) {
        Set<UUID> onlineSet_ = new HashSet<>(onlineSet);
        Set<UUID> holdSet_ = new HashSet<>(holdPlayers);
        Set<UUID> andSet = new HashSet<>(onlineSet);
        andSet.retainAll(holdSet_);

        onlineSet_.removeAll(andSet);
        holdSet_.removeAll(andSet);
        holdSet_.forEach(this::removePlayer);
        onlineSet_.forEach(this::addPlayer);
//        onlineSet_ = holdSet_ = andSet = null;
    }


}
