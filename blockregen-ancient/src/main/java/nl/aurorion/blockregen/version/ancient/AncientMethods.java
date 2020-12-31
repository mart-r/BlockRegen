package nl.aurorion.blockregen.version.ancient;

import nl.aurorion.blockregen.version.api.Methods;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AncientMethods implements Methods {

    @Override
    public ItemStack getItemInMainHand(@NotNull Player player) {
        return player.getInventory().getItemInHand();
    }
}
