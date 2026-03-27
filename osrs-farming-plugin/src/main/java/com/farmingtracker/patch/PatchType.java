package com.farmingtracker.patch;

/** The category of a farming patch tracked by this plugin. */
public enum PatchType
{
    TREE("Tree"),
    FRUIT_TREE("Fruit Tree");

    private final String displayName;

    PatchType(String displayName)
    {
        this.displayName = displayName;
    }

    /** Human-readable label used in overlays and webhook payloads. */
    public String getDisplayName()
    {
        return displayName;
    }
}
