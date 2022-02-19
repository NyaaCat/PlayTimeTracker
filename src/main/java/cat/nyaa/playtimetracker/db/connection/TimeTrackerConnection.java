package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.db.tables.TimeTrackerTable;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import com.google.common.collect.Sets;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeTrackerConnection {
    static ConcurrentHashMap<UUID, TimeTrackerDbModel> cache = new ConcurrentHashMap<>();
    private final TimeTrackerTable timeTrackerTable;
    private final HikariDataSource ds;
    private final Plugin plugin;

    public TimeTrackerConnection(HikariDataSource ds, Plugin plugin) {
        this.ds = ds;
        this.plugin = plugin;
        this.timeTrackerTable = new TimeTrackerTable(ds);
    }

    public void deletePlayerData(UUID playerId) {
        TimeTrackerDbModel trackerDbModel = getPlayerTimeTracker(playerId);
        if (trackerDbModel == null) return;
        cache.remove(trackerDbModel.getPlayerUniqueId());
        timeTrackerTable.deletePlayer(playerId);
    }

    public void insertPlayer(TimeTrackerDbModel trackerDbModel) {
        timeTrackerTable.insertPlayer(trackerDbModel);
    }

    public void insertPlayer(UUID playerId, long time) {
        TimeTrackerDbModel trackerDbModel = getPlayerTimeTracker(playerId);
        if (trackerDbModel == null) {
            trackerDbModel = new TimeTrackerDbModel();
            trackerDbModel.setPlayerUniqueId(playerId);
            trackerDbModel.setDailyTime(0L);
            trackerDbModel.setWeeklyTime(0L);
            trackerDbModel.setMonthlyTime(0L);
            trackerDbModel.setTotalTime(0L);
            trackerDbModel.setLastSeen(time);
            insertPlayer(trackerDbModel);
        }
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTracker(UUID playerId) {
        if (cache.containsKey(playerId)) {
            return cache.get(playerId);
        }
        return getPlayerTimeTrackerNocache(playerId);
    }

    @Nullable
    private TimeTrackerDbModel getPlayerTimeTrackerNocache(UUID playerId) {
        return timeTrackerTable.selectPlayer(playerId);
    }


    public void updateDbModel(@NotNull TimeTrackerDbModel model) {
        if (getPlayerTimeTracker(model.getPlayerUniqueId()) == null)
            insertPlayer(model.getPlayerUniqueId(), TimeUtils.getUnixTimeStampNow());
        cache.put(model.getPlayerUniqueId(), model);
//        timeTrackerTable.update(model, model.getPlayerUniqueId());
    }

    public void doAsyncUpdate() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var cacheSet = Sets.newHashSet(cache.values());
            timeTrackerTable.updateBatch(cacheSet);
            cacheSet.forEach(model -> cache.remove(model.getPlayerUniqueId(), model));
        });
    }
}
