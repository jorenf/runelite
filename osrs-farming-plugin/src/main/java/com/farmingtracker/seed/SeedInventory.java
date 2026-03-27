package com.farmingtracker.seed;

import com.farmingtracker.data.FarmingData;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the player's current supply of tree and fruit-tree saplings
 * across their inventory and (optionally) their bank.
 */
@Singleton
public class SeedInventory
{
    private static final Logger log = LoggerFactory.getLogger(SeedInventory.class);

    private final Map<Integer, Integer> inventoryQuantities = new HashMap<>();
    private final Map<Integer, Integer> bankQuantities      = new HashMap<>();

    /**
     * Updates sapling counts from the player's inventory container.
     *
     * @param container the inventory {@link ItemContainer}
     */
    public void updateFromInventory(ItemContainer container)
    {
        inventoryQuantities.clear();
        countItems(container, inventoryQuantities);
        log.debug("Inventory saplings: {}", inventoryQuantities);
    }

    /**
     * Updates sapling counts from the player's bank container.
     *
     * @param container the bank {@link ItemContainer}
     */
    public void updateFromBank(ItemContainer container)
    {
        bankQuantities.clear();
        countItems(container, bankQuantities);
        log.debug("Bank saplings: {}", bankQuantities);
    }

    /**
     * Decrements the inventory count of the given item by one immediately
     * after a planting is detected (before the next container event fires).
     *
     * @param itemId the sapling that was just planted
     */
    public void decrementSapling(int itemId)
    {
        inventoryQuantities.merge(itemId, -1, Integer::sum);
        if (inventoryQuantities.getOrDefault(itemId, 0) <= 0)
        {
            inventoryQuantities.remove(itemId);
        }
    }

    /**
     * Returns all saplings currently available (inventory + bank), with quantities.
     *
     * @return a new list; modifications do not affect internal state
     */
    public List<SeedItemWithQty> getAvailableSeeds()
    {
        List<SeedItemWithQty> result = new ArrayList<>();
        for (SeedItem seed : FarmingData.ALL_SAPLINGS)
        {
            int qty = inventoryQuantities.getOrDefault(seed.getItemId(), 0)
                    + bankQuantities.getOrDefault(seed.getItemId(), 0);
            if (qty > 0)
            {
                result.add(new SeedItemWithQty(seed, qty));
            }
        }
        return result;
    }

    private void countItems(ItemContainer container, Map<Integer, Integer> target)
    {
        for (Item item : container.getItems())
        {
            if (item.getId() > 0 && FarmingData.SAPLING_IDS.contains(item.getId()))
            {
                target.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }
    }
}
