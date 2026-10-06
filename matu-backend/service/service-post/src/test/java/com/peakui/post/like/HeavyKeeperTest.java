package com.peakui.post.like;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeavyKeeperTest {

    private static final long WINDOW_MILLIS = 1_000L;
    private static final double NEVER_DECAY = Math.nextDown(1.0d);

    @Test
    void repeatedKeyIsCountedAndQueriesDoNotMutateCounts() {
        HeavyKeeper keeper = new HeavyKeeper(64, 4, WINDOW_MILLIS,
                () -> { throw new AssertionError("Matching keys must not draw randomness"); }, () -> 0L);

        assertEquals(0, keeper.estimate("hot"));
        for (int i = 1; i <= 100; i++) {
            assertEquals(i, keeper.record("hot"));
            assertEquals(i, keeper.estimate("hot"));
            assertEquals(i, keeper.estimate("hot"));
        }
    }

    @Test
    void collidingColdKeysDoNotInheritHotCounts() {
        // 宽度 1 保证所有 key 冲突，固定随机值保证不衰减。
        HeavyKeeper keeper = new HeavyKeeper(1, 3, WINDOW_MILLIS, () -> NEVER_DECAY, () -> 0L);
        for (int i = 0; i < 100; i++) {
            keeper.record("hot");
        }

        for (int i = 0; i < 1_000; i++) {
            String coldKey = "cold-" + i;
            assertEquals(0, keeper.estimate(coldKey));
            assertEquals(0, keeper.record(coldKey));
            assertEquals(0, keeper.estimate(coldKey));
        }
        assertEquals(100, keeper.estimate("hot"));
    }

    @Test
    void equalJavaHashCodesHaveDifferentFingerprints() {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> NEVER_DECAY, () -> 0L);

        assertEquals(1, keeper.record("Aa"));
        assertEquals(0, keeper.record("BB"));
        assertEquals(0, keeper.estimate("BB"));
        assertEquals(1, keeper.estimate("Aa"));
    }

    @Test
    void conflictDecaysResidentAndReplacesOnlyWhenCountReachesZero() {
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> 0.0d, () -> 0L);
        keeper.record("resident");
        keeper.record("resident");
        keeper.record("resident");

        assertEquals(0, keeper.record("incoming"));
        assertEquals(2, keeper.estimate("resident"));
        assertEquals(0, keeper.record("incoming"));
        assertEquals(1, keeper.estimate("resident"));
        assertEquals(1, keeper.record("incoming"));
        assertEquals(0, keeper.estimate("resident"));
        assertEquals(1, keeper.estimate("incoming"));
        assertEquals(2, keeper.record("incoming"));
    }

    @Test
    void higherCountsHaveLowerDecayProbability() {
        // 1 / 1.08^2 < 0.9 < 1 / 1.08：同一随机值只会衰减计数为 1 的桶。
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> 0.9d, () -> 0L);
        assertEquals(1, keeper.record("first"));
        assertEquals(1, keeper.record("second"));
        assertEquals(0, keeper.estimate("first"));
        assertEquals(2, keeper.record("second"));
        assertEquals(0, keeper.record("third"));
        assertEquals(2, keeper.estimate("second"));
    }

    @Test
    void decayRequiresRandomValueStrictlyBelowProbability() {
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS,
                () -> Math.pow(1.08d, -1), () -> 0L);
        keeper.record("resident");

        assertEquals(0, keeper.record("incoming"));
        assertEquals(1, keeper.estimate("resident"));
    }

    @Test
    void estimateUsesMaximumMatchingCountAcrossRows() {
        AtomicInteger draws = new AtomicInteger();
        HeavyKeeper keeper = new HeavyKeeper(1, 2, WINDOW_MILLIS,
                () -> draws.getAndIncrement() % 2 == 0 ? 0.0d : NEVER_DECAY, () -> 0L);
        keeper.record("resident");
        keeper.record("resident");

        assertEquals(0, keeper.record("incoming")); // 两行 resident 分别为 1、2。
        assertEquals(2, keeper.estimate("resident"));
        assertEquals(1, keeper.record("incoming")); // 第一行替换，第二行仍为 resident。
        assertEquals(1, keeper.estimate("incoming"));
        assertEquals(2, keeper.estimate("resident"));
        assertEquals(3, keeper.record("resident"));
        assertEquals(3, keeper.estimate("resident"));
    }

    @Test
    void queryExpiresEntireWindowExactlyAtBoundary() {
        AtomicLong clock = new AtomicLong(100L);
        HeavyKeeper keeper = new HeavyKeeper(32, 3, WINDOW_MILLIS, () -> NEVER_DECAY, clock::get);
        for (int i = 0; i < 100; i++) {
            keeper.record("hot");
        }
        clock.set(1_099L);
        assertEquals(100, keeper.estimate("hot"));
        clock.set(1_100L);
        assertEquals(0, keeper.estimate("hot"));
        assertEquals(1, keeper.record("hot"));
    }

    @Test
    void recordAlsoRotatesWindowAndRetainsOriginalWindowBoundaries() {
        AtomicLong clock = new AtomicLong();
        HeavyKeeper keeper = new HeavyKeeper(1, 2, WINDOW_MILLIS, () -> NEVER_DECAY, clock::get);
        keeper.record("old");

        clock.set(3_500L); // 跳过多个窗口，当前窗口仍是 [3000, 4000)。
        assertEquals(1, keeper.record("new"));
        assertEquals(0, keeper.estimate("old"));
        clock.set(3_999L);
        assertEquals(1, keeper.estimate("new"));
        clock.set(4_000L);
        assertEquals(0, keeper.estimate("new"));
    }

    @Test
    void backwardsInjectedClockResetsWindow() {
        AtomicLong clock = new AtomicLong(100L);
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> NEVER_DECAY, clock::get);
        keeper.record("old");
        clock.set(99L);
        assertEquals(0, keeper.estimate("old"));
        assertEquals(1, keeper.record("new"));
    }

    @Test
    void countersSaturateWithoutOverflow() throws ReflectiveOperationException {
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> NEVER_DECAY, () -> 0L);
        keeper.record("hot");
        // 避免通过执行数十亿次 record 来到达整数边界。
        Field field = HeavyKeeper.class.getDeclaredField("counters");
        field.setAccessible(true);
        ((int[][]) field.get(keeper))[0][0] = Integer.MAX_VALUE - 1;

        assertEquals(Integer.MAX_VALUE, keeper.record("hot"));
        assertEquals(Integer.MAX_VALUE, keeper.record("hot"));
        assertEquals(Integer.MAX_VALUE, keeper.estimate("hot"));
        assertEquals(0, keeper.record("cold"));
        assertEquals(Integer.MAX_VALUE, keeper.estimate("hot"));
    }

    @Test
    void rejectsInvalidConfigurationAndNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> new HeavyKeeper(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new HeavyKeeper(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new HeavyKeeper(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new HeavyKeeper(1, 1, 0L));
        assertThrows(IllegalArgumentException.class, () -> new HeavyKeeper(1, 1, -1L));
        assertThrows(NullPointerException.class,
                () -> new HeavyKeeper(1, 1, WINDOW_MILLIS, null, () -> 0L));
        assertThrows(NullPointerException.class,
                () -> new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> 0.0d, null));
        HeavyKeeper keeper = new HeavyKeeper(1, 1);
        assertThrows(NullPointerException.class, () -> keeper.record(null));
        assertThrows(NullPointerException.class, () -> keeper.estimate(null));
    }

    @Test
    void emptyKeyIsSupported() {
        HeavyKeeper keeper = new HeavyKeeper(1, 1, WINDOW_MILLIS, () -> NEVER_DECAY, () -> 0L);
        assertEquals(0, keeper.estimate(""));
        assertEquals(1, keeper.record(""));
        assertEquals(1, keeper.estimate(""));
    }
}
