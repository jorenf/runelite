package com.farmingtracker;

import com.farmingtracker.data.FarmingData;
import com.farmingtracker.optimizer.PlantingAdvice;
import com.farmingtracker.optimizer.PlantingOptimizer;
import com.farmingtracker.patch.FarmingPatch;
import com.farmingtracker.patch.PatchType;
import com.farmingtracker.patch.TrackedPatch;
import com.farmingtracker.seed.SeedInventory;
import com.farmingtracker.seed.SeedItem;
import com.farmingtracker.seed.SeedItemWithQty;
import com.farmingtracker.webhook.WebhookService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Main plugin class for the OSRS Farming Tracker.
 *
 * <h3>How planting is detected</h3>
 * The plugin watches for the in-game chat message produced when a sapling
 * is planted in a tree or fruit-tree patch, for example:
 * <pre>"You plant a magic sapling in the tree patch."</pre>
 * The tree type is extracted via regex, matched to a {@link SeedItem} in
 * {@link FarmingData}, and a {@link TrackedPatch} entry is created with a
 * calculated ready-time ({@code now + growTimeMinutes}).
 *
 * <h3>How readiness is detected</h3>
 * Every game tick the plugin compares {@code System.currentTimeMillis()}
 * to each tracked patch's {@code readyAt} timestamp.  When the time is
 * reached a webhook is fired and an in-game notification is shown.  This
 * timer continues to work even when the player is logged off because the
 * real-world clock is used (not game ticks).
 *
 * <h3>State persistence</h3>
 * Tracked patches are serialised to JSON and stored in the player's
 * RuneLite RS-profile config ({@code ConfigManager#setRSProfileConfiguration}).
 * They survive plugin restarts and client restarts.
 */
@PluginDescriptor(
        name = "Farming Tracker",
        description = "Tracks tree and fruit-tree patch grow timers and sends " +
                      "push notifications to your phone when they are ready to harvest.",
        tags = {"farming", "trees", "fruit", "notification", "webhook", "timer", "tracker"}
)
public class FarmingTrackerPlugin extends Plugin
{
    /**
     * Matches the planting chat message and captures the tree-type keyword.
     * Examples matched:
     * <ul>
     *   <li>"You plant a magic sapling in the tree patch."  → group(1) = "magic"</li>
     *   <li>"You plant an apple tree sapling in the fruit tree patch." → group(1) = "apple tree"</li>
     * </ul>
     */
    private static final Logger log = LoggerFactory.getLogger(FarmingTrackerPlugin.class);

    private static final Pattern PLANT_PATTERN =
            Pattern.compile("You plant (?:a|an) (.+?) sapling", Pattern.CASE_INSENSITIVE);

    private static final String CONFIG_KEY_PATCHES = "trackedPatches";

    // -----------------------------------------------------------------------
    // Injected dependencies
    // -----------------------------------------------------------------------

    @Inject private Client            client;
    @Inject private FarmingTrackerConfig config;
    @Inject private ConfigManager     configManager;
    @Inject private FarmingTrackerOverlay overlay;
    @Inject private OverlayManager    overlayManager;
    @Inject private Notifier          notifier;
    @Inject private WebhookService    webhookService;
    @Inject private SeedInventory     seedInventory;
    @Inject private PlantingOptimizer optimizer;
    @Inject private Gson              gson;

    // -----------------------------------------------------------------------
    // Mutable state (access only from the client thread)
    // -----------------------------------------------------------------------

    private final List<TrackedPatch> trackedPatches = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Plugin lifecycle
    // -----------------------------------------------------------------------

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        loadTrackedPatches();
        log.info("Farming Tracker started — tracking {} active patches.", trackedPatches.size());
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        saveTrackedPatches();
        trackedPatches.clear();
        log.info("Farming Tracker stopped.");
    }

    @Provides
    FarmingTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FarmingTrackerConfig.class);
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Detects sapling-planting events from the game-message chat channel
     * and starts a new grow timer.
     */
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        Matcher matcher = PLANT_PATTERN.matcher(event.getMessage());
        if (!matcher.find())
        {
            return;
        }

        String keyword = matcher.group(1).toLowerCase().trim();
        SeedItem seedItem = FarmingData.findSeedByPlantKey(keyword);
        if (seedItem == null)
        {
            log.debug("Unrecognised plant keyword '{}' — skipping.", keyword);
            return;
        }

        // Respect per-type toggle
        if (seedItem.getPatchType() == PatchType.TREE && !config.trackTreePatches())     return;
        if (seedItem.getPatchType() == PatchType.FRUIT_TREE && !config.trackFruitTreePatches()) return;

        // Identify the nearest matching patch by the player's current position
        WorldPoint pos = client.getLocalPlayer().getWorldLocation();
        FarmingPatch nearest = FarmingData.findNearestPatch(pos, seedItem.getPatchType());
        String patchName = nearest != null
                ? nearest.getName()
                : seedItem.getPatchType().getDisplayName() + " Patch (unknown location)";

        long now     = System.currentTimeMillis();
        long readyAt = now + ((long) seedItem.getGrowTimeMinutes() * 60_000L);

        TrackedPatch tracked = new TrackedPatch(
                UUID.randomUUID().toString(),
                keyword,
                patchName,
                seedItem.getPatchType(),
                now,
                readyAt,
                false
        );

        trackedPatches.add(tracked);
        saveTrackedPatches();
        seedInventory.decrementSapling(seedItem.getItemId());

        log.info("Tracking planted: {} at {} — ready {}",
                keyword, patchName, new Date(readyAt));
    }

    /**
     * Every game tick: promote any patches whose grow timer has elapsed
     * from GROWING → READY (one-shot notification).
     */
    @Subscribe
    public void onGameTick(GameTick ignored)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        checkReadyPatches();
    }

    /**
     * Keeps the seed inventory up-to-date whenever inventory or bank contents change.
     */
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        int id = event.getContainerId();
        if (id == InventoryID.INVENTORY.getId())
        {
            seedInventory.updateFromInventory(event.getItemContainer());
        }
        else if (id == InventoryID.BANK.getId() && config.bankTracking())
        {
            seedInventory.updateFromBank(event.getItemContainer());
            sendSeedUpdate();   // push fresh inventory to the PWA
        }
    }

    /**
     * Reload persisted patches after a login (covers the case where the
     * client was restarted between sessions).
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            loadTrackedPatches();
        }
    }

    // -----------------------------------------------------------------------
    // Public API used by the overlay
    // -----------------------------------------------------------------------

    /**
     * Returns an unmodifiable snapshot of the currently tracked patches.
     *
     * @return never {@code null}
     */
    public List<TrackedPatch> getTrackedPatches()
    {
        return Collections.unmodifiableList(trackedPatches);
    }

    /**
     * Returns the optimizer's current planting recommendations based on
     * empty patches and available saplings.
     *
     * @return never {@code null}; may be empty
     */
    public List<PlantingAdvice> getPlantingAdvice()
    {
        List<FarmingPatch> emptyPatches = buildEmptyPatchList();
        List<SeedItemWithQty> available = seedInventory.getAvailableSeeds();
        return optimizer.optimize(available, emptyPatches);
    }

    /**
     * Manually removes a tracked patch (e.g. after the player has chopped
     * the tree and no longer needs the timer).
     *
     * @param patchId UUID returned by {@link TrackedPatch#getId()}
     */
    public void clearPatch(String patchId)
    {
        boolean removed = trackedPatches.removeIf(p -> p.getId().equals(patchId));
        if (removed)
        {
            saveTrackedPatches();
            log.debug("Manually cleared patch {}", patchId);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void checkReadyPatches()
    {
        long now = System.currentTimeMillis();
        boolean anyNotified = false;

        for (TrackedPatch patch : trackedPatches)
        {
            if (!patch.isNotified() && now >= patch.getReadyAt())
            {
                patch.setNotified(true);
                anyNotified = true;

                String player = client.getLocalPlayer() != null
                        ? client.getLocalPlayer().getName()
                        : "Unknown";

                webhookService.sendPatchReady(patch, player);
                notifier.notify("[Farming Tracker] "
                        + capitalise(patch.getCropName())
                        + " at " + patch.getPatchName() + " is ready!");

                log.info("Patch ready: {} at {}", patch.getCropName(), patch.getPatchName());
            }
        }

        if (anyNotified)
        {
            saveTrackedPatches();
            sendSeedUpdate();
        }
    }

    private void sendSeedUpdate()
    {
        if (client.getLocalPlayer() == null) return;
        String player = client.getLocalPlayer().getName();
        webhookService.sendSeedUpdate(seedInventory.getAvailableSeeds(), player);
    }

    /**
     * Builds the list of patches considered empty by the optimizer:
     * those not currently occupied by a still-growing tracked patch.
     */
    private List<FarmingPatch> buildEmptyPatchList()
    {
        return FarmingData.getAllPatches().stream()
                .filter(p -> {
                    if (p.getType() == PatchType.TREE && !config.trackTreePatches())           return false;
                    if (p.getType() == PatchType.FRUIT_TREE && !config.trackFruitTreePatches()) return false;
                    // Patch is considered occupied if a non-notified (i.e. still growing) entry exists for it
                    return trackedPatches.stream()
                            .noneMatch(t -> t.getPatchName().equals(p.getName()) && !t.isNotified());
                })
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Config persistence
    // -----------------------------------------------------------------------

    private void loadTrackedPatches()
    {
        String json = configManager.getRSProfileConfiguration(
                FarmingTrackerConfig.GROUP, CONFIG_KEY_PATCHES);
        trackedPatches.clear();
        if (json == null || json.isBlank())
        {
            return;
        }
        try
        {
            Type listType = new TypeToken<List<TrackedPatch>>() {}.getType();
            List<TrackedPatch> loaded = gson.fromJson(json, listType);
            if (loaded != null)
            {
                trackedPatches.addAll(loaded);
            }
            log.debug("Loaded {} tracked patches from config.", trackedPatches.size());
        }
        catch (Exception e)
        {
            log.warn("Failed to deserialise tracked patches from config — resetting.", e);
        }
    }

    private void saveTrackedPatches()
    {
        configManager.setRSProfileConfiguration(
                FarmingTrackerConfig.GROUP, CONFIG_KEY_PATCHES,
                gson.toJson(trackedPatches));
    }

    // -----------------------------------------------------------------------

    private static String capitalise(String s)
    {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
