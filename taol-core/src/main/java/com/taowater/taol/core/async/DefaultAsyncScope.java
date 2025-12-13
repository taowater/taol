package com.taowater.taol.core.async;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 默认的作用域
 */
@Builder
public class DefaultAsyncScope implements AsyncScope {

    @Getter
    private Executor executor;
    @Setter
    @Getter
    private long timeout;
    @Setter
    @Getter
    private TimeUnit unit;
    @Getter
    private boolean returnNullIfEx;

}
