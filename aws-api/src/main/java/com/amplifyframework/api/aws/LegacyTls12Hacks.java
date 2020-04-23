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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

/**
 * On APIs 16-20 (inclusive), addition work is needed to ensure the availability of TLSv1.2.
 * This is also reported to be necessary on some Samsung devices at API 21.
 * AWS endpoints require TLSv1.2. These hacks should be applied while initializing the plugin via
 * {@link AWSApiPlugin#initialize(Context)}.
 *
 * This code is factored with the intention of keeping all of the "hacks" in one place, and minimizing
 * their breadth of impact to the codebase, otherwise.
 *
 * @see <a href="https://developer.squareup.com/blog/okhttp-3-13-requires-android-5/">OkHttp 3.13 Release Notes</a>
 * @see <a href="https://github.com/square/okhttp/issues/2372#issuecomment-244807676">OkHttp Issue</a>
 * @see <a href="https://medium.com/tech-quizlet/working-with-tls-1-2-on-android-4-4-and-lower-f4f5205629a">Medium Article</a>
 */
@SuppressWarnings("checkstyle:LineLength") // JavaDoc links
final class LegacyTls12Hacks {
    private final Context context;

    private LegacyTls12Hacks(Context context) {
        this.context = context;
    }

    static LegacyTls12Hacks instance(@NonNull Context context) {
        return new LegacyTls12Hacks(context);
    }

    /**
     * Attempts to apply hacks to support TLSv1.2 on legacy clients.
     * The attempt is only made for Android API levels [16,22).
     * @param okHttp312xBuilder A {@link OkHttpClient.Builder} from version 3.12.x.
     * @throws ApiException On failure to apply Legacy TLSv1.2 hacks
     */
    void applyIfNeeded(OkHttpClient.Builder okHttp312xBuilder) throws ApiException {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            installTls12(context);
            enableTls120(okHttp312xBuilder);
        }
    }

    private static void installTls12(@NonNull Context context) throws ApiException {
        try {
            ProviderInstaller.installIfNeeded(context);
        } catch (GooglePlayServicesRepairableException error) {
            // Prompt the user to install/update/enable Google Play services.
            GoogleApiAvailability.getInstance()
                .showErrorNotification(context, error.getConnectionStatusCode());
        } catch (GooglePlayServicesNotAvailableException error) {
            // Indicates a non-recoverable error: let the user know.
            throw new ApiException(
                "On Android versions before Lollipop, Google Play Services are needed to support TLSv1.2.",
                error, "Ensure that Google Play Services are available on this device."
            );
        }
    }

    private static void enableTls120(OkHttpClient.Builder okHttp312xBuilder) throws ApiException {
        final X509TrustManager trustManager = findFirstX509TrustManager();
        okHttp312xBuilder
            .sslSocketFactory(createTls12SocketFactory(trustManager), trustManager)
            .connectionSpecs(Arrays.asList(
                new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build(),
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            ));
    }

    private static Tls12SocketFactory createTls12SocketFactory(TrustManager trustManager) throws ApiException {
        TrustManager[] trustManagers = Collections.singletonList(trustManager).toArray(new TrustManager[0]);
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance(TlsVersion.TLS_1_2.javaName());
            sslContext.init(null, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException tlsV12NotAvailable) {
            throw new ApiException(
                TlsVersion.TLS_1_2.javaName() + " is not available.",
                tlsV12NotAvailable, "If possible, update your Android distribution."
            );
        }
        return new Tls12SocketFactory(sslContext.getSocketFactory());
    }

    private static X509TrustManager findFirstX509TrustManager() throws ApiException {
        // Note: the algorithm needed here is PKIX (X509 or SunPKIX), SunX509, not TLSv1.2.
        final String defaultAlgorithm  = TrustManagerFactory.getDefaultAlgorithm();
        final TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
            trustManagerFactory.init((KeyStore) null);
        } catch (NoSuchAlgorithmException | KeyStoreException trustManagerError) {
            throw new ApiException(
                "Couldn't obtain trust manager for " + defaultAlgorithm + ".",
                trustManagerError, "Upgrade to Android Lollipop or better."
            );
        }
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof  X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new ApiException("No X509 Trust Manager available.", "Does your device support " + defaultAlgorithm);
    }

    /**
     * Enables TLS v1.2 when creating SSLSockets.
     * <p>
     * For some reason, android supports TLS v1.2 from API 16, but enables it by
     * default only from API 20.
     * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
     * @see SSLSocketFactory
     */
    public static final class Tls12SocketFactory extends SSLSocketFactory {
        private static final String[] TLS_V12_ONLY =
            Collections.singletonList(TlsVersion.TLS_1_2.javaName()).toArray(new String[0]);

        final SSLSocketFactory delegate;

        Tls12SocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(socket, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket socket) {
            if (socket instanceof SSLSocket) {
                ((SSLSocket) socket).setEnabledProtocols(TLS_V12_ONLY);
            }
            return socket;
        }
    }
}
