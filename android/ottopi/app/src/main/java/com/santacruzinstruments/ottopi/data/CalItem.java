package com.santacruzinstruments.ottopi.data;

public class CalItem {
    final public MeasuredDataType type;
    final public float value;

    public CalItem(MeasuredDataType type, float value) {
        this.type = type;
        this.value = value;
    }
}
