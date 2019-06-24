package nl.Aurorion.BlockRegen.System;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

public class RegenProcess {

    private Location loc;
    private BukkitTask task;
    private Material material;

    public RegenProcess(Location loc, BukkitTask task, Material material) {
        this.loc = loc;
        this.task = task;
        this.material = material;
    }

    public void cancelTask() {
        task.cancel();
    }

    public Location getLoc() {
        return loc;
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
