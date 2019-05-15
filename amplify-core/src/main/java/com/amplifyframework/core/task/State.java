package com.amplifyframework.core.task;

/**
 * Generic enum for state
 */
public class State extends AmplifyEvent {

    private TaskState taskState;

    enum TaskState {
        WAITING,
        IN_PROGRESS,
        PAUSED,
        CANCELED,
        WAITING_FOR_NETWORK,
        COMPLETED,
        FAILED;
    }

    public TaskState getState() {
        return taskState;
    }
}
