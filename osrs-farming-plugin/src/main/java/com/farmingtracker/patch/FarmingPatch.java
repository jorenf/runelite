package com.farmingtracker.patch;

/**
 * Immutable descriptor for a known farming patch location in the game world.
 * All instances are defined in {@link com.farmingtracker.data.FarmingData}.
 */
public final class FarmingPatch
{
    private final String name;
    private final PatchType type;
    private final int worldX;
    private final int worldY;
    private final int plane;

    public FarmingPatch(String name, PatchType type, int worldX, int worldY, int plane)
    {
        this.name   = name;
        this.type   = type;
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane  = plane;
    }

    /** Display name shown in overlays and webhook payloads, e.g. "Falador Tree Patch". */
    public String getName()   { return name; }

    /** Whether this is a regular tree or fruit-tree patch. */
    public PatchType getType() { return type; }

    /** World tile X coordinate (approximate). */
    public int getWorldX()    { return worldX; }

    /** World tile Y coordinate (approximate). */
    public int getWorldY()    { return worldY; }

    /** Plane (0 = surface). */
    public int getPlane()     { return plane; }

    @Override
    public String toString()
    {
        return "FarmingPatch{name='" + name + "', type=" + type + "}";
    }
}
