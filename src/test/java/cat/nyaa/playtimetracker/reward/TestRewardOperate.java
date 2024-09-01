package cat.nyaa.playtimetracker.reward;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import cat.nyaa.playtimetracker.DemoPTT;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class TestRewardOperate {

    private ServerMock server;
    private DemoPTT plugin;
    private ObjectArrayList<Component> outputMessages;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        PluginDescriptionFile description = new PluginDescriptionFile("PlayTimeTracker", "1.0.0", DemoPTT.class.getName());
        plugin = MockBukkit.loadWith(DemoPTT.class, description, new Object[0]);
        outputMessages = new ObjectArrayList<>();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testEcoReward1() {
        server.setPlayers(2);
        List<String> outputError = new ObjectArrayList<>();
        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.1;
        Assertions.assertTrue(ecoRewardData.validate(outputError));

        EcoReward ecoReward = new EcoReward(ecoRewardData);

        Player player = server.getPlayer(0);
        long completedTime = System.currentTimeMillis();

        Assertions.assertTrue(ecoReward.prepare("reward1", completedTime, player, plugin));

        Assertions.assertEquals(500, ecoReward.getAmount());
        Assertions.assertEquals(DemoPTT.FakeEcore.SYSTEM_BALANCE - 500, plugin.getEconomyCore().getSystemBalance());

        Assertions.assertTrue(ecoReward.distribute(player, plugin, outputMessages));
        Assertions.assertEquals(500, plugin.getEconomyCore().getPlayerBalance(player.getUniqueId()));
    }

    @Test
    void testEcoReward2() {
        server.setPlayers(2);
        List<String> outputError = new ObjectArrayList<>();
        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 2000;
        ecoRewardData.ratio = 0.15;
        Assertions.assertTrue(ecoRewardData.validate(outputError));

        EcoReward ecoReward = new EcoReward(ecoRewardData);

        Player player = server.getPlayer(0);
        long completedTime = System.currentTimeMillis();

        Assertions.assertTrue(ecoReward.prepare("reward1", completedTime, player, plugin));

        Assertions.assertEquals(1500, ecoReward.getAmount());
        Assertions.assertEquals(DemoPTT.FakeEcore.SYSTEM_BALANCE - 1500, plugin.getEconomyCore().getSystemBalance());

        Assertions.assertFalse(ecoReward.distribute(player, plugin, outputMessages));
        Assertions.assertEquals(DemoPTT.FakeEcore.SYSTEM_BALANCE, plugin.getEconomyCore().getSystemBalance());
    }

    @Test
    void testEcoReward3() {
        server.setPlayers(3);

        Player player0 = server.getPlayer(1);
        Player player1 = server.getPlayer(2);
        plugin.getEconomyCore().setPlayerBalance(player0.getUniqueId(), 100);
        plugin.getEconomyCore().setPlayerBalance(player1.getUniqueId(), 200);
        List<String> outputError = new ObjectArrayList<>();
        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.1;
        ecoRewardData.vault = player0.getUniqueId().toString();
        ecoRewardData.refVault = player1.getUniqueId().toString();
        Assertions.assertTrue(ecoRewardData.validate(outputError));

        EcoReward ecoReward = new EcoReward(ecoRewardData);

        Player player = server.getPlayer(0);
        long completedTime = System.currentTimeMillis();

        Assertions.assertTrue(ecoReward.prepare("reward1", completedTime, player, plugin));

        Assertions.assertEquals(20, ecoReward.getAmount());
        Assertions.assertEquals(100 - 20, plugin.getEconomyCore().getPlayerBalance(player0.getUniqueId()));

        Assertions.assertTrue(ecoReward.distribute(player, plugin, outputMessages));
        Assertions.assertEquals(20, plugin.getEconomyCore().getPlayerBalance(player.getUniqueId()));
    }

    @Test
    void testEcoReward4() {
        server.setPlayers(2);
        List<String> outputError = new ObjectArrayList<>();
        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.type = EcoRewardData.RewardType.ADD;
        ecoRewardData.amount = 45;
        Assertions.assertTrue(ecoRewardData.validate(outputError));

        EcoReward ecoReward = new EcoReward(ecoRewardData);

        Player player = server.getPlayer(0);
        long completedTime = System.currentTimeMillis();

        Assertions.assertTrue(ecoReward.prepare("reward1", completedTime, player, plugin));

        Assertions.assertEquals(45, ecoReward.getAmount());
        Assertions.assertEquals(DemoPTT.FakeEcore.SYSTEM_BALANCE, plugin.getEconomyCore().getSystemBalance());

        Assertions.assertTrue(ecoReward.distribute(player, plugin, outputMessages));
        Assertions.assertEquals(45, plugin.getEconomyCore().getPlayerBalance(player.getUniqueId()));
    }
}


