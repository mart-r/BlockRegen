package nl.Aurorion.BlockRegen.Particles.breaking;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class WitchSpell implements Runnable {

    int points = 40;
    double radius = 0.5d;
    private Location location;

    public WitchSpell(Location location) {
        this.location = location;
    }

    @Override
    public void run() {
        Location loc = location;
        World world = loc.getWorld();

        loc.add(0.5, 0.5, 0.5);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location point = loc.clone().add(radius * Math.sin(angle), 0.0d, radius * Math.cos(angle));
            world.spawnParticle(Particle.SPELL_WITCH, point, 1, 0, 0, 0);
        }
        loc.subtract(0.5, 0.5, 0.5);

    }
}
