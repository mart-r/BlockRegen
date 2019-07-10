package nl.Aurorion.BlockRegen.Particles.breaking;

import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class FireWorks implements Runnable {

    private Main main;
    private Block block;

    public FireWorks(Main instance, Block block) {
        this.main = instance;
        this.block = block;
    }

    @Override
    public void run() {
        World world = block.getWorld();
        Location loc = block.getLocation();
        loc.add(0.5, 0.5, 0.5);
        Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK);
        loc.subtract(0.5, 0.5, 0.5);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder()
                .with(Type.BALL)
                .withColor(Utils.colors.get(new Random().nextInt(Utils.colors.size())))
                .withFade(Color.WHITE)
                .flicker(true)
                .build());
        fw.setFireworkMeta(fwm);
        new BukkitRunnable() {

            @Override
            public void run() {
                fw.detonate();
            }

        }.runTaskLaterAsynchronously(main, 2L);
    }
}
