package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class RegenProcess {

    private Main plugin = Main.getInstance();

    /*
     * RegenProcess object is used to store information about currently running regeneration tasks.
     * List of processes in Utils.java
     * */

    // Location, where to regen?
    private Location loc;

    // Bukkit task responsible for regening
    private BukkitTask task;

    // Material to regen to, the original
    private Material material;

    private Block block;

    // BlockBR format to act by
    private BlockBR blockBR;

    // System time, when the block regenerates, probably.
    private long regenTime;

    private int regenDelay;

    private Player player;

    private BlockState state;

    // Default constructor
    public RegenProcess(Location loc, Player player, Block block) {
        this.loc = loc;
        this.block = block;

        this.player = player;
    }

    public void fetchBR() {
        this.blockBR = plugin.getFormatHandler().getBlockBR(material.name());
        plugin.cO.debug("Fetch by " + material.name() + " BlockBR == " + blockBR.getBlockType().name());

        // Fetch random/fixed regen delay
        this.regenDelay = blockBR.getRegenDelay().getAmount();

        this.regenTime = System.currentTimeMillis() + regenDelay * 1000;
        plugin.cO.debug("Regen Time: " + regenTime);
    }

    public void start() {
        if (regenDelay > 0) {
            // Task
            this.task = new BukkitRunnable() {
                @Override
                public void run() {
                    regen();
                }
            }.runTaskLater(plugin, regenDelay * 20);

            Utils.addProcess(this);
        } else
            regen();
    }

    // Persist load constructor
    public RegenProcess(Location loc, Material material, long untilRegen) {
        this.loc = loc;
        this.material = material;
        this.regenTime = System.currentTimeMillis() + untilRegen;

        this.blockBR = plugin.getFormatHandler().getBlockBR(material.name());

        this.block = loc.getBlock();
        this.state = block.getState();

        this.player = null;

        plugin.cO.debug("Loaded a regen process from persist, regen in " + (untilRegen / 1000) + "s");

        // Task
        if (untilRegen > 0) {
            this.task = new BukkitRunnable() {
                @Override
                public void run() {
                    regen();
                }
            }.runTaskLater(plugin, untilRegen / 50);

            Utils.addProcess(this);
        } else regen();
    }

    public void regen() {

        Location loc = block.getLocation();
        String blockType = block.getType().name();

        // DOUBLE PLANTS
        if ((blockBR.getBlockType().equals(Material.SUNFLOWER) || blockBR.getBlockType().equals(Material.ROSE_BUSH)) && !block.getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
            plugin.cO.debug("Double plant detected and can be placed");
            Utils.setFlower(block, blockBR.getBlockType());
        } else
            // Update the state
            block.setType(blockBR.getBlockType());

        // Clear this regen process
        Utils.clearProcess(loc);

        plugin.cO.debug("Cleared the process");

        // Recovery remove
        if (plugin.isDataRecovery()) {
            ConfigurationSection recoverySection = plugin.getFiles().getData().getConfigurationSection("Recovery");

            if (recoverySection.contains(blockType)) {

                List<String> locations = recoverySection.getStringList(blockType);

                if (!locations.isEmpty()) {
                    locations.remove(Utils.locationToString(loc));

                    recoverySection.set(blockType, locations);

                    // Save Data.yml
                    plugin.getFiles().saveData();
                } else
                    recoverySection.set(blockType, null);

                plugin.cO.debug("Removed from recovery locations");
            }
        }

        // Run onRegen actions
        blockBR.onRegen(player, block.getLocation());
    }

    public String getUntilRegenFormatted() {
        return secsUntilRegen() + "s";
    }

    public long getRegenTime() {
        return regenTime;
    }

    // Seconds to regen
    public double secsUntilRegen() {
        return (regenTime - System.currentTimeMillis()) / 1000D;
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

    public Main getPlugin() {
        return plugin;
    }

    public void setPlugin(Main plugin) {
        this.plugin = plugin;
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

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void setBlockBR(BlockBR blockBR) {
        this.blockBR = blockBR;
    }

    public void setRegenTime(long regenTime) {
        this.regenTime = regenTime;
    }

    public int getRegenDelay() {
        return regenDelay;
    }

    public void setRegenDelay(int regenDelay) {
        this.regenDelay = regenDelay;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public BlockState getState() {
        return state;
    }

    public void setState(BlockState state) {
        this.state = state;
    }
}
