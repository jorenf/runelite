package com.farmingtracker.patch;

import lombok.Value;

/**
 * Immutable descriptor for a known farming patch location in the game world.
 * All instances are defined in {@link com.farmingtracker.data.FarmingData}.
 */
@Value
public class FarmingPatch
{
    /** Display name shown in overlays and webhook payloads, e.g. "Falador Tree Patch". */
    String name;

    /** Whether this is a regular tree or fruit-tree patch. */
    PatchType type;

    /**
     * Approximate world tile coordinates (x, y, plane).
     * Used to identify which patch the player is standing near when they plant.
     * Verify these against the OSRS Wiki "Farming" article if patches are mis-identified.
     */
    int worldX;
    int worldY;
    int plane;
}
