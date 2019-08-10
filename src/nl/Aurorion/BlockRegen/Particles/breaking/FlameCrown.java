package nl.Aurorion.BlockRegen.Particles.breaking;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class FlameCrown implements Runnable {

    int points = 15;
    double radius = 0.5d;
    private Location location;

    public FlameCrown(Location location) {
        this.location = location;
    }

    @Override
    public void run() {
        Location loc = location;
        Main.getInstance().cO.debug("Particle calculation..");
        World world = loc.getWorld();

        loc.add(0.5, 1.2, 0.5);

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location point = loc.clone().add(radius * Math.sin(angle), 0.0d, radius * Math.cos(angle));
            world.spawnParticle(Particle.FLAME, point, 1, 0, 0, 0, 0.0D);
        }

        loc.subtract(0.5, 1.2, 0.5);
    }
}
