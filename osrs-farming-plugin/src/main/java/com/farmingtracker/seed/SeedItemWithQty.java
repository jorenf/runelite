package com.farmingtracker.seed;

import lombok.Value;

/**
 * A {@link SeedItem} paired with the player's current available quantity.
 * Returned by {@link SeedInventory#getAvailableSeeds()} and used both
 * in the planting optimizer and in the webhook seed-update payload.
 */
@Value
public class SeedItemWithQty
{
    SeedItem seed;
    int quantity;
}
