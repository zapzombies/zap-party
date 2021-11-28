package io.github.zap.party.invitation;

import io.github.zap.party.Party;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.namer.OfflinePlayerNamer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Basic {@link InvitationManager} with timeouts.
 */
public class TimedInvitationManager implements InvitationManager {

    private final Map<UUID, Integer> invitationMap = new HashMap<>();

    private final Plugin plugin;

    private final OfflinePlayerNamer playerNamer;

    /**
     * Creates a basic invitation manager
     * @param plugin The plugin that owns this {@link InvitationManager}
     * @param playerNamer A namer for invitation messages
     */
    public TimedInvitationManager(@NotNull Plugin plugin, @NotNull OfflinePlayerNamer playerNamer) {
        this.plugin = plugin;
        this.playerNamer = playerNamer;
    }

    @Override
    public boolean hasInvitation(@NotNull OfflinePlayer player) {
        return this.invitationMap.containsKey(player.getUniqueId());
    }

    @Override
    public @NotNull Set<UUID> getInvitations() {
        return new HashSet<>(this.invitationMap.keySet());
    }

    @Override
    public void addInvitation(@NotNull Party party, @NotNull OfflinePlayer invitee, @NotNull OfflinePlayer inviter) {
        if (!party.hasMember(inviter)) {
            return;
        }

        Optional<PartyMember> ownerOptional = party.getOwner();
        if (ownerOptional.isEmpty()) {
            return;
        }

        double expirationTime = party.getPartySettings().getInviteExpirationTime() / 20F;

        OfflinePlayer partyOwner = ownerOptional.get().getOfflinePlayer();
        Player onlineInvitee = invitee.getPlayer();

        Component inviterComponent = this.playerNamer.name(inviter).colorIfAbsent(NamedTextColor.WHITE);
        Component inviteeComponent = this.playerNamer.name(invitee).colorIfAbsent(NamedTextColor.WHITE);
        Component ownerComponent = this.playerNamer.name(partyOwner).colorIfAbsent(NamedTextColor.WHITE);

        String ownerName = Objects.toString(partyOwner.getName());

        if (onlineInvitee != null) {
            Component here = Component.translatable("io.github.zap.party.invite.here", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(TextComponent.ofChildren(
                            Component.text("/party join ", NamedTextColor.YELLOW),
                            ownerComponent))
                    )
                    .clickEvent(ClickEvent.runCommand("/party join " + ownerName));
            if (partyOwner.equals(inviter)) {
                onlineInvitee.sendMessage(Component.translatable("io.github.zap.party.invite.received.personal",
                        NamedTextColor.YELLOW, inviterComponent, here,
                        Component.text(String.format("%.1f", expirationTime))));
            }
            else {
                onlineInvitee.sendMessage(Component.translatable("io.github.zap.party.invite.received.other",
                        NamedTextColor.YELLOW, inviterComponent, ownerComponent, here,
                        Component.text(String.format("%.1f", expirationTime))));
            }
        }

        party.broadcastMessage(Component.translatable("io.github.zap.party.invite.created",
                NamedTextColor.YELLOW, inviterComponent, inviteeComponent,
                Component.text(String.format("%.1f", expirationTime))));

        int taskId = this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.invitationMap.remove(invitee.getUniqueId());

            if (party.hasMember(invitee)) {
                return;
            }

            party.broadcastMessage(Component.translatable("io.github.zap.party.invite.to.expired",
                    NamedTextColor.YELLOW, inviteeComponent));

            Player newOnlineInvitee = this.plugin.getServer().getPlayer(invitee.getUniqueId());
            if (newOnlineInvitee != null && newOnlineInvitee.isOnline()) {
                newOnlineInvitee.sendMessage(Component.translatable("io.github.zap.party.invite.from.expired",
                        NamedTextColor.YELLOW, ownerComponent));
            }
        }, party.getPartySettings().getInviteExpirationTime()).getTaskId();
        this.invitationMap.put(invitee.getUniqueId(), taskId);
    }

    @Override
    public boolean removeInvitation(@NotNull OfflinePlayer player) {
        Integer taskId = this.invitationMap.remove(player.getUniqueId());
        if (taskId != null) {
            this.plugin.getServer().getScheduler().cancelTask(taskId);
            return true;
        }

        return false;
    }

    @Override
    public void cancelAllOutgoingInvitations() {
        Iterator<Integer> iterator = this.invitationMap.values().iterator();
        while (iterator.hasNext()) {
            Integer taskId = iterator.next();
            if (taskId != null) {
                this.plugin.getServer().getScheduler().cancelTask(taskId);
            }

            iterator.remove();
        }
    }

}
