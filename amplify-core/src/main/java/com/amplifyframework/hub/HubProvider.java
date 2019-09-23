package com.amplifyframework.hub;

import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.task.Result;

public interface HubProvider {
    void listen(HubChannel hubChannel, Callback<? extends Result> callback);

    void dispatch(HubChannel hubChannel, HubPayload hubpayload);

    void remove(HubChannel hubChannel, Callback<? extends Result> callback);
}
