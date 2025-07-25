package cat.nyaa.playtimetracker.config.data;

import cat.nyaa.playtimetracker.condition.ICondition;
import cat.nyaa.playtimetracker.config.ISerializableExt;
import cat.nyaa.playtimetracker.config.IValidationContext;
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
    private @Nullable ICondition<?> compiledCondition = null;

    public MissionData() {
    }

    @Override
    public void validate(IValidationContext context) throws Exception {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expression cannot be null or empty");
        }
        if (context instanceof IConditionCompiler conditionCompiler) {
            compiledCondition = conditionCompiler.compile(expression);
        }
        List<ISerializableExt> sortedList = new ArrayList<>(rewardList.size());
        var keys = rewardList.keySet().stream().sorted().iterator();
        while (keys.hasNext()) {
            var key = keys.next();
            var reward = rewardList.get(key);
            if (reward == null) {
                throw new IllegalArgumentException("Reward for key '" + key + "' is null");
            }
            reward.validate(context);
            sortedList.add(reward);
        }
        sortedRewardList = sortedList;
    }

    public List<ISerializableExt> getSortedRewardList() {
        assert sortedRewardList != null : "Sorted reward list is not initialized. Call validate() first.";
        return sortedRewardList;
    }

    public ICondition<?> getTimeCondition() {
        assert compiledCondition != null : "Compiled condition is not initialized. Call validate() first.";
        return compiledCondition;
    }

    public interface IConditionCompiler extends IValidationContext {

        ICondition<?> compile(String expression) throws Exception;
    }
}
