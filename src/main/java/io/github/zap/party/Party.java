package io.github.zap.party;

import com.ibm.icu.text.PluralRules;
import io.github.zap.party.audience.PlayerAudience;
import io.github.zap.party.invitation.InvitationManager;
import io.github.zap.party.list.PartyLister;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.member.PartyMemberBuilder;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.settings.PartySettings;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A group of players which can join games together and chat together.
 */
public class Party {

    private final UUID uuid = UUID.randomUUID();

    private final Map<UUID, PartyMember> members = new HashMap<>();

    private final List<Consumer<PartyMember>> partyJoinHandlers = new ArrayList<>();

    private final List<Consumer<PartyMember>> partyLeaveHandlers = new ArrayList<>();

    private final Random random;

    private final PartySettings partySettings;

    private final PartyMemberBuilder partyMemberBuilder;

    private final InvitationManager invitationManager;

    private final List<Audience> spyAudiences;

    private final PartyLister partyLister;

    private final OfflinePlayerNamer playerNamer;

    private PartyMember owner;

    /**
     * Creates a party.
     * @param random A {@link Random} instance used for random selections in parties
     * @param owner The owner of the party
     * @param partySettings The settings for the party
     * @param partyMemberBuilder A builder for new party members
     * @param invitationManager The invitation manager for this party
     * @param spyAudiences A {@link List} of {@link Audience}s that will spy on any party events
     * @param partyLister A lister for party list components
     * @param playerNamer A namer for {@link Component} names of players
     */
    public Party(@NotNull Random random, @NotNull PartyMember owner, @NotNull PartySettings partySettings,
                 @NotNull PartyMemberBuilder partyMemberBuilder, @NotNull InvitationManager invitationManager,
                 @NotNull List<Audience> spyAudiences, @NotNull PartyLister partyLister,
                 @NotNull OfflinePlayerNamer playerNamer) {
        this.random = random;
        this.owner = owner;
        this.partySettings = partySettings;
        this.partyMemberBuilder = partyMemberBuilder;
        this.invitationManager = invitationManager;
        this.spyAudiences = spyAudiences;
        this.partyLister = partyLister;
        this.playerNamer = playerNamer;

        this.members.put(owner.getOfflinePlayer().getUniqueId(), owner);
    }

    /**
     * Registers a handler to be called when a player joins the party
     * @param joinHandler The handler to add
     */
    public void registerJoinHandler(@NotNull Consumer<PartyMember> joinHandler) {
        this.partyJoinHandlers.add(joinHandler);
    }

    /**
     * Registers a handler to be called when a player leaves the party
      * @param leaveHandler The handler to add
     */
    public void registerLeaveHandler(@NotNull Consumer<PartyMember> leaveHandler) {
        this.partyLeaveHandlers.add(leaveHandler);
    }

    /**
     * Adds a member to the party
     * @param player The new player
     * @return An optional of the party member that is present if the member is new
     */
    public @NotNull Optional<PartyMember> addMember(@NotNull Player player) {
        UUID memberUUID = player.getUniqueId();

        if (this.members.containsKey(memberUUID)) {
            return Optional.empty();
        }

        PartyMember partyMember = this.partyMemberBuilder.createPartyMember(player);
        this.members.put(memberUUID, partyMember);
        this.invitationManager.removeInvitation(player);
        this.spyAudiences.remove(new PlayerAudience(player));

        this.broadcastMessage(Component.translatable("io.github.zap.party.member.joined", NamedTextColor.YELLOW,
                player.displayName().colorIfAbsent(NamedTextColor.WHITE)));

        for (Consumer<PartyMember> handler : this.partyJoinHandlers) {
            handler.accept(partyMember);
        }

        return Optional.of(partyMember);
    }

    /**
     * Removes a member from the party
     * @param player The player to remove
     */
    public void removeMember(@NotNull OfflinePlayer player, boolean forced) {
        if (!this.members.containsKey(player.getUniqueId())) {
            return;
        }

        PartyMember removed = this.members.remove(player.getUniqueId());

        Component name = this.playerNamer.name(player).colorIfAbsent(NamedTextColor.WHITE);

        boolean clearHandlers = false;
        if (this.owner.equals(removed)) {
            chooseNewOwner();

            if (this.owner != null) {
                this.broadcastMessage(Component.translatable("io.github.zap.party.transferred",
                        NamedTextColor.YELLOW, name,
                        this.playerNamer.name(this.owner.getOfflinePlayer()).colorIfAbsent(NamedTextColor.WHITE)));
            }
            else {
                this.invitationManager.cancelAllOutgoingInvitations();
                clearHandlers = true;
            }
        }

        if (forced) {
            this.broadcastMessage(Component.translatable("io.github.zap.party.member.removed.remaining",
                    NamedTextColor.YELLOW, name));


            Player removedPlayer = player.getPlayer();
            if (removedPlayer != null && removedPlayer.isOnline()) {
                removedPlayer.sendMessage(Component.translatable("io.github.zap.party.member.removed.leaver",
                        NamedTextColor.YELLOW));
            }
        }
        else {
            this.broadcastMessage(Component.translatable("io.github.zap.party.member.left.remaining",
                    NamedTextColor.YELLOW, name));

            Player removedPlayer = player.getPlayer();
            if (removedPlayer != null && removedPlayer.isOnline()) {
                removedPlayer.sendMessage(Component.translatable("io.github.zap.party.member.left.leaver",
                        NamedTextColor.YELLOW));
            }
        }

        for (Consumer<PartyMember> handler : this.partyLeaveHandlers) {
            handler.accept(removed);
        }

        if (clearHandlers) {
            this.partyJoinHandlers.clear();
            this.partyLeaveHandlers.clear();
        }
    }

