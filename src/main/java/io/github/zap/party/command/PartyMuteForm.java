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
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Mute a player or the party chat
 */
public class PartyMuteForm extends CommandForm<OfflinePlayer> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("mute"),
            new Parameter("\\w+", "[player-name]", "")
    };

    private final PartyTracker partyTracker;

    private final CommandValidator<OfflinePlayer, ?> validator;

    public PartyMuteForm(@NotNull PartyTracker partyTracker, @NotNull OfflinePlayerNamer playerNamer) {
        super(Component.translatable("io.github.zap.party.command.mute.usage"), Permissions.NONE, PARAMETERS);

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
                        Component.translatable("io.github.zap.party.command.sender.notowner",
                                NamedTextColor.RED), null);
            }

            String playerName = (String) arguments[1];
            if (previousData.getName().equalsIgnoreCase(playerName)) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.mute.cannotmuteself",
                                NamedTextColor.RED), null);
            }

            if (!playerName.equals("")) {
                OfflinePlayer toMute = Bukkit.getOfflinePlayerIfCached(playerName);
                if (toMute == null) {
                    return ValidationResult.of(false,
                            Component.translatable("io.github.zap.party.command.notregistered",
                                    NamedTextColor.RED, Component.text(playerName)), null);
                }

                Component toMuteComponent = playerNamer.name(toMute);

                Optional<Party> toKickPartyOptional = partyTracker.getPartyForPlayer(toMute);
                if (toKickPartyOptional.isPresent()) {
                    if (!party.equals(toKickPartyOptional.get())) {
                        return ValidationResult.of(false,
                                Component.translatable("io.github.zap.party.command.notinyourparty",
                                        NamedTextColor.RED, toMuteComponent), null);
                    }
                }

                return ValidationResult.of(true, null, toMute);
            } else {
                return ValidationResult.of(true, null, null);
            }
        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<OfflinePlayer, ?> getValidator(Context context, Object[] arguments) {
        return validator;
    }

    @Override
    public Component execute(Context context, Object[] arguments, OfflinePlayer data) {
        Optional<Party> partyOptional = this.partyTracker.getPartyForPlayer((OfflinePlayer) context.getSender());
        if (partyOptional.isPresent()) {
            Party party = partyOptional.get();
            if (data == null) {
                party.mute();
            } else {
                party.mutePlayer(data);
            }
        }

        return Component.empty();
    }

}

