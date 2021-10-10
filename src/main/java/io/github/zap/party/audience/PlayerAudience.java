package io.github.zap.party.audience;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * An {@link Audience} that sends messages to a {@link Player} if they are online
 * from an original {@link Player} instance.
 */
public class PlayerAudience implements Audience {

    private final Server server;

    private final UUID playerUUID;

    public PlayerAudience(@NotNull Player player) {
        this.server = player.getServer();
        this.playerUUID = player.getUniqueId();
    }

    @Override
    public void sendMessage(final @NonNull Identity source, final @NonNull Component message,
                            final @NonNull MessageType type) {
        Player player = this.server.getPlayer(this.playerUUID);
        if (player != null) {
            player.sendMessage(source, message, type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerAudience that = (PlayerAudience) o;
        return this.playerUUID.equals(that.playerUUID);
    }

    @Override
    public int hashCode() {
        return this.playerUUID.hashCode();
    }

}
