package io.github.zap.party.list;

import io.github.zap.party.Party;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.namer.OfflinePlayerNamer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Basic implementation of a {@link PartyLister}.
 */
@SuppressWarnings("ClassCanBeRecord")
public class BasicPartyLister implements PartyLister {

    private final Plugin plugin;

    private final OfflinePlayerNamer onlineMemberNamer;

    private final OfflinePlayerNamer offlineMemberNamer;

    private final OfflinePlayerNamer invitedNamer;

    /**
     * Creates a simple party lister.
     * @param plugin The plugin that this party lister belongs to
     * @param onlineMemberNamer A namer for online members
     * @param offlineMemberNamer A namer for offline members
     * @param invitedNamer A namer for invited players
     */
    public BasicPartyLister(@NotNull Plugin plugin, @NotNull OfflinePlayerNamer onlineMemberNamer,
                            @NotNull OfflinePlayerNamer offlineMemberNamer, @NotNull OfflinePlayerNamer invitedNamer) {
        this.plugin = plugin;
        this.onlineMemberNamer = onlineMemberNamer;
        this.offlineMemberNamer = offlineMemberNamer;
        this.invitedNamer = invitedNamer;
    }

    @Override
    public @NotNull Collection<Component> getPartyListComponents(@NotNull Party party, @NotNull Locale locale) {
        Component colon = Component.translatable("io.github.zap.party.list.colon", NamedTextColor.WHITE);
        Component onlinePrefix = Component.translatable("io.github.zap.party.list.prefix.format",
                Component.translatable("io.github.zap.party.list.online", NamedTextColor.GREEN),
                colon);
        Component offlinePrefix = Component.translatable("io.github.zap.party.list.prefix.format",
                Component.translatable("io.github.zap.party.list.offline", NamedTextColor.RED),
                colon);
        Component invitesPrefix = Component.translatable("io.github.zap.party.list.prefix.format",
                Component.translatable("io.github.zap.party.list.invites", NamedTextColor.BLUE),
                colon);

        Collection<PartyMember> memberCollection = party.getMembers();
        List<Component> onlinePlayers = new ArrayList<>(memberCollection.size());
        List<Component> offlinePlayers = new ArrayList<>(memberCollection.size());
        List<Component> invitedPlayers = new ArrayList<>(party.getInvitationManager().getInvitations().size());

        for (PartyMember member : memberCollection) {
            OfflinePlayer offlinePlayer = member.getOfflinePlayer();
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayers.add(this.onlineMemberNamer.name(onlinePlayer));
                plugin.getLogger().info(this.onlineMemberNamer.name(onlinePlayer).toString());
            } else {
                offlinePlayers.add(this.offlineMemberNamer.name(offlinePlayer));
            }
        }

        for (UUID uuid : party.getInvitationManager().getInvitations()) {
            invitedPlayers.add(this.invitedNamer.name(this.plugin.getServer().getOfflinePlayer(uuid)));
        }

        Component online = Component.translatable("io.github.zap.party.list.format", onlinePrefix,
                ListFormatUtil.list(locale, onlinePlayers));
        Component offline = Component.translatable("io.github.zap.party.list.format", offlinePrefix,
                ListFormatUtil.list(locale, offlinePlayers));
        Component invites = Component.translatable("io.github.zap.party.list.format", invitesPrefix,
                ListFormatUtil.list(locale, invitedPlayers));

        return List.of(online, offline, invites);
    }

}
