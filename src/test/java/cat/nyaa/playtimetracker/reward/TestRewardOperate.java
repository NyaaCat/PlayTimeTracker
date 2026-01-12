package cat.nyaa.playtimetracker.reward;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import cat.nyaa.playtimetracker.DemoPTT;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import it.unimi.dsi.fastutil.doubles.DoubleObjectImmutablePair;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

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

    void testEcoRewardTransferInner(EcoRewardData cfg, long dt, List<DoubleObjectPair<Boolean>> results, List<Player> players) throws Exception {
        var logger = plugin.getSLF4JLogger();

        var playerNum = results.size();
        List<String> outputError = new ObjectArrayList<>();
        cfg.validate(null);

        DoubleSupplier getSrcVault = null;
        if(cfg.isRefVaultSystemVault()) {
            getSrcVault = () -> plugin.getEconomyCore().getSystemBalance();
        } else {
            final var srcVault = cfg.getVaultAsUUID();
            getSrcVault = () -> plugin.getEconomyCore().getPlayerBalance(srcVault);
        }

        List<EcoReward> ecoRewards = new ArrayList<>();

        long completedTime = System.currentTimeMillis();
        final double original = getSrcVault.getAsDouble();
        double acc = 0;
        for(int i = 0; i < playerNum; i++, completedTime += dt) {
            Player player = players.get(i);
            EcoReward ecoReward = new EcoReward(cfg);
            Assertions.assertTrue(ecoReward.prepare("reward1", completedTime, player, plugin));
            double result = results.get(i).leftDouble();
            acc += result;
            Assertions.assertEquals(result, ecoReward.getAmount());
            Assertions.assertEquals(original - acc, getSrcVault.getAsDouble());
            ecoRewards.add(ecoReward);
            logger.info("player {} ecoReward: {}", i, ecoReward.getAmount());
        }

        for(int i = 0; i < playerNum; i++) {
            Player player = players.get(i);
            EcoReward ecoReward = ecoRewards.get(i);
            var result = results.get(i);
            Assertions.assertEquals(result.right(), ecoReward.distribute(player, plugin, outputMessages));
            var balance = plugin.getEconomyCore().getPlayerBalance(player.getUniqueId());
            Assertions.assertEquals(result.right() ? result.leftDouble() : 0, balance);

            if(!result.right()) {
                acc -= result.leftDouble();
            }
            Assertions.assertEquals(original - acc, getSrcVault.getAsDouble());
            logger.info("player {} balance: {}", i, balance);
        }
    }

    @Test
    void testEcoReward1() throws Exception {
        server.setPlayers(4);
        List<Player> players = server.getOnlinePlayers().stream().map((playerMock -> (Player)playerMock)).toList();

        ((DemoPTT.FakeEcore)plugin.getEconomyCore()).init();

        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.1;
        List<DoubleObjectPair<Boolean>> results = List.of(
                DoubleObjectImmutablePair.of(500, true)
        );

        testEcoRewardTransferInner(ecoRewardData, 0, results, players);

        ((DemoPTT.FakeEcore)plugin.getEconomyCore()).init();

        ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 2000; // will fail in distribute
        ecoRewardData.ratio = 0.15;
        results = List.of(
                DoubleObjectImmutablePair.of(1500, false)
        );

        testEcoRewardTransferInner(ecoRewardData, 0, results, players);

        Player playerSrc = players.get(2);
        Player playerRef = players.get(3);

        ((DemoPTT.FakeEcore)plugin.getEconomyCore()).init();
        plugin.getEconomyCore().setPlayerBalance(playerSrc.getUniqueId(), 100);
        plugin.getEconomyCore().setPlayerBalance(playerRef.getUniqueId(), 200);

        ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.1;
        ecoRewardData.vault = playerSrc.getUniqueId().toString();
        ecoRewardData.refVault = playerRef.getUniqueId().toString();
        results = List.of(
                DoubleObjectImmutablePair.of(20, true)
        );
        testEcoRewardTransferInner(ecoRewardData, 0, results, players);
    }

    @Test
    void testEcoReward2() throws Exception {

        server.setPlayers(4);
        List<Player> players = server.getOnlinePlayers().stream().map((playerMock -> (Player)playerMock)).toList();

        ((DemoPTT.FakeEcore)plugin.getEconomyCore()).init();

        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.01;
        List<DoubleObjectPair<Boolean>> results = List.of(
                DoubleObjectImmutablePair.of(100, true),
                DoubleObjectImmutablePair.of(100, true),
                DoubleObjectImmutablePair.of(100, true)
        );

        testEcoRewardTransferInner(ecoRewardData, 5, results, players);


        ((DemoPTT.FakeEcore)plugin.getEconomyCore()).init();
        plugin.getPttConfiguration().missionConfig.syncRefCacheTime = 0;
        ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.01;
        results = List.of(
                DoubleObjectImmutablePair.of(100, true),
                DoubleObjectImmutablePair.of(99, true),
                DoubleObjectImmutablePair.of(98.01, true)
        );
        testEcoRewardTransferInner(ecoRewardData, 5, results, players);

        ((DemoPTT.FakeEcore)plugin.getEconomyCore()).init();
        plugin.getPttConfiguration().missionConfig.syncRefCacheTime = 1000;
        ecoRewardData = new EcoRewardData();
        ecoRewardData.min = 10;
        ecoRewardData.max = 500;
        ecoRewardData.ratio = 0.01;
        results = List.of(
                DoubleObjectImmutablePair.of(100, true),
                DoubleObjectImmutablePair.of(100, true),
                DoubleObjectImmutablePair.of(98, true)
        );
        testEcoRewardTransferInner(ecoRewardData, 800, results, players);
    }

    @Test
    void testEcoReward4() throws Exception {
        server.setPlayers(2);
        List<String> outputError = new ObjectArrayList<>();
        EcoRewardData ecoRewardData = new EcoRewardData();
        ecoRewardData.type = EcoRewardData.RewardType.ADD;
        ecoRewardData.amount = 45;
        ecoRewardData.validate(null);

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


