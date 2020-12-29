package nl.aurorion.blockregen.version.ancient;

import nl.aurorion.blockregen.NodeData;
import nl.aurorion.blockregen.version.api.Methods;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AncientMethods implements Methods {

    @Override
    public void setType(@NotNull Block block, @NotNull NodeData nodeData) {
        nodeData.place(block);
    }

    @Override
    public boolean compareType(@NotNull Block block, @NotNull NodeData nodeData) {
        return nodeData.matches(block);
    }

    @Override
    public ItemStack getItemInMainHand(@NotNull Player player) {
        return player.getInventory().getItemInHand();
    }
}
