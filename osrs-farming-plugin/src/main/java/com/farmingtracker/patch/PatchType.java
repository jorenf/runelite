package com.farmingtracker.patch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The category of a farming patch tracked by this plugin.
 * Only tree and fruit-tree patches are tracked (per user configuration).
 */
@Getter
@RequiredArgsConstructor
public enum PatchType
{
    TREE("Tree"),
    FRUIT_TREE("Fruit Tree");

    /** Human-readable label used in overlays and webhook payloads. */
    private final String displayName;
}
