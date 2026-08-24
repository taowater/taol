package com.taowater.taol.core.async;

import lombok.experimental.Delegate;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 异步任务
 */
public class AsyncFuture<T> extends CompletableFuture<T> {

    @Delegate
    private final CompletableFuture<T> future;

    private final AsyncScope scope;

    public AsyncFuture(CompletableFuture<T> future, AsyncScope scope) {
        this.future = future;
        this.scope = scope;
    }

    public static <T> AsyncFuture<T> of(CompletableFuture<T> cf, AsyncScope scope) {
        if (Objects.isNull(scope)) {
            throw new NullPointerException("scope is null");
        }
        return new AsyncFuture<>(cf, scope);
    }

    public static <T> AsyncFuture<T> supply(Supplier<T> supplier, AsyncScope scope) {
        Executor executor = Optional.ofNullable(scope).map(AsyncScope::getExecutor).orElse(ForkJoinPool.commonPool());
        return of(CompletableFuture.supplyAsync(supplier, executor), scope);
    }

    public static AsyncFuture<Void> run(Runnable runnable, AsyncScope scope) {
        Executor executor = Optional.ofNullable(scope).map(AsyncScope::getExecutor).orElse(ForkJoinPool.commonPool());
        return of(CompletableFuture.runAsync(runnable, executor), scope);
    }

    @Override
    public T join() {
        long timeout = scope.getTimeout();
        try {
            // 正数表示带时限等待，非正数保持无限等待语义
            if (timeout > 0) {
                return future.get(timeout, Optional.ofNullable(scope.getUnit()).orElse(TimeUnit.SECONDS));
            }
            return future.join();
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                // 超时后取消后续等待，避免调用方已返回但任务继续占用资源
                future.cancel(true);
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (scope.isReturnNullIfEx()) {
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    public T join(T defaultValue) {
        return Optional.ofNullable(join()).orElse(defaultValue);
    }
}
