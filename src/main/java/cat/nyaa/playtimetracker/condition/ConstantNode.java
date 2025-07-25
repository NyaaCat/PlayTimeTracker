package cat.nyaa.playtimetracker.condition;

import java.util.List;

public class ConstantNode<T> implements ICondition<T> {

    public final boolean value;

    public ConstantNode(boolean value) {
        this.value = value;
    }

    @Override
    public boolean test(T source) {
        return false;
    }

    @Override
    public List<Range> resolve(T source) {
        return List.of();
    }

    public static <T> ConstantNode<T> calculate(ComparisonOperator operator, long left, long right) {
        var value = switch (operator) {
            case EQUAL -> left == right;
            case GREATER_THAN -> left > right;
            case GREATER_THAN_OR_EQUAL -> left >= right;
            case LESS_THAN -> left < right;
            case LESS_THAN_OR_EQUAL -> left <= right;
        };
        return new ConstantNode<T>(value);
    }

    public static boolean isConstantFalse(ICondition<?> condition) {
        return condition instanceof ConstantNode<?> constantNode && !constantNode.value;
    }
}
