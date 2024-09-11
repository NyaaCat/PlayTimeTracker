package cat.nyaa.playtimetracker.db.utils;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils {

    private static final String[] TYPES_TABLE = { "TABLE" };

    public static boolean tryCreateTable(HikariDataSource ds, String tableName, String fileCreateTableSql, Plugin plugin) throws SQLException {
        try (var conn = ds.getConnection()) {
            ResultSet rs = conn.getMetaData().getTables(null, null, tableName, TYPES_TABLE);
            if(rs.next()){
                return false;
            }
            var path = "sql/" + fileCreateTableSql;
            var sqlInputStream = plugin.getResource(path);
            if(sqlInputStream == null){
                throw new SQLException("cannot find sql file from resource: " + path);
            }
            String sql;
            try(sqlInputStream){
                byte[] bytes = sqlInputStream.readAllBytes();
                sql = new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new SQLException("cannot read sql file from resource: " + path, e);
            }
            try(var ps = conn.prepareStatement(sql)){
                ps.execute();
            }
            return true;
        }
    }
}
