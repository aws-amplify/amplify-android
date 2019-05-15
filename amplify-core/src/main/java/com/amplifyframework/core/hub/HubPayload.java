package com.amplifyframework.core.hub;

import com.amplifyframework.core.exception.AmplifyException;
import com.amplifyframework.core.task.Result;

public class HubPayload {
    private HubEvent hubEvent;
    private Result result;
    private AmplifyException amplifyException;
}
