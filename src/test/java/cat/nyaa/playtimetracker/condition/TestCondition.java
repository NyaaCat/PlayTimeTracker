package cat.nyaa.playtimetracker.condition;

import cat.nyaa.playtimetracker.db.model.TimeTrackerDbModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TestCondition {

    private static final long precision = 1000;

    @Test
    public void testConditionSimple() throws Exception {
        TimeTrackerDbModel model = new TimeTrackerDbModel();
        model.lastUpdate = 24 * 60 * 60 * 1000;
        model.dailyTime = 24 * 60 * 1000;
        model.weeklyTime = 4 * 60 * 60 * 1000;

        var condition = buildCondition();
        var result = condition.test(model);
        Assertions.assertFalse(result);
        //var ranges = condition.resolve(0, model);
    }

    // (dailyTime >= 30min || weeklyTime < 5h) && lastSeen >= 1d
    private ConditionNode<TimeTrackerDbModel> buildCondition() {
        // dailyTime >= 30min
        var condition1 = new Condition<TimeTrackerDbModel>(
                ComparisonOperator.GREATER_THAN_OR_EQUAL,
                new IParametricVariable<TimeTrackerDbModel>() {
                    @Override
                    public String name() {
                        return "dailyTime";
                    }

                    @Override
                    public long getValue(TimeTrackerDbModel source) {
                        return 0;
                    }
                },
                30 * 60 * 1000,
                precision
        );
        // weeklyTime >= 5h
        var condition2 = new Condition<TimeTrackerDbModel>(
                ComparisonOperator.LESS_THAN,
                new IParametricVariable<TimeTrackerDbModel>() {
                    @Override
                    public String name() {
                        return "weeklyTime";
                    }

                    @Override
                    public long getValue(TimeTrackerDbModel source) {
                        return 0;
                    }
                },
                5 * 60 * 60 * 1000,
                precision
        );
        // lastSeen >= 1d
        var condition3 = new Condition<TimeTrackerDbModel>(
                ComparisonOperator.GREATER_THAN_OR_EQUAL,
                new IParametricVariable<TimeTrackerDbModel>() {
                    @Override
                    public String name() {
                        return "lastSeen";
                    }

                    @Override
                    public long getValue(TimeTrackerDbModel source) {
                        return 0;
                    }
                },
                24 * 60 * 60 * 1000,
                precision
        );
        // (dailyTime >= 30min || weeklyTime < 5h) && lastSeen >= 1d
        var node2 = new ConditionNode<>(LogicalOperator.OR, ConditionNode.wrap(condition1), ConditionNode.wrap(condition2));
        var mode1 = new ConditionNode<>(LogicalOperator.AND, node2, ConditionNode.wrap(condition3));
        return mode1;
    }
}
