package com.farmingtracker.patch;

/** The current runtime state of a tracked farming patch. */
public enum PatchState
{
    GROWING("Growing"),
    READY("Ready"),
    DISEASED("Diseased");

    private final String displayName;

    PatchState(String displayName)
    {
        this.displayName = displayName;
    }

    /** Human-readable label used in overlays. */
    public String getDisplayName()
    {
        return displayName;
    }
}
