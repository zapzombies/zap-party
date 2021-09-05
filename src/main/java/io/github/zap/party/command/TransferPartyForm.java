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
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Transfers the party to another player
 */
public class TransferPartyForm extends CommandForm<Pair<Party, Player>> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("transfer"),
            new Parameter("\\w+", "[player-name]")
    };

    private final CommandValidator<Pair<Party, Player>, ?> validator;

    public TransferPartyForm(@NotNull PartyTracker partyTracker, @NotNull OfflinePlayerNamer playerNamer) {
        super(Component.translatable("io.github.zap.party.command.transfer.usage"), Permissions.NONE, PARAMETERS);

        this.validator = new CommandValidator<>((context, arguments, previousData) -> {
            Optional<Party> partyOptional = partyTracker.getPartyForPlayer(previousData);
            if (partyOptional.isEmpty()) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.sender.notinparty",
                                NamedTextColor.RED), null);
            }

            Party party = partyOptional.get();

            if (!party.isOwner(previousData)) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.sender.notowner",
                                NamedTextColor.RED), null);
            }

            String playerName = (String) arguments[1];
            if (previousData.getName().equalsIgnoreCase(playerName)) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.transfer.cannottransferself",
                                NamedTextColor.RED), null);
            }

            Player toTransfer = Bukkit.getPlayer(playerName);
            if (toTransfer == null) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.notonline", NamedTextColor.RED,
                                Component.text(playerName)), null);
            }

            Optional<Party> toTransferPartyOptional = partyTracker.getPartyForPlayer(toTransfer);
            if (toTransferPartyOptional.isPresent()) {
                Party toTransferParty = toTransferPartyOptional.get();
                if (party.equals(toTransferParty)) {
                    return ValidationResult.of(true, null, Pair.of(party, toTransfer));
                }
            }

            return ValidationResult.of(false,
                    Component.translatable("io.github.zap.party.command.notinyourparty",
                            NamedTextColor.RED, playerNamer.name(toTransfer)), null);
        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<Pair<Party, Player>, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Pair<Party, Player> data) {
        data.getLeft().transferPartyToPlayer(data.getRight());
        return Component.empty();
    }

}
