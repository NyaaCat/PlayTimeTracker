package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.MissionConfig;
import cat.nyaa.playtimetracker.config.PTTConfiguration;
import cat.nyaa.playtimetracker.config.data.MissionData;
import cat.nyaa.playtimetracker.db.connection.CompletedMissionConnection;
import cat.nyaa.playtimetracker.db.model.CompletedMissionDbModel;
import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import cat.nyaa.playtimetracker.utils.PlaceholderAPIUtils;
import cat.nyaa.playtimetracker.utils.TaskUtils;
import cat.nyaa.playtimetracker.utils.TimeUtils;
import com.udojava.evalex.Expression;
import net.ess3.api.IEssentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;

public class PlayerMissionManager {
    @Nullable
    private static PlayerMissionManager instance;
    private final PlayTimeTracker plugin;
    //private final List<AwaitingReward> awaitingRewardList = new ArrayList<>();
    private final CompletedMissionConnection completedMissionConnection;
    private final PTTConfiguration pttConfiguration;
    private final TimeRecordManager timeRecordManager;
    private final PlayerRewardManager playerRewardManager;
    private final NotifyPlayerMissionComplete notifyAcquire;
    private int tickNum;

    public PlayerMissionManager(PlayTimeTracker playTimeTracker, PTTConfiguration pttConfiguration, TimeRecordManager timeRecordManager, PlayerRewardManager playerRewardManager, CompletedMissionConnection completedMissionConnection) {
        instance = this;
        this.plugin = playTimeTracker;
        this.pttConfiguration = pttConfiguration;
        this.completedMissionConnection = completedMissionConnection;
        this.timeRecordManager = timeRecordManager;
        this.playerRewardManager = playerRewardManager;
        this.notifyAcquire = new NotifyPlayerMissionComplete();
        this.tickNum = 0;
    }

    @Nullable
    public static PlayerMissionManager getInstance() {
        return instance;
    }

    public PlayTimeTracker getPlugin() {
        return plugin;
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
                        this.plugin.getSLF4JLogger().error("[PlayerMissionManager] Failed to evaluate expression {} for {} {}", missionData.expression, player.getUniqueId(), missionName);
                        return;
                    }
                    if (result.doubleValue() <= 0) {
                        return;
                    }

                    final long timestamp = TimeUtils.getUnixTimeStampNow();

                    this.playerRewardManager.putRewardAsync(player, missionName, timestamp, missionData.getSortedRewardList(), missionData.notify ? this.notifyAcquire : null);

                    // TODO: async & cache (completedMissionConnection should run in one thread; another in checkPlayerMission)
                    this.completedMissionConnection.writeMissionCompleted(player.getUniqueId(), missionName, timestamp);
//                    this.putAwaitingReward(player, missionName, missionData.getSortedRewardList(), missionData.notify);
//                    if (missionData.autoGive) {
//                        completeMission(player, missionName);
//                    }
                }
        );
    }

    public MissionConfig getMissionConfig() {
        return pttConfiguration.missionConfig;
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

    static class NotifyPlayerMissionComplete implements BiConsumer<Player, String> {
        @Override
        public void accept(Player player, String mission) {
            if(!player.isOnline()) return;
            String command = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.command", mission));
            String msg = PlaceholderAPIUtils.setPlaceholders(player, I18n.format("message.mission.notify.msg", mission));
            var builder = Component.text();
            builder.append(LegacyComponentSerializer.legacySection().deserialize(msg));
            builder.append(
                Component.text()
                        .content(command)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)
                )
            );
            player.sendMessage(builder.build());
        }
    }
}
