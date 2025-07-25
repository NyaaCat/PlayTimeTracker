package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.db.tables.TimeTrackerTable;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

@NotThreadSafe
public final class TimeTrackerConnection implements IBatchOperate {

    private Map<UUID, ObjectIntMutablePair<TimeTrackerDbModel>> cache;  // 0: select 1: insert 2: update
    private final TimeTrackerTable timeTrackerTable;

    public TimeTrackerConnection(HikariDataSource ds, Plugin plugin) {
        this.cache = null;
        this.timeTrackerTable = new TimeTrackerTable(ds);
        this.timeTrackerTable.tryCreateTable(plugin);
    }

    public void deletePlayerData(UUID playerId) {
        if (this.cache != null) {
            this.cache.remove(playerId);
        }
        // TODO: call by {@code insertPlayer} is not safe so execute delete always
        this.timeTrackerTable.deletePlayer(playerId);
    }

    public void insertPlayer(TimeTrackerDbModel trackerDbModel) {
        if (this.cache != null) {
            this.cache.put(trackerDbModel.getPlayerUniqueId(), new ObjectIntMutablePair<>(trackerDbModel, 1));
            return;
        }
        this.timeTrackerTable.insert(trackerDbModel);
    }

    public void insertPlayerIfNotPresent(UUID playerId, long time) {
        TimeTrackerDbModel trackerDbModel = this.getPlayerTimeTracker(playerId);
        if (trackerDbModel == null) {
            trackerDbModel = new TimeTrackerDbModel();
            trackerDbModel.setPlayerUniqueId(playerId);
            trackerDbModel.setDailyTime(0L);
            trackerDbModel.setWeeklyTime(0L);
            trackerDbModel.setMonthlyTime(0L);
            trackerDbModel.setTotalTime(0L);
            trackerDbModel.setLastSeen(time);
            trackerDbModel.setLastUpdate(time);
            this.insertPlayer(trackerDbModel);
        }
    }

    @Nullable
    public TimeTrackerDbModel getPlayerTimeTracker(UUID playerId) {
        if (this.cache != null) {
            var pair = this.cache.get(playerId);
            if (pair != null) {
                return pair.left();
            }
        }
        var result = this.timeTrackerTable.selectPlayer(playerId);
//        if (this.cache != null && result != null) {
//            this.cache.put(playerId, new ObjectIntMutablePair<>(result, 0));
//        }
        return result;
    }


    public void updateDbModel(@NotNull TimeTrackerDbModel model) {
        if (this.cache != null) {
            this.cache.compute(model.playerUniqueId, new UpdateCache(model));
            return;
        }
        this.timeTrackerTable.update(model);
    }


    public void close() {
        this.handleCache();
        this.cache = null;
    }


    @Override
    public void beginBatchMode() {
        this.cache = new Object2ObjectOpenHashMap<>();
    }


    @Override
    public void endBatchMode() {
        if (this.cache != null) {
            this.handleCache();
            this.cache = null;
        }
    }

    private void handleCache() {
        if (this.cache == null || this.cache.isEmpty()) {
            return;
        }
        List<TimeTrackerDbModel> insertList = new ObjectArrayList<>(cache.size());
        List<TimeTrackerDbModel> updateList = new ObjectArrayList<>(cache.size());
        for (var entry : this.cache.entrySet()) {
            var ty = entry.getValue().rightInt();
            if (ty == 1) {
                insertList.add(entry.getValue().left());
                entry.getValue().right(0);
                continue;
            }
            if (ty == 2) {
                updateList.add(entry.getValue().left());
                continue;
            }
        }
        if (!insertList.isEmpty()) {
            this.timeTrackerTable.insertBatch(insertList);
        }
        if (!updateList.isEmpty()) {
            this.timeTrackerTable.updateBatch(updateList);
        }
    }

    @Deprecated
    public void doAsyncUpdate(Plugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::handleCache);
    }

    private record UpdateCache(TimeTrackerDbModel model) implements BiFunction<UUID, ObjectIntMutablePair<TimeTrackerDbModel>, ObjectIntMutablePair<TimeTrackerDbModel>> {

        @Override
        public ObjectIntMutablePair<TimeTrackerDbModel> apply(UUID uuid, ObjectIntMutablePair<TimeTrackerDbModel> pair) {
            if (pair == null) {
                return new ObjectIntMutablePair<>(model, 2);
            } else {
                pair.left(model);
                if (pair.rightInt() == 0) {
                    pair.right(2);
                }
                return pair;
            }
        }
    }
}
