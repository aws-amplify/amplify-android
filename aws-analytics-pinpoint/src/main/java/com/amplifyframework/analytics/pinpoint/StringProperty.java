package com.amplifyframework.analytics.pinpoint;

import com.amplifyframework.analytics.Property;

public final class StringProperty implements Property<String> {
    String value;
    StringProperty(String value){
        this.value = value;
    }
    @Override
    public String getValue() {
        return value;
    }

    public StringProperty of(String value) {
        return new StringProperty(value);
    }
}
