package nl.Aurorion.BlockRegen.Events;

import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockBreak implements Listener {

    private Main main;

    public BlockBreak(Main main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        World world = player.getWorld();

        String blockName = block.getType().name();

        FileConfiguration settings = main.getFiles().getSettings();
        Set<String> blockTypes = main.getFormatHandler().getBlockNames();

        // Bypass command
        if (Utils.bypass.contains(player.getName()))
            return;

        // Already mined, waiting for regen to happen
        if (Utils.getProcess(block.getLocation()) != null) {
            event.setCancelled(true);
            return;
        }

        // Checking for blockType
        if (Utils.blockCheck.contains(player.getName())) {
            event.setCancelled(true);
            player.sendMessage(Messages.get("Data-Check").replace("%block%", blockName));
            return;
        }

        // Towny support
        if (main.isUseTowny())
            if (TownyUniverse.getTownBlock(block.getLocation()) != null)
                if (TownyUniverse.getTownBlock(block.getLocation()).hasTown())
                    return;

        // GriefPrevention support
        if (main.isUseGP()) {
            String noBuildReason = main.getGriefPrevention().allowBreak(player, block, block.getLocation(), event);

            if (noBuildReason != null)
                return;
        }

        if (!settings.getStringList("Worlds-Enabled").contains(world.getName()))
            return;

        boolean useRegions = settings.getBoolean("Use-Regions");

        boolean disableBreak = settings.getBoolean("Disable-Other-Break");
        boolean disableBreakRegions = settings.getBoolean("Disable-Other-Break-Region");

        boolean isInRegion = false;

        if (useRegions && main.getWorldEdit() != null) {

            Set<String> regionSet = main.getFiles().getRegions().getConfigurationSection("Regions").getKeys(false);

            for (String region : regionSet) {

                Location locA = Utils.stringToLocation(main.getFiles().getRegions().getString("Regions." + region + ".Max"));
                Location locB = Utils.stringToLocation(main.getFiles().getRegions().getString("Regions." + region + ".Min"));

                CuboidSelection selection = new CuboidSelection(world, locA, locB);

                if (selection.contains(block.getLocation())) {
                    isInRegion = true;
                    break;
                }
            }
        }

        if (blockTypes.contains(blockName.toUpperCase()) || blockTypes.contains(blockName.toUpperCase() + ";" + block.getData())) {
            BlockBR blockBR;

            if (blockTypes.contains(blockName.toUpperCase() + ";" + block.getData()))
                blockBR = Main.getInstance().getFormatHandler().getBlockBR(blockName.toUpperCase() + ";" + block.getData());
            else
                blockBR = Main.getInstance().getFormatHandler().getBlockBR(blockName.toUpperCase());

            main.cO.debug("Checking..");

            int expToDrop = event.getExpToDrop();

            // He is in region and it is our block.
            if (isInRegion) {

                if (!blockBR.check(player)) {
                    event.setCancelled(true);
                    return;
                }

                if (Main.getVersion().contains("1_12") || Main.getVersion().contains("1_10") || Main.getVersion().contains("1_11")) {
                    event.setDropItems(false);
                } else
                    event.setCancelled(true);

                event.setExpToDrop(0);

                // Continue to reward
                blockBreak(player, block, blockName, expToDrop, blockBR);
            } else {
                // World
                if (!useRegions) {
                    if (!blockBR.check(player)) {
                        event.setCancelled(true);
                        return;
                    }

                    if (Main.getVersion().contains("1_12") || Main.getVersion().contains("1_10") || Main.getVersion().contains("1_11")) {
                        event.setDropItems(false);
                    } else
                        event.setCancelled(true);

                    event.setExpToDrop(0);

                    // Continue to reward
                    blockBreak(player, block, blockName, expToDrop, blockBR);
                }
            }
        } else {
            if (isInRegion)
                if (disableBreakRegions | disableBreak)
                    event.setCancelled(true);
            if (disableBreak)
                event.setCancelled(true);
        }
    }

    private void blockBreak(Player player, Block block, String blockname, Integer exptodrop, BlockBR blockBR) {

        BlockState state = block.getState();
        Location loc = block.getLocation();

        // Reward player

        blockBR.reward(player, block, exptodrop);

        Material blockType = block.getType();
        Byte blockData = block.getData();

        // Replacing the block ---------------------------------------------------------------------------------
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(blockBR.getReplaceBlock());
                block.setData(blockBR.getReplaceBlockData());
                main.cO.debug("Replacing with: " + blockBR.getReplaceBlock().toString() + ";" + blockBR.getReplaceBlockData());
            }
        }.runTaskLater(main, 2L);

        if (blockBR.isRegenerate()) {
            // Actual Regeneration -------------------------------------------------------------------------------------

            if (blockBR.getRegenTimes() != 0) {

                // Check for number of regens
                if (blockBR.getRegenTimes() > 0) {
                    if (Utils.regenTimesBlocks.containsKey(block.getLocation())) {
                        if ((Utils.regenTimesBlocks.get(block.getLocation()) - 1) <= 0) {
                            Utils.regenTimesBlocks.remove(block.getLocation());
                            return;
                        } else
                            Utils.regenTimesBlocks.put(block.getLocation(), Utils.regenTimesBlocks.get(block.getLocation()) - 1);
                    } else
                        Utils.regenTimesBlocks.put(block.getLocation(), blockBR.getRegenTimes() - 1);
                }

                main.cO.debug("Breaks left: " + Utils.regenTimesBlocks.get(block.getLocation()));
            }

            // Data Recovery
            ConfigurationSection recovery = main.getFiles().getData().getConfigurationSection("Recovery");
            String blockString = blockname + ";" + blockData;

            if (main.isDataRecovery()) {
                main.cO.debug("Saving " + blockString + " for recovery on " + Utils.locationToString(loc));

                List<String> dataLocs = new ArrayList<>();

                if (recovery.contains(blockString))
                    dataLocs = recovery.getStringList(blockString);

                dataLocs.add(Utils.locationToString(loc));

                recovery.set(blockString, dataLocs);
                main.getFiles().saveData();
            }

            // Add the regen process

            int regenDelay = blockBR.getRegenDelay();

            BukkitTask task = new BukkitRunnable() {
                public void run() {
                    state.update(true);

                    Utils.clearProcess(loc);

                    if (main.isDataRecovery())
                        if (recovery.contains(blockString)) {

                            List<String> dataLocs = recovery.getStringList(blockString);

                            if (dataLocs != null && !dataLocs.isEmpty()) {
                                dataLocs.remove(Utils.locationToString(loc));

                                recovery.set(blockString, dataLocs);

                                main.getFiles().saveData();
                            }
                        }
                }
            }.runTaskLater(main, regenDelay * 20);

            Utils.addProcess(loc, task, blockType, blockData);
        }
    }
}
