package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;

public class EnchantUtil {

    //               KEY, ENCHANTMENT
    private HashMap<String, Enchantment> enchantKeys;

    public EnchantUtil() {
        enchantKeys = new HashMap<>();

        enchantKeys.put("UNBREAKING", Enchantment.DURABILITY);
        enchantKeys.put("ARROW_DAMAGE", Enchantment.ARROW_DAMAGE);
        enchantKeys.put("ARROW_FIRE", Enchantment.ARROW_FIRE);
        enchantKeys.put("ARROW_INFINITE", Enchantment.ARROW_INFINITE);
        enchantKeys.put("ARROW_KNOCKBACK", Enchantment.ARROW_KNOCKBACK);
        enchantKeys.put("DAMAGE_ALL", Enchantment.DAMAGE_ALL);
        enchantKeys.put("DAMAGE_ARTHROPODS", Enchantment.DAMAGE_ARTHROPODS);
        enchantKeys.put("DAMAGE_UNDEAD", Enchantment.DAMAGE_UNDEAD);
        enchantKeys.put("DEPTH_STRIDER", Enchantment.DEPTH_STRIDER);
        enchantKeys.put("DIG_SPEED", Enchantment.DIG_SPEED);
        enchantKeys.put("FIRE_ASPECT", Enchantment.FIRE_ASPECT);
        enchantKeys.put("KNOCKBACK", Enchantment.KNOCKBACK);
        enchantKeys.put("FORTUNE", Enchantment.LOOT_BONUS_BLOCKS);
        enchantKeys.put("LOOTING", Enchantment.LOOT_BONUS_MOBS);
        enchantKeys.put("LUCK", Enchantment.LUCK);
        enchantKeys.put("LURE", Enchantment.LURE);
        enchantKeys.put("OXYGEN", Enchantment.OXYGEN);
        enchantKeys.put("PROTECTION", Enchantment.PROTECTION_ENVIRONMENTAL);
        enchantKeys.put("PROTECTION_EXPLOSIONS", Enchantment.PROTECTION_EXPLOSIONS);
        enchantKeys.put("PROTECTION_FALL", Enchantment.PROTECTION_FALL);
        enchantKeys.put("PROTECTION_FIRE", Enchantment.PROTECTION_FIRE);
        enchantKeys.put("PROTECTION_PROJECTILE", Enchantment.PROTECTION_PROJECTILE);
        enchantKeys.put("SILK_TOUCH", Enchantment.SILK_TOUCH);
        enchantKeys.put("THORNS", Enchantment.THORNS);
        enchantKeys.put("WATER_WORKER", Enchantment.WATER_WORKER);

        if (Main.getVersion().contains("1_12") || Main.getVersion().contains("1_11"))
            enchantKeys.put("SWEEPING_EDGE", Enchantment.SWEEPING_EDGE);

        if (Main.getVersion().contains("1_12")) {
            enchantKeys.put("VANISHING_CURSE", Enchantment.VANISHING_CURSE);
            enchantKeys.put("BINDING_CURSE", Enchantment.BINDING_CURSE);
        }

        if (Main.getInstance().isOver18()) {
            enchantKeys.put("MENDING", Enchantment.MENDING);
            enchantKeys.put("FROST_WALKER", Enchantment.FROST_WALKER);
        }

        Main.getInstance().cO.debug("Loaded enchants: " + enchantKeys.toString());
    }

    public Enchantment get(String str) {
        return enchantKeys.get(str.toUpperCase());
    }
}
