package nl.aurorion.blockregen.system.preset.struct.material;

import com.cryptomorin.xseries.XMaterial;

import org.bukkit.block.Block;

import lombok.Getter;
import nl.aurorion.blockregen.BlockRegen;

public class RegularTargetMaterial implements TargetMaterial {
    private static final BlockRegen PLUGIN = BlockRegen.getInstance();
    @Getter
    private final XMaterial xMaterial;

    public RegularTargetMaterial(XMaterial xMaterial) {
        this.xMaterial = xMaterial;
    }

    @Override
    public boolean matchesMaterial(Block block) {
        return PLUGIN.getVersionManager().getMethods().compareType(block, xMaterial);
    }

}