    /**
     * Kicks all offline players.
     * @return The kicked players
     */
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull Collection<OfflinePlayer> kickOffline() {
        List<OfflinePlayer> offlinePlayers = new ArrayList<>();

        boolean clearHandlers = false;
        Iterator<PartyMember> iterator = this.members.values().iterator();
        while (iterator.hasNext()) {
            PartyMember partyMember = iterator.next();
            OfflinePlayer player = partyMember.getOfflinePlayer();
            if (!player.isOnline()) {
                if (this.owner.equals(partyMember)) {
                    chooseNewOwner();

                    if (this.owner != null) {
                        this.broadcastMessage(Component.translatable("io.github.zap.party.kickoffline.newowner",
                                NamedTextColor.YELLOW,
                                this.playerNamer.name(player).colorIfAbsent(NamedTextColor.WHITE),
                                this.playerNamer.name(this.owner.getOfflinePlayer())
                                        .colorIfAbsent(NamedTextColor.WHITE)));
                    }
                    else {
                        this.invitationManager.cancelAllOutgoingInvitations();
                        clearHandlers = true;
                    }
                }

                iterator.remove();
                offlinePlayers.add(player);

                for (Consumer<PartyMember> handler : this.partyLeaveHandlers) {
                    handler.accept(partyMember);
                }
            }
        }

        for (PartyMember member : this.members.values()) {
            member.getPlayerIfOnline().ifPresent(player -> {
                PluralRules rules = PluralRules.forLocale(player.locale());
                String rule = rules.select(offlinePlayers.size());

                player.sendMessage(Component.translatable("io.github.zap.party.kickoffline.kicked." + rule,
                        NamedTextColor.RED, Component.text(offlinePlayers.size())));
            });
        }

        if (clearHandlers) {
            this.partyJoinHandlers.clear();
            this.partyLeaveHandlers.clear();
        }

        return offlinePlayers;
    }

    /**
     * Toggles whether the party is muted
     */
    public void mute() {
        this.partySettings.setMuted(!this.partySettings.isMuted());
        if (this.partySettings.isMuted()) {
            this.broadcastMessage(Component.translatable("io.github.zap.party.muted", NamedTextColor.YELLOW));
        }
        else {
            this.broadcastMessage(Component.translatable("io.github.zap.party.unmuted", NamedTextColor.YELLOW));
        }
    }

    /**
     * Toggles whether a player is mute in the party
     * @param player The player to toggle on
     */
    public void mutePlayer(@NotNull OfflinePlayer player) {
        PartyMember member = this.members.get(player.getUniqueId());
        if (member != null && member != this.owner) {
            member.setMuted(!member.isMuted());

            Component name = this.playerNamer.name(member.getOfflinePlayer()).colorIfAbsent(NamedTextColor.WHITE);
            if (member.isMuted()) {
                this.broadcastMessage(Component.translatable("io.github.zap.party.member.muted",
                        NamedTextColor.YELLOW, name));
            }
            else {
                this.broadcastMessage(Component.translatable("io.github.zap.party.member.unmuted",
                        NamedTextColor.YELLOW, name));
            }
        }
    }

    private void chooseNewOwner() {
        if (this.members.size() == 0) {
            this.owner = null;
            return;
        }

        List<PartyMember> offlineMembers = new ArrayList<>(this.members.size() - 1);
        List<PartyMember> onlineMembers = new ArrayList<>(this.members.size() - 1);

        for (PartyMember partyMember : this.members.values()) {
            if (!this.owner.equals(partyMember)) {
                offlineMembers.add(partyMember);
            }
        }
        for (PartyMember partyMember : offlineMembers) {
            partyMember.getPlayerIfOnline().ifPresent(unused -> onlineMembers.add(partyMember));
        }

        if (onlineMembers.size() > 0) {
            this.owner = onlineMembers.get(this.random.nextInt(onlineMembers.size()));
        }
        else if (offlineMembers.size() > 0) {
            this.owner = offlineMembers.get(this.random.nextInt(offlineMembers.size()));
        }
        else {
            this.owner = null;
        }
    }

