package com.taowater.core;

import com.taowater.taol.core.async.AsyncFuture;
import com.taowater.taol.core.async.AsyncScope;
import lombok.var;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsyncTest {


    @Test
    void test() {

        AsyncScope as = AsyncScope.build()
                .executor(ForkJoinPool.commonPool())
                .returnNullIfEx(true)
                .timeout(5)
                .build();

        AsyncFuture<Integer> f1 = as.supply(() -> {
            sleep(2);
            return 234;
        });
        var f2 = as.supply(() -> (Integer) null);
        var f3 = as.supply(() -> {
            if (true) {
                throw new RuntimeException("123");
            }
            return 2;

        });

        Integer join1 = f1.join();
        Integer join2 = f2.join(666);
        Integer join3 = f3.join();
        assertEquals(234, join1);
        assertEquals(666, join2);
        assertEquals(null, join3);
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
