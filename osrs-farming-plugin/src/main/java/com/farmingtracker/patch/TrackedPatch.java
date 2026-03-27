package com.farmingtracker.patch;

/**
 * A live planting event persisted to the RuneLite config profile.
 * One instance is created each time the player plants a sapling and removed
 * once the patch has been cleared (harvested / chopped / died).
 */
public class TrackedPatch
{
    private String id;
    private String cropName;
    private String patchName;
    private PatchType patchType;
    private long plantedAt;
    private long readyAt;
    private boolean notified;

    /** Required for Gson deserialisation. */
    public TrackedPatch() {}

    public TrackedPatch(String id, String cropName, String patchName,
                        PatchType patchType, long plantedAt, long readyAt, boolean notified)
    {
        this.id        = id;
        this.cropName  = cropName;
        this.patchName = patchName;
        this.patchType = patchType;
        this.plantedAt = plantedAt;
        this.readyAt   = readyAt;
        this.notified  = notified;
    }

    public String    getId()        { return id; }
    public String    getCropName()  { return cropName; }
    public String    getPatchName() { return patchName; }
    public PatchType getPatchType() { return patchType; }
    public long      getPlantedAt() { return plantedAt; }
    public long      getReadyAt()   { return readyAt; }
    public boolean   isNotified()   { return notified; }

    public void setId(String id)              { this.id = id; }
    public void setCropName(String cropName)  { this.cropName = cropName; }
    public void setPatchName(String name)     { this.patchName = name; }
    public void setPatchType(PatchType t)     { this.patchType = t; }
    public void setPlantedAt(long plantedAt)  { this.plantedAt = plantedAt; }
    public void setReadyAt(long readyAt)      { this.readyAt = readyAt; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
