package nl.Aurorion.BlockRegen.Particles;

import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Particles.breaking.FireWorks;
import nl.Aurorion.BlockRegen.Particles.breaking.FlameCrown;
import nl.Aurorion.BlockRegen.Particles.breaking.WitchSpell;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class ParticleUtil {

    private Main main;

    public ParticleUtil(Main instance) {
        this.main = instance;
    }

    // Particle task running
    private HashMap<Location, BukkitTask> tasks = new HashMap<>();

    public void check(String particleName, Location location) {

        if (tasks.containsKey(location))
            Bukkit.getScheduler().cancelTask(tasks.get(location).getTaskId());

        BukkitTask task;

        switch (particleName) {
            case "flame_crown":
                task = Bukkit.getScheduler().runTaskAsynchronously(main, new FlameCrown(location));
                break;
            case "fireworks":
                task = Bukkit.getScheduler().runTask(main, new FireWorks(main, location));
                break;
            case "witch_spell":
                task = Bukkit.getScheduler().runTask(main, new WitchSpell(location));
                break;
            default:
                return;
        }

        tasks.put(location, task);
    }
}
