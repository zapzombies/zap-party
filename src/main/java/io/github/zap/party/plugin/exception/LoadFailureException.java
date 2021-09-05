package io.github.zap.party.plugin.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when an object required for a plugin was unable to load correct due to some exceptional condition.
 */
public class LoadFailureException extends Exception {

    /**
     * Creates an exception with just a message
     * @param message The message
     */
    public LoadFailureException(@NotNull String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause
     * @param message The message
     * @param cause The cause
     */
    public LoadFailureException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

}
