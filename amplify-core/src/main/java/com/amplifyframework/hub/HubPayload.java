package com.amplifyframework.hub;

public class HubPayload {
    private String event;
    private Object data;
    private String message;

    public HubPayload(String event, Object data, String message) {
        this.event = event;
        this.data = data;
        this.message = message;
    }
}
