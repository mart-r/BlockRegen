package nl.aurorion.blockregen.version.legacy;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Strings;
import com.google.gson.annotations.JsonAdapter;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockSkullUtil;
import nl.aurorion.blockregen.ParseUtil;
import nl.aurorion.blockregen.version.api.INodeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;

public class LegacyNodeData implements INodeData {

    @Getter
    @Setter
    private BlockData blockData;

    @Getter
    @Setter
    private String base64;

    public LegacyNodeData() {
    }

    private LegacyNodeData(BlockData blockData, String base64) {
        this.blockData = blockData;
        this.base64 = base64;
    }

    private static class BlockData implements Cloneable {

        @Getter
        @Setter
        private Material material;

        @Getter
        @Setter
        private byte data;

        @Getter
        @Setter
        private BlockFace rotation;

        public BlockData(Material material, byte data, BlockFace rotation) {
            this.material = material;
            this.data = data;
            this.rotation = rotation;
        }

        public BlockData(BlockData blockData) {
            this.material = blockData.getMaterial();
            this.data = blockData.getData();
            this.rotation = blockData.getRotation();
        }

        //TODO Add more blocks that have a rotation
        private static BlockFace getRotation(Block block) {
            BlockState state = block.getState();
            if (state instanceof Skull) {
                return ((Skull) state).getRotation();
            }
            return null;
        }

        //TODO Add more blocks that have a rotation
        private static void setRotation(BlockState state, BlockFace blockFace) {
            if (state instanceof Skull) {
                ((Skull) state).setRotation(blockFace);
            }
        }

        @SuppressWarnings("deprecation")
        public void toBlock(Block block) {
            BlockState state = block.getState();
            if (XMaterial.matchXMaterial(material) == XMaterial.PLAYER_HEAD) {
                state.setType(XMaterial.PLAYER_HEAD.parseMaterial());
                // BlockSkullUtil.setSkullType(state);
                Bukkit.getLogger().info("A player head ignored.");
            } else {
                state.setType(material);
                state.setRawData(data);
            }

            if (rotation != null)
                setRotation(state, rotation);
            state.update(true);
        }

        public void toBlock(Block block, BlockData blockData) {
            BlockState state = block.getState();
            if (XMaterial.matchXMaterial(material) == XMaterial.PLAYER_HEAD) {
                state.setType(XMaterial.PLAYER_HEAD.parseMaterial());
                // BlockSkullUtil.setSkullType(state);
                Bukkit.getLogger().info("A player head ignored.");
            } else {
                state.setType(material);
                if (data == -1)
                    state.setRawData(blockData.getData());
                else state.setRawData(data);
            }

            if (rotation != null)
                setRotation(state, rotation);
            else if (blockData.getRotation() != null)
                setRotation(state, blockData.getRotation());
            state.update(true);
        }

        public static BlockData fromBlock(Block block) {
            Material material = block.getType();
            byte data = block.getData();

            BlockFace blockFace = getRotation(block);

            return new BlockData(material, data, blockFace);
        }

        public static BlockData fromMaterial(XMaterial xMaterial) {
            Material material = xMaterial.parseMaterial();

            if (material == null)
                return null;

            byte data = xMaterial.getData();
            return new BlockData(material, data, null);
        }

        public static BlockData fromString(String str) {

            String[] arr = str.split("\\[");

            if (arr.length < 2)
                return null;

            String mat = arr[0].replace("minecraft:", "").toUpperCase();

            Material material = null;
            try {
                material = Material.valueOf(mat);
            } catch (IllegalArgumentException ignored) {
            }

            if (material == null)
                return null;

            String dataString = arr[1].replace("[", "").replace("]", "");
            String[] dataArr = dataString.split(",");

            byte data = 0;
            BlockFace rotation = null;
            for (String arg : dataArr) {
                if (arg.startsWith("data=")) {
                    data = Byte.parseByte(arg.replace("data=", ""));
                } else if (arg.startsWith("rotation=")) {
                    byte byteRotation = Byte.parseByte(arg.replace("rotation=", ""));
                    rotation = getBlockFace(byteRotation);
                }
            }
            return new BlockData(material, data, rotation);
        }

