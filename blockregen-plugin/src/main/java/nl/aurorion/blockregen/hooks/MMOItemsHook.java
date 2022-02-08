package nl.aurorion.blockregen.hooks;

import java.util.Optional;

import org.bukkit.block.Block;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.block.CustomBlock;

public final class MMOItemsHook {
    private final MMOItems mmoItems;

    public MMOItemsHook() {
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

}
