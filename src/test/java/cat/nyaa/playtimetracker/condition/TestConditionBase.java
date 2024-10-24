package cat.nyaa.playtimetracker.condition;

import it.unimi.dsi.fastutil.longs.LongIntPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConditionBase {

    @Test
    public void testCondition() {
        testCondition(ComparisonOperator.EQUAL, 1000, 10, 2,
                LongIntPair.of(990, 0b00000),
                LongIntPair.of(1000, 0b00111),
                LongIntPair.of(1010, 0b11000),
                LongIntPair.of(1020, 0b00000)
        );
        testCondition(ComparisonOperator.GREATER_THAN, 1000, 10, 2,
                LongIntPair.of(990, 0b00000),
                LongIntPair.of(1000, 0b00011),
                LongIntPair.of(1010, 0b11111),
                LongIntPair.of(1020, 0b11111)
        );
        testCondition(ComparisonOperator.GREATER_THAN_OR_EQUAL, 1000, 10, 2,
                LongIntPair.of(990, 0b00000),
                LongIntPair.of(1000, 0b00111),
                LongIntPair.of(1010, 0b11111),
                LongIntPair.of(1020, 0b11111)
        );
        testCondition(ComparisonOperator.LESS_THAN, 1000, 10, 2,
                LongIntPair.of(990, 0b11111),
                LongIntPair.of(1000, 0b11111),
                LongIntPair.of(1010, 0b10000),
                LongIntPair.of(1020, 0b00000)
        );
        testCondition(ComparisonOperator.LESS_THAN_OR_EQUAL, 1000, 10, 2,
                LongIntPair.of(990, 0b11111),
                LongIntPair.of(1000, 0b11111),
                LongIntPair.of(1010, 0b11000),
                LongIntPair.of(1020, 0b00000)
        );
    }

    private void testCondition(ComparisonOperator op, long value, long precision, int ext, LongIntPair... pairs) {
        var v0 = new SimpleVariable("test");
        Condition<Long> condition = new Condition<>(op, v0, value, precision);
        final int extTotal = 2 * ext + 1;
        final int offsetTotal = 2 * ext;
        for(var pair : pairs) {
            for(int i = 0; i < extTotal; ++i) {
                final Long variable = pair.leftLong() + (i - ext);
                final boolean expected = ((pair.rightInt() >> (offsetTotal - i)) & 0b1) == 0b1;
                final int finalI = i;
                final boolean result = condition.test(variable);
                Assertions.assertEquals(expected, result, () -> String.format("[%d] op=%s, value=%d, precision=%d, variable=%d", finalI, op, value, precision, variable));
            }
        }
    }

    @Test
    public void testConditionResolve() {
        testConditionResolve(ComparisonOperator.EQUAL, 1000, 10, 800L, Range.of(200, 209));
        testConditionResolve(ComparisonOperator.EQUAL, 1000, 10, 1200L, Range.of( -200, -191));

        testConditionResolve(ComparisonOperator.GREATER_THAN, 1000, 10, 800L, Range.upper(201));
        testConditionResolve(ComparisonOperator.GREATER_THAN, 1000, 10, 1200L, Range.upper(-199));

        testConditionResolve(ComparisonOperator.GREATER_THAN_OR_EQUAL, 1000, 10, 800L, Range.upper(200));
        testConditionResolve(ComparisonOperator.GREATER_THAN_OR_EQUAL, 1000, 10, 1200L, Range.upper(-200));

        testConditionResolve(ComparisonOperator.LESS_THAN, 1000, 10, 800L, Range.lower(208));
        testConditionResolve(ComparisonOperator.LESS_THAN, 1000, 10, 1200L, Range.lower(-192));

        testConditionResolve(ComparisonOperator.LESS_THAN_OR_EQUAL, 1000, 10, 800L, Range.lower(209));
        testConditionResolve(ComparisonOperator.LESS_THAN_OR_EQUAL, 1000, 10, 1200L, Range.lower(-191));
    }

    private void testConditionResolve(ComparisonOperator op, long value, long precision, Long variable, Range expected) {
        var v0 = new SimpleVariable("test");
        Condition<Long> condition = new Condition<>(op, v0, value, precision);
        var range = condition.resolve(variable);
        if(expected == null) {
            Assertions.assertTrue(range.isEmpty());
        } else {
            var range0 = range.getFirst();

            Assertions.assertEquals(expected, range0);

            var range1 = v0.getValueFromParam(variable, range0);

            Assertions.assertTrue(condition.test(range1.getLow()), () -> String.format("op=%s, value=%d, precision=%d, variable=%d, range=%s", op, value, precision, range1.getLow(), range0));

            Assertions.assertTrue(condition.test(range1.getHigh()), () -> String.format("op=%s, value=%d, precision=%d, variable=%d, range=%s", op, value, precision, range1.getHigh(), range0));
        }
    }
}
