package com.farmingtracker.seed;

import com.farmingtracker.patch.PatchType;

/**
 * Immutable descriptor for a tree sapling that can be planted in a farming patch.
 * All instances are defined in {@link com.farmingtracker.data.FarmingData}.
 */
public final class SeedItem
{
    private final String    displayName;
    private final int       itemId;
    private final PatchType patchType;
    private final double    harvestXp;
    private final int       growTimeMinutes;
    private final String    plantMessageKey;

    public SeedItem(String displayName, int itemId, PatchType patchType,
                    double harvestXp, int growTimeMinutes, String plantMessageKey)
    {
        this.displayName     = displayName;
        this.itemId          = itemId;
        this.patchType       = patchType;
        this.harvestXp       = harvestXp;
        this.growTimeMinutes = growTimeMinutes;
        this.plantMessageKey = plantMessageKey;
    }

    /** Human-readable name, e.g. "Magic sapling". */
    public String getDisplayName()     { return displayName; }

    /** RuneLite ItemID for the sapling item. */
    public int getItemId()             { return itemId; }

    /** The type of patch this sapling can be planted in. */
    public PatchType getPatchType()    { return patchType; }

    /** Farming XP awarded on harvest / health-check of the fully-grown tree. */
    public double getHarvestXp()       { return harvestXp; }

    /** Total grow time in minutes (stages × tick-rate from RuneLite Produce.java). */
    public int getGrowTimeMinutes()    { return growTimeMinutes; }

    /**
     * Lower-case keyword extracted from the planting chat message.
     * E.g. "You plant a magic sapling …" → {@code "magic"}.
     */
    public String getPlantMessageKey() { return plantMessageKey; }

    /** Farming XP per hour: {@code harvestXp / (growTimeMinutes / 60.0)}. */
    public double getXpPerHour()
    {
        return harvestXp / (growTimeMinutes / 60.0);
    }

    @Override
    public String toString()
    {
        return "SeedItem{" + displayName + "}";
    }
}
