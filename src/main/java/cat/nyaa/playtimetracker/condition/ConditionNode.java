package cat.nyaa.playtimetracker.condition;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConditionNode<T> implements ICondition<T> {

    protected static final Range FULL = Range.full();

    private final @Nullable LogicalOperator operator;
    private final ICondition<T> left;
    private final @Nullable ICondition<T> right;

    public ConditionNode(@Nullable LogicalOperator operator, ICondition<T> left, @Nullable ICondition<T> right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public static <T> ConditionNode<T> wrap(ICondition<T> left) {
        return new ConditionNode<>(null, left, null);
    }

    @Override
    public boolean test(T source) {
        if (operator == null) {
            return left.test(source);
        }
        return switch (this.operator) {
            case AND -> {
                if (!left.test(source)) yield false;
                assert right != null;
                yield right.test(source);
            }
            case OR -> {
                if (left.test(source)) yield true;
                assert right != null;
                yield right.test(source);
            }
            // case NOT -> !left.test(t);
        };
    }

    @Override
    public List<Range> resolve(T source) {
        if (operator == null) {
            return left.resolve(source);
        }
        return switch (this.operator) {
            case AND -> {
                List<Range> leftRanges = left.resolve(source);
                if(leftRanges.isEmpty()) {
                    yield leftRanges;
                }
                assert right != null;
                List<Range> rightRanges = right.resolve(source);
                if(rightRanges.isEmpty()) {
                    yield rightRanges;
                }
                if(leftRanges.size() == 1 && rightRanges.size() == 1) { // optimize for 1-element list
                    Range leftRange = leftRanges.getFirst();
                    Range rightRange = rightRanges.getFirst();
                    if(leftRange.intersect(rightRange)) {
                        yield leftRanges;
                    } else {
                        yield null;
                    }
                }
                yield Range.intersect(leftRanges, rightRanges);
            }
            case OR -> {
                List<Range> leftRanges = left.resolve(source);
                if(leftRanges.isEmpty()) {
                    yield leftRanges;
                }
                assert right != null;
                List<Range> rightRanges = right.resolve(source);
                if(rightRanges.isEmpty()) {
                    yield rightRanges;
                }
                if(leftRanges.size() == 1 && rightRanges.size() == 1) { // optimize for 1-element list
                    Range leftRange = leftRanges.getFirst();
                    Range rightRange = rightRanges.getFirst();
                    if(leftRange.union(rightRange)) {
                        yield List.of(leftRange);
                    } else {
                        if(leftRange.getHigh() < rightRange.getLow()) {
                            yield List.of(leftRange, rightRange);
                        } else {
                            yield List.of(rightRange, leftRange);
                        }
                    }
                }
                yield Range.union(leftRanges, rightRanges);
            }
            // case NOT -> {};
        };
    }
}
