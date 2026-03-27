package com.farmingtracker.seed;

/**
 * A {@link SeedItem} paired with the player's current available quantity.
 * Returned by {@link SeedInventory#getAvailableSeeds()}.
 */
public final class SeedItemWithQty
{
    private final SeedItem seed;
    private final int      quantity;

    public SeedItemWithQty(SeedItem seed, int quantity)
    {
        this.seed     = seed;
        this.quantity = quantity;
    }

    public SeedItem getSeed()     { return seed; }
    public int      getQuantity() { return quantity; }
}
