package cat.nyaa.playtimetracker.db.model;

import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;
import cat.nyaa.playtimetracker.reward.IReward;

import java.util.UUID;

@Table("rewards")
public class RewardDbModel {

    @Column(name = "id", primary = true, autoIncrement = true)
    public int id;

    @Column(name = "completedTime")
    public long completedTime;

    @Column(name = "player")
    public UUID playerUniqueID;

    @Column(name = "rewardName")
    public String rewardName;

    @Column(name = "rewardData")
    public IReward reward;

    public RewardDbModel(int id, long completedTime, UUID playerUniqueID, String rewardName, IReward reward) {
        this.id = id;
        this.completedTime = completedTime;
        this.playerUniqueID = playerUniqueID;
        this.rewardName = rewardName;
        this.reward = reward;
    }

    public RewardDbModel() {

    }

    public long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(long completedTime) {
        this.completedTime = completedTime;
    }

    public UUID getPlayerUniqueID() {
        return playerUniqueID;
    }

    public void setPlayerUniqueID(UUID playerUniqueID) {
        this.playerUniqueID = playerUniqueID;
    }

    public String getRewardName() {
        return rewardName;
    }

    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    public IReward getReward() {
        return reward;
    }

    public void setReward(IReward reward) {
        this.reward = reward;
    }

    public int getId() {
        return id;
    }
}
