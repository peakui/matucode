package com.peakui.post.like;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * 有界 HeavyKeeper：每行的桶保存 64 位指纹和计数，冲突按 b^(-count) 的概率衰减。
 * 查询取所有指纹匹配桶的最大计数；哈希冲突可能造成低估，指纹碰撞仍可能造成误判。
 *
 * <p>采用固定、非滑动窗口：窗口边界后第一次读写清空所有桶，旧热点不会永久保留。
 * 窗口以实例创建时间为起点，默认 60 秒；边界附近的访问不会跨窗口累加。
 * 常驻内存为 O(width * depth)，不保留原始 key；所有读写及轮转在同一锁内完成。
 */
@Component
public class HeavyKeeper {

    private static final long DEFAULT_WINDOW_MILLIS = 60_000L;
    private static final double DECAY_BASE = 1.08d;

    private final int width;
    private final int depth;
    private final long[][] fingerprints;
    private final int[][] counters;
    private final long windowMillis;
    private final DoubleSupplier random;
    private final LongSupplier clockMillis;
    private long windowStartMillis;

    /** 保留直接构造时的双参数接口。 */
    public HeavyKeeper(int width, int depth) {
        this(width, depth, DEFAULT_WINDOW_MILLIS);
    }

    @Autowired
    public HeavyKeeper(@Value("${post.like.hot-key.width:2048}") int width,
                       @Value("${post.like.hot-key.depth:4}") int depth,
                       @Value("${post.like.hot-key.window-millis:60000}") long windowMillis) {
        this(width, depth, windowMillis,
                () -> ThreadLocalRandom.current().nextDouble(),
                () -> TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
    }

    /** 测试入口：随机数须位于 [0, 1)，时钟以毫秒计，生产使用单调时钟。 */
    HeavyKeeper(int width, int depth, long windowMillis,
                DoubleSupplier random, LongSupplier clockMillis) {
        if (width <= 0 || depth <= 0 || windowMillis <= 0) {
            throw new IllegalArgumentException("width, depth and windowMillis must be positive");
        }
        this.width = width;
        this.depth = depth;
        this.windowMillis = windowMillis;
        this.random = Objects.requireNonNull(random, "random");
        this.clockMillis = Objects.requireNonNull(clockMillis, "clockMillis");
        this.fingerprints = new long[depth][width];
        this.counters = new int[depth][width];
        this.windowStartMillis = clockMillis.getAsLong();
    }

    /** 记录一次访问，返回本次更新后该 key 的近似计数。null key 不受支持。 */
    public synchronized int record(String key) {
        long fingerprint = fingerprint(Objects.requireNonNull(key, "key"));
        rotateWindow();
        int estimate = 0;
        for (int row = 0; row < depth; row++) {
            int index = index(fingerprint, row);
            int count = counters[row][index];
            if (count == 0) {
                fingerprints[row][index] = fingerprint;
                counters[row][index] = 1;
            } else if (fingerprints[row][index] == fingerprint) {
                // 饱和计数，避免长期高频访问导致整数溢出。
                if (count < Integer.MAX_VALUE) {
                    counters[row][index] = count + 1;
                }
            } else if (random.getAsDouble() < Math.pow(DECAY_BASE, -count)) {
                counters[row][index] = count - 1;
                if (count == 1) {
                    // 冲突只有衰减至零才能替换，且新 key 从 1 计数。
                    fingerprints[row][index] = fingerprint;
                    counters[row][index] = 1;
                }
            }
            if (fingerprints[row][index] == fingerprint) {
                estimate = Math.max(estimate, counters[row][index]);
            }
        }
        return estimate;
    }

    /** 只查询匹配指纹的桶，不衰减冲突桶；查询也会触发过期窗口清理。 */
    public synchronized int estimate(String key) {
        long fingerprint = fingerprint(Objects.requireNonNull(key, "key"));
        rotateWindow();
        int estimate = 0;
        for (int row = 0; row < depth; row++) {
            int index = index(fingerprint, row);
            if (fingerprints[row][index] == fingerprint) {
                estimate = Math.max(estimate, counters[row][index]);
            }
        }
        return estimate;
    }

    private void rotateWindow() {
        long now = clockMillis.getAsLong();
        long elapsed = now - windowStartMillis;
        if (elapsed >= windowMillis || elapsed < 0) {
            for (int row = 0; row < depth; row++) {
                Arrays.fill(counters[row], 0);
                Arrays.fill(fingerprints[row], 0L);
            }
            // 跳过任意多个空闲窗口，无需逐窗口循环。注入时钟倒退时重新开始。
            windowStartMillis = elapsed < 0 ? now : now - elapsed % windowMillis;
        }
    }

    private int index(long fingerprint, int row) {
        long hash = mix64(fingerprint ^ (0x9E3779B97F4A7C15L * (row + 1L)));
        return (int) Math.floorMod(hash, (long) width);
    }

    private static long fingerprint(String key) {
        // 对全部 UTF-16 字符计算 FNV-1a 后混合，而非依赖容易碰撞的 String.hashCode()。
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); i++) {
            char character = key.charAt(i);
            hash = (hash ^ (character & 0xff)) * 0x100000001b3L;
            hash = (hash ^ (character >>> 8)) * 0x100000001b3L;
        }
        return mix64(hash);
    }

    private static long mix64(long hash) {
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        return hash ^ (hash >>> 33);
    }
}
