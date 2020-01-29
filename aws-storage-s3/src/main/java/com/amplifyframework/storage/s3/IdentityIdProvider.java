package com.amplifyframework.storage.s3;

import androidx.annotation.Nullable;

public interface IdentityIdProvider {
    /**
     * Get an Identity ID (what on earth?)
     * @return An identity ID (TODO: what?)
     */
    @Nullable // is it?
    String getIdentityId();
}
