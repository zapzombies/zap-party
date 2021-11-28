package io.github.zap.party.command;

import io.github.zap.regularcommands.commands.CommandManager;
import io.github.zap.regularcommands.commands.PageBuilder;
import io.github.zap.regularcommands.commands.RegularCommand;
import io.github.zap.party.creator.PartyCreator;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Generic party command.
 */
public class PartyCommand extends RegularCommand {

    public PartyCommand(@NotNull CommandManager commandManager, @NotNull PageBuilder pageBuilder,
                        @NotNull PartyTracker partyTracker, @NotNull PartyCreator partyCreator,
                        @NotNull OfflinePlayerNamer commandPlayerNamer,
                        @NotNull TranslatableComponentRenderer<Locale> renderer, @NotNull Locale consoleLocale) {
        super(commandManager, "party", pageBuilder);
        addForm(new PartySettingsForm(this, partyTracker));
        addForm(new PartyChatForm(this, partyTracker));
        addForm(new CreatePartyForm(this, partyTracker, partyCreator));
        addForm(new InvitePlayerForm(this, partyTracker, partyCreator));
        addForm(new JoinPartyForm(this, partyTracker, commandPlayerNamer));
        addForm(new LeavePartyForm(this, partyTracker));
        addForm(new ListMembersForm(this, partyTracker));
        addForm(new PartyMuteForm(this, partyTracker, commandPlayerNamer));
        addForm(new KickMemberForm(this, partyTracker, commandPlayerNamer));
        addForm(new KickOfflineMembersForm(this, partyTracker));
        addForm(new SpyPartyForm(this, partyTracker, commandPlayerNamer, renderer, consoleLocale));
        addForm(new TransferPartyForm(this, partyTracker, commandPlayerNamer));
        addForm(new DisbandPartyForm(this, partyTracker));
    }

}
