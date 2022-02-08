package nl.aurorion.blockregen.system.preset.struct.material;

import org.bukkit.block.Block;

import lombok.Getter;
import nl.aurorion.blockregen.hooks.MMOHook;

public class MMOTargetMaterial implements TargetMaterial {
    private static final MMOHook MMO_HOOK = new MMOHook();
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
