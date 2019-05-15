package com.amplifyframework.core.provider;

import android.support.annotation.NonNull;

import org.json.JSONObject;

public interface Provider {
    Category category = null;

    Category getCategory();

    String getProviderName();

    boolean isDefault();

    void setDefault(boolean isDefault);

    void configure(@NonNull JSONObject jsonObject);

    void configure(@NonNull JSONObject jsonObject, @NonNull String key);

    Provider initWithConfiguration(@NonNull JSONObject jsonObject);

    Provider initWithConfiguration(@NonNull JSONObject jsonObject, @NonNull String key);
}
