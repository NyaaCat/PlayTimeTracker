package cat.nyaa.playtimetracker.db;

import cat.nyaa.playtimetracker.db.tables.CompletedMissionTable;
import cat.nyaa.playtimetracker.db.tables.RewardsTable;
import cat.nyaa.playtimetracker.db.tables.TimeTrackerTable;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestDatabase extends AbstractTestDatabase {

    @BeforeEach
    public void setUp() {
        this.setUp0();
    }

    @AfterEach
    public void tearDown() {
        this.tearDown0();
    }

    @Test
    public void testRewardsTable() {
        RewardsTable rewardsTable = new RewardsTable(this.ds);
        rewardsTable.tryCreateTable(this.plugin);
        this.server.setPlayers(3);
        long timestamp = System.currentTimeMillis();

        // insert 1
        var r1p1m1 = this.createRewardDbModel(0, "mission1", timestamp);
        rewardsTable.insertReward(r1p1m1);

        var r2p2m1 = this.createRewardDbModel(1, "mission1", timestamp + 1000);
        rewardsTable.insertReward(r2p2m1);

        var r3p1m2 = this.createRewardDbModel(0, "mission2", timestamp + 2000);
        rewardsTable.insertReward(r3p1m2);

        // query 1
        var list1p1 = rewardsTable.selectRewards(this.getPlayerUUID(0), null, false);
        Assertions.assertEquals(2, list1p1.size());
        Assertions.assertEquals(r1p1m1, list1p1.get(0));
        Assertions.assertEquals(r3p1m2, list1p1.get(1));

        var count1p1 = rewardsTable.selectRewardsCount(this.getPlayerUUID(0));
        Assertions.assertEquals(1, count1p1.get("mission1"));
        Assertions.assertEquals(1, count1p1.get("mission2"));

        var list1p2m1 = rewardsTable.selectRewards(this.getPlayerUUID(1), "mission1", false);
        Assertions.assertEquals(1, list1p2m1.size());
        Assertions.assertEquals(r2p2m1, list1p2m1.get(0));

        var count1p2m1 = rewardsTable.selectRewardsCount(this.getPlayerUUID(1), "mission1");
        Assertions.assertEquals(1, count1p2m1);

        var list1p2m2 = rewardsTable.selectRewards(this.getPlayerUUID(1), "mission2", false);
        Assertions.assertEquals(0, list1p2m2.size());

        var count1p2m2 = rewardsTable.selectRewardsCount(this.getPlayerUUID(1), "mission2");
        Assertions.assertEquals(0, count1p2m2);

        // insert 2
        var r4p2m1 = this.createRewardDbModel(1, "mission1", timestamp + 3000);
        var r5p2m1 = this.createRewardDbModel(1, "mission1", timestamp + 4000);
        var r6p1m2 = this.createRewardDbModel(0, "mission2", timestamp + 5000);
        var r7p2m2 = this.createRewardDbModel(1, "mission2", timestamp + 6000);
        rewardsTable.insertRewardBatch(List.of(r4p2m1, r5p2m1, r6p1m2, r7p2m2));

        // query 2
        var list2p2 = rewardsTable.selectRewards(this.getPlayerUUID(1), null, false);
        Assertions.assertEquals(4, list2p2.size());

        var list2p1m2 = rewardsTable.selectRewards(this.getPlayerUUID(0), "mission2", false);
        Assertions.assertEquals(2, list2p1m2.size());

        // delete 1
        rewardsTable.deleteReward(list1p1.get(0).id);

        // query 3
        var list3p1 = rewardsTable.selectRewards(this.getPlayerUUID(0), "mission1", false);
        Assertions.assertEquals(0, list3p1.size());

        // delete 2
        IntArrayList ids = new IntArrayList(2);
        for (var model : list2p2) {
            if (model.rewardName.equals("mission1")) {
                ids.add(model.id);
            }
        }
        rewardsTable.deleteRewardBatch(ids);

        // query 4
        var count4p2 = rewardsTable.selectRewardsCount(this.getPlayerUUID(1));
        Assertions.assertEquals(1, count4p2.get("mission2"));
        Assertions.assertNull(count4p2.get("mission1"));

        // query 5
        var list5p1 = rewardsTable.selectRewards(this.getPlayerUUID(0), null, false);
        Assertions.assertEquals(2, list5p1.size());

        // invalid reward
        var r8p1m1 = this.createInvalidRewardDbModel(0, "mission1", timestamp + 7000);
        rewardsTable.insertReward(r8p1m1);

        // query 6
        var list6p1 = rewardsTable.selectRewards(this.getPlayerUUID(0), null, false);
        var count6p1 = rewardsTable.selectRewardsCount(this.getPlayerUUID(0));
        Assertions.assertArrayEquals(list6p1.toArray(), list5p1.toArray());
        Assertions.assertEquals(count6p1.get("mission1"), 1);

        // query 7
        var list7p1 = rewardsTable.selectRewards(this.getPlayerUUID(0), null, true);
        var count7p1 = rewardsTable.selectRewardsCount(this.getPlayerUUID(0));
        Assertions.assertArrayEquals(list7p1.toArray(), list5p1.toArray());
        Assertions.assertNull(count7p1.get("mission1"));
    }

    @Test
    public void testCompletedMissionTable() {
        CompletedMissionTable completedMissionTable = new CompletedMissionTable(ds);
        completedMissionTable.tryCreateTable(plugin);
        server.setPlayers(3);
        long completedTime = System.currentTimeMillis();

        // insert 1
        var c1p1m1 = this.createCompletedMissionDbModel(0, "reward1", completedTime);
        completedMissionTable.insert(c1p1m1);

        var c2p2m2 = this.createCompletedMissionDbModel(1, "reward2", completedTime + 2000);
        completedMissionTable.insert(c2p2m2);

        var c3p1m2 = this.createCompletedMissionDbModel(0, "reward2", completedTime + 1000);
        completedMissionTable.insert(c3p1m2);

        // query 1
        var list1p1 = completedMissionTable.select(this.getPlayerUUID(0), null);
        Assertions.assertEquals(2, list1p1.size());
        Assertions.assertEquals(c1p1m1, list1p1.get(0));
        Assertions.assertEquals(c3p1m2, list1p1.get(1));

        var list1p2m2 = completedMissionTable.select(this.getPlayerUUID(1), "reward2");
        Assertions.assertEquals(1, list1p2m2.size());
        Assertions.assertEquals(c2p2m2, list1p2m2.get(0));

        var list1p2m1 = completedMissionTable.select(this.getPlayerUUID(1), "reward1");
        Assertions.assertEquals(0, list1p2m1.size());

        // update
        c1p1m1.setLastCompletedTime(c1p1m1.getLastCompletedTime() + 1000000);
        completedMissionTable.updatePlayer(c1p1m1, list1p1.get(0).id);

        // query 2
        var list2p1 = completedMissionTable.select(this.getPlayerUUID(0), "reward1");
        Assertions.assertEquals(1, list2p1.size());
        Assertions.assertEquals(c1p1m1, list2p1.get(0));

        // delete
        completedMissionTable.delete(this.getPlayerUUID(0), "reward2");

        // query 3
        var list3p1 = completedMissionTable.select(this.getPlayerUUID(0), null);
        Assertions.assertEquals(1, list3p1.size());
        Assertions.assertEquals("reward1", list3p1.get(0).missionName);
    }

    @Test
    public void testTimeTrackerTable() {
        TimeTrackerTable timeTrackerTable = new TimeTrackerTable(ds);
        timeTrackerTable.tryCreateTable(plugin);
        server.setPlayers(5);
        long timestamp = System.currentTimeMillis();

        // insert 1
        var m1 = this.createTimeTrackerDbModel(0, timestamp, 114 * 86400 + 976);
        timeTrackerTable.insert(m1);

        var m2 = this.createTimeTrackerDbModel(1, timestamp + 1000, 234 * 86400 + 543);
        var m3 = this.createTimeTrackerDbModel(2, timestamp + 2000);
        timeTrackerTable.insertBatch(List.of(m2, m3));

        // query 1
        var m1t = timeTrackerTable.selectPlayer(this.getPlayerUUID(0));
        Assertions.assertEquals(m1, m1t);
        var m2t = timeTrackerTable.selectPlayer(this.getPlayerUUID(1));
        Assertions.assertEquals(m2, m2t);
        var m3t = timeTrackerTable.selectPlayer(this.getPlayerUUID(2));
        Assertions.assertEquals(m3, m3t);
        var m4t = timeTrackerTable.selectPlayer(this.getPlayerUUID(3));
        Assertions.assertNull(m4t);

        // delete
        timeTrackerTable.deletePlayer(this.getPlayerUUID(0));
        m1t = timeTrackerTable.selectPlayer(this.getPlayerUUID(0));
        Assertions.assertNull(m1t);

        // insert 2
        var m4 = this.createTimeTrackerDbModel(3, timestamp + 23000, 345 * 86400 + 123);
        var m5 = this.createTimeTrackerDbModel(4, timestamp + 24000);
        timeTrackerTable.insertBatch(List.of(m4, m5));

        // update
        m2.addDailyTime(100089);
        timeTrackerTable.update(m2);

        m3.setLastSeen(m3.getLastSeen() + 1000);
        m5.addMonthlyTime(114514);
        timeTrackerTable.updateBatch(List.of(m3, m5));

        // query 2
        m2t = timeTrackerTable.selectPlayer(this.getPlayerUUID(1));
        Assertions.assertEquals(m2, m2t);
        m3t = timeTrackerTable.selectPlayer(this.getPlayerUUID(2));
        Assertions.assertEquals(m3, m3t);
        m4t = timeTrackerTable.selectPlayer(this.getPlayerUUID(3));
        Assertions.assertEquals(m4, m4t);
        var m5t = timeTrackerTable.selectPlayer(this.getPlayerUUID(4));
        Assertions.assertEquals(m5, m5t);
    }
}
