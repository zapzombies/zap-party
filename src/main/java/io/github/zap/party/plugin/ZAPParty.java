package io.github.zap.party.plugin;

import io.github.zap.party.tracker.PartyTracker;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Creates cool parties for you!
 */
public interface ZAPParty extends Plugin {

    /**
     * Gets the plugin's {@link PartyTracker}
     * @return The {@link PartyTracker}
     */
    @NotNull PartyTracker getPartyTracker();

}
