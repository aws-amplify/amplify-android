package com.amazonaws.amplifysample;

import android.support.annotation.NonNull;

import com.amplifyframework.api.RestApiPlugin;
import com.amplifyframework.core.plugin.PluginException;

public class MockRestClient extends RestApiPlugin {
    @Override
    public void get() {

    }

    @Override
    public void put() {

    }

    @Override
    public void post() {

    }

    @Override
    public void patch() {

    }

    @Override
    public void delete() {

    }

    @Override
    public String getPluginKey() {
        return "MockRest";
    }

    @Override
    public void configure(@NonNull Object pluginConfiguration) throws PluginException {

    }

    @Override
    public Object getEscapeHatch() {
        return null;
    }
}
