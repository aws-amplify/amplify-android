package com.example.aws_amplify_api_okhttp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amplifyframework.api.ApiPlugin;
import com.amplifyframework.api.ApiPluginConfiguration;
import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.PluginException;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkhttpApiPlugin implements ApiPlugin {

    private static final String TAG = OkhttpApiPlugin.class.getSimpleName();

    private Context context;

    private String endpointUrl ="https://farh6dgdobcxxdgcf43bcmmzby.appsync-api.us-east-1.amazonaws.com/graphql";

    private OkHttpClient okHttpClient;
    private Converter converter = new Converter(new Gson());

    public OkhttpApiPlugin(){
        //this.context = context;

        final String apiKey = "da2-7ieky2cqmjg5licjudrwd2ffjm";

        OkHttpClient.Builder okClient = new OkHttpClient.Builder();

        okClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("x-api-key", apiKey)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        OkHttpClient okHttpClient = okClient.build();
        this.okHttpClient = okHttpClient;

        Log.d(TAG, "OkHTTP API Plugin is initialized.");
    }

    @Override
    public CategoryType getCategoryType() {
        return CategoryType.API;
    }

    @Override
    public String getPluginKey() {
        return "okAPI";
    }

    @Override
    public void configure(@NonNull ApiPluginConfiguration pluginConfiguration) throws PluginException {
        Log.i(TAG, "OkhttpAPIPlugin configured");
    }

    @Override
    public void configure(@NonNull Context context) throws PluginException {

    }

    @Override
    public void reset() {

    }

    public Query<String> graphql(@NonNull String query) {
        Log.i(TAG, "Invoking query from plugin " + query);
        return new Query<>(this, query);
    }

    //Need to figure out our
    @Override
    public GraphQLQuery query(@NonNull String query) {
        return new GraphQLQuery();
    }

    protected <T> void enqueue(final AbstractQuery abstractQuery) {
        try {
            okHttpClient.newCall(
                    new Request.Builder()
                            .url(endpointUrl)
                            .addHeader("accept", "application/json")
                            .addHeader("content-type", "application/json")
                            .post(
                                    RequestBody.create(MediaType.parse("application/json"), abstractQuery.getContent())
                            )
                            .build())
                    .enqueue(new okhttp3.Callback() {
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            final String json = response.body().string();
                            Log.i("OKGraphQL", abstractQuery.getContent());
                            abstractQuery.onResponse(converter, json);
                        }

                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            abstractQuery.onError(e);
                        }
                    });
        } catch (Exception e) {
            abstractQuery.onError(e);
        }
    }
}
