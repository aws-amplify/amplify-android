package com.amplifyframework.api;

import android.support.annotation.NonNull;
import android.util.Log;

public interface Callback<T> {
    void onResponse(@NonNull T data);

    //Added JavaVersion.VERSION_1_8 in Gradle to support @default
    default void onError(@NonNull Throwable error){
        Log.e("API_ERROR", error.toString());
    }
}
