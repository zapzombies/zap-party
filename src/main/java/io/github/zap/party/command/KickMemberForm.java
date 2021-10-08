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
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Kicks a member from the party
 */
public class KickMemberForm extends CommandForm<OfflinePlayer> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("kick"),
            new Parameter("\\w+", Component.text("[player-name]"))
    };

    private final PartyTracker partyTracker;

    private final CommandValidator<OfflinePlayer, ?> validator;

    public KickMemberForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker,
                          @NotNull OfflinePlayerNamer playerNamer) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.kick.usage"), Permissions.NONE,
                PARAMETERS);

        this.partyTracker = partyTracker;
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
                        Component.translatable("io.github.zap.party.command.sender.notowner", NamedTextColor.RED),
                        null);
            }

            String playerName = (String) arguments[1];
            if (previousData.getName().equalsIgnoreCase(playerName)) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.kick.cannotkickself",
                                NamedTextColor.RED), null);
            }

            OfflinePlayer toKick = Bukkit.getOfflinePlayerIfCached(playerName);
            if (toKick == null) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.notregistered",
                                NamedTextColor.RED, Component.text(playerName)), null);
            }

            Optional<Party> toKickPartyOptional = partyTracker.getPartyForPlayer(toKick);
            if (toKickPartyOptional.isPresent()) {
                if (party.equals(toKickPartyOptional.get())) {
                    return ValidationResult.of(true, null, toKick);
                }
            }

            Component toKickComponent = playerNamer.name(toKick);

            return ValidationResult.of(false,
                    Component.translatable("io.github.zap.party.command.notinyourparty",
                            NamedTextColor.RED, toKickComponent), null);

        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<OfflinePlayer, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, OfflinePlayer data) {
        this.partyTracker.getPartyForPlayer(data).ifPresent(party -> party.removeMember(data, true));
        return Component.empty();
    }

}
