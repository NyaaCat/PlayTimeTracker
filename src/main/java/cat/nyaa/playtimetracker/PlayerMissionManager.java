package cat.nyaa.playtimetracker;

import cat.nyaa.nyaacore.utils.InventoryUtils;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import cat.nyaa.playtimetracker.Utils.TaskUtils;
import cat.nyaa.playtimetracker.Utils.TimeUtils;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import com.udojava.evalex.Expression;
import me.clip.placeholderapi.PlaceholderAPI;
import net.ess3.api.IEssentials;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class PlayerMissionManager {
    private final PlayTimeTracker plugin;
    @Nullable
    private static PlayerMissionManager instance;
    private final List<AwaitingReward> awaitingRewardList = new ArrayList<>();
    private final CompletedMissionConnection completedMissionConnection;
    private final PTTConfiguration pttConfiguration;
    private final TimeRecordManager timeRecordManager;
    private int tickNum;

    public PlayerMissionManager(PlayTimeTracker playTimeTracker, PTTConfiguration pttConfiguration, TimeRecordManager timeRecordManager, CompletedMissionConnection completedMissionConnection) {
        instance = this;
        this.plugin = playTimeTracker;
        this.pttConfiguration = pttConfiguration;
        this.completedMissionConnection = completedMissionConnection;
        this.timeRecordManager = timeRecordManager;
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
    }

    @Nullable
    public static PlayerMissionManager getInstance() {
        return instance;
    }

    public void getMissionReward(Player player, String mission) {
        Map<String, MissionData> missionDataMap = getMissionDataMap();
        if (!missionDataMap.containsKey(mission)) return;
        I18n.send(player, "message.mission.get_reward", mission);
        MissionData missionData = missionDataMap.get(mission);
        //item

        if (missionData.rewardItemsBase64 != null && !missionData.rewardItemsBase64.isEmpty()) {
            List<ItemStack> items = new ArrayList<>();
            try {
                items = ItemStackUtils.itemsFromBase64(missionData.rewardItemsBase64);
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (ItemStack item : items) {
                if(item == null || item.getType().isAir())continue;
                if (InventoryUtils.hasEnoughSpace(player.getInventory(), item)) {
                    InventoryUtils.addItem(player, item);
                } else {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }
        }

        //command
        if (missionData.rewardCommandList != null)
            for (String command : missionData.rewardCommandList) {
                if (command != null && !command.isEmpty()) {
                    try {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(player, command));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    }

    public boolean completeMission(@NotNull Player player, String mission) {
        if (completeMissionNoReward(player.getUniqueId(), mission)) {
            getMissionReward(player, mission);
            return true;
        }
        return false;
    }

    public boolean completeMissionNoReward(UUID playerId, String mission) {
        boolean write2db = removeAwaitingReward(playerId, mission);

        if (write2db) {
            completedMissionConnection.WriteMissionCompleted(playerId, mission, TimeUtils.getUnixTimeStampNow());
        }
        return write2db;
    }

//    public Map<String, MissionData> getMissionDataMap() {
//        Map<String, MissionData> missionDataMap = new HashMap<>();
//        getMissionDataList().forEach(missionData -> missionDataMap.put(missionData.missionName, missionData));
//        return missionDataMap;
//    }

    public void checkAwaitingRewardList() {
        Map<String, MissionData> missionDataMap = getMissionDataMap();
        new ArrayList<>(awaitingRewardList).forEach(
                awaitingReward -> {
                    if (!missionDataMap.containsKey(awaitingReward.mission)) return;
                    if (missionDataMap.get(awaitingReward.mission).timeoutMS < 0) return;
                    if ((TimeUtils.getUnixTimeStampNow() - awaitingReward.time) >= missionDataMap.get(awaitingReward.mission).timeoutMS) {
                        completeMissionNoReward(awaitingReward.playerId, awaitingReward.mission);
                    }
                }
        );
    }


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
                    AwaitingReward awaitingReward = getAwaitingReward(player, missionName);
                    if (awaitingReward != null) {
                        return; // after checkAwaitingRewardList
                    }
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

                    this.putAwaitingReward(player, missionName, missionData.notify);
                    if (missionData.autoGive) {
                        completeMission(player, missionName);
                    }
                }
        );
    }

    public Set<String> getAwaitingMissionNameSet(Player player) {
        return awaitingRewardList.stream()
                .filter(awaitingReward -> awaitingReward.playerId == player.getUniqueId())
                .map(awaitingReward -> awaitingReward.mission).collect(Collectors.toSet());
    }

    @Nullable
    private AwaitingReward getAwaitingReward(Player player, String missionName) {
        for (AwaitingReward awaitingReward : awaitingRewardList) {
            if (awaitingReward.playerId == player.getUniqueId() && awaitingReward.mission.equals(missionName)) {
                return awaitingReward;
            }
        }
        return null;
    }

    private boolean removeAwaitingReward(UUID playerId, String missionName) {
        AtomicBoolean result = new AtomicBoolean(false);
        awaitingRewardList.removeIf(awaitingReward -> {
                    if (awaitingReward.playerId == playerId
                            && awaitingReward.mission.equals(missionName)) {
                        result.set(true);
                        return true;
                    }
                    return false;
                }
        );
        return result.get();
    }

    private void putAwaitingReward(@NotNull Player player, String missionName, boolean notify) {
        removeAwaitingReward(player.getUniqueId(), missionName);
        awaitingRewardList.add(new AwaitingReward(player.getUniqueId(), missionName, TimeUtils.getUnixTimeStampNow(), notify));
    }

    public Map<String, MissionData> getMissionDataMap() {
        return pttConfiguration.missionConfig.missions;
    }

    public void missionCheckTick() {
        this.tickNum++;
        checkAwaitingRewardList();
        Bukkit.getOnlinePlayers().forEach(player ->
                TaskUtils.mod64TickToRun(this.tickNum, player.getUniqueId(), () -> checkPlayerMission(player))
        );
    }

    public void notifyAcquire() {
        awaitingRewardList.forEach(awaitingReward -> {
            if (awaitingReward.isNotify) {
                Player player = Bukkit.getPlayer(awaitingReward.playerId);
                if (player != null) {
                    String command = PlaceholderAPI.setPlaceholders(player, I18n.format("message.mission.notify.command", awaitingReward.mission));
                    String msg = PlaceholderAPI.setPlaceholders(player, I18n.format("message.mission.notify.msg", awaitingReward.mission));
                    BaseComponent[] commandComponent = new ComponentBuilder()
                            .append(msg)
                            .append(command)
                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                            .create();
                    player.spigot().sendMessage(new TextComponent(commandComponent));
                }
            }
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
        this.awaitingRewardList.removeIf(awaitingReward -> awaitingReward.playerId == playerId);
        this.completedMissionConnection.resetPlayerCompletedMission(playerId);
    }

    public void resetPlayerMissionData(UUID playerId, String missionName) {
        this.awaitingRewardList.removeIf(awaitingReward -> (awaitingReward.playerId == playerId && missionName.equals(awaitingReward.mission)));
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