        static BlockFace getBlockFace(byte rotation) {
            switch (rotation) {
                case 0:
                    return BlockFace.NORTH;
                case 1:
                    return BlockFace.NORTH_NORTH_EAST;
                case 2:
                    return BlockFace.NORTH_EAST;
                case 3:
                    return BlockFace.EAST_NORTH_EAST;
                case 4:
                    return BlockFace.EAST;
                case 5:
                    return BlockFace.EAST_SOUTH_EAST;
                case 6:
                    return BlockFace.SOUTH_EAST;
                case 7:
                    return BlockFace.SOUTH_SOUTH_EAST;
                case 8:
                    return BlockFace.SOUTH;
                case 9:
                    return BlockFace.SOUTH_SOUTH_WEST;
                case 10:
                    return BlockFace.SOUTH_WEST;
                case 11:
                    return BlockFace.WEST_SOUTH_WEST;
                case 12:
                    return BlockFace.WEST;
                case 13:
                    return BlockFace.WEST_NORTH_WEST;
                case 14:
                    return BlockFace.NORTH_WEST;
                case 15:
                    return BlockFace.NORTH_NORTH_WEST;
                default:
                    throw new AssertionError(rotation);
            }
        }

        static byte getBlockFace(BlockFace rotation) {
            switch (rotation.ordinal()) {
                case 1:
                    return 0;
                case 2:
                    return 4;
                case 3:
                    return 8;
                case 4:
                    return 12;
                case 5:
                case 6:
                default:
                    throw new IllegalArgumentException("Invalid BlockFace rotation: " + rotation);
                case 7:
                    return 2;
                case 8:
                    return 14;
                case 9:
                    return 6;
                case 10:
                    return 10;
                case 11:
                    return 13;
                case 12:
                    return 15;
                case 13:
                    return 1;
                case 14:
                    return 3;
                case 15:
                    return 5;
                case 16:
                    return 7;
                case 17:
                    return 9;
                case 18:
                    return 11;
            }
        }

        public String getAsString() {
            Bukkit.getLogger().info("FUCK THIS SHIT 1");
            StringBuilder str = new StringBuilder("minecraft:" + material.toString().toLowerCase());
            str.append("[");
            str.append("data=").append(data);
            if (rotation != null)
                str.append(",rotation=").append(getBlockFace(rotation));
            return str.append("]").toString();
        }

        public boolean matches(BlockData blockData) {
            return blockData.getMaterial() == material &&
                    (data == -1 || blockData.getData() == data) &&
                    (rotation == null || rotation == blockData.getRotation());
        }

        @Override
        public BlockData clone() {
            return new BlockData(this);
        }
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

        BlockData blockData = BlockData.fromString(arr[0]);

        if (blockData == null)
            return null;

        return new LegacyNodeData(blockData, base64);
    }

    @Override
    public INodeData fromMaterialName(String str) {

        XMaterial xMaterial = ParseUtil.parseMaterial(str, true);
        if (xMaterial == null)
            return null;

        BlockData blockData = BlockData.fromMaterial(xMaterial);
        return new LegacyNodeData(blockData, null);
    }

    @Override
    public INodeData fromBlock(Block block) {
        String b64 = BlockSkullUtil.base64fromBlock(block);

        BlockData blockData = BlockData.fromBlock(block);
        return new LegacyNodeData(blockData, b64);
    }

    @Override
    public boolean matches(Block block) {
        BlockState state = block.getState();
        BlockData blockData = BlockData.fromBlock(block);

        if (!this.blockData.matches(blockData))
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
        return String.format("{%s||%s}", blockData.getAsString(), base64 == null ? "null" : base64);
    }

    @Override
    public void mutate(Block block, INodeData nodeData) {

        if (nodeData == null)
            Bukkit.getLogger().info("NODE DATA NULL");

        BlockData oldData = BlockData.fromBlock(block);
        if (nodeData instanceof LegacyNodeData) {
            oldData = ((LegacyNodeData) nodeData).getBlockData();
        } else Bukkit.getLogger().info("NOT LEGACY DATA - mutate");

        if (oldData == null)
            Bukkit.getLogger().info("OLD DATA NULL");

        mutate(block, oldData);
    }

    private void mutate(Block block, BlockData blockData) {
        Bukkit.getLogger().info(blockData.getAsString());

        this.blockData.toBlock(block, blockData);

        if (base64 != null) {
            BlockSkullUtil.blockWithBase64(block, base64);
            Bukkit.getLogger().info("Base64 set.");
        }

        Bukkit.getLogger().info("Modified state: " + block.getType().name() + " : " + block.getData() + " - " + BlockData.getRotation(block) + " - " + BlockSkullUtil.base64fromBlock(block));
    }

    @Override
    public void mutate(Block block) {
        mutate(block, blockData);
    }
}
