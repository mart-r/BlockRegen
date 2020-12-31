package nl.aurorion.blockregen.version.api;

import org.bukkit.block.Block;
import org.jetbrains.annotations.Contract;

public interface INodeData {

    void mutate(Block block);

    void mutate(Block block, INodeData nodeData);

    @Contract("null -> null")
    INodeData fromBlock(Block block);

    @Contract("null -> null")
    INodeData fromString(String str);

    @Contract("null -> null")
    INodeData fromMaterialName(String str);

    String getAsString(boolean omitEmpty);

    default String getAsString() {
        return getAsString(false);
    }

    default boolean matches(Block block) {
        return matches(fromBlock(block));
    }

    default boolean matches(INodeData nodeData) {
        return getAsString().equals(nodeData.getAsString());
    }
}
