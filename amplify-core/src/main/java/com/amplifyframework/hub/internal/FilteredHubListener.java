package com.amplifyframework.hub.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubFilter;
import com.amplifyframework.hub.HubListener;

import java.io.Serializable;
import java.util.UUID;

public class FilteredHubListener implements Serializable {
    private HubChannel channel;
    private UUID listenerId;
    private HubFilter hubFilter;
    private HubListener hubListener;

    public FilteredHubListener(@NonNull final HubChannel channel,
                               @NonNull final UUID listenerId,
                               @Nullable HubFilter hubFilter,
                               @Nullable final HubListener hubListener) {
        this.channel = channel;
        this.listenerId = listenerId;
        this.hubFilter = hubFilter;
        this.hubListener = hubListener;
    }

    public HubChannel getChannel() {
        return channel;
    }

    public FilteredHubListener setChannel(HubChannel channel) {
        this.channel = channel;
        return this;
    }

    public UUID getListenerId() {
        return listenerId;
    }

    public FilteredHubListener setListenerId(UUID listenerId) {
        this.listenerId = listenerId;
        return this;
    }

    public HubFilter getHubFilter() {
        return hubFilter;
    }

    public FilteredHubListener setHubFilter(HubFilter hubFilter) {
        this.hubFilter = hubFilter;
        return this;
    }

    public HubListener getHubListener() {
        return hubListener;
    }

    public FilteredHubListener setHubListener(HubListener hubListener) {
        this.hubListener = hubListener;
        return this;
    }
}
