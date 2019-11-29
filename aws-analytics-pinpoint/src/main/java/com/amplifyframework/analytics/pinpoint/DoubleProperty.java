package com.amplifyframework.analytics.pinpoint;

import com.amplifyframework.analytics.Property;

public final class DoubleProperty implements Property<Double> {
    Double value;
    DoubleProperty(Double value){
        this.value = value;
    }
    @Override
    public Double getValue() {
        return value;
    }

    public DoubleProperty of(Double value) {
        return new DoubleProperty(value);
    }
}
