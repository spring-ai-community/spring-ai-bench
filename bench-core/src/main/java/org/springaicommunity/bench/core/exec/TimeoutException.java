/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.bench.core.exec;

import java.time.Duration;

import org.springaicommunity.bench.core.exec.sandbox.Sandbox;

/**
 * Exception thrown when a command execution exceeds its specified timeout.
 * <p>
 * This exception is thrown by {@link Sandbox} implementations when a command
 * takes longer than the timeout specified in {@link ExecSpec#timeout()}.
 */
public class TimeoutException extends RuntimeException {

    private final Duration timeout;
    private final Duration elapsed;

    /**
     * Creates a timeout exception with a simple message.
     *
     * @param message the detail message
     */
    public TimeoutException(String message) {
        super(message);
        this.timeout = null;
        this.elapsed = null;
    }

    /**
     * Creates a timeout exception with timeout details.
     *
     * @param message the detail message
     * @param timeout the timeout duration that was exceeded
     */
    public TimeoutException(String message, Duration timeout) {
        super(message);
        this.timeout = timeout;
        this.elapsed = null;
    }

    /**
     * Creates a timeout exception with timeout and elapsed time details.
     *
     * @param message the detail message
     * @param timeout the timeout duration that was exceeded
     * @param elapsed the actual elapsed time when timeout occurred
     */
    public TimeoutException(String message, Duration timeout, Duration elapsed) {
        super(message);
        this.timeout = timeout;
        this.elapsed = elapsed;
    }

    /**
     * Creates a timeout exception with a cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
        this.timeout = null;
        this.elapsed = null;
    }

    /**
     * Gets the timeout duration that was exceeded, if available.
     *
     * @return the timeout duration, or null if not provided
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Gets the elapsed time when the timeout occurred, if available.
     *
     * @return the elapsed duration, or null if not provided
     */
    public Duration getElapsed() {
        return elapsed;
    }

    /**
     * Checks if timeout details are available.
     *
     * @return true if timeout duration is available
     */
    public boolean hasTimeoutDetails() {
        return timeout != null;
    }

    /**
     * Creates a formatted message including timeout details if available.
     *
     * @return formatted timeout message
     */
    public String getDetailedMessage() {
        if (timeout == null) {
            return getMessage();
        }

        StringBuilder sb = new StringBuilder(getMessage());
        sb.append(" (timeout: ").append(timeout);

        if (elapsed != null) {
            sb.append(", elapsed: ").append(elapsed);
        }

        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getDetailedMessage();
    }
}