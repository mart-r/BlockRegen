package nl.aurorion.blockregen.version.current;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockSkullUtil;
import nl.aurorion.blockregen.ParseUtil;
import nl.aurorion.blockregen.version.api.INodeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;

public class LatestNodeData implements INodeData {

    @Getter
    @Setter
    private BlockData blockData;
    @Getter
    @Setter
    private String base64;

    public LatestNodeData() {
    }

    private LatestNodeData(BlockData blockData, String base64) {
        this.blockData = blockData;
        this.base64 = base64;
    }

    @Override
    public INodeData fromString(String str) {

        if (Strings.isNullOrEmpty(str))
            return null;

        if (!str.startsWith("{") || !str.endsWith("}"))
            return fromMaterialName(str);

        String data = str.replace("{", "").replace("}", "");
        String[] arr = data.split("\\|\\|");

        String base64 = null;
        if (arr.length > 1)
            base64 = arr[1].equals("null") ? null : arr[1];

        BlockData blockData = Bukkit.createBlockData(arr[0]);
        return new LatestNodeData(blockData, base64);
    }

    @Override
    public INodeData fromMaterialName(String str) {

        XMaterial xMaterial = ParseUtil.parseMaterial(str, true);
        if (xMaterial == null)
            return null;

        Material material = xMaterial.parseMaterial();
        return material == null ? null : fromMaterialName(material);
    }

    public static INodeData fromMaterialName(Material material) {
        BlockData data = Bukkit.createBlockData(material);
        return new LatestNodeData(data, null);
    }

    @Override
    public INodeData fromBlock(Block block) {
        BlockState state = block.getState();
        String b64 = BlockSkullUtil.base64fromBlock(block);
        return new LatestNodeData(state.getBlockData(), b64);
    }

    @Override
    public boolean matches(Block block) {
        BlockState state = block.getState();
        BlockData blockData = block.getBlockData();

        if (!blockData.matches(this.blockData))
            return false;

        if (state instanceof Skull && base64 != null)
            BlockSkullUtil.blockWithBase64(block, base64);
        return true;
    }

    public Material getMaterial() {
        return blockData.getMaterial();
    }

    @Override
    public String getAsString(boolean omitEmpty) {
        return String.format("{%s||%s}", blockData.getAsString(omitEmpty), base64 == null ? "null" : base64);
    }

    @Override
    public void mutate(Block block, INodeData nodeData) {
        //TODO
        // Copy rotation
        /* if (blockData instanceof Rotatable && data instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) data;
            Rotatable newRotatable = (Rotatable) newData;
            newRotatable.setRotation(rotatable.getRotation());
        }*/
        mutate(block);
    }

    @Override
    public void mutate(Block block) {
        BlockState state = block.getState();

        // BlockData data = state.getBlockData();
        BlockData newData = blockData.clone();

        state.setBlockData(newData);
        state.update(true);

        if (base64 != null)
            BlockSkullUtil.blockWithBase64(block, base64);
    }
}
