package com.farmingtracker.optimizer;

import com.farmingtracker.patch.FarmingPatch;
import com.farmingtracker.patch.PatchType;
import com.farmingtracker.seed.SeedItem;
import com.farmingtracker.seed.SeedItemWithQty;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the optimal planting order for maximum farming XP per hour.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Build a mutable copy of available sapling quantities.</li>
 *   <li>Sort available saplings by XP/hr descending.</li>
 *   <li>For each empty patch (trees first, then fruit trees), assign the
 *       highest-XP/hr sapling that:
 *       <ul>
 *         <li>matches the patch type, and</li>
 *         <li>still has remaining quantity.</li>
 *       </ul>
 *   </li>
 *   <li>Decrement the assigned sapling's remaining quantity.</li>
 *   <li>Return the ordered list of {@link PlantingAdvice} records.</li>
 * </ol>
 *
 * <p>Disease protection / farmer payment is intentionally not considered
 * (the user chose "always unprotected" during setup).
 */
@Slf4j
@Singleton
public class PlantingOptimizer
{
    /**
     * Produces an ordered list of planting recommendations.
     *
     * @param availableSaplings saplings in the player's inventory + bank
     * @param emptyPatches      farming patches not currently occupied
     * @return advice list sorted by XP/hr descending; may be empty
     */
    public List<PlantingAdvice> optimize(
            List<SeedItemWithQty> availableSaplings,
            List<FarmingPatch> emptyPatches)
    {
        if (availableSaplings.isEmpty() || emptyPatches.isEmpty())
        {
            return Collections.emptyList();
        }

        // Mutable quantity map so we don't over-assign the same sapling
        Map<SeedItem, Integer> remaining = new HashMap<>();
        for (SeedItemWithQty sqty : availableSaplings)
        {
            remaining.put(sqty.getSeed(), sqty.getQuantity());
        }

        // Sort saplings best-first (highest XP/hr)
        List<SeedItem> rankedSeeds = new ArrayList<>(remaining.keySet());
        rankedSeeds.sort(Comparator.comparingDouble(SeedItem::getXpPerHour).reversed());

        List<PlantingAdvice> advice = new ArrayList<>();

        for (FarmingPatch patch : emptyPatches)
        {
            SeedItem best = findBestSeedForPatch(patch.getType(), rankedSeeds, remaining);
            if (best != null)
            {
                advice.add(new PlantingAdvice(patch, best, best.getXpPerHour()));
                remaining.merge(best, -1, Integer::sum);
                if (remaining.get(best) <= 0)
                {
                    remaining.remove(best);
                    rankedSeeds.remove(best);
                }
            }
        }

        log.debug("Optimizer produced {} advice entries for {} empty patches",
                advice.size(), emptyPatches.size());
        return advice;
    }

    // -----------------------------------------------------------------------

    private SeedItem findBestSeedForPatch(
            PatchType type,
            List<SeedItem> rankedSeeds,
            Map<SeedItem, Integer> remaining)
    {
        for (SeedItem seed : rankedSeeds)
        {
            if (seed.getPatchType() == type && remaining.getOrDefault(seed, 0) > 0)
            {
                return seed;
            }
        }
        return null;
    }
}
