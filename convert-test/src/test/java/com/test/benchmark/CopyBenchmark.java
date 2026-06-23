package com.test.benchmark;

import lombok.experimental.UtilityClass;

/**
 * 拷贝性能粗测：预热 + nanoTime，仅作开发参考，不作 CI 门槛。
 */
@UtilityClass
public class CopyBenchmark {

    public static final int WARMUP_ITERATIONS = 3_000;
    public static final int MEASURE_ITERATIONS = 10_000;

    public static void beginSuite(String scenario) {
        System.out.println();
        System.out.println("======== " + scenario + " ========");
    }

    public static void beginSection(String section) {
        System.out.println();
        System.out.println("--- " + section + " (" + MEASURE_ITERATIONS + " iter, after " + WARMUP_ITERATIONS + " warmup) ---");
        System.out.printf("%-12s %12s%n", "framework", "elapsed");
    }

    public static double measure(String name, Runnable action) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            action.run();
        }
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            action.run();
        }
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf("%-12s %10.2f ms%n", name, millis);
        return millis;
    }
}
