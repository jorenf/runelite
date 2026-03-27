package com.farmingtracker.patch;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The current runtime state of a tracked farming patch.
 */
@Getter
@RequiredArgsConstructor
public enum PatchState
{
    GROWING("Growing"),
    READY("Ready"),
    DISEASED("Diseased");

    /** Human-readable label used in overlays. */
    private final String displayName;
}
