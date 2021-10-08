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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Creates a new party
 */
public class CreatePartyForm extends CommandForm<Void> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("create")
    };

    private final PartyTracker partyTracker;

    private final PartyCreator partyCreator;

    private final CommandValidator<Void, ?> validator;

    public CreatePartyForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker,
                           @NotNull PartyCreator partyCreator) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.create.usage"), Permissions.NONE,
                PARAMETERS);

        this.partyTracker = partyTracker;
        this.partyCreator = partyCreator;
        this.validator = new CommandValidator<>((context, arguments, previousData) -> {
            Optional<Party> partyOptional = partyTracker.getPartyForPlayer((OfflinePlayer) context.getSender());

            if (partyOptional.isPresent()) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.sender.alreadyinparty",
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
        this.partyTracker.trackParty(this.partyCreator.createParty((Player) context.getSender()));
        return Component.translatable("io.github.zap.party.command.create.success", NamedTextColor.GOLD);
    }

}
