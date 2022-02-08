package nl.aurorion.blockregen.hooks;

import java.util.Optional;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.block.CustomBlock;

public final class MMOItemsHook {
    private static final MMOItemsHook INSTANCE = new MMOItemsHook();
    private static final String MMOITEMS_ITEM_ID = "MMOITEMS_ITEM_ID";
    private final MMOItems mmoItems;

    public static MMOItemsHook getInstance() {
        return INSTANCE;
    }

    private MMOItemsHook() {
        mmoItems = MMOItems.plugin;
    }

    public boolean isMMOItemOfType(Block block, int id) {
        Optional<CustomBlock> customBlock = mmoItems.getCustomBlocks().getFromBlock(block.getBlockData());
        if (!customBlock.isPresent()) {
            return false;
        }
        CustomBlock cBlock = customBlock.get();
        return cBlock.getId() == id;
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

}
