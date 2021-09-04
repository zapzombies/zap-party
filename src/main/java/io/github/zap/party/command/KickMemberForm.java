package io.github.zap.party.command;

import io.github.regularcommands.commands.CommandForm;
import io.github.regularcommands.commands.Context;
import io.github.regularcommands.converter.Parameter;
import io.github.regularcommands.util.Permissions;
import io.github.regularcommands.util.Validators;
import io.github.regularcommands.validator.CommandValidator;
import io.github.regularcommands.validator.ValidationResult;
import io.github.zap.party.Party;
import io.github.zap.party.tracker.PartyTracker;
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
            new Parameter("\\w+", "[player-name]")
    };

    private final PartyTracker partyTracker;

    private final CommandValidator<OfflinePlayer, ?> validator;

    public KickMemberForm(@NotNull PartyTracker partyTracker) {
        super("Kicks a member from your party.", Permissions.NONE, PARAMETERS);

        this.partyTracker = partyTracker;
        this.validator = new CommandValidator<>((context, arguments, previousData) -> {
            Optional<Party> partyOptional = partyTracker.getPartyForPlayer(previousData);
            if (partyOptional.isEmpty()) {
                return ValidationResult.of(false, "You are not currently in a party.", null);
            }

            Party party = partyOptional.get();

            if (!party.isOwner(previousData)) {
                return ValidationResult.of(false, "You are not the party owner.", null);
            }

            String playerName = (String) arguments[1];
            if (previousData.getName().equalsIgnoreCase(playerName)) {
                return ValidationResult.of(false, "You cannot kick yourself.", null);
            }

            OfflinePlayer toKick = Bukkit.getOfflinePlayerIfCached(playerName);
            if (toKick == null) {
                return ValidationResult.of(false, String.format("%s is not registered on the server!", playerName),
                        null);
            }

            Optional<Party> toKickPartyOptional = partyTracker.getPartyForPlayer(toKick);
            if (toKickPartyOptional.isPresent()) {
                if (party.equals(toKickPartyOptional.get())) {
                    return ValidationResult.of(true, null, toKick);
                }
            }

            return ValidationResult.of(false, String.format("%s is not in your party.", playerName), null);

        }, Validators.PLAYER_EXECUTOR);
    }

    @Override
    public CommandValidator<OfflinePlayer, ?> getValidator(Context context, Object[] arguments) {
        return this.validator;
    }

    @Override
    public String execute(Context context, Object[] arguments, OfflinePlayer data) {
        this.partyTracker.getPartyForPlayer(data).ifPresent(party -> party.removeMember(data, true));
        return null;
    }

}