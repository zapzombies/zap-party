package io.github.zap.party.plugin.chat;

import io.github.zap.party.Party;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.tracker.PartyTracker;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

/**
 * Basic implementation of a {@link AsyncChatHandler}.
 */
public class BasicAsyncChatHandler implements AsyncChatHandler {

    public final static AudienceInclusionTester DEFAULT_AUDIENCE_INCLUSTION_TESTER = audience -> false;

    public final static Collection<Audience> DEFAULT_INCLUDED_AUDIENCES = Collections.emptySet();

    private final Plugin plugin;

    private final PartyTracker partyTracker;

    private final Component partyPrefix;

    private final AudienceInclusionTester inclusionTester;

    private final Collection<Audience> includedAudiences;

    /**
     * Creates a simple party chat handler that deals with parties being muted and party chat along with the ability to
     * include certain audiences for party chat messages and test if certain audiences may be included.
     * @param plugin The plugin this chat handler belongs to
     * @param partyTracker A tracker for parties to handle {@link AsyncChatEvent}s with
     * @param partyPrefix A prefix for party chat messages
     * @param inclusionTester The tester for {@link Audience} that will receive party chat messages
     * @param includedAudiences {@link Audience}s to add to party chat messages
     */
    public BasicAsyncChatHandler(@NotNull Plugin plugin, @NotNull PartyTracker partyTracker,
                                 @NotNull Component partyPrefix, @NotNull AudienceInclusionTester inclusionTester,
                                 @NotNull Collection<Audience> includedAudiences) {
        this.plugin = plugin;
        this.partyTracker = partyTracker;
        this.partyPrefix = partyPrefix;
        this.inclusionTester = inclusionTester;
        this.includedAudiences = includedAudiences;
    }

    /**
     * Creates a simple party chat handler that deals with parties being muted and party chat along with the ability to
     * test if certain audiences may be included.
     * @param plugin The plugin this chat handler belongs to
     * @param partyTracker A tracker for parties to handle {@link AsyncChatEvent}s with
     * @param partyPrefix A prefix for party chat messages
     * @param inclusionTester The tester for {@link Audience} that will receive party chat messages
     */
    @SuppressWarnings("unused")
    public BasicAsyncChatHandler(@NotNull Plugin plugin, @NotNull PartyTracker partyTracker,
                                 @NotNull Component partyPrefix, @NotNull AudienceInclusionTester inclusionTester) {
        this(plugin, partyTracker, partyPrefix, inclusionTester, DEFAULT_INCLUDED_AUDIENCES);
    }

    /**
     * Creates a simple party chat handler that deals with parties being muted and party chat along with the ability to
     * include certain audiences for party chat messages.
     * @param plugin The plugin this chat handler belongs to
     * @param partyTracker A tracker for parties to handle {@link AsyncChatEvent}s with
     * @param partyPrefix A prefix for party chat messages
     * @param includedAudiences {@link Audience}s to add to party chat messages
     */
    public BasicAsyncChatHandler(@NotNull Plugin plugin, @NotNull PartyTracker partyTracker,
                                 @NotNull Component partyPrefix, @NotNull Collection<Audience> includedAudiences) {
        this(plugin, partyTracker, partyPrefix, DEFAULT_AUDIENCE_INCLUSTION_TESTER, includedAudiences);
    }

    /**
     * Creates a simple party chat handler that deals with parties being muted and party chat.
     * @param plugin The plugin this chat handler belongs to
     * @param partyTracker A tracker for parties to handle {@link AsyncChatEvent}s with
     * @param partyPrefix A prefix for party chat messages
     */
    public BasicAsyncChatHandler(@NotNull Plugin plugin, @NotNull PartyTracker partyTracker,
                                 @NotNull Component partyPrefix) {
        this(plugin, partyTracker, partyPrefix, DEFAULT_AUDIENCE_INCLUSTION_TESTER, DEFAULT_INCLUDED_AUDIENCES);
    }

    @EventHandler
    @Override
    public void onAsyncChat(@NotNull AsyncChatEvent event) {
        Optional<Party> partyOptional = this.partyTracker.getPartyForPlayer(event.getPlayer());
        if (partyOptional.isEmpty()) {
            return;
        }
        Party party = partyOptional.get();

        Optional<PartyMember> optionalPartyMember = party.getMember(event.getPlayer());
        if (optionalPartyMember.isEmpty()) {
            return;
        }

        PartyMember partyMember = optionalPartyMember.get();
        if (!partyMember.isInPartyChat()) {
            return;
        }

        if (partyMember.isMuted()) {
            event.getPlayer().sendMessage(Component.translatable("io.github.zap.party.chat.member.muted",
                    NamedTextColor.RED));
            event.setCancelled(true);
        }
        else if (party.getPartySettings().isMuted() && !party.isOwner(event.getPlayer())) {
            event.getPlayer().sendMessage(Component.translatable("io.github.zap.party.chat.muted",
                    NamedTextColor.RED));
            event.setCancelled(true);
        }
        else {
            Iterator<Audience> iterator = event.viewers().iterator();
            while (iterator.hasNext()) {
                Audience audience = iterator.next();
                if (this.inclusionTester.include(audience)
                        || (!(audience instanceof Player player && party.hasMember(player)))) {
                    try {
                        iterator.remove();
                    }
                    catch (UnsupportedOperationException e) {
                        this.plugin.getLogger().warning("Could not prevent sending a party chat message to " +
                                audience + " from " + event.getPlayer().getName() + " due to an event being called " +
                                "which does not support audience removal!");
                    }
                }
            }

            for (Audience audience : this.includedAudiences) {
                try {
                    event.viewers().add(audience);
                }
                catch (UnsupportedOperationException e) {
                    this.plugin.getLogger().warning("Could not add an audience to the party chat message to " +
                            audience + " from " + event.getPlayer().getName() + " due to an event being called " +
                            "which does not support audience addition!");
                }
            }

            ChatRenderer oldRenderer = event.renderer();
            event.renderer((source, sourceDisplayName, message, viewer) ->
                    Component.translatable("io.github.zap.party.chat.message.format", this.partyPrefix,
                            oldRenderer.render(source, sourceDisplayName, message, viewer)));
        }
    }

}
