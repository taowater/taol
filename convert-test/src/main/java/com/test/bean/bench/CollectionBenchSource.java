package com.test.bean.bench;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CollectionBenchSource {
    private List<Byte> bytesToLongs;
    private List<Short> shortsToInts;
    private List<Integer> intsToDoubles;
    private List<Long> longsToFloats;
    private List<Long> longsToInts;
    private Set<Short> setShortToListLong;
    private long[] longsToDoubles;
}
