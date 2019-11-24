package nl.Aurorion.BlockRegen.Particles;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;

public class ParticleBR {

    private Particle type;

    // Show only to the breaking player
    private boolean playerOnly;

    private Shape shape;

    private int extra = 0;
    private int particleCount = 10;
    private int speed = 0;

    // Redstone particle only
    private Color color;
    private int size;

    // Frame shape only
    private double space;

    // Task data
    private int period;
    private int delay;
    private int count;

    private double x = 1, y = 1, z = 1;

    private HashMap<Location, BukkitTask> tasks;

    private boolean valid = false;

    public ParticleBR(String type, String shape) {
        try {
            this.type = Particle.valueOf(type);
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn("Particle type " + type + " is not valid, skipping.");
            return;
        }

        this.shape = Shape.fromString(shape);

        if (shape == null) {
            Main.getInstance().cO.warn("Particle shape " + shape + " is not valid, skipping.");
            return;
        }

        tasks = new HashMap<>();
        valid = true;
    }

    public void setOffset(String offset) {
        String[] split = offset.split(";");
        try {
            x = Double.parseDouble(split[0]);
            y = Double.parseDouble(split[1]);
            z = Double.parseDouble(split[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
            x = 1;
            y = 1;
            z = 1;
            Main.getInstance().cO.warn("Offset string is invalid, using default values.");
        }
    }

    private void singleParticle(Location loc, Player player) {
        try {
            // Redstone colored
            if (type.equals(Particle.REDSTONE)) {
                Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);

                if (playerOnly)
                    player.spawnParticle(type, loc, particleCount, x, y, z, dustOptions);
                else
                    loc.getWorld().spawnParticle(type, loc, particleCount, x, y, z, dustOptions);
                return;
            }

            if (playerOnly)
                player.spawnParticle(type, loc, particleCount, x, y, z, extra);
            else
                loc.getWorld().spawnParticle(type, loc, particleCount, x, y, z, extra);
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.err("Error when spawning particle");
            Main.getInstance().cO.err(e.getMessage());
            getTasks().get(loc).cancel();
        }
    }

    // Show particles based on desired location
    // center == block center
    public void castParticles(Location blockLocation, Player player) {
        Location center = blockLocation.clone();
        center.add(0.5, 0.5, 0.5);

        Main.getInstance().cO.debug("Spawning " + shape.toString() + " particle on location " + center.getX() + ", " + center.getY() + ", " + center.getZ());

        // Do the magic
        switch (shape) {
            case CIRCLE:
                center.add(0, 0.7, 0);

                tasks.put(blockLocation, new BukkitRunnable() {
                    int pCount = 0;

                    @Override
                    public void run() {
                        // Location math

                        int points = 15;
                        double radius = 0.5d;

                        for (int i = 0; i < points; i++) {
                            double angle = 2 * Math.PI * i / points;
                            Location point = center.clone().add(radius * Math.sin(angle), 0, radius * Math.cos(angle));
                            singleParticle(point, player);
                        }

                        pCount++;
                        if (pCount >= count) {
                            tasks.remove(blockLocation);
                            this.cancel();
                        }
                    }
                }.runTaskTimerAsynchronously(Main.getInstance(), delay, period));

                break;
            case CENTER:
                // Well, that was easy.
                tasks.put(blockLocation, new BukkitRunnable() {
                    int pCount = 0;

                    @Override
                    public void run() {
                        singleParticle(center, player);
                        pCount++;

                        if (pCount >= count) {
                            tasks.remove(blockLocation);
                            this.cancel();
                        }
                    }
                }.runTaskTimerAsynchronously(Main.getInstance(), delay, period));
                break;
            case FRAME:
                // ..
                tasks.put(blockLocation, new BukkitRunnable() {
                    int pCount = 0;

                    @Override
                    public void run() {
                        Location point = center.clone();

                        point.add(0.5, 0.5, 0.5);

                        // Top frame

                        castSquare(point, space, player);

                        // Bottom frame

                        point.subtract(0, 1, 0);

                        castSquare(point, space, player);

                        // Slopes

                        point = center.clone();
                        point.add(0.5, 0.5, 0.5);

                        castSlope(point, space, player);

                        point.subtract(1, 0, 0);

                        castSlope(point, space, player);

                        point.subtract(0, 0, 1);

                        castSlope(point, space, player);

                        point.add(1, 0, 0);

                        castSlope(point, space, player);

                        pCount++;

                        if (pCount >= count) {
                            tasks.remove(blockLocation);
                            this.cancel();
                        }
                    }
                }.runTaskTimerAsynchronously(Main.getInstance(), delay, period));
                break;
        }
    }

    private void castSlope(Location point, double space, Player player) {
        for (int i = 0; i < (1 / space); i++) {
            point.subtract(0, space, 0);
            singleParticle(point, player);
        }

        point.add(0, 1, 0);
    }

    private void castSquare(Location point, double space, Player player) {
        for (int i = 0; i < 1 / space; i++) {
            point.subtract(space, 0, 0);
            singleParticle(point, player);
        }

        for (int i = 0; i < 1 / space; i++) {
            point.subtract(0, 0, space);
            singleParticle(point, player);
        }

        for (int i = 0; i < 1 / space; i++) {
            point.add(space, 0, 0);
            singleParticle(point, player);
        }

        for (int i = 0; i < 1 / space; i++) {
            point.add(0, 0, space);
            singleParticle(point, player);
        }
    }

    public void setColor(String str) {
        if (str.contains(";")) {
            String[] split = str.split(";");

            try {
                color = Color.fromBGR(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]));
            } catch (ArrayIndexOutOfBoundsException e) {
                color = Color.WHITE;
                Main.getInstance().cO.warn("Offset string is invalid, using default value, pure white.");
            }
        } else color = Color.WHITE;
    }

    // Todo add more particle shapes
    public enum Shape {
        // Circle on top
        CIRCLE,
        // Around the edges
        FRAME,
        // Center of the block
        CENTER;

        public static Shape fromString(String str) {
            switch (str.toLowerCase()) {
                case "circle":
                    return Shape.CIRCLE;
                case "frame":
                    return Shape.FRAME;
                case "center":
                    return Shape.CENTER;
            }

            return null;
        }

        public String toString() {
            return this.getClass().getSimpleName();
        }
    }

    public String toString() {
        return type.toString() + "." + shape.toString() + ".S:" + speed + ".C:" + particleCount + ".OFF:" + x + ";" + y + ";" + z
                + ".EX:" + extra + ".PO:" + playerOnly + ".TP:" + period + ".TD:" + delay + ".TC:" + count;
    }

    public void setParticleCount(int count) {
        this.particleCount = count;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setPlayerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
    }

    public void setExtra(int extra) {
        this.extra = extra;
    }

    public boolean isValid() {
        return valid;
    }

    public void setSpace(double space) {
        this.space = space;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public HashMap<Location, BukkitTask> getTasks() {
        return tasks;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
