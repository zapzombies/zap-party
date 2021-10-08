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
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

// TODO: far post-beta, allow for settings to be more flexible
public class PartySettingsForm extends CommandForm<Pair<Party, Object[]>> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("settings"),
            new Parameter(".*", Component.text("[setting-name]")),
            new Parameter(".*", Component.text("[setting-value]"))
    };

    private final CommandValidator<Pair<Party, Object[]>, ?> validator;

    public PartySettingsForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.settings.usage"),
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

            return ValidationResult.of(true, null,
                    Pair.of(party, Arrays.copyOfRange(arguments, 1, arguments.length)));
        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<Pair<Party, Object[]>, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, Pair<Party, Object[]> data) {
        Party party = data.getLeft();
        Object[] parameters = data.getRight();
        String settingName = ((String) parameters[0]).toLowerCase();

        switch (settingName) {
            case "allinvite":
                party.getPartySettings().setAnyoneCanJoin(Boolean.parseBoolean((String)parameters[1]));
                Component isAllInvite = (party.getPartySettings().isAllInvite())
                        ? Component.translatable("io.github.zap.party.command.settings.on")
                        : Component.translatable("io.github.zap.party.command.settings.off");
                return Component.translatable("io.github.zap.party.command.settings.set", NamedTextColor.GOLD,
                        Component.text("allinvite"), isAllInvite);
            case "anyonecanjoin":
                party.getPartySettings().setAnyoneCanJoin(Boolean.parseBoolean((String)parameters[1]));
                Component isAnyoneCanJoin = (party.getPartySettings().isAnyoneCanJoin())
                        ? Component.translatable("io.github.zap.party.command.settings.on")
                        : Component.translatable("io.github.zap.party.command.settings.off");
                return Component.translatable("io.github.zap.party.command.settings.set", NamedTextColor.GOLD,
                        Component.text("anyonecanjoin"), isAnyoneCanJoin);
            case "inviteexpirationtime":
                try {
                    party.getPartySettings().setInviteExpirationTime(Long.parseLong((String) parameters[1]));
                    return Component.translatable("io.github.zap.party.command.settings.set", NamedTextColor.GOLD,
                            Component.text("inviteexpirationtime"),
                            Component.text(party.getPartySettings().getInviteExpirationTime()));
                } catch (NumberFormatException e) {
                    return Component.translatable("io.github.zap.party.command.settings.invalidexpirationtime",
                            NamedTextColor.RED);
                }
        }

        return Component.empty();
    }

}
