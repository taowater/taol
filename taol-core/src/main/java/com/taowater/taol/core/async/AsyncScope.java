package com.taowater.taol.core.async;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        List<AsyncFuture<Object>> collect = Arrays.stream(suppliers).map(this::supply).collect(Collectors.toList());
        return collect.stream().map(AsyncFuture::join).collect(Collectors.toList());
    }

    static DefaultAsyncScope.DefaultAsyncScopeBuilder build() {
        return new DefaultAsyncScope.DefaultAsyncScopeBuilder();
    }

}
