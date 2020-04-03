/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.reachability;

import android.net.Uri;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

/**
 * A Host whose reachability is determined by opening an Internet socket against
 * a hostname / port number pair.
 */
@SuppressWarnings("unused")
public final class SocketHost implements Host {
    private static final int CONNECTION_TIMEOUT_MS = 1_000;

    private final String host;
    private final int port;

    private SocketHost(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Creates a SocketHost from a URI String. The URI must contain
     * at least a host portion and port portion, e.g. "amazon.com:80". {@link #isReachable()}
     * will use this information to open a socket connection, to determine if the host
     * is reachable.
     * @param hostUri A URI as String, containing a host and port
     * @return A SocketHost
     */
    @NonNull
    public static SocketHost from(@NonNull final String hostUri) {
        final Uri uri = Uri.parse(hostUri);
        return from(Objects.requireNonNull(uri.getHost()), uri.getPort());
    }

    /**
     * Creates a SocketHost from a host name/IP String and a port.
     * {@link #isReachable()} will use this information to open a socket connection
     * to determine reachability.
     * @param host Name/IP string for a host
     * @param port Port for the host
     * @return A SocketHost
     */
    @NonNull
    public static SocketHost from(@NonNull final String host, final int port) {
        return new SocketHost(
            Objects.requireNonNull(host),
            Port.requireWithinRange(port)
        );
    }

    @Override
    public boolean isReachable() {
        final Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            socket.close();
            return true;
        } catch (final IOException socketFailure) {
            return false;
        }
    }

    private static final class Port {
        private static final int MIN_PORT = 1;
        private static final int MAX_PORT = 65_535;

        private Port() {}

        static int requireWithinRange(int port) {
            if (port < MIN_PORT || port > MAX_PORT) {
                throw new IllegalArgumentException(String.format(
                    "Port %d is out of range [%d,%d].", port, MIN_PORT, MAX_PORT
                ));
            }
            return port;
        }
    }
}
