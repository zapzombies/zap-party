package io.github.zap.party.plugin.chat;

import io.github.zap.party.member.PartyMember;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Handles chat from {@link PartyMember}s.
 */
@FunctionalInterface
public interface AsyncChatHandler extends Listener {

    /**
     * Called when a player chats.
     * @param event The chat event
     */
    @EventHandler
    void onAsyncChat(@NotNull AsyncChatEvent event);

}
