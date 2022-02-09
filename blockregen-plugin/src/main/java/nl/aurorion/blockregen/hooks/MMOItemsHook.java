package nl.aurorion.blockregen.hooks;

import java.util.Optional;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.block.CustomBlock;
import nl.aurorion.blockregen.BlockRegen;

public final class MMOItemsHook {
    private static final MMOItemsHook INSTANCE = new MMOItemsHook();
    private static final String MMOITEMS_ITEM_ID = "MMOITEMS_ITEM_ID";
    private final MMOItems mmoItems;
    private final BlockRegen plugin = BlockRegen.getInstance();
    private final long startTime;

    public static MMOItemsHook getInstance() {
        return INSTANCE;
    }

    private MMOItemsHook() {
        mmoItems = MMOItems.plugin;
        startTime = plugin.getServer().getWorlds().get(0).getGameTime();
    }

    public boolean isMMOItemOfType(Block block, int id) {
        Optional<CustomBlock> customBlock = mmoItems.getCustomBlocks().getFromBlock(block.getBlockData());
        if (!customBlock.isPresent()) {
            return false;
        }
        CustomBlock cBlock = customBlock.get();
        return cBlock.getId() == id;
    }

    public boolean isSpecialName(String toolId) {
        if (plugin.getServer().getWorlds().get(0).getGameTime() == startTime) {
            // MMOItems not fully initialized
            plugin.getLogger().warning("MMO custom item names are treated as correct at startup "
                    + "since MMOItems is not yet ready at this time");
            return true;
        }
        for (Type type : mmoItems.getTypes().getAll()) {
            if (mmoItems.getItem(type, toolId) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isOfType(ItemStack tool, String expectedId) {
        if (tool == null || tool.getType().isAir()) {
            return false;
        }
        NBTItem item = NBTItem.get(tool);
        String curType = item.getType();
        if (curType == null) {
            return false;
        }
        String idOfItem = item.getString(MMOITEMS_ITEM_ID);
        return expectedId.equals(idOfItem);
    }

    public void setType(Block block, int id) {
        CustomBlock cb = mmoItems.getCustomBlocks().getBlock(id);
        block.setType(cb.getState().getType(), false);
        block.setBlockData(cb.getState().getBlockData(), false);
        // TODO - call event?
    }

}
