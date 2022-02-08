package nl.aurorion.blockregen.system.preset.struct.material;

import org.bukkit.block.Block;

import lombok.Getter;
import nl.aurorion.blockregen.hooks.MMOItemsHook;

public class MMOTargetMaterial implements TargetMaterial {
    private static final MMOItemsHook MMO_HOOK = MMOItemsHook.getInstance();
    @Getter
    private final int mmoId;

    public MMOTargetMaterial(int mmoId) {
        this.mmoId = mmoId;
    }

    @Override
    public boolean matchesMaterial(Block block) {
        return MMO_HOOK.isMMOItemOfType(block, mmoId);
    }

}
