package com.amazonaws.amplifysample;

import android.support.annotation.NonNull;

import com.amplifyframework.api.GraphQLApiPlugin;
import com.amplifyframework.core.plugin.PluginException;

public class MockGqlClient extends GraphQLApiPlugin {
    @Override
    public void query() {

    }

    @Override
    public void mutate() {

    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public String getPluginKey() {
        return "MockGQL";
    }

    @Override
    public void configure(@NonNull Object pluginConfiguration) throws PluginException {

    }

    @Override
    public Object getEscapeHatch() {
        return null;
    }
}
