package cat.nyaa.playtimetracker.db.async;

import cat.nyaa.nyaacore.orm.DatabaseUtils;
import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;
import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncDbManager<T> {
    private final String tableName;
    private final ConcurrentHashMap<String, Field> columnMap;
    private final Field primaryField;
    private final String primaryName;
    private final Connection jdbcConnection;
    private final Plugin plugin;

    public static <U> @Nullable AsyncDbManager<U> create(@NotNull Class<U> recordClass, Plugin plugin, BackendConfig backendConfig) {
        Table table = recordClass.getAnnotation(cat.nyaa.nyaacore.orm.annotations.Table.class);
        if (table == null) return null;
        String tableName = table.value();
        if (tableName == null || tableName.isEmpty()) return null;
        Map<String, Field> fieldMap = new HashMap<>();
        String primaryName = null;
        Field primaryField = null;
        for (Field classField : recordClass.getDeclaredFields()) {
            Column column = classField.getAnnotation(Column.class);
            if (column != null) {
                String fieldName = (column.name() == null || column.name().isEmpty()) ? classField.getName() : column.name();
                fieldMap.put(fieldName, classField);
                if (column.primary()) {
                    primaryName = fieldName;
                    primaryField = classField;
                }
            }
        }
        if (primaryField == null) return null;
        if (fieldMap.isEmpty()) return null;

        if (!primaryField.trySetAccessible()) return null;
        for (String key : fieldMap.keySet()) {
            if (!fieldMap.containsKey(key)) return null;
            if (!fieldMap.get(key).trySetAccessible()) return null;
        }

        Connection jdbcConnection;
        try {
            jdbcConnection = DatabaseUtils.newJdbcConnection(plugin, backendConfig);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }

        return new AsyncDbManager<>(recordClass, tableName, fieldMap, primaryField, primaryName, jdbcConnection, plugin);
    }

    AsyncDbManager(Class<T> recordClass, String tableName, Map<String, Field> columnMap, Field primaryField, String primaryName, Connection jdbcConnection, Plugin plugin) {
        this.tableName = tableName;
        this.columnMap = new ConcurrentHashMap<>(columnMap);
        this.primaryField = primaryField;
        this.primaryName = primaryName;
        this.jdbcConnection = jdbcConnection;
        this.plugin = plugin;
    }

    public boolean saveModel(Collection<T> models, boolean async) {//main server thread,Arguments must be thread safe
        Runnable runnable = () -> {
            try {
                saveModelAsync(models);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        } else {
            runnable.run();
        }
        return true;
    }

    private boolean saveModelAsync(Collection<T> models) {
        synchronized (this.jdbcConnection) {
            List<T> modelList_ = new ArrayList<>(models);
            ArrayList<String> keys = new ArrayList<>(columnMap.keySet());

            String selectSql = "SELECT " + primaryName + " FROM " + tableName + " WHERE " + primaryName + "=?";
            selectSql += " LIMIT 1";
            PreparedStatement selectPreparedStatement;
            StringBuilder spdateSql = new StringBuilder("UPDATE " + tableName + " SET ");
            for (int i = 0; i < keys.size(); i++) {
                spdateSql.append(keys.get(i)).append("=?");
                if (i + 1 < keys.size()) spdateSql.append(",");
            }
            spdateSql.append(" WHERE ").append(primaryName).append("=?");
            PreparedStatement updatePreparedStatement;
            try {
                selectPreparedStatement = jdbcConnection.prepareStatement(selectSql);
                updatePreparedStatement = jdbcConnection.prepareStatement(spdateSql.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            modelList_.removeIf((model) -> {
                        try {
                            selectPreparedStatement.setObject(1, primaryField.get(model));
                            ResultSet resultSet = selectPreparedStatement.executeQuery();
                            if (resultSet.getObject(primaryName) == null) return true;
                        } catch (SQLException | IllegalAccessException e) {
                            e.printStackTrace();
                            return true;
                        }
                        return false;
                    }
            );

            for (T model : modelList_) {
                if (!models.contains(model)) continue;
                try {
                    for (int i = 1; i <= keys.size(); i++) {
                        String columnName = keys.get(i - 1);
                        if (!columnMap.containsKey(columnName)) break;
                        updatePreparedStatement.setObject(i, columnMap.get(columnName).get(model));
                    }
                    updatePreparedStatement.setObject(keys.size() + 1, primaryField.get(model));
                    updatePreparedStatement.executeUpdate();
                } catch (SQLException | IllegalAccessException e) {
                    e.printStackTrace();
                    break;
                }
                models.remove(model);
            }
            return true;
        }
    }

    public void close() {
        try {
            jdbcConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
