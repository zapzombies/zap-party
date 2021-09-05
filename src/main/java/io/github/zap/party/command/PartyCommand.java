package io.github.zap.party.command;

import io.github.regularcommands.commands.RegularCommand;
import io.github.zap.party.creator.PartyCreator;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.tracker.PartyTracker;
import org.jetbrains.annotations.NotNull;

/**
 * Generic party command.
 */
public class PartyCommand extends RegularCommand {

    public PartyCommand(@NotNull PartyTracker partyTracker, @NotNull PartyCreator partyCreator,
                        @NotNull OfflinePlayerNamer commandPlayerNamer) {
        super("party");
        addForm(new PartySettingsForm(partyTracker));
        addForm(new PartyChatForm(partyTracker));
        addForm(new CreatePartyForm(partyTracker, partyCreator));
        addForm(new InvitePlayerForm(partyTracker, partyCreator));
        addForm(new JoinPartyForm(partyTracker, commandPlayerNamer));
        addForm(new LeavePartyForm(partyTracker));
        addForm(new ListMembersForm(partyTracker));
        addForm(new PartyMuteForm(partyTracker, commandPlayerNamer));
        addForm(new KickMemberForm(partyTracker, commandPlayerNamer));
        addForm(new KickOfflineMembersForm(partyTracker));
        addForm(new TransferPartyForm(partyTracker, commandPlayerNamer));
        addForm(new DisbandPartyForm(partyTracker));
    }

}
