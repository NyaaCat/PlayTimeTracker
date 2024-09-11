package cat.nyaa.playtimetracker.db.tables;

import cat.nyaa.playtimetracker.db.DatabaseManager;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.db.utils.DatabaseUtils;
import cat.nyaa.playtimetracker.reward.IReward;
import cat.nyaa.playtimetracker.utils.Constants;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardsTable {

    private final static Logger logger = Constants.getPluginLogger();

    public static final String TABLE_NAME = "rewards";

    public static final int HEAD_MAGIC = 0x505454FE;

    private final HikariDataSource ds;

    public RewardsTable(HikariDataSource ds) {
        this.ds = ds;
    }

    private static ByteOutputStreamEx serializeReward(IReward reward, int capacity) throws Exception {
        ByteOutputStreamEx bos = new ByteOutputStreamEx(capacity);
        try (DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(HEAD_MAGIC);
            byte[] className = reward.getClass().getName().getBytes(StandardCharsets.UTF_8);
            dos.writeInt(className.length);
            dos.write(className);
            dos.writeInt(0);
            reward.serialize(dos);
        }
        return bos;
    }

    private static IReward deserializeReward(DataInputStream dis) throws Exception {
        int headMagic = dis.readInt();
        if (headMagic != HEAD_MAGIC) {
            throw new ParseException("Invalid head magic", 0);
        }
        int classNameLength = dis.readInt();
        if(classNameLength < 0) {
            throw new ParseException("Invalid class name length", 4);
        }
        byte[] classNameBuffer = new byte[classNameLength];
        dis.readFully(classNameBuffer);
        String className = new String(classNameBuffer, StandardCharsets.UTF_8);
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        IReward reward = (IReward) constructor.newInstance();
        final int reserved = dis.readInt();
        reward.deserialize(dis);
        return reward;
    }

    public boolean tryCreateTable(Plugin plugin) {
        synchronized (DatabaseManager.lock) {
            try {
                return DatabaseUtils.tryCreateTable(ds, TABLE_NAME, "create_table_rewards.sql", plugin);
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
                deleteRewardBatch0(ids, conn);
            } catch (SQLException e) {
                logger.error("Failed to delete from {}", TABLE_NAME, e);
            }
        }
    }

    private static void deleteRewardBatch0(IntIterable ids, Connection conn) throws SQLException {
        try (var ps = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE id = ?")) {
            var it = ids.iterator();
            while (it.hasNext()) {
                ps.setInt(1, it.nextInt());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<RewardDbModel> selectRewards(UUID playerUniqueID, @Nullable String rewardName, boolean autoRemoveOnFail) {
        synchronized (DatabaseManager.lock) {
            IntList ids = null;
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
                            InputStream inputStream = rs.getBinaryStream(5);
                            try(var dis = new DataInputStream(inputStream)) {
                                reward.reward = deserializeReward(dis);
                            } catch (Exception e) {
                                logger.error("Failed to deserialize", e);
                                if (autoRemoveOnFail) {
                                    if (ids == null) {
                                        ids = new IntArrayList();
                                    }
                                    ids.add(reward.id);
                                    logger.warn("Remove invalid reward {}", reward.id);
                                }
                                continue;
                            }
                            rewards.add(reward);
                        }
                        return rewards;
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to select from {}", TABLE_NAME, e);
                return null;
            } finally {
                if(autoRemoveOnFail && ids != null) {
                    try (var conn = this.ds.getConnection()) {
                        deleteRewardBatch0(ids, conn);
                    } catch (SQLException e) {
                        logger.error("Failed to delete invalid from {}", TABLE_NAME, e);
                    }
                }
            }
        }
    }

    public int selectRewardsCount(UUID playerUniqueID, String rewardName){
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE player = ? AND rewardName = ?")) {
                    ps.setString(1, playerUniqueID.toString());
                    ps.setString(2, rewardName);
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

    public @Nullable Object2IntMap<String> selectRewardsCount(UUID playerUniqueID){
        synchronized (DatabaseManager.lock) {
            try (var conn = this.ds.getConnection()) {
                try (var ps = conn.prepareStatement("SELECT rewardName, COUNT(rewardName) FROM " + TABLE_NAME + " WHERE player = ? GROUP BY rewardName")) {
                    ps.setString(1, playerUniqueID.toString());
                    try (var rs = ps.executeQuery()) {
                        Object2IntMap<String> map = new Object2IntOpenHashMap<>();
                        while (rs.next()) {
                            String rewardName = rs.getString(1);
                            int count = rs.getInt(2);
                            map.put(rewardName, count);
                        }
                        return map;
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

