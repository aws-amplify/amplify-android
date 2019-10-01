package com.example.aws_amplify_api_okhttp;

import android.support.annotation.NonNull;
import android.util.Log;

public interface Callback<T> {
    void onResponse(@NonNull T data);
    void onError(Throwable error);
}
