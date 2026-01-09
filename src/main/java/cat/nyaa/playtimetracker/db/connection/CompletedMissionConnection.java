package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.tables.CompletedMissionTable;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CompletedMissionConnection {
    private final CompletedMissionTable completedMissionTable;
    private final ExecutorService ioExecutor;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, CompletedMissionDbModel>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> cacheLoading = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Set<String>> pendingMissionResets = new ConcurrentHashMap<>();

    public CompletedMissionConnection(HikariDataSource ds, Plugin plugin) {
        this.completedMissionTable = new CompletedMissionTable(ds);
        this.completedMissionTable.tryCreateTable(plugin);
        this.ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ptt-completed-mission-io");
            thread.setDaemon(true);
            return thread;
        });
    }

    public boolean isCacheLoaded(UUID playerId) {
        return cache.containsKey(playerId);
    }

    public void warmupPlayerCache(UUID playerId) {
        if (cache.containsKey(playerId) || !cacheLoading.add(playerId)) {
            return;
        }
        ioExecutor.execute(() -> {
            try {
                List<CompletedMissionDbModel> models = completedMissionTable.select(playerId, null);
                ConcurrentHashMap<String, CompletedMissionDbModel> missionMap = new ConcurrentHashMap<>(Math.max(16, models.size()));
                for (CompletedMissionDbModel model : models) {
                    missionMap.put(model.getMissionName(), model);
                }
                var existing = cache.putIfAbsent(playerId, missionMap);
                var target = existing != null ? existing : missionMap;
                var pending = pendingMissionResets.remove(playerId);
                if (pending != null && !pending.isEmpty()) {
                    pending.forEach(target::remove);
                }
            } finally {
                cacheLoading.remove(playerId);
            }
        });
    }

    public void unloadPlayerCache(UUID playerId) {
        cache.remove(playerId);
        cacheLoading.remove(playerId);
    }

    @Nullable
    public CompletedMissionDbModel getCachedPlayerCompletedMission(UUID playerUniqueId, String missionName) {
        var missions = cache.get(playerUniqueId);
        if (missions == null) {
            return null;
        }
        return missions.get(missionName);
    }

    public void resetPlayerCompletedMission(UUID playerId) {
        cache.put(playerId, new ConcurrentHashMap<>());
        cacheLoading.remove(playerId);
        pendingMissionResets.remove(playerId);
        ioExecutor.execute(() -> completedMissionTable.delete(playerId));
    }

    public void resetPlayerCompletedMission(String missionName, UUID playerUniqueId) {
        var missions = cache.get(playerUniqueId);
        if (missions != null) {
            missions.remove(missionName);
            var pending = pendingMissionResets.get(playerUniqueId);
            if (pending != null) {
                pending.remove(missionName);
            }
        } else {
            pendingMissionResets.computeIfAbsent(playerUniqueId, key -> ConcurrentHashMap.newKeySet()).add(missionName);
        }
        ioExecutor.execute(() -> completedMissionTable.delete(playerUniqueId, missionName));
    }

    public void writeMissionCompleted(UUID playerUniqueId, String missionName, long lastCompletedTime) {
        var missions = cache.get(playerUniqueId);
        if (missions != null) {
            CompletedMissionDbModel cached = missions.get(missionName);
            if (cached == null) {
                CompletedMissionDbModel newModel = new CompletedMissionDbModel();
                newModel.setMissionName(missionName);
                newModel.setLastCompletedTime(lastCompletedTime);
                newModel.setPlayerUniqueId(playerUniqueId);
                missions.put(missionName, newModel);
            } else {
                cached.setLastCompletedTime(lastCompletedTime);
            }
        }
        ioExecutor.execute(() -> {
            var rs = completedMissionTable.select(playerUniqueId, missionName);
            if (rs.isEmpty()) {
                CompletedMissionDbModel newModel = new CompletedMissionDbModel();
                newModel.setMissionName(missionName);
                newModel.setLastCompletedTime(lastCompletedTime);
                newModel.setPlayerUniqueId(playerUniqueId);
                completedMissionTable.insert(newModel);
            } else {
                var model = rs.getFirst();
                model.setLastCompletedTime(lastCompletedTime);
                completedMissionTable.updatePlayer(model, model.getId());
            }
        });
    }

    @Nullable
    public CompletedMissionDbModel getPlayerCompletedMission(UUID playerUniqueId, String missionName) {
        if (cache.containsKey(playerUniqueId)) {
            return getCachedPlayerCompletedMission(playerUniqueId, missionName);
        }
        var rs = completedMissionTable.select(playerUniqueId, missionName);
        if (!rs.isEmpty()) {
            return rs.getFirst();
        }
        return null;
    }

    @NotNull
    public List<CompletedMissionDbModel> getPlayerCompletedMissionList(UUID playerUniqueId) {
        if (cache.containsKey(playerUniqueId)) {
            var missions = cache.get(playerUniqueId);
            if (missions == null || missions.isEmpty()) {
                return List.of();
            }
            return new ArrayList<>(missions.values());
        }
        return completedMissionTable.select(playerUniqueId, null);
    }

    public void close() {
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
