package com.taowater.core.cache;

import com.taowater.taol.core.cache.Cache;
import org.junit.jupiter.api.Test;

public class CacheTest {

    @Test
    void test() throws InterruptedException {
        Cache<String, String> cache = new Cache<>(5);
        cache.computeIfAbsent("key", k -> "value");
        cache.computeIfAbsent("key2", k -> "value2");
        cache.computeIfAbsent("key3", k -> "value3");
        cache.computeIfAbsent("key4", k -> "value4");
        cache.computeIfAbsent("key5", k -> "value5");
        Thread.sleep(2000);
        cache.computeIfAbsent("key6", k -> "value6");
        cache.computeIfAbsent("key7", k -> "value7");
        cache.computeIfAbsent("key8", k -> "value8");
        cache.computeIfAbsent("key3", k -> "value8");
        cache.computeIfAbsent("key4", k -> "value8");
        cache.computeIfAbsent("key2", k -> "value8");


        System.out.println(1234);
    }

}
