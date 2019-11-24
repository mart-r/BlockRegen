package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.Configurations.Configuration;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OptionHandler {

    private Main plugin;

    private HashMap<String, RegionBR> regionCache;
    private HashMap<String, WorldBR> worldCache;

    private Configuration storage;

    public OptionHandler() {
        plugin = Main.getInstance();

        regionCache = new HashMap<>();
        worldCache = new HashMap<>();

        storage = plugin.getFiles().regions;
    }

    public void createRegion(String name, Location locA, Location locB) {
        RegionBR region = new RegionBR(name, locA, locB);

        regionCache.put(name, region);
        plugin.cO.debug("Created new region " + name);
    }

    public void removeRegion(String name) {
        regionCache.remove(name);
        plugin.cO.debug("Removed region " + name);
    }

    // Is region name valid?
    public boolean isValid(String name) {
        return regionCache.containsKey(name);
    }

    // Is location in region?
    public boolean isInRegion(Location loc) {
        return getRegion(loc) != null;
    }

    // Get Region by block location
    public RegionBR getRegion(Location loc) {
        for (RegionBR reg : regionCache.values())
            if (reg.contains(loc))
                return reg;

        return null;
    }

    // Get region by name
    public RegionBR getRegion(String name) {
        return regionCache.getOrDefault(name, null);
    }

    public WorldBR getWorld(String name) {
        return worldCache.getOrDefault(name, new WorldBR(name));
    }

    // Load region from Regions.yml
    public void loadRegions() {
        ConfigurationSection section = storage.getYaml().contains("Regions") ? storage.getYaml().getConfigurationSection("Regions") : storage.getYaml().createSection("Regions");

        regionCache = new HashMap<>();

        for (String name : section.getKeys(false)) {
            ConfigurationSection regSection = section.getConfigurationSection(name);

            RegionBR region;

            if (regSection.getBoolean("polygon")) {
                region = new RegionBR(name, regSection.getString("polygon-name"));
            } else
                region = new RegionBR(name, Utils.stringToLocation(regSection.getString("Min")), Utils.stringToLocation(regSection.getString("Max")));

            if (!region.isValid()) {
                plugin.cO.warn("Region " + name + " is invalid, removing it.");
                continue;
            }

            region.setBlockTypes(regSection.contains("block-types") ? regSection.getStringList("block-types") : new ArrayList<>());

            region.setEnabled(regSection.getBoolean("enabled", true));

            region.setUseAll(regSection.getBoolean("use-all", true));

            region.setAllBlocks(regSection.getBoolean("all-blocks", false));

            regionCache.put(name, region);
            plugin.cO.debug("Loaded region " + name);
        }

        plugin.cO.info("Loaded " + regionCache.size() + " region(s).");
    }

    public void saveRegions() {
        storage.getYaml().set("Regions", null);

        ConfigurationSection section = storage.getYaml().contains("Regions") ? storage.getYaml().getConfigurationSection("Regions") : storage.getYaml().createSection("Regions");

        for (RegionBR reg : regionCache.values()) {
            ConfigurationSection regSection = section.createSection(reg.getName());

            regSection.set("block-types", reg.getBlockTypes());
            regSection.set("enabled", reg.isEnabled());
            regSection.set("use-all", reg.isUseAll());
            regSection.set("all-blocks", reg.isAllBlocks());
            regSection.set("polygon", reg.isPolygon());
            regSection.set("polygon-name", reg.getPolygonName());

            regSection.set("Min", Utils.locationToString(reg.getLocA()));
            regSection.set("Max", Utils.locationToString(reg.getLocB()));

            plugin.cO.debug("Saved region " + reg.getName());
        }

        storage.save();
    }

    public void loadWorlds() {
        ConfigurationSection section = storage.getYaml().contains("Worlds") ? storage.getYaml().getConfigurationSection("Worlds") : storage.getYaml().createSection("Worlds");

        worldCache = new HashMap<>();

        for (String name : section.getKeys(false)) {
            ConfigurationSection worldSection = section.getConfigurationSection(name);

            WorldBR world = new WorldBR(name);

            world.setBlockTypes(worldSection.contains("block-types") ? worldSection.getStringList("block-types") : new ArrayList<>());
            world.setEnabled(worldSection.getBoolean("enabled", true));
            world.setUseAll(worldSection.getBoolean("use-all", true));
            world.setAllBlocks(worldSection.getBoolean("all-blocks", false));

            worldCache.put(name, world);
            plugin.cO.debug("Loaded world " + name);
        }

        plugin.cO.info("Loaded " + worldCache.size() + " world(s).");
    }

    public void saveWorlds() {
        storage.getYaml().set("Worlds", null);

        ConfigurationSection section = storage.getYaml().contains("Worlds") ? storage.getYaml().getConfigurationSection("Worlds") : storage.getYaml().createSection("Worlds");

        for (WorldBR worldBR : worldCache.values()) {
            ConfigurationSection worldSection = section.createSection(worldBR.getName());

            worldSection.set("block-types", worldBR.getBlockTypes());
            worldSection.set("enabled", worldBR.isEnabled());
            worldSection.set("use-all", worldBR.isUseAll());
            worldSection.set("all-blocks", worldBR.isAllBlocks());

            plugin.cO.debug("Saved world " + worldBR.getName());
        }

        worldCache.clear();

        storage.save();
    }

    public HashMap<String, RegionBR> getRegionCache() {
        return regionCache;
    }

    public HashMap<String, WorldBR> getWorldCache() {
        return worldCache;
    }

    public List<String> getRegions() {
        return new ArrayList<>(regionCache.keySet());
    }

    public List<String> getWorlds() {
        return new ArrayList<>(worldCache.keySet());
    }
}
