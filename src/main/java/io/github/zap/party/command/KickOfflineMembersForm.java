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
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Kicks all offline members from the party
 */
public class KickOfflineMembersForm extends CommandForm<Party> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("kickoffline", Component.text("kickoffline"))
    };

    private final CommandValidator<Party, ?> validator;

    public KickOfflineMembersForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.kickoffline.usage"),
                Permissions.NONE, PARAMETERS);

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

            return ValidationResult.of(true, null, party);
        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<Party, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Party data) {
        data.kickOffline();
        return Component.empty();
    }


}
