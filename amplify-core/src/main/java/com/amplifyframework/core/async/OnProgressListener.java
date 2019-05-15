package com.amplifyframework.core.async;

import com.amplifyframework.core.task.Progress;

public interface OnProgressListener {
    void onProgressChanged(Progress progress);
}
