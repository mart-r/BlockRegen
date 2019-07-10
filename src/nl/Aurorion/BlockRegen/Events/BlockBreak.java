package nl.Aurorion.BlockRegen.Events;

import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
        Set<String> blockTypes = main.getFiles().getBlocklist().getConfigurationSection("Blocks").getKeys(false);

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

                CuboidRegion selection = new CuboidRegion(BukkitAdapter.asBlockVector(locA), BukkitAdapter.asBlockVector(locB));

                if (selection.contains(BukkitAdapter.asBlockVector(block.getLocation()))) {
                    isInRegion = true;
                    break;
                }
            }
        }

        // Not our block
        if (!blockTypes.contains(blockName)) {
            if (isInRegion)
                if (disableBreakRegions | disableBreak)
                    event.setCancelled(true);
            if (disableBreak)
                event.setCancelled(true);
        } else {
            BlockBR blockBR = Main.getInstance().getFormatHandler().getBlockBR(blockName);

            int expToDrop = event.getExpToDrop();

            // He is in region and it is our block.
            if (isInRegion) {

                if (!blockBR.check(player)) {
                    event.setCancelled(true);
                    return;
                }

                event.setDropItems(false);
                event.setExpToDrop(0);

                // Continue to reward
                blockBreak(player, block, blockName, expToDrop);
            } else {
                // World
                if (useRegions) return;
                else {

                    if (!blockBR.check(player)) {
                        event.setCancelled(true);
                        return;
                    }

                    event.setDropItems(false);
                    event.setExpToDrop(0);

                    // Continue to reward
                    blockBreak(player, block, blockName, expToDrop);
                }
            }
        }
    }

    private void blockBreak(Player player, Block block, String blockname, Integer exptodrop) {

        BlockState state = block.getState();
        Location loc = block.getLocation();

        // Reward player

        BlockBR blockBR = Main.getInstance().getFormatHandler().getBlockBR(blockname);

        blockBR.reward(player, block, exptodrop, loc);

        // Replacing the block ---------------------------------------------------------------------------------
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(blockBR.getReplaceBlock());
            }
        }.runTaskLater(main, 2l);

        if (blockBR.isRegenerate()) {

            main.cO.debug(Utils.regenTimesBlocks.toString());

            // Check for number of breaks
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

            // Actual Regeneration -------------------------------------------------------------------------------------

            // Data Recovery
            FileConfiguration data = main.getFiles().getData();

            if (main.isDataRecovery()) {

                List<String> dataLocs = new ArrayList<>();

                if (data.contains(blockname))
                    dataLocs = data.getStringList(blockname);

                dataLocs.add(Utils.locationToString(loc));
                data.set(blockname, dataLocs);
                main.getFiles().saveData();
            }

            int regendelay = blockBR.getRegenDelay();

            BukkitTask task = new BukkitRunnable() {
                public void run() {
                    state.update(true);

                    Utils.clearProcess(loc);

                    if (data != null && data.contains(blockname)) {
                        List<String> dataLocs = data.getStringList(blockname);
                        if (dataLocs != null && !dataLocs.isEmpty()) {
                            dataLocs.remove(Utils.locationToString(loc));
                            data.set(blockname, dataLocs);
                            main.getFiles().saveData();
                        }
                    }

                    blockBR.onRegen(player, block.getLocation());
                }
            }.runTaskLater(main, regendelay * 20);

            Utils.addProcess(loc, task, block.getType());
        }
        return;
    }

}
