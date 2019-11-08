package nl.Aurorion.BlockRegen.System;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class RegionBR {

    // Name, just for fun,... q.q
    private String name;

    // Region selection
    private CuboidRegion region;

    // Base region locations
    private Location locA;
    private Location locB;

    // Used block types
    private List<String> blockTypes = new ArrayList<>();
    // Overrides ^^
    private boolean useAll = true;

    // Regen all blocks by defaults in the settings.yml
    private boolean allBlocks = false;

    // Enables/disables the region
    private boolean enabled = true;

    private boolean valid = false;

    public boolean isValid() {
        return valid;
    }

    // Basic constructor for creating new, prob for loading too
    public RegionBR(String name, Location locA, Location locB) {
        this.name = name;

        if (locA == null || locB == null)
            return;

        this.locA = locA;
        this.locB = locB;

        this.region = new CuboidRegion(BukkitAdapter.asBlockVector(locA), BukkitAdapter.asBlockVector(locB));

        this.valid = true;
    }

    // Contains location?
    public boolean contains(Location loc) {
        return region.contains(BukkitAdapter.asBlockVector(loc));
    }

    public void addType(String name) {
        if (!blockTypes.contains(name))
            blockTypes.add(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CuboidRegion getRegion() {
        return region;
    }

    public void setRegion(CuboidRegion region) {
        this.region = region;
    }

    public Location getLocA() {
        return locA;
    }

    public void setLocA(Location locA) {
        this.locA = locA;
    }

    public Location getLocB() {
        return locB;
    }

    public void setLocB(Location locB) {
        this.locB = locB;
    }

    public List<String> getBlockTypes() {
        return blockTypes;
    }

    public void setBlockTypes(List<String> blockTypes) {
        this.blockTypes = blockTypes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseAll() {
        return useAll;
    }

    public void setUseAll(boolean useAll) {
        this.useAll = useAll;
    }

    public boolean isAllBlocks() {
        return allBlocks;
    }

    public void setAllBlocks(boolean allBlocks) {
        this.allBlocks = allBlocks;
    }
}
