/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie.helm.misc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * NextSafeAvailablePort.
 *
 * @since 0.1
 */
public class NextSafeAvailablePort {

    /**
     * The minimum number of server port number as first non-privileged port.
     */
    private static final int MIN_PORT = 1024;

    /**
     * The maximum number of server port number.
     */
    private static final int MAX_PORT = 49_151;

    /**
     * The first and minimum port to scan for availability.
     */
    private final int from;

    /**
     * Ctor.
     */
    public NextSafeAvailablePort() {
        this(NextSafeAvailablePort.MIN_PORT);
    }

    /**
     * Ctor.
     *
     * @param from Port to start scan from
     */
    public NextSafeAvailablePort(final int from) {
        this.from = from;
    }

    /**
     * Gets the next available port starting at a port.
     *
     * @return Next available port
     * @throws IllegalArgumentException if there are no ports available
     */
    public int value() {
        if (this.from < NextSafeAvailablePort.MIN_PORT
            || this.from > NextSafeAvailablePort.MAX_PORT) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid start port: %d", this.from
                )
            );
        }
        for (int port = this.from; port <= NextSafeAvailablePort.MAX_PORT; port += 1) {
            if (available(port)) {
                return port;
            }
        }
        throw new IllegalArgumentException(
            String.format(
                "Could not find an available port above %d", this.from
            )
        );
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port The port to check for availability
     * @return If the ports is available
     * @checkstyle ReturnCountCheck (50 lines)
     * @checkstyle FinalParametersCheck (50 lines)
     * @checkstyle EmptyCatchBlock (50 lines)
     * @checkstyle MethodBodyCommentsCheck (50 lines)
     * @checkstyle RegexpSinglelineCheck (50 lines)
     */
    @SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.OnlyOneReturn"})
    private static boolean available(final int port) {
        ServerSocket sersock = null;
        DatagramSocket dgrmsock = null;
        try {
            sersock = new ServerSocket(port);
            sersock.setReuseAddress(true);
            dgrmsock = new DatagramSocket(port);
            dgrmsock.setReuseAddress(true);
            return true;
        } catch (IOException exp) {
            // should not be thrown
        } finally {
            if (dgrmsock != null) {
                dgrmsock.close();
            }
            if (sersock != null) {
                try {
                    sersock.close();
                } catch (IOException exp) {
                    // should not be thrown
                }
            }
        }
        return false;
    }
}
