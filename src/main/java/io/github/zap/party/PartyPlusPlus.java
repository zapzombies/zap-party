package io.github.zap.party;

import io.github.zap.party.tracker.PartyTracker;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Creates cool parties for you!
 */
public interface PartyPlusPlus extends Plugin {

    /**
     * Gets the plugin's {@link PartyTracker}
     * @return The {@link PartyTracker}
     */
    @NotNull PartyTracker getPartyTracker();

}
