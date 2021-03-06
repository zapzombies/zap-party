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

    private final Plugin plugin;

    private final PartyTracker partyTracker;

    private final Component partyPrefix;

    private final Component spyPartyPrefix;

    /**
     * Creates a simple party chat handler that deals with parties being muted and party chat along with the ability to
     * include certain audiences for party chat messages and test if certain audiences may be included.
     * @param plugin The plugin this chat handler belongs to
     * @param partyTracker A tracker for parties to handle {@link AsyncChatEvent}s with
     * @param partyPrefix A prefix for party chat messages
     * @param spyPartyPrefix A prefix for spied party chat messages
     */
    public BasicAsyncChatHandler(@NotNull Plugin plugin, @NotNull PartyTracker partyTracker,
                                 @NotNull Component partyPrefix, @NotNull Component spyPartyPrefix) {
        this.plugin = plugin;
        this.partyTracker = partyTracker;
        this.partyPrefix = partyPrefix;
        this.spyPartyPrefix = spyPartyPrefix;
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
                if (!(audience instanceof Player player && party.hasMember(player))) {
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

            for (Audience audience : party.getSpyAudiences()) {
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
            event.renderer((source, sourceDisplayName, message, viewer) -> {
                if (party.getSpyAudiences().contains(viewer)) {
                    return Component.translatable("io.github.zap.party.chat.message.format", this.spyPartyPrefix,
                            oldRenderer.render(source, sourceDisplayName, message, viewer));
                }
                else {
                    return Component.translatable("io.github.zap.party.chat.message.format", this.partyPrefix,
                            oldRenderer.render(source, sourceDisplayName, message, viewer));
                }
            });
        }
    }

}
