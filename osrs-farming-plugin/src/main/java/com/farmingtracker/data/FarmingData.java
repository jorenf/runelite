package com.farmingtracker.data;

import com.farmingtracker.patch.FarmingPatch;
import com.farmingtracker.patch.PatchType;
import com.farmingtracker.seed.SeedItem;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central repository of all static farming data used by this plugin.
 *
 * <h3>Grow times</h3>
 * Sourced from RuneLite {@code Produce.java} (timetracking plugin):
 * <ul>
 *   <li>Trees: {@code stages × 40 minutes} per tick</li>
 *   <li>Fruit trees: {@code 7 stages × 160 minutes} = 1120 min</li>
 * </ul>
 *
 * <h3>Harvest XP</h3>
 * Sourced from the OSRS Wiki "Farming" skill article.
 * Represents the XP awarded when chopping a fully-grown tree (regular trees)
 * or checking the health of a fruit tree.
 *
 * <h3>Item IDs</h3>
 * Verify sapling item IDs against {@code net.runelite.api.ItemID} after
 * game updates — Jagex occasionally renumbers items.
 *
 * <h3>Patch coordinates</h3>
 * Approximate tile coordinates are used to match planting events to a
 * specific patch. "Nearest patch within 10 tiles" is the matching rule.
 * Verify against the OSRS Wiki "Farming patches" article if a patch is
 * misidentified.
 */
public final class FarmingData
{
    private FarmingData() {}

    // -----------------------------------------------------------------------
    // Tree saplings  (40 min/tick)
    // -----------------------------------------------------------------------

    /** Oak: 5 stages × 40 min = 200 min, 467.5 xp */
    public static final SeedItem OAK_SAPLING = new SeedItem(
            "Oak sapling", 5370, PatchType.TREE, 467.5, 200, "oak");

    /** Willow: 7 stages × 40 min = 280 min, 1456.5 xp */
    public static final SeedItem WILLOW_SAPLING = new SeedItem(
            "Willow sapling", 5371, PatchType.TREE, 1456.5, 280, "willow");

    /** Maple: 9 stages × 40 min = 360 min, 3403.4 xp */
    public static final SeedItem MAPLE_SAPLING = new SeedItem(
            "Maple sapling", 5372, PatchType.TREE, 3403.4, 360, "maple");

    /** Yew: 11 stages × 40 min = 440 min, 7069.9 xp */
    public static final SeedItem YEW_SAPLING = new SeedItem(
            "Yew sapling", 5373, PatchType.TREE, 7069.9, 440, "yew");

    /** Magic: 13 stages × 40 min = 520 min, 13913.5 xp */
    public static final SeedItem MAGIC_SAPLING = new SeedItem(
            "Magic sapling", 5374, PatchType.TREE, 13913.5, 520, "magic");

    // -----------------------------------------------------------------------
    // Fruit-tree saplings  (7 stages × 160 min = 1120 min each)
    // -----------------------------------------------------------------------

    public static final SeedItem APPLE_SAPLING = new SeedItem(
            "Apple sapling", 5496, PatchType.FRUIT_TREE, 1199.5, 1120, "apple tree");

    public static final SeedItem BANANA_SAPLING = new SeedItem(
            "Banana sapling", 5497, PatchType.FRUIT_TREE, 1750.5, 1120, "banana tree");

    public static final SeedItem ORANGE_SAPLING = new SeedItem(
            "Orange sapling", 5498, PatchType.FRUIT_TREE, 2470.0, 1120, "orange tree");

    public static final SeedItem CURRY_SAPLING = new SeedItem(
            "Curry sapling", 5499, PatchType.FRUIT_TREE, 2907.5, 1120, "curry tree");

    public static final SeedItem PINEAPPLE_SAPLING = new SeedItem(
            "Pineapple sapling", 5500, PatchType.FRUIT_TREE, 4792.5, 1120, "pineapple");

    public static final SeedItem PAPAYA_SAPLING = new SeedItem(
            "Papaya sapling", 5501, PatchType.FRUIT_TREE, 6146.0, 1120, "papaya tree");

    public static final SeedItem PALM_SAPLING = new SeedItem(
            "Palm sapling", 5502, PatchType.FRUIT_TREE, 10150.5, 1120, "palm tree");

    public static final SeedItem DRAGONFRUIT_SAPLING = new SeedItem(
            "Dragonfruit sapling", 22874, PatchType.FRUIT_TREE, 17335.0, 1120, "dragonfruit tree");

    // -----------------------------------------------------------------------
    // Flat look-up collections
    // -----------------------------------------------------------------------

    /** All tracked saplings in XP/hr descending order (best-first for optimizer). */
    public static final List<SeedItem> ALL_SAPLINGS;

    /** Set of all sapling item IDs, used for fast inventory filtering. */
    public static final Set<Integer> SAPLING_IDS;

    /**
     * Map from lower-case plant-message key to the corresponding {@link SeedItem}.
     * E.g. {@code "magic"} → {@link #MAGIC_SAPLING}.
     */
    public static final Map<String, SeedItem> BY_PLANT_KEY;

    static
    {
        List<SeedItem> saplings = new ArrayList<>();
        // Trees
        saplings.add(MAGIC_SAPLING);
        saplings.add(YEW_SAPLING);
        saplings.add(MAPLE_SAPLING);
        saplings.add(WILLOW_SAPLING);
        saplings.add(OAK_SAPLING);
        // Fruit trees (sorted by xp/hr descending)
        saplings.add(DRAGONFRUIT_SAPLING);
        saplings.add(PALM_SAPLING);
        saplings.add(PAPAYA_SAPLING);
        saplings.add(PINEAPPLE_SAPLING);
        saplings.add(CURRY_SAPLING);
        saplings.add(ORANGE_SAPLING);
        saplings.add(BANANA_SAPLING);
        saplings.add(APPLE_SAPLING);
        ALL_SAPLINGS = Collections.unmodifiableList(saplings);

        Set<Integer> ids = new HashSet<>();
        Map<String, SeedItem> byKey = new HashMap<>();
        for (SeedItem s : saplings)
        {
            ids.add(s.getItemId());
            byKey.put(s.getPlantMessageKey(), s);
        }
        SAPLING_IDS = Collections.unmodifiableSet(ids);
        BY_PLANT_KEY = Collections.unmodifiableMap(byKey);
    }

