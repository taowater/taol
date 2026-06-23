package com.test.bean.bench;

import lombok.Data;

@Data
public class NumericBenchTarget {
    private long byteToLong;
    private double byteToDouble;
    private int shortToInt;
    private long shortToLong;
    private float shortToFloat;
    private long intToLong;
    private double intToDouble;
    private float intToFloat;
    private double longToDouble;
    private float longToFloat;
    private int longToInt;
    private double floatToDouble;
    private Long byteWrapToLong;
    private Double intWrapToDouble;
    private Float longWrapToFloat;
    private Float doubleWrapToFloat;
    private Double shortWrapToDouble;
    private Long intWrapToLong;
}
