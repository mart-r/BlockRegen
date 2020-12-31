package nl.aurorion.blockregen.version.api;


import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Methods {

    default boolean isBarColorValid(@Nullable String string) {
        return false;
    }

    default boolean isBarStyleValid(@Nullable String string) {
        return false;
    }

    @Nullable
    default BossBar createBossBar(@Nullable String text, @Nullable String color, @Nullable String style) {
        return null;
    }

    ItemStack getItemInMainHand(@NotNull Player player);
}
