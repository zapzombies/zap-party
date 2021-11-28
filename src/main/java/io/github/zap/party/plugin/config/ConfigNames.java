package io.github.zap.party.plugin.config;

/**
 * Final class for holding public static strings that represent plugin configuration file keys.
 */
public final class ConfigNames {

    /**
     * The directory for translation and localization
     */
    public final static String LOCALIZATION_DIRECTORY = "localizationDirectory";

    /**
     * The default locale language tag for translations
     */
    public final static String DEFAULT_LOCALE_LANGUAGE_TAG = "defaultLocateLanguageTag";

    /**
     * The prefix for party messages
     */
    public final static String PARTY_PREFIX = "partyPrefix";

    /**
     * The prefix for spied party messages
     */
    public final static String SPY_PARTY_PREFIX = "spyPartyPrefix";

    /**
     * Whether the {@link org.bukkit.command.ConsoleCommandSender} should automatically spy on party messages
     */
    public final static String AUTO_CONSOLE_SPY = "autoConsoleSpy";

}
