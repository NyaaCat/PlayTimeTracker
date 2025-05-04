package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.playtimetracker.condition.ICondition;
import cat.nyaa.playtimetracker.config.ISerializableExt;
import cat.nyaa.playtimetracker.workflow.LimitedTimeTrackerModel;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MissionData implements ISerializableExt {

    @Serializable
    public List<String> group = new ArrayList<>();
    @Serializable
    public String expression = "lastSeen>1&&dailyTime>1&&weeklyTime>1&&monthlyTime>1&&totalTime>1&&1==2";
    @Serializable(name = "reset-daily")
    public boolean resetDaily = false;
    @Serializable(name = "reset-weekly")
    public boolean resetWeekly = false;
    @Serializable(name = "reset-monthly")
    public boolean resetMonthly = false;
    @Serializable(name = "reward-list")
    public Map<String, ISerializableExt> rewardList = new HashMap<>(); // ConfigurationSection does not support Object List; use Map instead; will be sorted by key
    @Serializable
    public boolean notify = true;


    {
        rewardList.put("reward1", new EcoRewardData());
        rewardList.put("reward2", new CommandRewardData());
    }

    private @Nullable List<ISerializableExt> sortedRewardList = null;

    public MissionData() {
    }

    @Override
    public boolean validate(List<String> outputError) {
        List<ISerializableExt> sortedList = new ArrayList<>(rewardList.size());
        var keys = rewardList.keySet().stream().sorted().iterator();
        while (keys.hasNext()) {
            var key = keys.next();
            var reward = rewardList.get(key);
            if(reward == null || !reward.validate(outputError)) {
                outputError.add("Invalid reward: " + key);
                return false;
            }
            sortedList.add(reward);
        }
        sortedRewardList = sortedList;
        return true;
    }

    public List<ISerializableExt> getSortedRewardList() {
        return sortedRewardList;
    }

    public ICondition<LimitedTimeTrackerModel> getTimeCondition() {
        return null;
    }
}
