package com.example.aws_amplify_api_okhttp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.ApiPluginConfiguration;
import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.PluginException;

import okhttp3.OkHttpClient;

public class OkhttpApiPlugin implements ApiPlugin {

    private static final String TAG = OkhttpApiPlugin.class.getSimpleName();

    private Context context;

    private String endpointUrl;

    private OkHttpClient okHttpClient = new OkHttpClient(); //TODO: Make pluggable
    private IConverter converter = new Converter();

    public OkhttpApiPlugin(@NonNull Context context){
        this.context = context;

        Log.d(TAG, "OkHTTP API Plugin is initialized.");
    }

    @Override
    public CategoryType getCategoryType() {
        return null;
    }

    @Override
    public String getPluginKey() {
        return null;
    }

    @Override
    public void configure(@NonNull ApiPluginConfiguration pluginConfiguration) throws PluginException {

    }

    @Override
    public void configure(@NonNull Context context, @NonNull String environment) throws PluginException {

    }

    @Override
    public void reset() {

    }

    @Override
    public GraphQLQuery query(@NonNull String query) {
        return null;
    }

    // Might not have this as a builder later, need to revisit
    public static class Builder {
        private OkhttpApiPlugin okhttpApiPlugin;

        public Builder() { okhttpApiPlugin = new OkhttpApiPlugin(this.okhttpApiPlugin.context); }

        public OkhttpApiPlugin build() { return okhttpApiPlugin; }

        public Builder converter(IConverter converter){
            okhttpApiPlugin.converter = converter;
            return this;
        }

        public Builder EndpointUrl(String endpointUrl){
            okhttpApiPlugin.endpointUrl = endpointUrl;
            return this;
        }
    }
}
