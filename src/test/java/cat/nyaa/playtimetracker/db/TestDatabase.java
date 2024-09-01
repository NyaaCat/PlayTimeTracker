package cat.nyaa.playtimetracker.db;

import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.MockBukkit;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.db.tables.CompletedMissionTable;
import cat.nyaa.playtimetracker.db.tables.RewardsTable;
import cat.nyaa.playtimetracker.db.tables.TimeTrackerTable;
import cat.nyaa.playtimetracker.reward.DemoReward;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class TestDatabase {

    private Logger logger = LoggerFactory.getLogger(TestDatabase.class);

    private ServerMock server;
    private MockPlugin plugin;
    private HikariDataSource ds;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();

        plugin = MockBukkit.createMockPlugin("PlayTimeTracker");

        File root = new File("build");
        if(!root.isDirectory()) {
            root = new File("tmp");
            root.mkdir();
        }
        File db = new File(root, "test.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + db.getAbsolutePath());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() {
        ds.close();
        MockBukkit.unmock();
    }

    @Test
    public void testRewardsTable() {
        RewardsTable rewardsTable = new RewardsTable(ds, logger);
        rewardsTable.tryCreateTable(plugin);

        server.setPlayers(3);

        Player player1 = server.getPlayer(1);
        String rewardName1 = "reward1";
        DemoReward reward1 = new DemoReward();
        long completedTime1 = System.currentTimeMillis();
        reward1.prepare(rewardName1, completedTime1, player1, plugin);
        RewardDbModel model1 = new RewardDbModel(0, completedTime1, player1.getUniqueId(), rewardName1, reward1);
        rewardsTable.insertReward(model1);

        String rewardName2 = "reward2";
        DemoReward reward2 = new DemoReward();
        long completedTime2 = completedTime1 + 1000;
        reward2.prepare(rewardName2, completedTime2, player1, plugin);
        RewardDbModel model2 = new RewardDbModel(0, completedTime2, player1.getUniqueId(), rewardName2, reward2);
        rewardsTable.insertReward(model2);

        Player player2 = server.getPlayer(2);
        DemoReward reward3 = new DemoReward();
        long completedTime3 = completedTime2 + 1000;
        reward3.prepare(rewardName1, completedTime3, player2, plugin);
        RewardDbModel model3 = new RewardDbModel(0, completedTime3, player2.getUniqueId(), rewardName1, reward3);
        rewardsTable.insertReward(model3);

        // test query
        var listP1 = rewardsTable.selectRewards(player1.getUniqueId(), null);
        var listP1c = rewardsTable.selectRewardsCount(player1.getUniqueId());
        Assertions.assertEquals(2, listP1c.values().intStream().sum());

        var listP2 = rewardsTable.selectRewards(player2.getUniqueId(), rewardName1);
        var listP2c = rewardsTable.selectRewardsCount(player2.getUniqueId(), rewardName1);
        Assertions.assertEquals(1, listP2.size());
        Assertions.assertEquals(1, listP2c);

        logger.info("add new records: {} {} {}", listP1.get(0).id, listP1.get(1).id, listP2.get(0).id);

        // delete
        rewardsTable.deleteReward(listP1.get(0).id);
        IntArrayList ids = new IntArrayList(2);
        ids.add(listP1.get(1).id);
        ids.add(listP2.get(0).id);
        rewardsTable.deleteRewardBatch(ids);

        var list01c = rewardsTable.selectRewardsCount(player1.getUniqueId());
        Assertions.assertEquals(0, list01c.values().intStream().sum());
        var list02c = rewardsTable.selectRewardsCount(player2.getUniqueId());
        Assertions.assertEquals(0, list02c.values().intStream().sum());
    }

    @Test
    void testCompletedMissionTable() {

        CompletedMissionTable completedMissionTable = new CompletedMissionTable(ds, logger);
        completedMissionTable.tryCreateTable(plugin);

        server.setPlayers(3);

        Player player1 = server.getPlayer(1);
        String rewardName1 = "reward1";
        long completedTime1 = System.currentTimeMillis();
        CompletedMissionDbModel mission1 = new CompletedMissionDbModel(0, player1.getUniqueId(), rewardName1, completedTime1);
        completedMissionTable.insert(mission1);

        String rewardName2 = "reward2";
        long completedTime2 = completedTime1 + 1000;
        CompletedMissionDbModel mission2 = new CompletedMissionDbModel(0, player1.getUniqueId(), rewardName2, completedTime2);
        completedMissionTable.insert(mission2);

        Player player2 = server.getPlayer(2);
        long completedTime3 = completedTime2 + 1000;
        CompletedMissionDbModel mission3 = new CompletedMissionDbModel(0, player2.getUniqueId(), rewardName2, completedTime3);
        completedMissionTable.insert(mission3);

        // test query
        var listP1 = completedMissionTable.select(player1.getUniqueId(), null);
        Assertions.assertEquals(2, listP1.size());

        var listP2 = completedMissionTable.select(player2.getUniqueId(), rewardName2);
        Assertions.assertEquals(1, listP2.size());

        logger.info("add new records: {} {} {}", listP1.get(0).id, listP1.get(1).id, listP2.get(0).id);

        // delete
        completedMissionTable.delete(player1.getUniqueId());

        var list01 = completedMissionTable.select(player1.getUniqueId(), null);
        Assertions.assertEquals(0, list01.size());

        var p2model = listP2.get(0);
        var id = p2model.id;
        p2model.setLastCompletedTime(completedTime1);
        completedMissionTable.updatePlayer(p2model, id);

        var list02 = completedMissionTable.select(player2.getUniqueId(), rewardName2);
        Assertions.assertEquals(1, list02.size());
        p2model = list02.get(0);
        Assertions.assertEquals(completedTime1, p2model.lastCompletedTime);

        completedMissionTable.delete(player2.getUniqueId(), rewardName2);
        var list03 = completedMissionTable.select(player2.getUniqueId(), null);
        Assertions.assertEquals(0, list03.size());
    }

    @Test
    void testTimeTrackerTable() {
        TimeTrackerTable timeTrackerTable = new TimeTrackerTable(ds, logger);
        timeTrackerTable.tryCreateTable(plugin);

        server.setPlayers(3);

        Player player1 = server.getPlayer(1);
        long time1 = System.currentTimeMillis();
        TimeTrackerDbModel model1 = new TimeTrackerDbModel(
                player1.getUniqueId(),
                time1, 1000, 2100, 3100, 4100);
        timeTrackerTable.insertPlayer(model1);

        Player player2 = server.getPlayer(2);
        long time2 = time1 + 10000;
        TimeTrackerDbModel model2 = new TimeTrackerDbModel(
                player2.getUniqueId(),
                time2, 1030, 2130, 3130, 4130);
        timeTrackerTable.insertPlayer(model2);

        Player player3 = server.getPlayer(0);
        long time3 = time2 + 10000;
        TimeTrackerDbModel model3 = new TimeTrackerDbModel(
                player3.getUniqueId(),
                time3, 1070, 2170, 3170, 4170);
        timeTrackerTable.insertPlayer(model3);

        // test query
        var modelP1 = timeTrackerTable.selectPlayer(player1.getUniqueId());
        Assertions.assertNotNull(modelP1);
        Assertions.assertEquals(time1, modelP1.lastSeen);

        var modelP2 = timeTrackerTable.selectPlayer(player2.getUniqueId());
        Assertions.assertNotNull(modelP2);
        Assertions.assertEquals(time2, modelP2.lastSeen);

        var modelP3 = timeTrackerTable.selectPlayer(player3.getUniqueId());
        Assertions.assertNotNull(modelP3);
        Assertions.assertEquals(time3, modelP3.lastSeen);

        logger.info("add new records: {} {} {}", modelP1.playerUniqueId, modelP2.playerUniqueId, modelP3.playerUniqueId);

        // update
        modelP1.totalTime += 100000;
        timeTrackerTable.update(modelP1, player1.getUniqueId());

        modelP2.totalTime += 200000;
        modelP3.totalTime += 300000;
        timeTrackerTable.updateBatch(List.of(modelP2, modelP3));

        var modelP1_1 = timeTrackerTable.selectPlayer(player1.getUniqueId());
        Assertions.assertNotNull(modelP1_1);
        Assertions.assertEquals(modelP1.totalTime, modelP1_1.totalTime);

        var modelP2_1 = timeTrackerTable.selectPlayer(player2.getUniqueId());
        Assertions.assertNotNull(modelP2_1);
        Assertions.assertEquals(modelP2.totalTime, modelP2_1.totalTime);

        var modelP3_1 = timeTrackerTable.selectPlayer(player3.getUniqueId());
        Assertions.assertNotNull(modelP3_1);
        Assertions.assertEquals(modelP3.totalTime, modelP3_1.totalTime);

        // delete
        timeTrackerTable.deletePlayer(player1.getUniqueId());
        timeTrackerTable.deletePlayer(player2.getUniqueId());
        timeTrackerTable.deletePlayer(player3.getUniqueId());

        var modelP1_2 = timeTrackerTable.selectPlayer(player1.getUniqueId());
        Assertions.assertNull(modelP1_2);
    }
}
