package io.github.zap.party.plugin;

import io.github.regularcommands.commands.CommandManager;
import io.github.zap.party.Party;
import io.github.zap.party.command.PartyCommand;
import io.github.zap.party.plugin.chat.AsyncChatHandler;
import io.github.zap.party.plugin.chat.BasicAsyncChatHandler;
import io.github.zap.party.invitation.TimedInvitationManager;
import io.github.zap.party.list.BasicPartyLister;
import io.github.zap.party.list.PartyLister;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.namer.SingleTextColorOfflinePlayerNamer;
import io.github.zap.party.settings.PartySettings;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.logging.Level;

/**
 * ZAP implementation of {@link ZAPParty}.
 */
public class PartyPlugin extends JavaPlugin implements ZAPParty {

    private PartyTracker partyTracker;

    @SuppressWarnings("FieldCanBeLocal")
    private AsyncChatHandler asyncChatHandler;

    @SuppressWarnings("FieldCanBeLocal")
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        StopWatch timer = StopWatch.createStarted();

        initPartyTracker();
        initAsyncChatEventHandler(MiniMessage.get());
        initCommands(MiniMessage.get());

        timer.stop();
        this.getLogger().log(Level.INFO, String.format("Enabled successfully; ~%sms elapsed.", timer.getTime()));
    }

    /**
     * Initializes the {@link PartyTracker}
     */
    private void initPartyTracker() {
        this.partyTracker = new PartyTracker();
    }

    /**
     * Initializes the {@link AsyncChatHandler}.
     * @param miniMessage A {@link MiniMessage} instance to parse messages
     */
    private void initAsyncChatEventHandler(@NotNull MiniMessage miniMessage) {
        this.asyncChatHandler = new BasicAsyncChatHandler(this, this.partyTracker, miniMessage);
        Bukkit.getPluginManager().registerEvents(this.asyncChatHandler, this);
    }

    /**
     * Registers the {@link CommandManager}
     */
    private void initCommands(@NotNull MiniMessage miniMessage) {
        this.commandManager = new CommandManager(this);

        Random random = new Random();
        OfflinePlayerNamer playerNamer = new SingleTextColorOfflinePlayerNamer();
        PartyLister partyLister = new BasicPartyLister(this, miniMessage,
                new SingleTextColorOfflinePlayerNamer(NamedTextColor.GREEN),
                new SingleTextColorOfflinePlayerNamer(NamedTextColor.RED),
                new SingleTextColorOfflinePlayerNamer(NamedTextColor.BLUE));
        this.commandManager.registerCommand(new PartyCommand(this.partyTracker,
                owner -> new Party(miniMessage, random, new PartyMember(owner),
                        new PartySettings(), PartyMember::new,
                        new TimedInvitationManager(this, miniMessage, playerNamer), partyLister, playerNamer)));
    }

    @Override
    public @NotNull PartyTracker getPartyTracker() {
        return this.partyTracker;
    }

}
