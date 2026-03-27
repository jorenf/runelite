package com.farmingtracker.seed;

import com.farmingtracker.data.FarmingData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the player's current supply of tree and fruit-tree saplings
 * across their inventory and (optionally) their bank.
 *
 * <p>Quantities are merged: a sapling found in both inventory and bank
 * will show the combined total.
 */
@Slf4j
@Singleton
public class SeedInventory
{
    /** Quantities held in the player's inventory (itemId → count). */
    private final Map<Integer, Integer> inventoryQuantities = new HashMap<>();

    /** Quantities held in the player's bank (itemId → count). */
    private final Map<Integer, Integer> bankQuantities = new HashMap<>();

    /**
     * Updates sapling counts from the player's inventory container.
     *
     * @param container the inventory {@link ItemContainer}, never {@code null}
     */
    public void updateFromInventory(ItemContainer container)
    {
        inventoryQuantities.clear();
        countItems(container, inventoryQuantities);
        log.debug("Inventory saplings: {}", inventoryQuantities);
    }

    /**
     * Updates sapling counts from the player's bank container.
     * Only called when {@code bankTracking} is enabled in config.
     *
     * @param container the bank {@link ItemContainer}, never {@code null}
     */
    public void updateFromBank(ItemContainer container)
    {
        bankQuantities.clear();
        countItems(container, bankQuantities);
        log.debug("Bank saplings: {}", bankQuantities);
    }

    /**
     * Decrements the inventory count of the given item by one.
     * Called immediately after a planting is detected so that the
     * optimizer reflects the change before the next container update.
     *
     * @param itemId the item ID of the sapling that was just planted
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
     * Returns all saplings the player currently has available (inventory + bank),
     * with non-zero quantities populated on each {@link SeedItem}.
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

    // -----------------------------------------------------------------------

    private void countItems(ItemContainer container, Map<Integer, Integer> target)
    {
        for (Item item : container.getItems())
        {
            if (item.getId() <= 0)
            {
                continue;
            }
            // Only count items that are known saplings
            if (FarmingData.SAPLING_IDS.contains(item.getId()))
            {
                target.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }
    }
}
