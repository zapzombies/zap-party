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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * Lists all members in a party
 */
public class ListMembersForm extends CommandForm<Party> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("list", Component.text("list"))
    };

    private final CommandValidator<Party, ?> validator;

    public ListMembersForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.list.usage"), Permissions.NONE,
                PARAMETERS);

        this.validator = new CommandValidator<>((context, arguments, previousData) -> {
            Optional<Party> partyOptional = partyTracker.getPartyForPlayer(previousData);
            if (partyOptional.isEmpty()) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.sender.notinparty",
                                NamedTextColor.RED), null);
            }

            return ValidationResult.of(true, null, partyOptional.get());
        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<Party, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Party data) {
        Locale locale = ((Player) context.getSender()).locale();
        for (Component component : data.getPartyLister().getPartyListComponents(data, locale)) {
            context.getSender().sendMessage(component);
        }
        return Component.empty();
    }

}
