package cat.nyaa.playtimetracker.db;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.reward.DemoReward;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.util.UUID;

public abstract class AbstractTestDatabase {


    protected Logger logger;
    protected ServerMock server;
    protected MockPlugin plugin;
    protected HikariDataSource ds;


    protected void setUp0() {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("PlayTimeTracker");

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
        this.ds = new HikariDataSource(config);
    }

    protected void tearDown0() {
        this.ds.close();
        MockBukkit.unmock();
    }

    protected UUID getPlayerUUID(int index) {
        return this.server.getPlayer(index).getUniqueId();
    }

    protected RewardDbModel createRewardDbModel(int playerIndex, String missionName, long completedTime) {
        var player = this.server.getPlayer(playerIndex);
        TestReward reward = new TestReward();
        reward.prepare(missionName, completedTime, player, this.plugin);
        return new RewardDbModel(0, completedTime, player.getUniqueId(), missionName, reward);
    }

    protected RewardDbModel createInvalidRewardDbModel(int playerIndex, String missionName, long completedTime) {
        var player = this.server.getPlayer(playerIndex);
        TestReward reward = new TestReward(true);
        reward.prepare(missionName, completedTime, player, this.plugin);
        return new RewardDbModel(0, completedTime, player.getUniqueId(), missionName, reward);
    }

    protected CompletedMissionDbModel createCompletedMissionDbModel(int playerIndex, String missionName, long completedTime) {
        Player player = this.server.getPlayer(playerIndex);
        return new CompletedMissionDbModel(0, player.getUniqueId(), missionName, completedTime);
    }

    protected TimeTrackerDbModel createTimeTrackerDbModel(int playerIndex, long timestamp, long totalTime) {
        Player player = this.server.getPlayer(playerIndex);
        long dailyTime = (totalTime - 976) % 86400;
        long weeklyTime = (totalTime - 1463) % 604800;
        long monthlyTime = (totalTime - 45887) % 2592000;
        return new TimeTrackerDbModel(player.getUniqueId(), timestamp, dailyTime, weeklyTime, monthlyTime, totalTime, Math.max(0, timestamp - 86400));
    }

    protected TimeTrackerDbModel createTimeTrackerDbModel(int playerIndex, long timestamp) {
        Player player = this.server.getPlayer(playerIndex);
        return new TimeTrackerDbModel(player.getUniqueId(), timestamp, 0, 0, 0, 0, timestamp);
    }

    public static class TestReward extends DemoReward {

        private final boolean invalid;

        public TestReward() {
            super();
            this.invalid = false;
        }

        public TestReward(boolean invalid) {
            super();
            this.invalid = invalid;
        }

        @Override
        public void serialize(OutputStream outputStream) throws Exception {
            if (this.invalid) {
                outputStream.write(0x1);
            }
            super.serialize(outputStream);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TestReward) {
                return true;
            }
            return false;
        }
    }
}
