package io.github.zap.party.list;

import io.github.zap.party.Party;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;

/**
 * Gets a list of {@link Component}s for a party.
 * This is used in the {@link io.github.zap.party.command.ListMembersForm}.
 */
@FunctionalInterface
public interface PartyLister {

    /**
     * Gets a collection of {@link Component}s for display.
     * @param party The party to get {@link Component}s for
     * @param locale The locale used to format the {@link Component}s
     * @return A collection of the display {@link Component}s
     */
    @NotNull Collection<Component> getPartyListComponents(@NotNull Party party, @NotNull Locale locale);

}
