package cat.nyaa.playtimetracker.config;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import cat.nyaa.playtimetracker.condition.ICondition;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.workflow.LimitedTimeTrackerModel;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class TestConfig {

    private ServerMock server;
    private MockPlugin plugin;
    private File root;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("PlayTimeTracker");
        root = new File("build");
        if(!root.isDirectory()) {
            root = new File("tmp");
            root.mkdir();
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testMissionConfig() throws Exception {
        File cfg = new File(root, "mission.yml");

        YamlConfiguration configFile = new YamlConfiguration();

        MissionConfig missionConfig = new MissionConfig(plugin);

        missionConfig.serialize(configFile);

        var ctx = new ValidationContext();

        missionConfig.validate(ctx);

        configFile.save(cfg);

        configFile = YamlConfiguration.loadConfiguration(cfg);

        MissionConfig missionConfig2 = new MissionConfig(plugin);
        missionConfig2.deserialize(configFile);

        Assertions.assertEquals(missionConfig.missions.size(), missionConfig2.missions.size());
    }

    public static class ValidationContext implements MissionData.IConditionCompiler {

        @Override
        public ICondition<?> compile(String expression) throws Exception {
            return LimitedTimeTrackerModel.compileCondition(expression, 1000L);
        }
    }
}
