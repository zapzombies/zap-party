package io.github.zap.party.plugin;

import io.github.regularcommands.commands.CommandManager;
import io.github.zap.party.Party;
import io.github.zap.party.command.PartyCommand;
import io.github.zap.party.invitation.TimedInvitationManager;
import io.github.zap.party.list.BasicPartyLister;
import io.github.zap.party.list.PartyLister;
import io.github.zap.party.member.PartyMember;
import io.github.zap.party.namer.OfflinePlayerNamer;
import io.github.zap.party.namer.SingleTextColorOfflinePlayerNamer;
import io.github.zap.party.plugin.chat.AsyncChatHandler;
import io.github.zap.party.plugin.chat.BasicAsyncChatHandler;
import io.github.zap.party.plugin.config.ConfigNames;
import io.github.zap.party.plugin.exception.LoadFailureException;
import io.github.zap.party.settings.PartySettings;
import io.github.zap.party.tracker.PartyTracker;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.apache.commons.lang3.time.StopWatch;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

/**
 * ZAP implementation of {@link ZAPParty}.
 */
@SuppressWarnings("unused")
public class PartyPlugin extends JavaPlugin implements ZAPParty {

    public final static Key TRANSLATION_REGISTRY_KEY
            = Key.key("io.github.zap", "party.translation.registry");

    public final static Locale DEFAULT_LOCALE = Locale.US;

    public final static String LOCALE_LANGUAGE_TAG_KEY = "io.github.zap.locale";

    public final static String LOCALIZATION_FOLDER_NAME = "localization";

    public final static String DEFAULT_TRANSLATION_FILE_NAME = "en-US.lang";

    public final static String PARTY_PREFIX = "<blue><lang:io.github.zap.party.chat.prefix.party> " +
            "<dark_gray><lang:io.github.zap.party.chat.prefix.rightarrow>";

    private TranslationRegistry translationRegistry;

    private PartyTracker partyTracker;

    @SuppressWarnings("FieldCanBeLocal")
    private AsyncChatHandler asyncChatHandler;

