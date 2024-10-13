package cat.nyaa.playtimetracker.db.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class DatabaseUtils {

    private static final String[] TYPES_TABLE = {"TABLE"};

    public static boolean tryCreateTable(HikariDataSource ds, String tableName, String fileCreateTableSql, Plugin plugin, String ... migrationTableAndFile) throws SQLException {
        try (var conn = ds.getConnection()) {
            var meta = conn.getMetaData();
            var rs = meta.getTables(null, null, tableName, TYPES_TABLE);
            if (rs.next()) {
                return false;
            }
            String tableLegacy = null;
            String sqlMigration = null;
            if (migrationTableAndFile.length > 0) {
                if (migrationTableAndFile.length % 2 != 0) {
                    throw new IllegalArgumentException("migrationTableAndFile should be a pair of table name and migration file name");
                }
                for (int i = 0; i < migrationTableAndFile.length; i += 2) {
                    var legacyTableName = migrationTableAndFile[i];
                    var migrationFileName = migrationTableAndFile[i + 1];
                    var rs0 = meta.getTables(null, null, legacyTableName, TYPES_TABLE);
                    if (rs0.next()) {
                        sqlMigration = readSqlFromResource(plugin, "sql/" + migrationFileName);
                        tableLegacy = legacyTableName;
                    }
                }
            }
            String sql = readSqlFromResource(plugin, "sql/" + fileCreateTableSql);
            var logger = plugin.getSLF4JLogger();
            logger.info("Creating table: {}", tableName);
            try (var ps = conn.prepareStatement(sql)) {
                ps.execute();
            }
            logger.info("Table created: {}", tableName);
            if (sqlMigration != null) {
                logger.info("Migrating data from table: {}", tableLegacy);
                try (var ps = conn.prepareStatement(sqlMigration)) {
                    ps.execute();
                }
                logger.info("Data migrated: {} <= {}", tableName, tableLegacy);
            }
            return true;
        }
    }

    private static String readSqlFromResource(Plugin plugin, String path) throws SQLException {
        var sqlInputStream = plugin.getResource(path);
        if (sqlInputStream == null) {
            throw new SQLException("cannot find sql file from resource: " + path);
        }
        try (sqlInputStream) {
            byte[] bytes = sqlInputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("cannot read sql file from resource: " + path, e);
        }
    }
}
