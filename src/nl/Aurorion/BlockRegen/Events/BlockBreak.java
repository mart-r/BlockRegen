package nl.Aurorion.BlockRegen.Events;

import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.System.Getters;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
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

    private final Main main;

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
        if (main.getGetters().useTowny())
            if (TownyUniverse.getTownBlock(block.getLocation()) != null)
                if (TownyUniverse.getTownBlock(block.getLocation()).hasTown())
                    return;

        // GriefPrevention support
        if (main.getGetters().useGP()) {
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

        Material blockType = block.getType();

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

                if (Main.getInstance().isOver18())
                    event.setDropItems(false);
                else
                    event.setCancelled(true);

                event.setExpToDrop(0);

                // Continue to reward
                blockBreak(player, block, blockName, expToDrop, blockType);
            } else {
                // World
                if (!useRegions) {
                    if (!blockBR.check(player)) {
                        event.setCancelled(true);
                        return;
                    }

                    if (Main.getInstance().isOver18())
                        event.setDropItems(false);
                     else
                        event.setCancelled(true);

                    event.setExpToDrop(0);

                    // Continue to reward
                    blockBreak(player, block, blockName, expToDrop, blockType);
                }
            }
        }
    }

    private void blockBreak(Player player, Block block, String blockname, Integer exptodrop, Material blockType) {

        Getters getters = main.getGetters();
        BlockState state = block.getState();
        Location loc = block.getLocation();

        // Reward player

        BlockBR blockBR = Main.getInstance().getFormatHandler().getBlockBR(blockname);

        blockBR.reward(player, block, exptodrop);

        // Replacing the block ---------------------------------------------------------------------------------
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(blockBR.getReplaceBlock());
            }
        }.runTaskLater(main, 2L);

        if (blockBR.isRegenerate()) {
            // Actual Regeneration -------------------------------------------------------------------------------------

            // Data Recovery
            FileConfiguration data = main.getFiles().getData();

            if (getters.dataRecovery()) {

                List<String> dataLocs = new ArrayList<>();

                if (data.contains(blockname))
                    dataLocs = data.getStringList(blockname);

                dataLocs.add(Utils.locationToString(loc));
                data.set(blockname, dataLocs);
                main.getFiles().saveData();
            }

            int regenDelay = blockBR.getRegenDelay();

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
                }
            }.runTaskLater(main, regenDelay * 20);

            Utils.addProcess(loc, task, blockType);
        }
    }

}