    @SuppressWarnings("FieldCanBeLocal")
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        try {
            StopWatch timer = StopWatch.createStarted();

            initConfig();
            initTranslations(GlobalTranslator.get(), TRANSLATION_REGISTRY_KEY);
            initPartyTracker();
            initAsyncChatEventHandler(MiniMessage.get());
            initCommands();

            timer.stop();
            this.getLogger().info("Enabled successfully; ~" + timer.getTime() + "ms elapsed.");
        }
        catch (LoadFailureException e) {
            this.getLogger().log(Level.SEVERE,
                    "A fatal error occurred that prevented the plugin from enabling properly", e);
            this.getPluginLoader().disablePlugin(this, false);
        }
    }

    @Override
    public void onDisable() {
        GlobalTranslator.get().removeSource(this.translationRegistry);
    }

    /**
     * Loads values from {@link #getConfig()}.
     */
    private void initConfig() {
        FileConfiguration config = this.getConfig();

        config.addDefault(ConfigNames.LOCALIZATION_DIRECTORY,
                Paths.get(this.getDataFolder().getPath(), LOCALIZATION_FOLDER_NAME).normalize().toString());
        config.addDefault(ConfigNames.DEFAULT_LOCALE_LANGUAGE_TAG, Locale.US.toLanguageTag());
        config.addDefault(ConfigNames.PARTY_PREFIX, PARTY_PREFIX);

        config.options().copyDefaults(true);
        this.saveConfig();
    }

    /**
     * Initializes and registers translations to a {@link GlobalTranslator}.
     * @param globalTranslator The {@link GlobalTranslator} to register translations to
     * @param key The key for the {@link TranslationRegistry} that will be registered
     * @throws LoadFailureException If the translations fail to load
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initTranslations(@NotNull GlobalTranslator globalTranslator,
                                  @SuppressWarnings("SameParameterValue") @NotNull Key key)
            throws LoadFailureException {
        this.translationRegistry = TranslationRegistry.create(key);

        String path = this.getConfig().getString(ConfigNames.LOCALIZATION_DIRECTORY);
        if (path == null) {
            throw new LoadFailureException("The localization directory for PartyPlusPlus was not defined!");
        }

        File localizationDirectory = new File(path);
        try {
            File newLocalizationDirectory = null;
            if (localizationDirectory.mkdirs()) {
                this.getLogger().info("No localization directory was detected, a new one has been created at " +
                        localizationDirectory.getAbsolutePath() + ".");
                newLocalizationDirectory = localizationDirectory;
            }

            if (!localizationDirectory.isDirectory()) {
                Path newPath = Paths.get(this.getDataFolder().getPath(), LOCALIZATION_FOLDER_NAME)
                        .normalize()
                        .toAbsolutePath();

                this.getLogger().info("The provided localization directory was not a directory, resetting " +
                        "the directory to " + newPath + ".");

                this.getConfig().set(ConfigNames.LOCALIZATION_DIRECTORY, newPath);
                this.saveConfig();

                newLocalizationDirectory = newPath.toFile();
            }

            if (newLocalizationDirectory != null) {
                // write translations if a new directory is being used
                this.writeEnglishTranslations(newLocalizationDirectory);
            }

            File[] translations = localizationDirectory.listFiles();
            if (translations == null) {
                throw new LoadFailureException("Could not list the files in the translation directory!");
            }

            if (translations.length == 0) {
                this.getLogger().info("No translation files were found, copying defaults " +
                        "for basic plugin functionality.");
                this.writeEnglishTranslations(localizationDirectory);
            }

            for (File translation : translations) {
                Properties properties = new Properties();
                try {
                    properties.load(new BufferedReader(new FileReader(translation)));
                }
                catch (FileNotFoundException e) {
                    this.getLogger().warning("Could not read from file " + translation.getAbsolutePath() +
                            " as it no longer exists, skipping...");
                    continue;
                }
                catch (IOException e) {
                    this.getLogger().log(Level.WARNING, "Failed to read translation file "
                            + translation.getAbsolutePath() + ", skipping...", e);
                    continue;
                }
                catch (IllegalArgumentException e) {
                    this.getLogger().log(Level.WARNING, "Malformed Unicode in translation file "
                            + translation.getAbsolutePath() + ", skipping...", e);
                    continue;
                }

                String languageTag = properties.getProperty(LOCALE_LANGUAGE_TAG_KEY);
                if (languageTag == null) {
                    this.getLogger().warning("Translation file " + translation.getAbsolutePath() +
                            " did not specify a language tag, skipping...");
                    continue;
                }

                properties.remove(LOCALE_LANGUAGE_TAG_KEY);

                Locale locale = Locale.forLanguageTag(languageTag);
                // the PropertyResourceBundle does raw generics here so we may as well too
                this.translationRegistry.registerAll(locale, (Set) properties.keySet(),
                        translationKey -> new MessageFormat(properties.getProperty(translationKey), locale));
            }
        }
        catch (SecurityException e) {
            throw new LoadFailureException("Failed to create a localization directory!", e);
        }

        String defaultLanguageTag = this.getConfig().getString(ConfigNames.DEFAULT_LOCALE_LANGUAGE_TAG);
        if (defaultLanguageTag == null) {
            this.getLogger().info("A default language tag was not specified, using "
                    + DEFAULT_LOCALE.toLanguageTag() + ".");
            this.translationRegistry.defaultLocale(DEFAULT_LOCALE);
        } else {
            this.translationRegistry.defaultLocale(Locale.forLanguageTag(defaultLanguageTag));
        }

        globalTranslator.addSource(this.translationRegistry);
    }

    /**
     * Writes the English translations to the localization directory.
     * @param directory The directory to write translations to
     * @throws LoadFailureException If an error occurred while writing the English translations
     */
    private void writeEnglishTranslations(@NotNull File directory) throws LoadFailureException {
        File outputFile = new File(directory, DEFAULT_TRANSLATION_FILE_NAME);
        this.getLogger().info("Writing English translations to " + outputFile.getAbsolutePath() + ".");

        try (InputStream input = this.getResource(DEFAULT_TRANSLATION_FILE_NAME)) {
            if (input == null) {
                throw new LoadFailureException("Expected default en-US.lang file exists in the plugin" +
                        " resources!");
            }

            try (OutputStream output = new FileOutputStream(outputFile)) {
                input.transferTo(output);
            }
        }
        catch (IOException e) {
            throw new LoadFailureException("Failed to copy default English translation file!");
        }
    }

    /**
     * Initializes the {@link PartyTracker}.
     */
    private void initPartyTracker() {
        this.partyTracker = new PartyTracker();
    }

    /**
     * Initializes the {@link AsyncChatHandler}.
     * @param miniMessage A {@link MiniMessage} instance to parse messages
     */
    private void initAsyncChatEventHandler(@NotNull MiniMessage miniMessage) {
        this.asyncChatHandler = new BasicAsyncChatHandler(this, this.partyTracker,
                miniMessage.parse(this.getConfig().getString(ConfigNames.PARTY_PREFIX, PARTY_PREFIX)));
        Bukkit.getPluginManager().registerEvents(this.asyncChatHandler, this);
    }

    /**
     * Registers the {@link CommandManager}.
     */
    private void initCommands() {
        this.commandManager = new CommandManager(this);

        Random random = new Random();
        OfflinePlayerNamer playerNamer = new SingleTextColorOfflinePlayerNamer();
        PartyLister partyLister = new BasicPartyLister(this,
                new SingleTextColorOfflinePlayerNamer(NamedTextColor.GREEN),
                new SingleTextColorOfflinePlayerNamer(NamedTextColor.RED),
                new SingleTextColorOfflinePlayerNamer(NamedTextColor.BLUE));
        this.commandManager.registerCommand(new PartyCommand(this.partyTracker,
                owner -> new Party(random, new PartyMember(owner), new PartySettings(), PartyMember::new,
                        new TimedInvitationManager(this, playerNamer), partyLister, playerNamer)));
    }

    @Override
    public @NotNull PartyTracker getPartyTracker() {
        return this.partyTracker;
    }

}
