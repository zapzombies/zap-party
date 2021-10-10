package io.github.zap.party.command;

import io.github.zap.party.Party;
import io.github.zap.party.audience.PlayerAudience;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.tracker.PartyTracker;
import io.github.zap.regularcommands.commands.CommandForm;
import io.github.zap.regularcommands.commands.Context;
import io.github.zap.regularcommands.commands.RegularCommand;
import io.github.zap.regularcommands.converter.Parameter;
import io.github.zap.regularcommands.util.Permissions;
import io.github.zap.regularcommands.util.Validators;
import io.github.zap.regularcommands.validator.CommandValidator;
import io.github.zap.regularcommands.validator.ValidationResult;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SpyPartyForm extends CommandForm<Party> {

    private final static Parameter[] PARAMETERS = new Parameter[] {
            new Parameter("spy", Component.text("spy")),
            new Parameter("\\w+", Component.text("player-name"), false)
    };

    private final CommandValidator<Party, ?> validator;

    private final OfflinePlayerNamer playerNamer;

    public SpyPartyForm(@NotNull RegularCommand regularCommand, @NotNull PartyTracker partyTracker,
                        @NotNull OfflinePlayerNamer playerNamer) {
        super(regularCommand, Component.translatable("io.github.zap.party.command.spy.usage"),
                Permissions.OPERATOR, PARAMETERS);

        this.validator = new CommandValidator<>((context, arguments, previousData) -> {
            String playerName = (String) arguments[1];
            if (previousData.getName().equalsIgnoreCase(playerName)) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.spy.cannotspyself",
                                NamedTextColor.RED), null);
            }

            OfflinePlayer toSpy = Bukkit.getOfflinePlayerIfCached(playerName);
            if (toSpy == null) {
                return ValidationResult.of(false,
                        Component.translatable("io.github.zap.party.command.notregistered",
                                NamedTextColor.RED, Component.text(playerName)), null);
            }

            Optional<Party> toSpyPartyOptional = partyTracker.getPartyForPlayer(toSpy);
            if (toSpyPartyOptional.isPresent()) {
                if (previousData instanceof Player player) {
                    Optional<Party> partyOptional = partyTracker.getPartyForPlayer(player);
                    if (partyOptional.isPresent() && partyOptional.get().equals(toSpyPartyOptional.get())) {
                        return ValidationResult.of(false,
                                Component.translatable("io.github.zap.party.command.spy.cannotspyself",
                                        NamedTextColor.RED), null);
                    }
                }

                return ValidationResult.of(true, null, toSpyPartyOptional.get());
            }

            Component toSpyComponent = playerNamer.name(toSpy);
            return ValidationResult.of(false,
                    Component.translatable("io.github.zap.party.command.notinparty",
                            NamedTextColor.RED, toSpyComponent), null);
        }, Validators.ANY);
        this.playerNamer = playerNamer;
    }

    @Override
    public @Nullable CommandValidator<Party, ?> getValidator(Context context, Object[] arguments) {
        return validator;
    }

    @Override
    public @Nullable Component execute(Context context, Object[] arguments, Party data) {
        Component spyComponent = data
                .getOwner()
                .map(partyMember -> this.playerNamer.name(partyMember.getOfflinePlayer()))
                .orElseGet(() -> Component.translatable("io.github.zap.party.command.spy.someone"));

        Audience spy;
        if (context.getSender() instanceof Player player) {
            spy = new PlayerAudience(player);
        }
        else {
            spy = context.getSender();
        }

        if (data.getSpyAudiences().contains(spy)) {
            data.getSpyAudiences().remove(spy);
            return Component.translatable("io.github.zap.party.command.spy.notspying", NamedTextColor.RED,
                    spyComponent);
        }
        else {
            data.getSpyAudiences().add(spy);
            return Component.translatable("io.github.zap.party.command.spy.spying", NamedTextColor.GREEN,
                    spyComponent);
        }
    }
}
