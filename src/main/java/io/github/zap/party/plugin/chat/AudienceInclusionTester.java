package io.github.zap.party.plugin.chat;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

/**
 * Tests whether an {@link Audience} should be included from party chat messages
 */
@FunctionalInterface
public interface AudienceInclusionTester {

    /**
     * Checks whether an {@link Audience} should be included from party chat messages.
     * @param audience The {@link Audience} to check
     * @return Whether the {@link Audience} should be included
     */
    boolean include(@NotNull Audience audience);

}
