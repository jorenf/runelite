package com.farmingtracker.optimizer;

import com.farmingtracker.patch.FarmingPatch;
import com.farmingtracker.patch.PatchType;
import com.farmingtracker.seed.SeedItem;
import com.farmingtracker.seed.SeedItemWithQty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the optimal planting order for maximum farming XP per hour.
 * Disease protection / farmer payment is not considered (always unprotected).
 */
@Singleton
public class PlantingOptimizer
{
    private static final Logger log = LoggerFactory.getLogger(PlantingOptimizer.class);

    /**
     * Produces an ordered list of planting recommendations.
     *
     * @param availableSaplings saplings in inventory + bank
     * @param emptyPatches      patches not currently occupied
     * @return advice sorted by XP/hr descending; may be empty
     */
    public List<PlantingAdvice> optimize(
            List<SeedItemWithQty> availableSaplings,
            List<FarmingPatch> emptyPatches)
    {
        if (availableSaplings.isEmpty() || emptyPatches.isEmpty())
        {
            return Collections.emptyList();
        }

        // Mutable quantity map to avoid over-assigning the same sapling
        Map<SeedItem, Integer> remaining = new HashMap<>();
        for (SeedItemWithQty sqty : availableSaplings)
        {
            remaining.put(sqty.getSeed(), sqty.getQuantity());
        }

        // Sort best-first (highest XP/hr)
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

        log.debug("Optimizer: {} advice entries for {} empty patches",
                advice.size(), emptyPatches.size());
        return advice;
    }

    private SeedItem findBestSeedForPatch(PatchType type,
                                           List<SeedItem> ranked,
                                           Map<SeedItem, Integer> remaining)
    {
        for (SeedItem seed : ranked)
        {
            if (seed.getPatchType() == type && remaining.getOrDefault(seed, 0) > 0)
            {
                return seed;
            }
        }
        return null;
    }
}