    /**
     * Disbands the party
     * @return The players that were in the party
     */
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull Collection<OfflinePlayer> disband() {
        Collection<PartyMember> memberCollection = this.members.values();
        List<OfflinePlayer> offlinePlayers = new ArrayList<>(memberCollection.size());

        Component disband = Component.translatable("io.github.zap.party.disbanded", NamedTextColor.RED);

        this.owner = null;

        Iterator<PartyMember> iterator = memberCollection.iterator();
        while (iterator.hasNext()) {
            PartyMember partyMember = iterator.next();
            OfflinePlayer offlinePlayer = partyMember.getOfflinePlayer();
            Player player = offlinePlayer.getPlayer();

            if (player != null) {
                player.sendMessage(disband);
            }

            iterator.remove();
            offlinePlayers.add(offlinePlayer);

            for (Consumer<PartyMember> handler : this.partyLeaveHandlers) {
                handler.accept(partyMember);
            }
        }
        this.invitationManager.cancelAllOutgoingInvitations();

        this.partyJoinHandlers.clear();
        this.partyLeaveHandlers.clear();

        return offlinePlayers;
    }

    /**
     * Gets the {@link InvitationManager} for this party
     * @return The {@link InvitationManager}
     */
    public @NotNull InvitationManager getInvitationManager() {
        return invitationManager;
    }

    /**
     * Gets the {@link List} of spying {@link Audience}s
     * @return The {@link List} of spying {@link Audience}s
     */
    public @NotNull List<Audience> getSpyAudiences() {
        return spyAudiences;
    }

    /**
     * Determines if a player is the owner of the party
     * @param player The player to test
     * @return Whether the player is the party owner
     */
    public boolean isOwner(@NotNull OfflinePlayer player) {
        if (this.owner == null) {
            return false;
        }

        return this.owner.getOfflinePlayer().getUniqueId().equals(player.getUniqueId());
    }

    /**
     * Gets the owner of the party
     * @return The owner of the party
     */
    public @NotNull Optional<PartyMember> getOwner() {
        return Optional.ofNullable(this.owner);
    }

    /**
     * Transfers the party to another player
     * @param player The player to transfer the party to
     */
    public void transferPartyToPlayer(@NotNull OfflinePlayer player) {
        PartyMember member = this.members.get(player.getUniqueId());

        if (member == null) {
            throw new IllegalArgumentException("Tried to transfer the party to a member that is not in the party!");
        }

        this.broadcastMessage(Component.translatable("io.github.zap.party.transferred", NamedTextColor.YELLOW,
                this.playerNamer.name(member.getOfflinePlayer()).colorIfAbsent(NamedTextColor.WHITE),
                this.playerNamer.name(player).colorIfAbsent(NamedTextColor.WHITE)));

        this.owner = member;
    }

    /**
     * Gets a party member
     * @param player The player
     * @return The party member corresponding to the player, or null if it does not exist
     */
    public @NotNull Optional<PartyMember> getMember(@NotNull OfflinePlayer player) {
        return Optional.ofNullable(this.members.get(player.getUniqueId()));
    }

    /**
     * Gets all party members
     * @return All of the party members
     */
    public @NotNull Collection<PartyMember> getMembers() {
        return new ArrayList<>(this.members.values());
    }

    /**
     * Gets all online players in the party
     * @return The online players in the party
     */
    public @NotNull List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();

        for (PartyMember partyMember : this.members.values()) {
            Optional<Player> partyMemberOptional = partyMember.getPlayerIfOnline();
            partyMemberOptional.ifPresent(players::add);
        }

        return players;
    }

    /**
     * Determines if the party has a member
     * @param player The member
     * @return Whether the party has the member
     */
    public boolean hasMember(@NotNull OfflinePlayer player) {
        return this.members.containsKey(player.getUniqueId());
    }

    /**
     * Broadcasts a message to the entire party
     * @param message The component to send
     */
    public void broadcastMessage(@NotNull Component message) {
        for (PartyMember member : this.members.values()) {
            member.getPlayerIfOnline().ifPresent(player -> player.sendMessage(message));
        }
        for (Audience audience : this.spyAudiences) {
            audience.sendMessage(message);
        }
    }

    /**
     * Gets the {@link PartyLister} for party list components
     * @return The {@link PartyLister}
     */
    public @NotNull PartyLister getPartyLister() {
        return this.partyLister;
    }

    /**
     * Gets the unique identifier of this party
     * @return The {@link UUID}
     */
    @SuppressWarnings("unused")
    public @NotNull UUID getId() {
        return this.uuid;
    }

    /**
     * Gets the party's settings
     * @return The settings
     */
    public @NotNull PartySettings getPartySettings() {
        return this.partySettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        Party that = (Party) o;
        return Objects.equals(this.uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

}
