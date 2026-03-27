package com.farmingtracker.patch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A live planting event persisted to the RuneLite config profile.
 * One instance is created each time the player plants a sapling and removed
 * once the patch has been cleared (harvested/chopped/died).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackedPatch
{
    /** Stable random UUID assigned at plant time. Used as a config key. */
    private String id;

    /** Crop name as captured from the planting chat message, e.g. "magic". */
    private String cropName;

    /**
     * Name of the nearest known patch at plant time,
     * e.g. "Falador Tree Patch". Falls back to the PatchType display name
     * when no patch is close enough.
     */
    private String patchName;

    /** Category of the patch. */
    private PatchType patchType;

    /** Epoch-millisecond timestamp when the sapling was planted. */
    private long plantedAt;

    /**
     * Epoch-millisecond timestamp when the tree should be fully grown.
     * Calculated as {@code plantedAt + (growTimeMinutes * 60_000)}.
     */
    private long readyAt;

    /**
     * Whether the "ready" webhook + in-game notification has already been sent.
     * Prevents duplicate notifications across multiple game ticks.
     */
    private boolean notified;
}
