package com.taowater.taol.core.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 异步作用域
 */
public interface AsyncScope {

    Executor getExecutor();

    long getTimeout();

    TimeUnit getUnit();

    boolean isReturnNullIfEx();


    default <T> AsyncFuture<T> supply(Supplier<T> supplier) {
        return AsyncFuture.supply(supplier, this);
    }

    default AsyncFuture<Void> run(Runnable runnable) {
        return AsyncFuture.run(runnable, this);
    }

    default List<Object> all(Supplier<Object>... suppliers) {
        List<AsyncFuture<Object>> futures = new ArrayList<>(suppliers.length);
        // 先提交全部任务，再按输入顺序等待结果，保持原有并发语义
        for (Supplier<Object> supplier : suppliers) {
            futures.add(supply(supplier));
        }
        List<Object> results = new ArrayList<>(futures.size());
        for (AsyncFuture<Object> future : futures) {
            results.add(future.join());
        }
        return results;
    }

    static DefaultAsyncScope.DefaultAsyncScopeBuilder build() {
        return new DefaultAsyncScope.DefaultAsyncScopeBuilder();
    }

}
