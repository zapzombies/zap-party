package io.github.zap.party.command;

import io.github.regularcommands.commands.CommandForm;
import io.github.regularcommands.commands.Context;
import io.github.regularcommands.converter.Parameter;
import io.github.regularcommands.util.Permissions;
import io.github.regularcommands.util.Validators;
import io.github.regularcommands.validator.CommandValidator;
import io.github.regularcommands.validator.ValidationResult;
import io.github.zap.party.Party;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PartyChatForm extends CommandForm<Party> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("chat")
    };

    private final CommandValidator<Party, ?> validator;

    public PartyChatForm(@NotNull PartyTracker partyTracker) {
        super(Component.translatable("io.github.zap.party.command.chat.usage"), Permissions.NONE, PARAMETERS);

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
    public boolean canStylize() {
        return true;
    }

    @Override
    public CommandValidator<Party, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Party data) {
        Optional<PartyMember> partyMemberOptional = data.getMember((Player) context.getSender());
        if (partyMemberOptional.isPresent()) {
            PartyMember partyMember = partyMemberOptional.get();
            partyMember.setInPartyChat(!partyMember.isInPartyChat());
            if (partyMember.isInPartyChat()) {
                return Component.translatable("io.github.zap.party.command.chat.on", NamedTextColor.GOLD);
            }

            return Component.translatable("io.github.zap.party.command.chat.off", NamedTextColor.GOLD);
        }

        return Component.empty();
    }
}
