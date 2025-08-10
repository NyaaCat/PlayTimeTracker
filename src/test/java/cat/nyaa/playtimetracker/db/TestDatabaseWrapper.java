package cat.nyaa.playtimetracker.db;

import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.IBatchOperate;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.connection.TimeTrackerConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@Disabled("Temporarily disabling all tests in this class")
public class TestDatabaseWrapper extends AbstractTestDatabase {

    @BeforeEach
    public void setUp() {
        this.setUp0();
    }

    @AfterEach
    public void tearDown() {
        this.tearDown0();
    }

    protected HikariDataSource buildDataSource(int mask) {
        File root = new File("build");
        if(!root.isDirectory()) {
            root = new File("tmp");
            root.mkdir();
        }
        File db = new File(root, String.format("test_%x.db", mask));
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + db.getAbsolutePath());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }


    @ParameterizedTest
    @ValueSource(ints = {
            0,
            12, 13, 14, 15, 16, 17, 18,
            23, 24, 25, 26, 27, 28,
            34, 35, 36, 37, 38,
            45, 46, 47, 48,
            56, 57, 58,
            67, 68,
            78,
    })
    public void testRewardsConnection(int mask) {
        RewardsConnection rewardsConnection = new RewardsConnection(this.ds, plugin);
        this.server.setPlayers(3);
        long timestamp = System.currentTimeMillis();
        var mk = new Mask(rewardsConnection, mask, 10);

        // insert 1
        mk.mark(1);
        var r1p1m1 = this.createRewardDbModel(0, "mission1", timestamp);
        rewardsConnection.addReward(r1p1m1.playerUniqueID, r1p1m1.rewardName, r1p1m1.reward, r1p1m1.completedTime);

        var r2p2m1 = this.createRewardDbModel(1, "mission1", timestamp + 1000);
        rewardsConnection.addReward(r2p2m1.playerUniqueID, r2p2m1.rewardName, r2p2m1.reward, r2p2m1.completedTime);

        var r3p1m2 = this.createRewardDbModel(0, "mission2", timestamp + 2000);
        rewardsConnection.addReward(r3p1m2.playerUniqueID, r3p1m2.rewardName, r3p1m2.reward, r3p1m2.completedTime);

        // query 1
        mk.mark(2);
        var list1p1 = rewardsConnection.getRewards(this.getPlayerUUID(0), null);
        Assertions.assertEquals(2, list1p1.size());
        Assertions.assertEquals(r1p1m1, list1p1.get(0));
        Assertions.assertEquals(r3p1m2, list1p1.get(1));

        var count1p1 = rewardsConnection.countRewards(this.getPlayerUUID(0));
        Assertions.assertEquals(1, count1p1.get("mission1"));
        Assertions.assertEquals(1, count1p1.get("mission2"));

        var list1p2m1 = rewardsConnection.getRewards(this.getPlayerUUID(1), "mission1");
        Assertions.assertEquals(1, list1p2m1.size());
        Assertions.assertEquals(r2p2m1, list1p2m1.get(0));

        var count1p2m1 = rewardsConnection.countReward(this.getPlayerUUID(1), "mission1");
        Assertions.assertEquals(1, count1p2m1);

        var list1p2m2 = rewardsConnection.getRewards(this.getPlayerUUID(1), "mission2");
        Assertions.assertEquals(0, list1p2m2.size());

        var count1p2m2 = rewardsConnection.countReward(this.getPlayerUUID(1), "mission2");
        Assertions.assertEquals(0, count1p2m2);

        // insert 2
        mk.mark(3);
        var r4p2m1 = this.createRewardDbModel(1, "mission1", timestamp + 3000);
        var r5p2m1 = this.createRewardDbModel(1, "mission1", timestamp + 3000);
        var r6p1m2 = this.createRewardDbModel(0, "mission2", timestamp + 5000);
        var r7p2m2 = this.createRewardDbModel(1, "mission2", timestamp + 6000);
        rewardsConnection.addRewards(r4p2m1.playerUniqueID, r4p2m1.rewardName,  List.of(r4p2m1.reward, r5p2m1.reward), r4p2m1.completedTime);
        rewardsConnection.addRewards(r6p1m2.playerUniqueID, r6p1m2.rewardName,  List.of(r6p1m2.reward), r6p1m2.completedTime);
        rewardsConnection.addRewards(r7p2m2.playerUniqueID, r7p2m2.rewardName,  List.of(r7p2m2.reward), r7p2m2.completedTime);

        // query 2
        mk.mark(4);
        var list2p2 = rewardsConnection.getRewards(this.getPlayerUUID(1), null);
        Assertions.assertEquals(4, list2p2.size());

        var list2p1m2 = rewardsConnection.getRewards(this.getPlayerUUID(0), "mission2");
        Assertions.assertEquals(2, list2p1m2.size());

        // delete 1
        mk.mark(5);
        rewardsConnection.removeReward(list1p1.get(0).id);

        // query 3
        mk.mark(6);
        var list3p1 = rewardsConnection.getRewards(this.getPlayerUUID(0), "mission1");
        Assertions.assertEquals(0, list3p1.size());

        // delete 2
        mk.mark(7);
        IntArrayList ids = new IntArrayList(2);
        for (var model : list2p2) {
            if (model.rewardName.equals("mission1")) {
                ids.add(model.id);
            }
        }
        rewardsConnection.removeRewards(ids);

        // query 4
        mk.mark(8);
        var count4p2 = rewardsConnection.countRewards(this.getPlayerUUID(1));
        Assertions.assertEquals(1, count4p2.get("mission2"));
        Assertions.assertNull(count4p2.get("mission1"));

        // query 5
        var list5p1 = rewardsConnection.getRewards(this.getPlayerUUID(0), null);
        Assertions.assertEquals(2, list5p1.size());
    }


    @ParameterizedTest
    @ValueSource(ints = {
            0,
            12, 13, 14, 15, 16, 17, 18, 19,
            23, 24, 25, 26, 27, 28, 29,
            34, 35, 36, 37, 38, 39,
            45, 46, 47, 48, 49,
            56, 57, 58, 59,
            67, 68, 69,
            78, 79,
            89
    })
    public void testCompletedMissionConnection(int mask) {
        CompletedMissionConnection completedMissionConnection = new CompletedMissionConnection(this.ds, plugin);
        server.setPlayers(3);
        long completedTime = System.currentTimeMillis();
        var mk = new Mask(completedMissionConnection, mask, 10);

        // insert 1
        mk.mark(1);
        var c1p1m1 = this.createCompletedMissionDbModel(0, "reward1", completedTime);
        completedMissionConnection.writeMissionCompleted(c1p1m1.playerUniqueId, c1p1m1.missionName, c1p1m1.lastCompletedTime);

        var c2p2m2 = this.createCompletedMissionDbModel(1, "reward2", completedTime + 2000);
        completedMissionConnection.writeMissionCompleted(c2p2m2.playerUniqueId, c2p2m2.missionName, c2p2m2.lastCompletedTime);

        mk.mark(2);
        var c3p1m2 = this.createCompletedMissionDbModel(0, "reward2", completedTime + 1000);
        completedMissionConnection.writeMissionCompleted(c3p1m2.playerUniqueId, c3p1m2.missionName, c3p1m2.lastCompletedTime);


        // query 1
        mk.mark(3);
        var list1p1 = completedMissionConnection.getPlayerCompletedMissionList(this.getPlayerUUID(0));
        Assertions.assertEquals(2, list1p1.size());
        Assertions.assertEquals(c1p1m1, list1p1.get(0));
        Assertions.assertEquals(c3p1m2, list1p1.get(1));

        var elem1p2m2 = completedMissionConnection.getPlayerCompletedMission(this.getPlayerUUID(1), "reward2");
        Assertions.assertEquals(c2p2m2, elem1p2m2);

        var elem1p2m1 = completedMissionConnection.getPlayerCompletedMission(this.getPlayerUUID(1), "reward1");
        Assertions.assertNull(elem1p2m1);

        // update
        mk.mark(4);
        c1p1m1.setLastCompletedTime(c1p1m1.getLastCompletedTime() + 1000000);
        completedMissionConnection.writeMissionCompleted(c1p1m1.playerUniqueId, c1p1m1.missionName, c1p1m1.lastCompletedTime);

        // query 2
        mk.mark(5);
        var elem2p1 = completedMissionConnection.getPlayerCompletedMission(this.getPlayerUUID(0), "reward1");
        Assertions.assertEquals(c1p1m1, elem2p1);

        // delete
        mk.mark(6);
        completedMissionConnection.resetPlayerCompletedMission("reward2", this.getPlayerUUID(0));

        // query 3
        mk.mark(7);
        var list3p1 = completedMissionConnection.getPlayerCompletedMissionList(this.getPlayerUUID(0));
        Assertions.assertEquals(1, list3p1.size());
        Assertions.assertEquals("reward1", list3p1.get(0).missionName);

        // delete
        mk.mark(8);
        completedMissionConnection.resetPlayerCompletedMission(this.getPlayerUUID(1));

        // query 4
        mk.mark(9);
        var list4p1 = completedMissionConnection.getPlayerCompletedMissionList(this.getPlayerUUID(0));
        Assertions.assertEquals(1, list4p1.size());
        var list4p2 = completedMissionConnection.getPlayerCompletedMissionList(this.getPlayerUUID(1));
        Assertions.assertEquals(0, list4p2.size());
    }


    @ParameterizedTest
    @ValueSource(ints = {
            0,
            12, 13, 14, 15, 16, 17, 18, 19,
            23, 24, 25, 26, 27, 28, 29,
            34, 35, 36, 37, 38, 39,
            45, 46, 47, 48, 49,
            56, 57, 58, 59,
            67, 68, 69,
            78, 79,
            89
    })
    public void testTimeTrackerConnection(int mask) {
        TimeTrackerConnection timeTrackerConnection = new TimeTrackerConnection(this.ds, plugin);
        server.setPlayers(5);
        long timestamp = System.currentTimeMillis();
        var mk = new Mask(timeTrackerConnection, mask, 10);

        // insert 1
        mk.mark(1);
        var m1 = this.createTimeTrackerDbModel(0, timestamp, 114 * 86400 + 976);
        timeTrackerConnection.insertPlayer(m1);

        mk.mark(2);
        var m2 = this.createTimeTrackerDbModel(1, timestamp + 1000, 234 * 86400 + 543);
        var m3 = this.createTimeTrackerDbModel(2, timestamp + 2000);
        timeTrackerConnection.insertPlayer(m2);
        timeTrackerConnection.insertPlayerIfNotPresent(m3.playerUniqueId, timestamp + 2000);

        // query 1
        mk.mark(3);
        var m1t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(0));
        Assertions.assertEquals(m1, m1t);
        var m2t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(1));
        Assertions.assertEquals(m2, m2t);
        var m3t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(2));
        Assertions.assertEquals(m3, m3t);
        var m4t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(3));
        Assertions.assertNull(m4t);

        // delete
        mk.mark(4);
        timeTrackerConnection.deletePlayerData(this.getPlayerUUID(0));
        m1t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(0));
        Assertions.assertNull(m1t);

        // insert 2
        mk.mark(5);
        var m4 = this.createTimeTrackerDbModel(3, timestamp + 23000, 345 * 86400 + 123);
        var m5 = this.createTimeTrackerDbModel(4, timestamp + 24000);
        timeTrackerConnection.insertPlayer(m4);
        timeTrackerConnection.insertPlayerIfNotPresent(m5.playerUniqueId, timestamp + 24000);
        mk.mark(6);
        timeTrackerConnection.insertPlayerIfNotPresent(m4.playerUniqueId, timestamp + 36000);

        // update
        mk.mark(7);
        m2.addDailyTime(100089);
        timeTrackerConnection.updateDbModel(m2);

        mk.mark(8);
        m3.setLastSeen(m3.getLastSeen() + 1000);
        m5.addMonthlyTime(114514);
        timeTrackerConnection.updateDbModel(m3);
        timeTrackerConnection.updateDbModel(m5);

        // query 2
        mk.mark(9);
        m2t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(1));
        Assertions.assertEquals(m2, m2t);
        m3t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(2));
        Assertions.assertEquals(m3, m3t);
        m4t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(3));
        Assertions.assertEquals(m4, m4t);
        var m5t = timeTrackerConnection.getPlayerTimeTracker(this.getPlayerUUID(4));
        Assertions.assertEquals(m5, m5t);
    }


    static class Mask {

        private final IBatchOperate batchOperate;
        private final int maskStart;
        private final int maskEnd;
        private final Logger logger = LoggerFactory.getLogger(Mask.class);

        public Mask(IBatchOperate batchOperate, int mask, int base) {
            this.batchOperate = batchOperate;
            this.maskStart = (mask / base) % base;
            this.maskEnd = mask % base;
        }

        public void mark(int mask) {
            if (mask == this.maskStart) {
                logger.info("begin batch mode (mask={})", mask);
                this.batchOperate.beginBatchMode();
            }
            if (mask == this.maskEnd) {
                logger.info("end batch mode (mask={})", mask);
                this.batchOperate.endBatchMode();
            }
        }
    }
}
