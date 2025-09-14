package cat.nyaa.playtimetracker.condition;

import java.util.List;

public class Condition<T> implements ICondition<T> {

    public static final double EPSILON = 1e-6;
    private final ComparisonOperator operator;

    private final IParametricVariable<T> variable;
    private final long value;

    /**
     * precision of right-constant in calculation;
     * for example, if precision is 1000, a > 17356 will be calculated as a > floor(17356 / 1000) * 1000
     *                                     a < 17356 will be calculated as a < ceil(17356 / 1000) * 1000
     */

    private final long precision;

    public Condition(ComparisonOperator operator, IParametricVariable<T> variable, long value, long precision) {
        this.operator = operator;
        this.variable = variable;
        this.value = value;
        this.precision = precision;
    }

    @Override
    public boolean test(T source) {
        long variable = this.variable.getValue(source);
        return switch (this.operator) {
            case EQUAL -> variable / this.precision == this.value / this.precision;
            case GREATER_THAN -> variable > (this.value / this.precision) * this.precision;
            case GREATER_THAN_OR_EQUAL -> variable >= (this.value / this.precision) * this.precision;
            case LESS_THAN -> variable < ((this.value / this.precision) + 1) * this.precision - 1;
            case LESS_THAN_OR_EQUAL -> variable <= ((this.value / this.precision) + 1) * this.precision - 1;
        };
    }

    public List<Range> resolve(T source) {
        long right = this.value / this.precision;
        long leftLow = right * this.precision;
        long leftHigh = (right + 1) * this.precision - 1;
        var range = switch (this.operator) {
            case EQUAL -> Range.of(leftLow, leftHigh);
            case GREATER_THAN -> Range.upper(leftLow + 1);
            case GREATER_THAN_OR_EQUAL -> Range.upper(leftLow);
            case LESS_THAN -> Range.lower(leftHigh - 1);
            case LESS_THAN_OR_EQUAL -> Range.lower(leftHigh);
        };
        return this.variable.resolve(range, source);
    }
}
