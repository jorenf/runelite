package com.farmingtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * RuneLite config interface for the Farming Tracker plugin.
 * All options are exposed in the RuneLite Settings panel under "Farming Tracker".
 */
@ConfigGroup(FarmingTrackerConfig.GROUP)
public interface FarmingTrackerConfig extends Config
{
    /** Config group key — must match the value used in all {@link net.runelite.client.config.ConfigManager} calls. */
    String GROUP = "farmingtracker";

    // -----------------------------------------------------------------------
    // Sections
    // -----------------------------------------------------------------------

    @ConfigSection(
            name = "Webhook",
            description = "Settings for the PWA backend push notification server",
            position = 0
    )
    String webhookSection = "webhook";

    @ConfigSection(
            name = "Tracking",
            description = "Choose which patch types to track",
            position = 1
    )
    String trackingSection = "tracking";

    @ConfigSection(
            name = "Display",
            description = "In-game overlay options",
            position = 2
    )
    String displaySection = "display";

    // -----------------------------------------------------------------------
    // Webhook options
    // -----------------------------------------------------------------------

    @ConfigItem(
            keyName = "webhookUrl",
            name = "Webhook URL",
            description = "Base URL of your osrs-farming-pwa server, e.g. https://my-server.example.com",
            section = webhookSection,
            position = 0
    )
    default String webhookUrl()
    {
        return "";
    }

    // -----------------------------------------------------------------------
    // Tracking options
    // -----------------------------------------------------------------------

    @ConfigItem(
            keyName = "trackTreePatches",
            name = "Track tree patches",
            description = "Send notifications when regular tree patches (oak → magic) are fully grown",
            section = trackingSection,
            position = 0
    )
    default boolean trackTreePatches()
    {
        return true;
    }

    @ConfigItem(
            keyName = "trackFruitTreePatches",
            name = "Track fruit-tree patches",
            description = "Send notifications when fruit-tree patches (apple → dragonfruit) are fully grown",
            section = trackingSection,
            position = 1
    )
    default boolean trackFruitTreePatches()
    {
        return true;
    }

    @ConfigItem(
            keyName = "bankTracking",
            name = "Include bank in seed inventory",
            description = "Scan the bank for saplings when it is opened (improves optimizer accuracy)",
            section = trackingSection,
            position = 2
    )
    default boolean bankTracking()
    {
        return true;
    }

    // -----------------------------------------------------------------------
    // Display options
    // -----------------------------------------------------------------------

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show overlay",
            description = "Display a panel showing tracked patches and their grow timers",
            section = displaySection,
            position = 0
    )
    default boolean showOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showOptimizer",
            name = "Show planting advice",
            description = "Include the optimizer's planting advice in the overlay",
            section = displaySection,
            position = 1
    )
    default boolean showOptimizer()
    {
        return true;
    }
}
