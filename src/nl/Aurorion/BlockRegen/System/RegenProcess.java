package nl.Aurorion.BlockRegen.System;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

public class RegenProcess {

    private Location loc;
    private BukkitTask task;
    private Material material;
    private byte data;

    public RegenProcess(Location loc, BukkitTask task, Material material, Byte data) {
        this.loc = loc;
        this.task = task;
        this.material = material;
        this.data = data;
    }

    public byte getData() {
        return data;
    }

    public void setData(byte data) {
        this.data = data;
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
