package cat.nyaa.playtimetracker;

import cat.nyaa.playtimetracker.config.data.EcoRewardData;
import cat.nyaa.playtimetracker.config.ISerializableExt;
import cat.nyaa.playtimetracker.db.connection.RewardsConnection;
import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.reward.EcoReward;
import cat.nyaa.playtimetracker.reward.IReward;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PlayerRewardManager {

    private final Plugin plugin;
    private final RewardsConnection rewardsConnection;
    private ConcurrentHashMap<UUID, String> playerAcquiringRewards;

    public PlayerRewardManager(Plugin plugin, RewardsConnection rewardsConnection) {
        this.plugin = plugin;
        this.rewardsConnection = rewardsConnection;
        this.playerAcquiringRewards = new ConcurrentHashMap<>();
    }

    public void putRewardAsync(final Player player, final String missionName, final long completedTime, final Iterable<IReward> rewards, final @Nullable BiConsumer<Player, String> notifyCallback) {
        final var scheduler = this.plugin.getServer().getScheduler();
        final var logger = this.plugin.getLogger();
        final var playerId = player.getUniqueId();
        scheduler.runTaskAsynchronously(this.plugin, () -> {
            this.rewardsConnection.addRewards(playerId, missionName, rewards, completedTime);
            if (notifyCallback != null) {
                scheduler.runTask(this.plugin, () -> notifyCallback.accept(player, missionName));
            }
        });
    }

    public void putRewardAsync(final Player player, final String missionName, final long completedTime, final List<ISerializableExt> rewardDataList, final @Nullable BiConsumer<Player, String> notifyCallback) {
        final var logger = this.plugin.getSLF4JLogger();
        List<IReward> rewardList = new ArrayList<>(rewardDataList.size());
        for (ISerializableExt rewardData : rewardDataList) {
            IReward reward = this.createReward(rewardData);
            if(reward != null) {
                if(reward.prepare(missionName, completedTime, player, this.plugin)) {
                    rewardList.add(reward);
                } else {
                    logger.error("[PlayerRewardManager] Failed to prepare reward {} for {} {}", reward.getClass(), player.getUniqueId(), missionName);
                }
            } else {
                logger.error("[PlayerRewardManager] Unknown reward data type {} for {} {}", rewardData.getClass(), player.getUniqueId(), missionName);
            }
        }
        this.putRewardAsync(player, missionName, completedTime, rewardList, notifyCallback);
    }

    public void executeDistributeRewardsAsync(final Player player, final @Nullable String missionName) {
        final var scheduler = this.plugin.getServer().getScheduler();
        final var logger = this.plugin.getSLF4JLogger();
        scheduler.runTaskAsynchronously(this.plugin, () -> {
            final var rewardList = this.rewardsConnection.getRewards(player.getUniqueId(), missionName);
            if(rewardList.isEmpty()) {
                scheduler.runTask(this.plugin, () -> {
                    if(missionName == null) {
                        I18n.send(player, "command.acquire.empty");
                    } else {
                        I18n.send(player, "command.acquire.not_found", missionName);
                    }
                    this.playerAcquiringRewards.remove(player.getUniqueId());
                });
            } else {
                logger.info("[PlayerRewardManager] Player {} is acquiring {} rewards", player.getUniqueId(), rewardList.size());
                scheduler.runTask(this.plugin, () -> {
                    IntArrayList rewardIdList = new IntArrayList(rewardList.size());
                    ObjectArrayList<Component> outputMessages = new ObjectArrayList<>(16);
                    for (RewardDbModel reward : rewardList) {
                        Boolean distributeRet = reward.getReward().distribute(player, this.plugin, outputMessages);
                        if(distributeRet == null) {
                            logger.warn("[PlayerRewardManager] Player {} blocked acquire reward {}", player.getUniqueId(), reward.getId());
                            // TODO: send message to player?
                            break;
                        } else {
                            var builder = Component.text();
                            if (distributeRet) {
                                rewardIdList.add(reward.getId());
                                // TODO: I18n
                                String successText = I18n.format("command.acquire.success", reward.getRewardName());
                                builder.append(LegacyComponentSerializer.legacySection().deserialize(successText));
                            } else {
                                String failText = I18n.format("command.acquire.failed", reward.getRewardName());
                                builder.append(LegacyComponentSerializer.legacySection().deserialize(failText));
                            }
                            if(!outputMessages.isEmpty()) {
                                builder.append(Component.text(' '));
                                for (Component outputMessage : outputMessages) {
                                    builder.append(outputMessage);
                                }
                            }
                            player.sendMessage(builder.build());
                        }
                        outputMessages.clear();
                    }
                    if(rewardIdList.isEmpty()) {
                        this.playerAcquiringRewards.remove(player.getUniqueId());
                    } else {
                        scheduler.runTaskAsynchronously(this.plugin, () -> {
                            this.rewardsConnection.removeRewards(rewardIdList);
                            this.playerAcquiringRewards.remove(player.getUniqueId());
                            logger.info("[PlayerRewardManager] Player {} has acquired {} rewards", player.getUniqueId(), rewardIdList.size());
                        });
                    }
                });
            }
        });
    }

    public void executeListRewardsAsync(final Player player, final @Nullable String rewardName) {
        final var scheduler = this.plugin.getServer().getScheduler();
        final var logger = this.plugin.getSLF4JLogger();
        scheduler.runTaskAsynchronously(this.plugin, () -> {
            if(rewardName == null) {
                Object2IntMap<String> missions = this.rewardsConnection.countRewards(player.getUniqueId());
                scheduler.runTask(this.plugin, () -> {
                    if(missions == null) {
                        I18n.send(player, "command.listrewards.err");
                    } else if(missions.isEmpty()) {
                        I18n.send(player, "command.listrewards.empty_all");
                    } else {
                        int count = missions.values().intStream().sum();
                        String header = I18n.format("command.listrewards.show_all",count);
                        var builder = Component.text();
                        builder.append(LegacyComponentSerializer.legacySection().deserialize(header));
                        for(var entry : missions.object2IntEntrySet()) {
                            String line = I18n.format("command.listrewards.show_item", entry.getKey(), entry.getIntValue());
                            builder.append(Component.newline());
                            builder.append(LegacyComponentSerializer.legacySection().deserialize(line));
                        }
                        player.sendMessage(builder.build());
                    }
                });
            } else {
                int count = this.rewardsConnection.countReward(player.getUniqueId(), rewardName);
                scheduler.runTask(this.plugin, () -> {
                    if(count == 0) {
                        I18n.send(player, "command.listrewards.empty", rewardName);
                    } else {
                        I18n.send(player, "command.listrewards.show", count, rewardName);
                    }
                });
            }
        });
    }

    public void executeRewardsAutoCheckAsync(final Player player, int delayTicks) {
        final var scheduler = this.plugin.getServer().getScheduler();
        final var logger = this.plugin.getSLF4JLogger();
        scheduler.runTaskAsynchronously(this.plugin, () -> {
            final var rewardList = this.rewardsConnection.countRewards(player.getUniqueId());
            if (rewardList == null) {
                logger.error("[PlayerRewardManager] Failed to auto-check rewards count for player {}", player.getUniqueId());
            } else if(rewardList.isEmpty()){

            } else {
                final int totalRewards = rewardList.values().intStream().sum();
                scheduler.runTaskLater(this.plugin, () -> {
                    String header = I18n.format("message.reward.notify.msg", totalRewards);
                    String cmd = I18n.format("message.reward.notify.command");
                    var builder = Component.text();
                    builder.append(LegacyComponentSerializer.legacySection().deserialize(header));
                    builder.append(
                        Component.text()
                                .content(cmd)
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                    );
                    player.sendMessage(builder.build());
                }, delayTicks);
            }
        });
    }

    public IReward createReward(ISerializableExt data) {
        if(data instanceof EcoRewardData ecoRewardData) {
            return new EcoReward(ecoRewardData);
        }
        return null;
    }
}
