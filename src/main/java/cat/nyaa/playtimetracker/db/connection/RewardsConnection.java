package cat.nyaa.playtimetracker.db.connection;

import cat.nyaa.playtimetracker.db.model.RewardDbModel;
import cat.nyaa.playtimetracker.db.tables.RewardsTable;
import cat.nyaa.playtimetracker.reward.IReward;
import com.zaxxer.hikari.HikariDataSource;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

@NotThreadSafe
public class RewardsConnection implements IBatchOperate {
    private final RewardsTable rewardsTable;

    public RewardsConnection(HikariDataSource ds, Plugin plugin) {
        this.rewardsTable = new RewardsTable(ds);
        this.rewardsTable.tryCreateTable(plugin);
    }

    public void close() {


    }

    public void addReward(UUID playerUniqueID, String rewardName, IReward reward, long completedTime) {
        RewardDbModel rewardDbModel = new RewardDbModel(0, completedTime, playerUniqueID, rewardName, reward);
        this.rewardsTable.insertReward(rewardDbModel);
    }

    public void addRewards(UUID playerUniqueID, String rewardName, Iterable<IReward> rewards, long completedTime) {
        List<RewardDbModel> rewardDbModels = new ObjectArrayList<>();
        for (IReward reward : rewards) {
            RewardDbModel rewardDbModel = new RewardDbModel(0, completedTime, playerUniqueID, rewardName, reward);
            rewardDbModels.add(rewardDbModel);
        }
        this.rewardsTable.insertRewardBatch(rewardDbModels);
    }

    public List<RewardDbModel> getRewards(UUID playerUniqueID, @Nullable String rewardName) {
        return this.rewardsTable.selectRewards(playerUniqueID, rewardName, true);
    }

    public void removeReward(int rewardId) {
        this.rewardsTable.deleteReward(rewardId);
    }

    public void removeRewards(IntCollection rewardIds) {
        this.rewardsTable.deleteRewardBatch(rewardIds);
    }

    public int countReward(UUID playerUniqueID, String rewardName) {
        return this.rewardsTable.selectRewardsCount(playerUniqueID, rewardName);
    }

    public Object2IntMap<String> countRewards(UUID playerUniqueID) {
        return this.rewardsTable.selectRewardsCount(playerUniqueID);
    }

    @Override
    public void beginBatchMode() {

    }

    @Override
    public void endBatchMode() {

    }
}