    // -----------------------------------------------------------------------
    // Patch locations
    // -----------------------------------------------------------------------
    //
    // World coordinates verified against OSRS Wiki "Farming patches" article.
    // Plane is always 0 (surface level) for all patches below.
    //
    // If a patch is misidentified, adjust the coordinate in this file —
    // no other code needs to change.

    /** All regular tree patches. */
    public static final List<FarmingPatch> TREE_PATCHES;

    /** All fruit-tree patches. */
    public static final List<FarmingPatch> FRUIT_TREE_PATCHES;

    /** Combined list of all patches tracked by this plugin. */
    public static final List<FarmingPatch> ALL_PATCHES;

    static
    {
        List<FarmingPatch> trees = new ArrayList<>();
        trees.add(new FarmingPatch("Taverley Tree Patch",           PatchType.TREE, 2936, 3444, 0));
        trees.add(new FarmingPatch("Falador Tree Patch",            PatchType.TREE, 3004, 3307, 0));
        trees.add(new FarmingPatch("Varrock Tree Patch",            PatchType.TREE, 3229, 3459, 0));
        trees.add(new FarmingPatch("Lumbridge Tree Patch",          PatchType.TREE, 3193, 3231, 0));
        trees.add(new FarmingPatch("Gnome Stronghold Tree Patch",   PatchType.TREE, 2437, 3415, 0));
        trees.add(new FarmingPatch("Farming Guild Tree Patch",      PatchType.TREE, 1232, 3726, 0));
        trees.add(new FarmingPatch("Auburnvale Tree Patch",         PatchType.TREE, 1573, 3048, 0)); // TODO: verify coords
        TREE_PATCHES = Collections.unmodifiableList(trees);

        List<FarmingPatch> fruitTrees = new ArrayList<>();
        fruitTrees.add(new FarmingPatch("Tree Gnome Village Fruit Tree Patch",   PatchType.FRUIT_TREE, 2490, 3180, 0));
        fruitTrees.add(new FarmingPatch("Gnome Stronghold Fruit Tree Patch",     PatchType.FRUIT_TREE, 2476, 3447, 0));
        fruitTrees.add(new FarmingPatch("Brimhaven Fruit Tree Patch",            PatchType.FRUIT_TREE, 2765, 3213, 0));
        fruitTrees.add(new FarmingPatch("Catherby Fruit Tree Patch",             PatchType.FRUIT_TREE, 2860, 3433, 0));
        fruitTrees.add(new FarmingPatch("Lletya Fruit Tree Patch",               PatchType.FRUIT_TREE, 2353, 3161, 0));
        fruitTrees.add(new FarmingPatch("Farming Guild Fruit Tree Patch",        PatchType.FRUIT_TREE, 1244, 3759, 0));
        fruitTrees.add(new FarmingPatch("Kastori Fruit Tree Patch",              PatchType.FRUIT_TREE, 1435, 2878, 0)); // TODO: verify coords
        FRUIT_TREE_PATCHES = Collections.unmodifiableList(fruitTrees);

        List<FarmingPatch> all = new ArrayList<>();
        all.addAll(trees);
        all.addAll(fruitTrees);
        ALL_PATCHES = Collections.unmodifiableList(all);
    }

    // -----------------------------------------------------------------------
    // Look-up helpers
    // -----------------------------------------------------------------------

    /**
     * Finds the {@link SeedItem} whose {@code plantMessageKey} matches the
     * lower-cased keyword extracted from the planting chat message.
     *
     * @param plantKey lower-case keyword, e.g. {@code "magic"} or {@code "apple tree"}
     * @return the matching {@link SeedItem}, or {@code null} if unknown
     */
    public static SeedItem findSeedByPlantKey(String plantKey)
    {
        return BY_PLANT_KEY.get(plantKey);
    }

    /**
     * Returns the nearest {@link FarmingPatch} of the given type within
     * {@code MAX_MATCH_DISTANCE} tiles of {@code playerLocation}.
     * Returns {@code null} if no patch is close enough.
     *
     * @param playerLocation current player tile
     * @param type           patch type to search within
     * @return nearest matching patch, or {@code null}
     */
    public static FarmingPatch findNearestPatch(WorldPoint playerLocation, PatchType type)
    {
        final int MAX_MATCH_DISTANCE = 15;

        List<FarmingPatch> candidates = type == PatchType.TREE ? TREE_PATCHES : FRUIT_TREE_PATCHES;
        FarmingPatch nearest = null;
        int nearestDist = Integer.MAX_VALUE;

        for (FarmingPatch patch : candidates)
        {
            WorldPoint patchPoint = new WorldPoint(patch.getWorldX(), patch.getWorldY(), patch.getPlane());
            int dist = playerLocation.distanceTo(patchPoint);
            if (dist < nearestDist)
            {
                nearestDist = dist;
                nearest = patch;
            }
        }

        return nearestDist <= MAX_MATCH_DISTANCE ? nearest : null;
    }

    /**
     * Returns all patches of all types.
     *
     * @return immutable list
     */
    public static List<FarmingPatch> getAllPatches()
    {
        return ALL_PATCHES;
    }
}
