package com.farmingtracker.optimizer;

import com.farmingtracker.patch.FarmingPatch;
import com.farmingtracker.seed.SeedItem;

/**
 * A single recommendation produced by {@link PlantingOptimizer}.
 * Represents planting a specific sapling in a specific patch,
 * ranked by expected XP per hour.
 */
public final class PlantingAdvice
{
    private final FarmingPatch patch;
    private final SeedItem     seed;
    private final double       xpPerHour;

    public PlantingAdvice(FarmingPatch patch, SeedItem seed, double xpPerHour)
    {
        this.patch     = patch;
        this.seed      = seed;
        this.xpPerHour = xpPerHour;
    }

    /** The empty patch in which the sapling should be planted. */
    public FarmingPatch getPatch()     { return patch; }

    /** The sapling to plant. */
    public SeedItem getSeed()          { return seed; }

    /** Expected XP/hr from planting this combination. */
    public double getXpPerHour()       { return xpPerHour; }
}
