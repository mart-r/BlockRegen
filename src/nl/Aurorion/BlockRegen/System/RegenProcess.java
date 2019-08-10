package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

public class RegenProcess {

    /*
    * RegenProcess object is used to store information about currently running regeneration tasks.
    * List of processes running is in Utils.java
    * */

    // Location, where to regen?
    private Location loc;

    // Bukkit task responsible for regening
    private BukkitTask task;

    // Material to regen
    private Material material;

    // BlockBR format to act by
    private BlockBR blockBR;

    // System time, when the block regenerates, probably.
    // Can differentiate based on TPS? I guess?
    private long regenTime;

    public RegenProcess(Location loc, BukkitTask task, Material material, BlockBR blockBR, long regenTime) {
        this.loc = loc;
        this.task = task;
        this.material = material;
        this.regenTime = regenTime;
        this.blockBR = blockBR;
    }

    // Todo integrate
    public String getUntilRegenFormatted() {
        return secsUntilRegen() + "s";
    }

    // Seconds to regen
    public double secsUntilRegen() {
        return (regenTime - System.currentTimeMillis()) / 1000;
    }

    public Location getLoc() {
        return loc;
    }

    public Material getMaterial() {
        return material;
    }

    public BlockBR getBlockBR() {
        return blockBR;
    }
}
