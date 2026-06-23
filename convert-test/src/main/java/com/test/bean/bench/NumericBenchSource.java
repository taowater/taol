package com.test.bean.bench;

import lombok.Data;

@Data
public class NumericBenchSource {
    private byte byteToLong;
    private byte byteToDouble;
    private short shortToInt;
    private short shortToLong;
    private short shortToFloat;
    private int intToLong;
    private int intToDouble;
    private int intToFloat;
    private long longToDouble;
    private long longToFloat;
    private long longToInt;
    private float floatToDouble;
    private Byte byteWrapToLong;
    private Integer intWrapToDouble;
    private Long longWrapToFloat;
    private Double doubleWrapToFloat;
    private Short shortWrapToDouble;
    private Integer intWrapToLong;
}
