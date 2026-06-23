package com.test.bean.bench;

import lombok.Data;

import java.util.List;

@Data
public class CollectionBenchTarget {
    private List<Long> bytesToLongs;
    private List<Integer> shortsToInts;
    private List<Double> intsToDoubles;
    private List<Float> longsToFloats;
    private List<Integer> longsToInts;
    private List<Long> setShortToListLong;
    private double[] longsToDoubles;
}
