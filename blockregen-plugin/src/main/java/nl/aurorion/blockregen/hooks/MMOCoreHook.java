package nl.aurorion.blockregen.hooks;

import org.bukkit.entity.Player;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.Profession;

public final class MMOCoreHook {
    private static final MMOCoreHook INSTANCE = new MMOCoreHook();
    private final MMOCore core;

    public static MMOCoreHook getInstance() {
        return INSTANCE;
    }

    private MMOCoreHook() {
        this.core = MMOCore.plugin;
    }

    public boolean isProfession(String name) {
        return getProfession(name) != null;
    }

    public Profession getProfession(String name) {
        return core.professionManager.get(name);
    }

    public int getLevel(Player player, Profession profession) {
        PlayerData data = PlayerData.get(player);
        return data.getCollectionSkills().getLevel(profession);
    }

}
