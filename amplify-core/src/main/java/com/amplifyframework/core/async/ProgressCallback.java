package com.amplifyframework.core.async;

public interface ProgressCallback<PR, Result> extends Callback<Result> {
    void onProgress(PR progress);
}