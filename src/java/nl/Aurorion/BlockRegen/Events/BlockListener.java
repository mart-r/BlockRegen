package nl.Aurorion.BlockRegen.Events;

import com.palmergames.bukkit.towny.object.TownyUniverse;
import nl.Aurorion.BlockRegen.BlockFormat.BlockFormat;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.System.OptionHandler;
import nl.Aurorion.BlockRegen.System.RegenProcess;
import nl.Aurorion.BlockRegen.System.RegionBR;
import nl.Aurorion.BlockRegen.System.WorldBR;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class BlockListener implements Listener {

    private Main plugin;
    private OptionHandler optionHandler;

    public BlockListener() {
        this.plugin = Main.getInstance();
        this.optionHandler = plugin.getOptionHandler();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        FileConfiguration settings = plugin.getFiles().getSettings();

        World world = player.getWorld();

        if (!settings.getStringList("Worlds-Enabled").contains(world.getName()))
            return;

        Block block = event.getBlock();

        String blockType = block.getType().name();

        List<String> blockTypes = plugin.getFormatHandler().getTypes();

        // Bypass command
        if (Utils.bypass.contains(player.getName())) {
            Main.getInstance().cO.debug("Bypass found", player);
            return;
        }

        // Already mined, waiting for regen to happen
        if (Utils.getProcess(block.getLocation()) != null) {
            Main.getInstance().cO.debug("Already mined", player);
            event.setCancelled(true);
            return;
        }

        // Data check
        if (Utils.blockCheck.contains(player.getName())) {
            Main.getInstance().cO.debug("Data check found", player);
            event.setCancelled(true);
            player.sendMessage(Messages.get("Data-Check").replace("%block%", blockType));
            return;
        }

        // Towny support
        if (plugin.isUseTowny())
            if (TownyUniverse.getTownBlock(block.getLocation()) != null)
                if (TownyUniverse.getTownBlock(block.getLocation()).hasTown()) {
                    Main.getInstance().cO.debug("Towny support intercept.", player);
                    return;
                }

        // GriefPrevention support
        if (plugin.isUseGP()) {
            String noBuildReason = plugin.getGriefPrevention().allowBreak(player, block, block.getLocation(), event);

            if (noBuildReason != null) {
                Main.getInstance().cO.debug("Grief prevention intercept.", player);
                return;
            }
        }

        // Should we use regions?
        boolean useRegions = settings.getBoolean("Use-Regions");

        // Disable other block break in regions/worlds /^^\
        boolean disableBreak = settings.getBoolean("Disable-Other-Break");

        // Is in region?
        boolean isInRegion = false;

        if (plugin.getWorldEdit() != null && useRegions)
            isInRegion = optionHandler.isInRegion(block.getLocation());

        List<String> finalBlocks = new ArrayList<>();

        // New logic
        if (useRegions) {
            if (!isInRegion)
                return;

            // Declare region
            RegionBR region = optionHandler.getRegion(block.getLocation());

            if (!player.hasPermission("blockregen.region." + region.getName()) && !player.hasPermission("blockregen.region.*")) {
                plugin.cO.debug("No region permission");
                return;
            }

            if (!region.isEnabled())
                return;

            // Filter blocks based on region configuration
            if (!region.isUseAll()) {
                for (String type : blockTypes)
                    if (region.getBlockTypes().contains(type))
                        finalBlocks.add(type);
            } else finalBlocks.addAll(blockTypes);

            // Deny blocks we don't want if configured deny: true
            if (!finalBlocks.contains(blockType)) {

                // All blocks regen prototype, cannot use default methods.
                if (region.isAllBlocks()) {
                    Material type = block.getType();

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            block.setType(plugin.defaultReplaceBlock);
                        }
                    }.runTaskLater(plugin, 21);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            block.setType(type);
                        }
                    }.runTaskLater(plugin, plugin.defaultRegenDelay.getAmount() * 20);

                    return;
                } else if (disableBreak)
                    event.setCancelled(true);
                return;
            }
        } else {
            // Filter blocks based on world configuration
            WorldBR worldBR = optionHandler.getWorld(world.getName());

            if (!player.hasPermission("blockregen.world." + worldBR.getName()) && !player.hasPermission("blockregen.world.*")) {
                plugin.cO.debug("No world permission");
                return;
            }

            if (!worldBR.isEnabled())
                return;

            if (!worldBR.isUseAll()) {
                for (String type : blockTypes)
                    if (worldBR.getBlockTypes().contains(type))
                        finalBlocks.add(type);
            } else finalBlocks.addAll(blockTypes);

            // Deny blocks we don't want if configured deny: true
            if (!finalBlocks.contains(blockType)) {

                // All blocks regen prototype, cannot use default methods.
                if (worldBR.isAllBlocks()) {
                    Material type = block.getType();

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            block.setType(plugin.defaultReplaceBlock);
                        }
                    }.runTaskLater(plugin, 21);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            block.setType(type);
                        }
                    }.runTaskLater(plugin, plugin.defaultRegenDelay.getAmount() * 20);

                    return;
                } else if (disableBreak)
                    event.setCancelled(true);

                return;
            }
        }

        // Proceed with check and the block break
        BlockFormat blockFormat = plugin.getFormatHandler().getBlockBR(blockType);

        // Check if conditions are met
        if (!blockFormat.check(player)) {
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);

        // Save default exp to drop
        int expToDrop = event.getExpToDrop();
        event.setExpToDrop(0);

        // Proceed to rewards and regeneration
        blockBreak(player, block, expToDrop);
    }

    // When the constellations are in the right positions, we are okay to reward and regenerate.
    // expToDrop == vanilla xp drop, needed for vanilla drops
    private void blockBreak(Player player, Block block, int expToDrop) {

        String blockType = block.getType().name();

        BlockState state = block.getState();
        Material type = block.getType();
        Location loc = block.getLocation();

        BlockFormat blockFormat = plugin.getFormatHandler().getBlockBR(blockType);

        // Reward the player
        blockFormat.reward(player, block, expToDrop);

        // Replacing the block ---------------------------------------------------------------------------------

        // DOUBLE PLANTS
        if ((blockFormat.getBlockType().equals(Material.SUNFLOWER) || blockFormat.getBlockType().equals(Material.ROSE_BUSH)) && !block.getRelative(BlockFace.DOWN).getType().equals(Material.AIR))
            Utils.setFlower(block, blockFormat.getReplaceBlock());
        else {
            // Replace the block, needs to be delayed a bit.
            new BukkitRunnable() {
                @Override
                public void run() {
                    block.setType(blockFormat.getReplaceBlock());
                }
            }.runTaskLater(plugin, 1);
        }

        if (blockFormat.isRegenerate()) {
            // Check for number of breaks
            if (blockFormat.getRegenTimes() > 0) {
                if (Utils.regenTimesBlocks.containsKey(block.getLocation())) {
                    if ((Utils.regenTimesBlocks.get(block.getLocation()) - 1) <= 0) {
                        Utils.regenTimesBlocks.remove(block.getLocation());
                        return;
                    } else
                        Utils.regenTimesBlocks.put(block.getLocation(), Utils.regenTimesBlocks.get(block.getLocation()) - 1);
                } else
                    Utils.regenTimesBlocks.put(block.getLocation(), blockFormat.getRegenTimes() - 1);
            }

            plugin.cO.debug("Breaks left: " + (Utils.regenTimesBlocks.containsKey(block.getLocation()) ? Utils.regenTimesBlocks.get(block.getLocation()) : "Unlimited"), player);

            // Data Recovery -------------------------------------------------------------------------------------------
            if (plugin.isDataRecovery()) {
                ConfigurationSection recoverySection = plugin.getFiles().getData().getConfigurationSection("Recovery");

                List<String> locations = new ArrayList<>();

                if (recoverySection.contains(blockType))
                    locations = recoverySection.getStringList(blockType);

                locations.add(Utils.locationToString(loc));

                // blockType in string, locations
                recoverySection.set(blockType, locations);

                // Save instantly
                plugin.getFiles().data.save();
            }

            // Actual Regeneration -------------------------------------------------------------------------------------

            // Everything moved to RegenProcess class
            RegenProcess regenProcess = new RegenProcess(loc, player, block);

            // Set more params, don't want that many arguments in a constructor
            regenProcess.setState(state);
            regenProcess.setMaterial(type);

            // Fetch from Format Handler
            regenProcess.fetchBR();

            // Start the task
            regenProcess.start();
        }
    }
}
