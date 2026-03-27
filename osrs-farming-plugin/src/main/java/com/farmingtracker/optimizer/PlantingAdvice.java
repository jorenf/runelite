package com.farmingtracker.optimizer;

import com.farmingtracker.patch.FarmingPatch;
import com.farmingtracker.seed.SeedItem;
import lombok.Value;

/**
 * A single recommendation produced by {@link PlantingOptimizer}.
 * Represents planting a specific sapling in a specific patch,
 * ranked by expected XP per hour.
 */
@Value
public class PlantingAdvice
{
    /**
     * The patch in which the sapling should be planted.
     * This patch was empty at the time the optimizer ran.
     */
    FarmingPatch patch;

    /**
     * The sapling to plant in this patch.
     * The player must have at least one of this sapling in their
     * inventory or bank for this advice to appear.
     */
    SeedItem seed;

    /**
     * Farming XP per hour expected from planting this seed in this patch.
     * Calculated as {@code harvestXp / (growTimeMinutes / 60.0)}.
     * Used to rank recommendations highest-first.
     */
    double xpPerHour;
}
