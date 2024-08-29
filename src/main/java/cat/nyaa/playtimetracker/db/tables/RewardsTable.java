package cat.nyaa.playtimetracker.db.tables;

import cat.nyaa.nyaacore.orm.BundledSQLUtils;
import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.reward.IReward;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.ints.IntIterable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardsTable {

    private static final Logger logger = LoggerFactory.getLogger(RewardsTable.class);

    public static final String TABLE_NAME = "rewards";

    public static final int HEAD_MAGIC = 0x505454FE;

    private final HikariDataSource ds;

    public RewardsTable(HikariDataSource ds) {
        this.ds = ds;
    }

    private static ByteOutputStreamEx serializeReward(IReward reward, int capacity) throws Exception {
        ByteOutputStreamEx bos = new ByteOutputStreamEx(capacity);
        try (bos) {
            byte[] intBuffer = new byte[4];

            intBuffer[0] = (byte) ((HEAD_MAGIC >>> 24) & 0xFF);
            intBuffer[1] = (byte) ((HEAD_MAGIC >>> 16) & 0xFF);
            intBuffer[2] = (byte) ((HEAD_MAGIC >>> 8) & 0xFF);
            intBuffer[3] = (byte) ((HEAD_MAGIC) & 0xFF);
            bos.write(intBuffer);

            byte[] className = reward.getClass().getName().getBytes(StandardCharsets.UTF_8);
            final int classNameLength = className.length;
            intBuffer[0] = (byte) ((classNameLength >>> 24) & 0xFF);
            intBuffer[1] = (byte) ((classNameLength >>> 16) & 0xFF);
            intBuffer[2] = (byte) ((classNameLength >>> 8) & 0xFF);
            intBuffer[3] = (byte) ((classNameLength) & 0xFF);
            bos.write(intBuffer);
            bos.write(className);

            intBuffer[0] = 0;
            intBuffer[1] = 0;
            intBuffer[2] = 0;
            intBuffer[3] = 0;
            bos.write(intBuffer);

            reward.serialize(bos);
        }
        return bos;
    }

    private static IReward deserializeReward(InputStream inputStream) throws Exception {
        try (inputStream) {
            byte[] intBuffer = new byte[4];

            if(inputStream.read(intBuffer) < 4) {
                throw new IOException("invalid head magic length");
            }
            int headMagic = ((intBuffer[0] & 0xFF) << 24)
                          | ((intBuffer[1] & 0xFF) << 16)
                          | ((intBuffer[2] & 0xFF) << 8)
                          | (intBuffer[3] & 0xFF);
            if (headMagic != HEAD_MAGIC) {
                throw new ParseException("Invalid head magic", 0);
            }

            if(inputStream.read(intBuffer) < 4) {
                throw new IOException("invalid class name length");
            }
            int classNameLength = ((intBuffer[0] & 0xFF) << 24)
                                | ((intBuffer[1] & 0xFF) << 16)
                                | ((intBuffer[2] & 0xFF) << 8)
                                | (intBuffer[3] & 0xFF);
            if(classNameLength < 0) {
                throw new ParseException("Invalid class name length", 4);
            }
            byte[] classNameBuffer = new byte[classNameLength];
            if(inputStream.read(classNameBuffer) < 0) {
                throw new IOException("invalid class name");
            }
            String className = new String(classNameBuffer, StandardCharsets.UTF_8);
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            IReward reward = (IReward) constructor.newInstance();

            if(inputStream.skip(4) < 4) {
                throw new IOException("invalid format");
            }

            reward.deserialize(inputStream);
            return reward;
        }
    }

    public boolean tryCreateTable(Plugin plugin) {
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                final String[] types = { "TABLE" };
                ResultSet rs = conn.getMetaData().getTables(null, null, TABLE_NAME, types);
                if(rs.next()){
                    return false;
                }
                BundledSQLUtils.queryBundledAs(plugin, conn, "create_table_rewards.sql", null, null);
                return true;
            } catch (SQLException e) {
                logger.error("Failed to create {}", TABLE_NAME, e);
                return false;
            }
        }
    }

    public void insertReward(RewardDbModel rewardDbModel) {
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (completedTime, player, rewardName, rewardData) VALUES (?,?,?,?)")) {
                    ps.setLong(1, rewardDbModel.completedTime);
                    ps.setString(2, rewardDbModel.playerUniqueID.toString());
                    ps.setString(3, rewardDbModel.rewardName);
                    ByteOutputStreamEx bos = serializeReward(rewardDbModel.reward, 1024);
                    int size = bos.size();
                    ps.setBinaryStream(4, bos.flip(), size);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to insert into {}", TABLE_NAME, e);
            } catch (Exception e) {
                logger.error("Failed to serialize", e);
            }
        }
    }

    public void insertRewardBatch(Iterable<RewardDbModel> rewards) {
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                try (var ps = conn.prepareStatement("INSERT INTO " + TABLE_NAME + " (completedTime, player, rewardName, rewardData) VALUES (?,?,?,?)")) {
                    for (var reward : rewards) {
                        ps.setLong(1, reward.completedTime);
                        ps.setString(2, reward.playerUniqueID.toString());
                        ps.setString(3, reward.rewardName);
                        ByteOutputStreamEx bos = serializeReward(reward.reward, 1024);
                        int size = bos.size();
                        ps.setBinaryStream(4, bos.flip(), size);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.error("Failed to insert into {}", TABLE_NAME, e);
            } catch (Exception e) {
                logger.error("Failed to serialize", e);
            }
        }
    }

    public void deleteReward(int id) {
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to delete from {}", TABLE_NAME, e);
            }
        }
    }

    public void deleteRewardBatch(IntIterable ids) {
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                try (var ps = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE id = ?")) {
                    var it = ids.iterator();
                    while (it.hasNext()) {
                        ps.setInt(1, it.nextInt());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                logger.error("Failed to delete from {}", TABLE_NAME, e);
            }
        }
    }

    public List<RewardDbModel> selectRewards(UUID playerUniqueID, @Nullable String rewardName){
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                String sql;
                if (rewardName == null) {
                    sql = "SELECT * FROM " + TABLE_NAME + " WHERE player = ?";
                } else {
                    sql = "SELECT * FROM " + TABLE_NAME + " WHERE player = ? AND rewardName = ?";
                }
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUniqueID.toString());
                    if (rewardName != null) {
                        ps.setString(2, rewardName);
                    }
                    try (var rs = ps.executeQuery()) {
                        var rewards = new ArrayList<RewardDbModel>();
                        while (rs.next()) {
                            var reward = new RewardDbModel();
                            reward.id = rs.getInt(1);
                            reward.completedTime = rs.getLong(2);
                            reward.playerUniqueID = UUID.fromString(rs.getString(3));
                            reward.rewardName = rs.getString(4);
                            reward.reward = deserializeReward(rs.getBinaryStream(5));
                            rewards.add(reward);
                        }
                        return rewards;
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to select from {}", TABLE_NAME, e);
                return null;
            } catch (Exception e) {
                logger.error("Failed to deserialize", e);
                return null;
            }
        }
    }

    public int selectRewardsCount(UUID playerUniqueID, @Nullable String rewardName){
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                String sql;
                if (rewardName == null) {
                    sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE player = ?";
                } else {
                    sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE player = ? AND rewardName = ?";
                }
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUniqueID.toString());
                    if (rewardName != null) {
                        ps.setString(2, rewardName);
                    }
                    try (var rs = ps.executeQuery()) {
                        if(rs.next()) {
                            return rs.getInt(1);
                        }
                        return 0;
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to select from {}", TABLE_NAME, e);
                return 0;
            } catch (Exception e) {
                logger.error("Failed to deserialize", e);
                return 0;
            }
        }
    }

