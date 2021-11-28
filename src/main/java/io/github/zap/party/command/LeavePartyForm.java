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
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Leaves your current party
 */
public class LeavePartyForm extends CommandForm<Void> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("leave", Component.text("leave"))
    };

    private final PartyTracker partyTracker;

    private final CommandValidator<Void, ?> validator;

    public LeavePartyForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.leave.usage"), Permissions.NONE, PARAMETERS);

        this.partyTracker = partyTracker;
        this.validator = new CommandValidator<>((context, arguments, previousData) -> {
            Optional<Party> party = partyTracker.getPartyForPlayer(previousData);
            if (party.isEmpty()) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.sender.notinparty",
                                NamedTextColor.RED), null);
            }

            return ValidationResult.of(true, null, null);
        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<Void, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Void data) {
        OfflinePlayer sender = (OfflinePlayer) context.getSender();
        this.partyTracker.getPartyForPlayer(sender).ifPresent(party -> party.removeMember(sender, false));
        return Component.empty();
    }

}
