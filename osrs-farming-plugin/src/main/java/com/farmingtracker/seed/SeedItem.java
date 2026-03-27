package com.farmingtracker.seed;

import com.farmingtracker.patch.PatchType;
import lombok.Value;

/**
 * Immutable descriptor for a tree seed or sapling that can be planted
 * in a farming patch. All instances are defined in
 * {@link com.farmingtracker.data.FarmingData}.
 *
 * <p>Grow times come from RuneLite's {@code Produce.java}:
 * trees use a 40-minute tick-rate; fruit trees use 160 minutes.
 *
 * <p>XP values are the farming XP awarded when the tree is chopped down
 * (regular trees) or when the player checks the health of a fully-grown
 * fruit tree. Source: OSRS Wiki.
 */
@Value
public class SeedItem
{
    /** Human-readable name, e.g. "Magic sapling" or "Dragonfruit sapling". */
    String displayName;

    /**
     * RuneLite {@code ItemID} for the sapling (the item planted in the patch).
     * Verify against {@code net.runelite.api.ItemID} if item IDs are
     * wrong after a game update.
     */
    int itemId;

    /** The type of patch this sapling can be planted in. */
    PatchType patchType;

    /**
     * Farming XP awarded on harvest / health-check of the fully-grown tree.
     * Used for XP/hr calculations in the planting optimizer.
     */
    double harvestXp;

    /**
     * Total minutes from planting to fully-grown, based on:
     * <pre>stages × tickRateMinutes</pre>
     * (stages and tick-rates from RuneLite {@code Produce.java}).
     */
    int growTimeMinutes;

    /**
     * Lowercase keyword extracted from the planting chat message.
     * E.g. the message "You plant a magic sapling in the tree patch." yields
     * the key {@code "magic"}.
     * Fruit trees use multi-word keys such as {@code "apple tree"}.
     */
    String plantMessageKey;

    /**
     * Calculates the effective XP per hour for this crop.
     *
     * @return {@code harvestXp / (growTimeMinutes / 60.0)}
     */
    public double getXpPerHour()
    {
        return harvestXp / (growTimeMinutes / 60.0);
    }
}