//    public RewardDbModel selectReward(UUID playerUniqueID, String rewardName){
//        synchronized (DatabaseManager.lock) {
//            try (var conn = this.ds.getConnection()) {
//                try (var ps = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE player = ? AND rewardName = ?")) {
//                    ps.setString(1, playerUniqueID.toString());
//                    ps.setString(2, rewardName);
//                    try (var rs = ps.executeQuery()) {
//                        if (rs.next()) {
//                            var reward = new RewardDbModel();
//                            reward.id = rs.getInt(1);
//                            reward.completedTime = rs.getLong(2);
//                            reward.playerUniqueID = UUID.fromString(rs.getString(3));
//                            reward.rewardName = rs.getString(4);
//                            reward.reward = deserializeReward(rs.getBinaryStream(5));
//                            return reward;
//                        }
//                        return null;
//                    }
//                }
//            } catch (SQLException e) {
//                logger.error("Failed to select from {}", TABLE_NAME, e);
//                return null;
//            } catch (Exception e) {
//                logger.error("Failed to deserialize reward", e);
//                return null;
//            }
//        }
//    }


    // for extract byte array from ByteArrayOutputStream without copying
    static class ByteOutputStreamEx extends ByteArrayOutputStream {

        public ByteOutputStreamEx(int size) {
            super(size);
        }

        public ByteArrayInputStream flip() {
            return new ByteArrayInputStream(this.buf, 0, this.count);
        }
    }
}

