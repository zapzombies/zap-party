package io.github.zap.party.command;

import io.github.regularcommands.commands.CommandForm;
import io.github.regularcommands.commands.Context;
import io.github.regularcommands.converter.Parameter;
import io.github.regularcommands.util.Permissions;
import io.github.regularcommands.util.Validators;
import io.github.regularcommands.validator.CommandValidator;
import io.github.regularcommands.validator.ValidationResult;
import io.github.zap.party.Party;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Joins a player's party if it exists
 */
public class JoinPartyForm extends CommandForm<Party> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("join"),
            new Parameter("\\w+", "[owner-name]")
    };

    private final CommandValidator<Party, ?> validator;

    public JoinPartyForm(@NotNull PartyTracker partyTracker, @NotNull OfflinePlayerNamer playerNamer) {
        super(Component.translatable("io.github.zap.party.command.join.usage"), Permissions.NONE, PARAMETERS);
        this.validator = new CommandValidator<>(((context, arguments, previousData) -> {
            if (partyTracker.getPartyForPlayer(previousData).isPresent()) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.join.alreadyinparty",
                                NamedTextColor.RED), null);
            }

            String ownerName = (String) arguments[1];
            if (previousData.getName().equalsIgnoreCase(ownerName)) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.join.cannotjoinown",
                                NamedTextColor.RED), null);
            }

            OfflinePlayer owner = Bukkit.getOfflinePlayerIfCached(ownerName);
            if (owner == null) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.notregistered", NamedTextColor.RED,
                                Component.text(ownerName)), null);
            }

            Component ownerComponent = playerNamer.name(owner);

            Optional<Party> partyOptional = partyTracker.getPartyForPlayer(owner);
            if (partyOptional.isEmpty()) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.notinparty", NamedTextColor.RED,
                                ownerComponent), null);
            }

            Party party = partyOptional.get();
            if (!(party.getInvitationManager().hasInvitation(previousData)
                    || party.getPartySettings().isAnyoneCanJoin())) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.join.noinvite",
                                NamedTextColor.RED, ownerComponent), null);
            }

            return ValidationResult.of(true, null, party);
        }), Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<Party, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Party data) {
        data.addMember((Player) context.getSender());
        return Component.empty();
    }

}
