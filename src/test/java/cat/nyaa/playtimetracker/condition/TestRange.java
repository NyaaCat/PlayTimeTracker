package cat.nyaa.playtimetracker.condition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestRange {

    @Test
    public void testRange() {
        var range = Range.of(10, 20);
        Assertions.assertTrue(range.contains(15));
        Assertions.assertFalse(range.contains(25));

        range = Range.upper(10);
        Assertions.assertTrue(range.contains(15));
        Assertions.assertTrue(range.contains(25));
    }

    @Test
    public void testRangeUnion() {
        testRangeUnion(Range.of(10, 20), Range.of(30, 40), null);
        testRangeUnion(Range.of(10, 30), Range.of(20, 40), Range.of(10, 40));
        testRangeUnion(Range.of(10, 30), Range.of(20, 30), Range.of(10, 30));
        testRangeUnion(Range.of(10, 30), Range.of(10, 30), Range.of(10, 30));
        testRangeUnion(Range.of(10, 30), Range.of(30, 40), Range.of(10, 40));
        testRangeUnion(Range.upper(10), Range.of(30, 40), Range.upper(10));
        testRangeUnion(Range.upper(10), Range.of(0, 40), Range.upper(0));
        testRangeUnion(Range.lower(30), Range.upper(20), Range.full());
    }

    private void testRangeUnion(Range range1, Range range2, Range expected) {
        var merged1 = Range.clone(range1);
        var result1 = merged1.union(range2);

        var merged2 = Range.clone(range2);
        var result2 = merged2.union(range1);

        if(expected == null) {
            Assertions.assertFalse(result1);
            Assertions.assertEquals(merged1, range1);
            Assertions.assertFalse(result2);
            Assertions.assertEquals(merged2, range2);
        } else {
            Assertions.assertTrue(result1);
            Assertions.assertTrue(result2);
            Assertions.assertEquals(merged1, expected);
            Assertions.assertEquals(merged2, expected);
        }
    }

    @Test
    public void testRangeIntersect() {
        testRangeIntersect(Range.of(10, 20), Range.of(30, 40), null);
        testRangeIntersect(Range.of(10, 30), Range.of(20, 40), Range.of(20, 30));
        testRangeIntersect(Range.of(10, 30), Range.of(20, 30), Range.of(20, 30));
        testRangeIntersect(Range.of(10, 30), Range.of(10, 30), Range.of(10, 30));
        testRangeIntersect(Range.of(10, 30), Range.of(30, 40), Range.of(30, 30));
        testRangeIntersect(Range.upper(10), Range.of(30, 40), Range.of(30, 40));
        testRangeIntersect(Range.upper(10), Range.of(0, 40), Range.of(10, 40));
        testRangeIntersect(Range.lower(30), Range.upper(20), Range.of(20, 30));
    }

    private void testRangeIntersect(Range range1, Range range2, Range expected) {
        var merged1 = Range.clone(range1);
        var result1 = merged1.intersect(range2);

        var merged2 = Range.clone(range2);
        var result2 = merged2.intersect(range1);

        if(expected == null) {
            Assertions.assertFalse(result1);
            Assertions.assertEquals(merged1, range1);
            Assertions.assertFalse(result2);
            Assertions.assertEquals(merged2, range2);
        } else {
            Assertions.assertTrue(result1);
            Assertions.assertTrue(result2);
            Assertions.assertEquals(merged1, expected);
            Assertions.assertEquals(merged2, expected);
        }
    }


    @Test
    public void testRangeMultiUnion() {
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40));
            var rightRanges = List.of(Range.of(50, 60), Range.of(70, 80));
            var expected = List.of(Range.of(10, 20), Range.of(30, 40), Range.of(50, 60), Range.of(70, 80));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40));
            var rightRanges = List.of(Range.of(5, 15), Range.of(35, 80));
            var expected = List.of(Range.of(5, 20), Range.of(30, 80));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40));
            var rightRanges = List.of(Range.of(5, 35), Range.of(60, 80));
            var expected = List.of(Range.of(5, 40), Range.of(60, 80));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.of(50, 60));
            var rightRanges = List.of(Range.of(25, 65));
            var expected = List.of(Range.of(10, 20), Range.of(25, 65));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.of(50, 60));
            var rightRanges = List.of(Range.of(25, 55));
            var expected = List.of(Range.of(10, 20), Range.of(25, 60));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.upper(50));
            var rightRanges = List.of(Range.of(15, 30), Range.upper(80));
            var expected = List.of(Range.of(10, 40), Range.upper(50));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.upper(50));
            var rightRanges = List.of(Range.lower(35), Range.upper(80));
            var expected = List.of(Range.lower(40), Range.upper(50));
            testRangeMultiUnion(leftRanges, rightRanges, expected);
        }
    }

    private void testRangeMultiUnion(List<Range> leftRanges, List<Range> rightRanges, List<Range> expected) {
        var result = Range.union(leftRanges, rightRanges);
        Assertions.assertArrayEquals(expected.toArray(), result.toArray());
        result = Range.union(rightRanges, leftRanges);
        Assertions.assertArrayEquals(expected.toArray(), result.toArray());
    }


    @Test
    public void testRangeMultiIntersect() {
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40));
            var rightRanges = List.of(Range.of(50, 60), Range.of(70, 80));
            List<Range> expected = List.of();
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40));
            var rightRanges = List.of(Range.of(5, 15), Range.of(35, 80));
            var expected = List.of(Range.of(10, 15), Range.of(35, 40));
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40));
            var rightRanges = List.of(Range.of(5, 35), Range.of(60, 80));
            var expected = List.of(Range.of(10, 20), Range.of(30, 35));
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.of(50, 60));
            var rightRanges = List.of(Range.of(25, 65));
            var expected = List.of(Range.of(30, 40), Range.of(50, 60));
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.of(50, 60));
            var rightRanges = List.of(Range.of(25, 55));
            var expected = List.of(Range.of(30, 40), Range.of(50, 55));
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.upper(50));
            var rightRanges = List.of(Range.of(15, 30), Range.upper(80));
            var expected = List.of(Range.of(15, 20), Range.of(30, 30), Range.upper(80));
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
        {
            var leftRanges = List.of(Range.of(10, 20), Range.of(30, 40), Range.upper(50));
            var rightRanges = List.of(Range.lower(35), Range.upper(80));
            var expected = List.of(Range.of(10, 20), Range.of(30, 35), Range.upper(80));
            testRangeMultiIntersect(leftRanges, rightRanges, expected);
        }
    }

    private void testRangeMultiIntersect(List<Range> leftRanges, List<Range> rightRanges, List<Range> expected) {
        var result = Range.intersect(leftRanges, rightRanges);
        Assertions.assertArrayEquals(expected.toArray(), result.toArray());
        result = Range.intersect(rightRanges, leftRanges);
        Assertions.assertArrayEquals(expected.toArray(), result.toArray());
    }
}
