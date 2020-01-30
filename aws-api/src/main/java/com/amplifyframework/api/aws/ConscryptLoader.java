/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;

import org.conscrypt.Conscrypt;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

/**
 * Load Conscrypt Provider into Java Security system.
 * https://github.com/square/okhttp#requirements
 * https://github.com/google/conscrypt#android
 */
final class ConscryptLoader {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-api");
    private static boolean installed = false;

    @SuppressWarnings("checkstyle:all") private ConscryptLoader() {}

    static void load() {
        if (!installed) {
            synchronized (Security.class) {
                synchronized (Conscrypt.class) {
                    installConscrypt();
                }
            }
        }
    }

    private static void installConscrypt() {
        // If conscrypt isn't available, we're done. Nothing to install.
        if (!Conscrypt.isAvailable()) {
            return;
        }
        // Remove any existing installations.
        for (Provider provider : findInstalledConscryptProviders()) {
            Security.removeProvider(provider.getName());
        }
        // Now, install Conscrypt.
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        installed = true;
        LOG.info("Installed Conscrypt security provider.");
    }

    @NonNull
    private static List<Provider> findInstalledConscryptProviders() {
        final List<Provider> conscrypts = new ArrayList<>();
        for (Provider provider : Security.getProviders()) {
            if (Conscrypt.isConscrypt(provider)) {
                conscrypts.add(provider);
            }
        }
        return conscrypts;
    }
}
