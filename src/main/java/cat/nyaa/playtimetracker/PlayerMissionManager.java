package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import cat.nyaa.playtimetracker.config.data.ISerializableExt;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.reward.EcoReward;
import cat.nyaa.playtimetracker.reward.IReward;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import cat.nyaa.playtimetracker.utils.TaskUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import com.udojava.evalex.Expression;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.ess3.api.IEssentials;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerMissionManager {
    @Nullable
    private static PlayerMissionManager instance;
    private final PlayTimeTracker plugin;
    //private final List<AwaitingReward> awaitingRewardList = new ArrayList<>();
    private final CompletedMissionConnection completedMissionConnection;
    private final RewardsConnection rewardsConnection;
    private final PTTConfiguration pttConfiguration;
    private final TimeRecordManager timeRecordManager;
    private final ConcurrentHashMap<UUID, String> playerAcquiringRewards;
    private int tickNum;

    public PlayerMissionManager(PlayTimeTracker playTimeTracker, PTTConfiguration pttConfiguration, TimeRecordManager timeRecordManager, CompletedMissionConnection completedMissionConnection, RewardsConnection rewardsConnection) {
        instance = this;
        this.plugin = playTimeTracker;
        this.pttConfiguration = pttConfiguration;
        this.completedMissionConnection = completedMissionConnection;
        this.rewardsConnection = rewardsConnection;
        this.timeRecordManager = timeRecordManager;
        this.playerAcquiringRewards = new ConcurrentHashMap<>();
        this.tickNum = 0;
    }

    @Nullable
    public static PlayerMissionManager getInstance() {
        return instance;
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }

//    public void getMissionReward(Player player, String mission) {
//        Map<String, MissionData> missionDataMap = getMissionDataMap();
//        if (!missionDataMap.containsKey(mission)) return;
//        I18n.send(player, "message.mission.get_reward", mission);
//        MissionData missionData = missionDataMap.get(mission);
//        //item
//
//        if (missionData.rewardItemBase64List != null && !missionData.rewardItemBase64List.isEmpty()) {
//            List<ItemStack> items = new ArrayList<>();
//            missionData.rewardItemBase64List.forEach(s -> {
//                try {
//                    if (s != null && !s.isEmpty())
//                        items.addAll(ItemStackUtils.itemsFromBase64(s));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//            for (ItemStack item : items) {
//                if (item == null || item.getType().isAir()) continue;
//                if (InventoryUtils.hasEnoughSpace(player.getInventory(), item)) {
//                    InventoryUtils.addItem(player, item);
//                } else {
//                    player.getWorld().dropItem(player.getLocation(), item);
//                }
//            }
//        }
//
//        //command
//        if (missionData.rewardCommandList != null)
//            for (String command : missionData.rewardCommandList) {
//                if (command != null && !command.isEmpty()) {
//                    try {
//                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPIUtils.setPlaceholders(player, command));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//    }

//    public boolean completeMission(@NotNull Player player, String mission) {
//        if (completeMissionNoReward(player.getUniqueId(), mission)) {
//            getMissionReward(player, mission);
//            return true;
//        }
//        return false;
//    }

//    public boolean completeMissionNoReward(UUID playerId, String mission) {
//        boolean write2db = removeAwaitingReward(playerId, mission);
//
//        if (write2db) {
//            completedMissionConnection.WriteMissionCompleted(playerId, mission, TimeUtils.getUnixTimeStampNow());
//        }
//        return write2db;
//    }

//    public Map<String, MissionData> getMissionDataMap() {
//        Map<String, MissionData> missionDataMap = new HashMap<>();
//        getMissionDataList().forEach(missionData -> missionDataMap.put(missionData.missionName, missionData));
//        return missionDataMap;
//    }

//    public void checkAwaitingRewardList() {
//        Map<String, MissionData> missionDataMap = getMissionDataMap();
//        new ArrayList<>(awaitingRewardList).forEach(
//                awaitingReward -> {
//                    if (!missionDataMap.containsKey(awaitingReward.mission)) return;
//                    if (missionDataMap.get(awaitingReward.mission).timeoutMS < 0) return;
//                    if ((TimeUtils.getUnixTimeStampNow() - awaitingReward.time) >= missionDataMap.get(awaitingReward.mission).timeoutMS) {
//                        completeMissionNoReward(awaitingReward.playerId, awaitingReward.mission);
//                    }
//                }
//        );
//    }


    public void checkPlayerMission(@NotNull Player player) {
        if (!player.isOnline()) return;
        TimeTrackerDbModel trackerDbModel = timeRecordManager.getPlayerTimeTrackerDbModel(player);
        if (trackerDbModel == null) return;
        //this.checkAndResetPlayerCompletedMission(player);
        getMissionDataMap().forEach(
                (missionName, missionData) -> {
                    CompletedMissionDbModel completedMissionDbModel = this.getPlayerCompletedMission(player.getUniqueId(), missionName);
                    if (completedMissionDbModel != null) {
                        return;
                    }
                    //awaitingReward
                    // TODO
//                    AwaitingReward awaitingReward = getAwaitingReward(player, missionName);
//                    if (awaitingReward != null) {
//                        return; // after checkAwaitingRewardList
//                    }
                    //group
                    if (missionData.group != null && !missionData.group.isEmpty()) {
                        if (getPlugin().getEssentialsPlugin() == null) return;
                        boolean inGroup = false;
                        for (String s : missionData.group) {
                            if (((IEssentials) getPlugin().getEssentialsPlugin()).getUser(player).inGroup(s)) {
                                inGroup = true;
                                break;
                            }
                        }
                        if (!inGroup) return;
                    }
                    //
                    if (missionData.expression == null || missionData.expression.isEmpty()) return;
                    BigDecimal lastSeen = new BigDecimal(trackerDbModel.getLastSeen());
                    BigDecimal dailyTime = new BigDecimal(trackerDbModel.getDailyTime());
                    BigDecimal weeklyTime = new BigDecimal(trackerDbModel.getWeeklyTime());
                    BigDecimal monthlyTime = new BigDecimal(trackerDbModel.getMonthlyTime());
                    BigDecimal totalTime = new BigDecimal(trackerDbModel.getTotalTime());
                    BigDecimal result;
                    try {
                        result = new com.udojava.evalex.Expression(missionData.expression)
                                .with("lastSeen", lastSeen)
                                .with("dailyTime", dailyTime)
                                .with("weeklyTime", weeklyTime)
                                .with("monthlyTime", monthlyTime)
                                .with("totalTime", totalTime)
                                .eval();
                    } catch (Expression.ExpressionException e) {
                        e.printStackTrace();
                        return;
                    }
                    if (result.doubleValue() <= 0) {
                        return;
                    }

                    this.putAwaitingReward(player, missionName, missionData.getSortedRewardList(), missionData.notify);
//                    if (missionData.autoGive) {
//                        completeMission(player, missionName);
//                    }
                }
        );
    }

//    public Set<String> getAwaitingMissionNameSet(Player player) {
//        return awaitingRewardList.stream()
//                .filter(awaitingReward -> awaitingReward.playerId == player.getUniqueId())
//                .map(awaitingReward -> awaitingReward.mission).collect(Collectors.toSet());
//    }

//    @Nullable
//    private AwaitingReward getAwaitingReward(Player player, String missionName) {
//        for (AwaitingReward awaitingReward : awaitingRewardList) {
//            if (awaitingReward.playerId == player.getUniqueId() && awaitingReward.mission.equals(missionName)) {
//                return awaitingReward;
//            }
//        }
//        return null;
//    }

//    private boolean removeAwaitingReward(UUID playerId, String missionName) {
//        AtomicBoolean result = new AtomicBoolean(false);
//        awaitingRewardList.removeIf(awaitingReward -> {
//                    if (awaitingReward.playerId == playerId
//                            && awaitingReward.mission.equals(missionName)) {
//                        result.set(true);
//                        return true;
//                    }
//                    return false;
//                }
//        );
//        return result.get();
//    }

    public static IReward createReward(ISerializableExt data) {
        if(data instanceof EcoRewardData ecoRewardData) {
            return new EcoReward(ecoRewardData);
        }
        return null;
    }

    private void putAwaitingReward(@NotNull Player player, String missionName, List<ISerializableExt> rewardDataList, boolean notify) {
        // TODO
        //removeAwaitingReward(player.getUniqueId(), missionName);
        // TODO
        //awaitingRewardList.add(new AwaitingReward(player.getUniqueId(), missionName, TimeUtils.getUnixTimeStampNow(), notify));
        // TODO: Add to completed mission
        final long timestamp = TimeUtils.getUnixTimeStampNow();
        // TODO: async & cache (completedMissionConnection should run in one thread; another in checkPlayerMission)
        this.completedMissionConnection.writeMissionCompleted(player.getUniqueId(), missionName, timestamp);

        List<IReward> rewardList = new ArrayList<>(rewardDataList.size());
        for (ISerializableExt rewardData : rewardDataList) {
            IReward reward = createReward(rewardData);
            if(reward != null) {
                if(reward.prepare(missionName, timestamp, player, this.plugin)) {
                    rewardList.add(reward);
                } else {
                    this.plugin.getSLF4JLogger().error("Failed to prepare reward {} for {} {}", reward.getClass(), player.getUniqueId(), missionName);
                }
            } else {
                this.plugin.getSLF4JLogger().error("Unknown reward data type {} for {} {}", rewardData.getClass(), player.getUniqueId(), missionName);
            }
        }
        final var scheduler = this.plugin.getServer().getScheduler();
        scheduler.runTaskAsynchronously(
                this.plugin,
                () -> {
                    var rewardDbModelList = rewardList.stream()
                            .map(reward -> new RewardDbModel(0, timestamp, player.getUniqueId(), missionName, reward))
                            .toList();
                    this.rewardsConnection.getRewardsTable().insertRewardBatch(rewardDbModelList);
                    if(notify) {
                        scheduler.runTask(this.plugin, () -> notifyAcquire(player, missionName));
                    }
                }
        );
    }

    public Map<String, MissionData> getMissionDataMap() {
        return pttConfiguration.missionConfig.missions;
    }

    public void missionCheckTick() {
        this.tickNum++;
        //checkAwaitingRewardList();
        Bukkit.getOnlinePlayers().forEach(player ->
                TaskUtils.mod64TickToRun(this.tickNum, player.getUniqueId(), () -> checkPlayerMission(player))
        );
    }

    public void notifyAcquire(Player player, String mission) {
        if(!player.isOnline()) return;
        String command = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.command", mission));
        String msg = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.msg", mission));
        BaseComponent[] commandComponent = new ComponentBuilder()
                .append(msg)
                .append(command)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .create();
        player.spigot().sendMessage(new TextComponent(commandComponent));
    }

    public void notifyAcquire() {
        // TODO
//        awaitingRewardList.forEach(awaitingReward -> {
//            if (awaitingReward.isNotify) {
//                Player player = Bukkit.getPlayer(awaitingReward.playerId);
//                if (player != null) {
//                    String command = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.command", awaitingReward.mission));
//                    String msg = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.msg", awaitingReward.mission));
//                    BaseComponent[] commandComponent = new ComponentBuilder()
//                            .append(msg)
//                            .append(command)
//                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
//                            .create();
//                    player.spigot().sendMessage(new TextComponent(commandComponent));
//                }
//            }
//        });
    }

    public void executeAcquire(Player player, String mission) {
        final UUID playerId = player.getUniqueId();
        final String oldMission = playerAcquiringRewards.putIfAbsent(playerId, mission);
        if(oldMission != null) {
            // database in operate; lock
            I18n.send(player, "command.acquire.err");
            return;
        }
        final var scheduler = plugin.getServer().getScheduler();
        final String finalMission = "all".equals(mission) ? null : mission;
        scheduler.runTaskAsynchronously(this.plugin, () -> {
            final var rewardList = this.rewardsConnection.getRewardsTable().selectRewards(playerId, finalMission);
            if(rewardList.isEmpty()) {
                scheduler.runTask(this.plugin, () -> {
                    I18n.send(player, "command.acquire.empty", mission);
                    this.playerAcquiringRewards.remove(playerId);
                });
            } else {
                this.plugin.getSLF4JLogger().info("Player {} is acquiring {} rewards", playerId, rewardList.size());
                scheduler.runTask(this.plugin, () -> {
                    IntArrayList rewardIdList = new IntArrayList(rewardList.size());
                    for (RewardDbModel reward : rewardList) {
                        Boolean distributeRet = reward.getReward().distribute(player, this.plugin);
                        if(distributeRet == null) {
                            this.plugin.getSLF4JLogger().warn("Player {} blocked acquire reward {}", playerId, reward.getId());
                            break;
                        } else {
                            if (distributeRet) {
                                rewardIdList.add(reward.getId());
                                I18n.send(player, "command.acquire.success", reward.getRewardName());
                            } else {
                                I18n.send(player, "command.acquire.failed", reward.getRewardName());
                            }
                        }
                    }
                    if(!rewardIdList.isEmpty()) {
                        scheduler.runTaskAsynchronously(this.plugin, () -> {
                            this.rewardsConnection.getRewardsTable().deleteRewardBatch(rewardIdList);
                            this.playerAcquiringRewards.remove(playerId);
                            this.plugin.getSLF4JLogger().info("Player {} has acquired {} rewards", playerId, rewardIdList.size());
                        });
                    }
                });
            }
        });
    }

    public void showPlayerRewards(Player player, @Nullable String mission, boolean notifyAcquire) {
        final UUID playerId = player.getUniqueId();
        final var scheduler = plugin.getServer().getScheduler();
        final String finalMission = "all".equals(mission) ? null : mission;
        scheduler.runTaskAsynchronously(this.plugin, () -> {
            final var rewardListCount = this.rewardsConnection.getRewardsTable().selectRewardsCount(playerId, finalMission);
            scheduler.runTaskLater(this.plugin, () -> {
                if(rewardListCount == 0) {
                    if(notifyAcquire) {

                    } else {
                        if(finalMission == null) {
                            I18n.send(player, "command.listrewards.empty_all");
                        } else {
                            I18n.send(player, "command.listrewards.empty", finalMission);
                        }
                    }
                } else {
                    if(notifyAcquire) {
                        String command = PlaceholderAPIUtils.setPlaceholders(
                                player,
                                I18n.format(
                                        "message.mission.notify.command",
                                        finalMission == null ? "all" : finalMission
                                )
                        );
                        String msg = PlaceholderAPIUtils.setPlaceholders(
                                player,
                                finalMission == null ?
                                        I18n.format("command.listrewards.show_all", rewardListCount) :
                                        I18n.format("command.listrewards.show", rewardListCount, finalMission)
                        );
                        BaseComponent[] commandComponent = new ComponentBuilder()
                                .append(msg)
                                .append(command)
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                                .create();
                        player.spigot().sendMessage(new TextComponent(commandComponent));
                    } else {
                        if(finalMission == null) {
                            I18n.send(player, "command.listrewards.show_all", rewardListCount);
                        } else {
                            I18n.send(player, "command.listrewards.show", rewardListCount, finalMission);
                        }
                    }
                }
            }, notifyAcquire ? 10 : 1);
        });
    }

    public void onDailyReset(UUID playerId) {
        resetMission(true, false, false, playerId);
    }

    public void onWeeklyReset(UUID playerId) {
        resetMission(false, true, false, playerId);
    }

    public void onMonthlyReset(UUID playerId) {
        resetMission(false, false, true, playerId);
    }

    private void resetMission(boolean daily, boolean weekly, boolean monthly, UUID playerId) {
        if (!daily && !weekly && !monthly) return;
        getMissionDataMap().forEach(
                (missionName, missionData) -> {
                    if ((missionData.resetDaily && daily) || (missionData.resetWeekly && weekly) || (missionData.resetMonthly && monthly)) {
                        resetPlayerMissionData(playerId, missionName);
                    }
                }
        );
    }

    public void resetPlayerMissionData(UUID playerId) {
        // TODO
        //this.awaitingRewardList.removeIf(awaitingReward -> awaitingReward.playerId == playerId);
        this.completedMissionConnection.resetPlayerCompletedMission(playerId);
    }

    public void resetPlayerMissionData(UUID playerId, String missionName) {
        // TODO
        //this.awaitingRewardList.removeIf(awaitingReward -> (awaitingReward.playerId == playerId && missionName.equals(awaitingReward.mission)));
        this.completedMissionConnection.resetPlayerCompletedMission(missionName, playerId);
    }

    public List<CompletedMissionDbModel> getPlayerCompletedMissionList(UUID playerId) {
        return this.completedMissionConnection.getPlayerCompletedMissionList(playerId);
    }

    public CompletedMissionDbModel getPlayerCompletedMission(UUID playerId, String missionName) {
        return this.completedMissionConnection.getPlayerCompletedMission(playerId, missionName);
    }

    public record AwaitingReward(UUID playerId, String mission, long time, boolean isNotify) {
    }
}
