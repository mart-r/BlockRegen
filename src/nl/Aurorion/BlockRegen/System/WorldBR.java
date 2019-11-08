package nl.Aurorion.BlockRegen.System;

import java.util.ArrayList;
import java.util.List;

public class WorldBR {

    // Name, just for fun,... q.q
    private String name;

    // Used block types
    private List<String> blockTypes = new ArrayList<>();
    // Overrides ^^
    private boolean useAll = true;

    // Regen all the blocks
    private boolean allBlocks = false;

    // Enables/disables the region
    private boolean enabled = true;

    public WorldBR(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getBlockTypes() {
        return blockTypes;
    }

    public void setBlockTypes(List<String> blockTypes) {
        this.blockTypes = blockTypes;
    }

    public boolean isUseAll() {
        return useAll;
    }

    public void setUseAll(boolean useAll) {
        this.useAll = useAll;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addType(String name) {
        if (!blockTypes.contains(name))
            blockTypes.add(name);
    }

    public boolean isAllBlocks() {
        return allBlocks;
    }

    public void setAllBlocks(boolean allBlocks) {
        this.allBlocks = allBlocks;
    }
}
