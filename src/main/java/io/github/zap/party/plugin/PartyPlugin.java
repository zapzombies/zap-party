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
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * ZAP implementation of {@link ZAPParty}.
 */
@SuppressWarnings("unused")
public class PartyPlugin extends JavaPlugin implements ZAPParty {

    public final static Key TRANSLATION_REGISTRY_KEY
            = Key.key("io.github.zap", "party.translation.registry");

    public final static Locale DEFAULT_LOCALE = Locale.US;

    public final static String LOCALE_LANGUAGE_TAG_KEY = "io.github.zap.locale";

    public final static String LOCALIZATION_DIRECTORY_NAME = "localization";

    public final static String DEFAULT_TRANSLATIONS_DIRECTORY = "translations/";

    public final static String PARTY_PREFIX = "<blue><lang:io.github.zap.party.chat.prefix.party> " +
            "<dark_gray><lang:io.github.zap.party.chat.prefix.rightarrow> ";

    private PartyTracker partyTracker;

    @SuppressWarnings("FieldCanBeLocal")
    private AsyncChatHandler asyncChatHandler;

    @SuppressWarnings("FieldCanBeLocal")
    private CommandManager commandManager;

    private TranslationRegistry translationRegistry;

    private Locale defaultLocale;

    private boolean defaultLocaleTranslationsLoaded = false;

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
                Paths.get(this.getDataFolder().getPath(), LOCALIZATION_DIRECTORY_NAME).normalize().toString());
        config.addDefault(ConfigNames.DEFAULT_LOCALE_LANGUAGE_TAG, DEFAULT_LOCALE.toLanguageTag());
        config.addDefault(ConfigNames.PARTY_PREFIX, PARTY_PREFIX);

        config.options().copyDefaults(true);
        this.saveConfig();

        String defaultLocaleTag = config.getString(ConfigNames.DEFAULT_LOCALE_LANGUAGE_TAG);
        this.defaultLocale = (defaultLocaleTag != null)
                ? Locale.forLanguageTag(defaultLocaleTag)
                : DEFAULT_LOCALE;
    }

    /**
     * Initializes and registers translations to a {@link GlobalTranslator}.
     * @param globalTranslator The {@link GlobalTranslator} to register translations to
     * @param key The key for the {@link TranslationRegistry} that will be registered
     * @throws LoadFailureException If the translations fail to load
     */
    private void initTranslations(@NotNull GlobalTranslator globalTranslator,
                                  @SuppressWarnings("SameParameterValue") @NotNull Key key)
            throws LoadFailureException {
        this.translationRegistry = TranslationRegistry.create(key);
        this.translationRegistry.defaultLocale(this.defaultLocale);

        this.tryLoadTranslations(this.findLocalizationDirectory());

        globalTranslator.addSource(this.translationRegistry);
    }

    /**
     * Finds the localization directory
     * @return The localization directory
     * @throws LoadFailureException If the localization directory was unable to be found
     */
    private @NotNull Path findLocalizationDirectory() throws LoadFailureException {
        String path = this.getConfig().getString(ConfigNames.LOCALIZATION_DIRECTORY);
        if (path == null) {
            throw new LoadFailureException("The localization directory for ZAPParty was not defined!");
        }

        Path localizationDirectory = Paths.get(path);

        boolean exists;
        try {
            exists = Files.exists(localizationDirectory);
        }
        catch (SecurityException e) {
            throw new LoadFailureException("Failed to check if localization directory exists!");
        }

        if (!exists) {
            try {
                Files.createDirectories(localizationDirectory);
            } catch (IOException | SecurityException e) {
                throw new LoadFailureException("Unable to create localization directory!");
            }

            Path absoluteLocalizationDirectory = localizationDirectory;
            try {
                absoluteLocalizationDirectory = localizationDirectory.toAbsolutePath();
            }
            catch (IOError | SecurityException e) {
                this.getLogger().warning("Failed to find absolute path for localization directory, " +
                        "using " + localizationDirectory + ".");
            }

            this.getLogger().info("No localization directory was detected, a new one has been created at " +
                    absoluteLocalizationDirectory + ".");
        }

        boolean isDirectory;
        try {
            isDirectory = Files.isDirectory(localizationDirectory);
        }
        catch (SecurityException e) {
            throw new LoadFailureException("Failed to check if a localization directory was a directory!", e);
        }

        if (!isDirectory) {
            localizationDirectory = this.resetLocalizationDirectory();
        }

        return localizationDirectory;
    }

    /**
     * Resets the default localization directory
     * @return The new localization directory
     * @throws LoadFailureException If a new localization directory was unable to be created
     */
    private @NotNull Path resetLocalizationDirectory() throws LoadFailureException {
        Path newPath;
        try {
            newPath = Paths.get(this.getDataFolder().getPath(), LOCALIZATION_DIRECTORY_NAME);
        }
        catch (InvalidPathException e) {
            throw new LoadFailureException("Could not resolve default localization directory path!", e);
        }

        newPath = newPath.normalize();
        try {
            newPath = newPath.toAbsolutePath();
        }
        catch (IOError | SecurityException e) {
            this.getLogger().warning("Failed to find absolute path for default localization directory, " +
                    "using " + newPath + ".");
        }

        this.getLogger().info("The provided localization directory was not a directory, resetting " +
                "the directory to " + newPath + ".");

        try {
            Files.createDirectories(newPath);
        }
        catch (IOException | SecurityException e) {
            throw new LoadFailureException("Could not create a new localization directory!", e);
        }

        this.getConfig().set(ConfigNames.LOCALIZATION_DIRECTORY, newPath);
        this.saveConfig();

        return newPath;
    }

    /**
     * Attempts to load translations from a localization directory
     * @param localizationDirectory The localization directory to load translations from
     * @throws LoadFailureException If loading translations failed
     */
    private void tryLoadTranslations(@NotNull Path localizationDirectory) throws LoadFailureException {
        try (Stream<Path> stream = Files.list(localizationDirectory)) {
            Iterator<Path> iterator = stream.iterator();

            if (!iterator.hasNext()) {
                this.getLogger().info("No translation files were found, copying defaults " +
                        "for basic plugin functionality.");
                this.writeDefaultTranslations(localizationDirectory);
            }
            else {
                while (iterator.hasNext()) {
                    this.loadTranslations(iterator.next());
                }

                if (!this.defaultLocaleTranslationsLoaded) {
                    throw new LoadFailureException("The default locale " + this.defaultLocale +
                            " was unable to be loaded!");
                }
            }
        } catch (IOException e) {
            throw new LoadFailureException("Failed to list files in the localization directory!");
        }
    }

    /**
     * Loads actual translations from a filee and registers them to the {@link TranslationRegistry}.
     * @param path The file to read from
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadTranslations(@NotNull Path path) throws LoadFailureException {
        Properties properties = new Properties();

        try {
            path = path.toAbsolutePath();
        }
        catch (IOError | SecurityException e) {
            this.getLogger().warning("Failed to find absolute path for translation file, using " +
                    path + ".");
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            try {
                properties.load(reader);
            }
            catch (FileNotFoundException e) {
                this.getLogger().log(Level.WARNING, "Could not read from file " + path + " as it " +
                        "no longer exists, skipping...", e);
                return;
            }
            catch (IOException e) {
                this.getLogger().log(Level.WARNING, "Failed to read translation file " + path +
                        ", skipping...", e);
                return;
            }
            catch (IllegalArgumentException e) {
                this.getLogger().log(Level.WARNING, "Malformed Unicode in translation file " + path +
                        ", skipping...", e);
                return;
            }
        } catch (IOException | SecurityException e) {
            this.getLogger().log(Level.WARNING, "Failed to read translation file "
                    + path + ", skipping...", e);
        }

        String languageTag = properties.getProperty(LOCALE_LANGUAGE_TAG_KEY);
        if (languageTag == null) {
            this.getLogger().warning("Translation file " + path +
                    " did not specify a language tag, skipping...");
            return;
        }

        properties.remove(LOCALE_LANGUAGE_TAG_KEY);

        Locale locale = Locale.forLanguageTag(languageTag);
        // the PropertyResourceBundle does raw generics here, so we may as well too
        try {
            this.translationRegistry.registerAll(locale, (Set) properties.keySet(),
                    translationKey -> new MessageFormat(properties.getProperty(translationKey), locale));

            if (this.defaultLocale.equals(locale)) {
                this.defaultLocaleTranslationsLoaded = true;
            }
        }
        catch (IllegalArgumentException e) {
            throw new LoadFailureException("Party translations have already been registered!", e);
        }
    }

    /**
     * Writes the default translations to the localization directory.
     * @param target The directory to write translations to
     * @throws LoadFailureException If an error occurred while writing the default translations
     */
    private void writeDefaultTranslations(@NotNull Path target) throws LoadFailureException {
        target = target.normalize();
        try {
            target = target.toAbsolutePath();
        }
        catch (IOError | SecurityException e) {
            this.getLogger().warning("Failed to find absolute path for localization directory, using " +
                    target + ".");
        }

        Path finalPath = target;

        this.getLogger().info("Writing default translations to " + target + ".");

        URL translations = this.getClassLoader().getResource(DEFAULT_TRANSLATIONS_DIRECTORY);
        if (translations == null) {
            throw new LoadFailureException("Could not find a default translation directory for plugin resources!");
        }

        try (FileSystem fileSystem = FileSystems.newFileSystem(translations.toURI(), Collections.emptyMap())) {
            Path root;
            try {
                root = fileSystem.getPath(DEFAULT_TRANSLATIONS_DIRECTORY);
            }
            catch (InvalidPathException e) {
                throw new LoadFailureException("Failed to create a valid path for default translations!", e);
            }

            LoadFailureException[] exception = new LoadFailureException[] { null };
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        Path nextPath = finalPath.resolve(root.relativize(dir).toString());
                        try {
                            Files.createDirectories(nextPath);
                        }
                        catch (IOException | SecurityException e) {
                            PartyPlugin.this.getLogger().log(Level.WARNING, "Unable to copy " +
                                    "translation directory to " + nextPath + ", skipping...", e);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path nextPath = finalPath.resolve(root.relativize(file).toString());
                        try {
                            Files.copy(file, nextPath);
                            PartyPlugin.this.loadTranslations(nextPath);
                        }
                        catch (UnsupportedOperationException | IOException | SecurityException e) {
                            PartyPlugin.this.getLogger().log(Level.WARNING, "Unable to copy " +
                                    "translation file to " + nextPath + ", skipping...", e);
                        }
                        catch (LoadFailureException e) {
                            exception[0] = e;
                            return FileVisitResult.TERMINATE;
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });

                if (exception[0] != null) {
                    throw exception[0];
                }

                if (!this.defaultLocaleTranslationsLoaded) {
                    throw new LoadFailureException("The default locale " + this.defaultLocale +
                            " was unable to be loaded!");
                }
            } catch (IOException e) {
                throw new LoadFailureException("Failed to walk through translation files!", e);
            }
        }
        catch (IOException | URISyntaxException e) {
            throw new LoadFailureException("Could not create a file system for default translations!", e);
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
                        new TimedInvitationManager(this, playerNamer), partyLister, playerNamer),
                new SingleTextColorOfflinePlayerNamer(null)));
    }

    @Override
    public @NotNull PartyTracker getPartyTracker() {
        return this.partyTracker;
    }

}
