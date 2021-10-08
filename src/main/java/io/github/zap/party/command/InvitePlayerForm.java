package io.github.zap.party.command;

import io.github.zap.regularcommands.commands.CommandForm;
import io.github.zap.regularcommands.commands.Context;
import io.github.zap.regularcommands.commands.RegularCommand;
import io.github.zap.regularcommands.converter.Parameter;
import io.github.zap.regularcommands.util.Permissions;
import io.github.zap.regularcommands.util.Validators;
import io.github.zap.regularcommands.validator.CommandValidator;
import io.github.zap.regularcommands.validator.ValidationResult;
import io.github.zap.party.Party;
import io.github.zap.party.creator.PartyCreator;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Invites a player to your party
 */
public class InvitePlayerForm extends CommandForm<Player> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("invite"),
            new Parameter("\\w+", Component.text("[player-name]"))
    };

    private final static CommandValidator<Player, ?> VALIDATOR
            = new CommandValidator<>((context, arguments, previousData) -> {
        String playerName = (String) arguments[1];

        if (previousData.getName().equalsIgnoreCase(playerName)) {
            return ValidationResult.of(false,
                    Component.translatable("io.github.zap.party.command.invite.cannotinviteown",
                            NamedTextColor.RED), null);
        }

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return ValidationResult.of(false, Component.translatable("io.github.zap.party.command.notonline",
                    NamedTextColor.RED, Component.text(playerName)), null);
        }

        return ValidationResult.of(true, null, player);
    }, Validators.PLAYER_EXECUTOR);

    private final PartyTracker partyTracker;

    private final PartyCreator partyCreator;

    public InvitePlayerForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker,
                            @NotNull PartyCreator partyCreator) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.invite.usage"), Permissions.NONE,
                PARAMETERS);

        this.partyTracker = partyTracker;
        this.partyCreator = partyCreator;
    }

    @Override
    public CommandValidator<Player, ?> getValidator(Context context, Object[] arguments) {
        return VALIDATOR;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Player data) {
        Player sender = (Player) context.getSender();

        Party party = this.partyTracker.getPartyForPlayer(sender).orElseGet(() -> {
            Party newParty = this.partyCreator.createParty(sender);
            this.partyTracker.trackParty(newParty);

            return newParty;
        });

        if (party.isOwner(sender) || party.getPartySettings().isAllInvite()) {
            party.getInvitationManager().addInvitation(party, data, sender);
            return Component.empty();
        }

        return Component.translatable("io.github.zap.party.command.invite.nopermission", NamedTextColor.RED);
    }

}
