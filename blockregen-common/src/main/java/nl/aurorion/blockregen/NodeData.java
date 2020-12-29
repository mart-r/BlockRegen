package nl.aurorion.blockregen;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.Contract;

public class NodeData {

    @Getter
    @Setter
    private BlockData blockData;
    @Getter
    @Setter
    private String base64;

    public NodeData(BlockData blockData, String base64) {
        this.blockData = blockData;
        this.base64 = base64;
    }

    @Contract("null -> null")
    public static NodeData fromString(String str) {

        if (Strings.isNullOrEmpty(str))
            return null;

        if (!str.startsWith("{") || !str.endsWith("}"))
            return fromMaterial(str);

        String data = str.replace("{", "").replace("}", "");
        String[] arr = data.split("\\|\\|");

        String base64 = null;
        if (arr.length > 1)
            base64 = arr[1].equals("null") ? null : arr[1];

        BlockData blockData = Bukkit.createBlockData(arr[0]);
        return new NodeData(blockData, base64);
    }

    public static NodeData fromMaterial(String str) {

        XMaterial xMaterial = ParseUtil.parseMaterial(str, true);
        if (xMaterial == null)
            return null;

        Material material = xMaterial.parseMaterial();
        return material == null ? null : fromMaterial(material);
    }

    public static NodeData fromMaterial(Material material) {
        BlockData data = Bukkit.createBlockData(material);
        return new NodeData(data, null);
    }

    public static NodeData fromBlock(Block block) {
        BlockState state = block.getState();
        String b64 = BlockSkullUtil.base64fromBlock(block);
        return new NodeData(state.getBlockData(), b64);
    }

    public boolean matches(Block block) {
        BlockState state = block.getState();
        BlockData blockData = block.getBlockData();

        if (!blockData.matches(this.blockData))
            return false;

        if (state instanceof Skull && base64 != null)
            BlockSkullUtil.blockWithBase64(block, base64);
        return true;
    }

    public NodeData place(Location location) {
        Block block = location.getBlock();
        return place(block);
    }

    public NodeData place(Block block) {
        BlockState state = block.getState();

        BlockData data = state.getBlockData();
        BlockData newData = blockData.clone();

        // Copy rotation
        if (blockData instanceof Rotatable && data instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) data;
            Rotatable newRotatable = (Rotatable) newData;
            newRotatable.setRotation(rotatable.getRotation());
        }

        state.setBlockData(newData);
        state.update(true);

        if (base64 != null)
            BlockSkullUtil.blockWithBase64(block, base64);

        return new NodeData(newData, base64);
    }

    public Material getMaterial() {
        return blockData.getMaterial();
    }

    public String getAsString() {
        return getAsString(false);
    }

    public String getAsString(boolean omitEmpty) {
        return String.format("{%s||%s}", blockData.getAsString(omitEmpty), base64 == null ? "null" : base64);
    }
}
