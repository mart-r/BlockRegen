package nl.aurorion.blockregen.system.preset;

import com.cryptomorin.xseries.XSound;
import com.google.common.base.Strings;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.ConsoleOutput;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.struct.Amount;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.PresetConditions;
import nl.aurorion.blockregen.system.preset.struct.PresetRewards;
import nl.aurorion.blockregen.system.preset.struct.material.DynamicMaterial;
import nl.aurorion.blockregen.version.api.INodeData;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PresetManager {

    private final BlockRegen plugin;

    private final Map<String, BlockPreset> presets = new HashMap<>();

    private final Map<String, INodeData> typeAliases = new HashMap<>();

    public PresetManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Contract("null -> null")
    public INodeData getNodeData(String key) {
        if (Strings.isNullOrEmpty(key))
            return null;

        return typeAliases.containsKey(key) ? typeAliases.get(key) : plugin.getVersionManager().obtainNodeData().fromMaterialName(key.toUpperCase());
    }

    private String replaceAll(String str) {
        for (Map.Entry<String, INodeData> entry : typeAliases.entrySet()) {
            str = str.replace(entry.getKey(), entry.getValue().getAsString(true));
        }
        return str;
    }

    public Optional<BlockPreset> getPreset(String name) {
        return Optional.ofNullable(presets.getOrDefault(name, null));
    }

    public Optional<BlockPreset> getPresetByBlock(Block block) {
        return presets.values().stream()
                .filter(p -> p.getTargetMaterial().matches(block))
                .findAny();
    }

    public Map<String, BlockPreset> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public void loadAll() {

        typeAliases.clear();

        ConfigurationSection aliases = plugin.getFiles().getBlockList().getFileConfiguration().getConfigurationSection("Types");

        if (aliases != null) {
            for (String key : aliases.getKeys(false)) {
                INodeData value = plugin.getVersionManager().obtainNodeData().fromString(aliases.getString(key));
                if (value == null) {
                    ConsoleOutput.getInstance().warn("Invalid NodeData " + key);
                    continue;
                }
                typeAliases.put(key, value);
            }
            ConsoleOutput.getInstance().info("Loaded " + typeAliases.size() + " type aliase(s)...");
        }

        presets.clear();

        // Clear all events before loading.
        plugin.getEventManager().clearEvents();

        ConfigurationSection blocks = plugin.getFiles().getBlockList().getFileConfiguration().getConfigurationSection("Blocks");

        if (blocks == null) return;

        for (String key : blocks.getKeys(false)) {
            load(key);
        }

        ConsoleOutput.getInstance().info("Loaded " + presets.size() + " block preset(s)...");
        ConsoleOutput.getInstance().info("Added " + plugin.getEventManager().getLoadedEvents().size() + " event(s)...");
    }

    public void load(String name) {
        FileConfiguration file = plugin.getFiles().getBlockList().getFileConfiguration();

        ConfigurationSection section = file.getConfigurationSection("Blocks." + name);

        if (section == null) return;

        BlockPreset preset = new BlockPreset(name);

        // Target material
        String targetMaterial = section.getString("target-material");

        if (Strings.isNullOrEmpty(targetMaterial))
            targetMaterial = name;

        INodeData targetData = getNodeData(targetMaterial);

        if (targetData == null) {
            ConsoleOutput.getInstance().warn("Could not load preset " + name + ", invalid target material.");
            return;
        }

        preset.setTargetMaterial(targetData);

        // Replace material
        String replaceMaterial = section.getString("replace-block");

        if (Strings.isNullOrEmpty(replaceMaterial))
            replaceMaterial = "AIR";

        try {
            preset.setReplaceMaterial(DynamicMaterial.fromString(plugin, replaceAll(replaceMaterial)));
        } catch (IllegalArgumentException e) {
            plugin.getConsoleOutput().err("Dynamic material ( " + replaceMaterial + " ) in replace-block material for " + name + " is invalid: " + e.getMessage());
            if (plugin.getConsoleOutput().isDebug())
                e.printStackTrace();
            return;
        }

        // Regenerate into
        String regenerateInto = section.getString("regenerate-into");

        if (Strings.isNullOrEmpty(regenerateInto))
            regenerateInto = targetMaterial;

        try {
            preset.setRegenMaterial(DynamicMaterial.fromString(plugin, replaceAll(regenerateInto)));
        } catch (IllegalArgumentException e) {
            plugin.getConsoleOutput().err("Dynamic material ( " + regenerateInto + " ) in regenerate-into material for " + name + " is invalid: " + e.getMessage());
            if (plugin.getConsoleOutput().isDebug())
                e.printStackTrace();
            return;
        }

        // Delay
        preset.setDelay(Amount.load(file, "Blocks." + name + ".regen-delay", 3));

        // Affect drops
        preset.setAffectDrops(section.getBoolean("affect-drops", true));

        // Natural break
        preset.setNaturalBreak(section.getBoolean("natural-break", true));

        // Apply fortune
        preset.setApplyFortune(section.getBoolean("apply-fortune", true));

        // Drop naturally
        preset.setDropNaturally(section.getBoolean("drop-naturally", true));

        // Block Break Sound
        String sound = section.getString("sound");

        if (!Strings.isNullOrEmpty(sound)) {
            Optional<XSound> xSound = XSound.matchXSound(sound);
            if (!xSound.isPresent()) {
                ConsoleOutput.getInstance().warn("Sound " + sound + " in preset " + name + " is invalid.");
            } else preset.setSound(xSound.get());
        }

        // Particle
        String particleName = section.getString("particles");

        if (!Strings.isNullOrEmpty(particleName))
            preset.setParticle(particleName);

        String regenParticle = section.getString("regeneration-particles");

        if (!Strings.isNullOrEmpty(regenParticle))
            preset.setRegenerationParticle(regenParticle);

        // Conditions
        PresetConditions conditions = new PresetConditions();

        // Tools
        String toolsRequired = section.getString("tool-required");
        if (!Strings.isNullOrEmpty(toolsRequired)) {
            conditions.setToolsRequired(toolsRequired);
        }

        // Enchants
        String enchantsRequired = section.getString("enchant-required");
        if (!Strings.isNullOrEmpty(enchantsRequired)) {
            conditions.setEnchantsRequired(enchantsRequired);
        }

        // Jobs
        if (plugin.getJobsProvider() != null) {
            String jobsRequired = section.getString("jobs-check");
            if (!Strings.isNullOrEmpty(jobsRequired)) {
                conditions.setJobsRequired(jobsRequired);
            }
        }

        preset.setConditions(conditions);

        // Rewards
        PresetRewards rewards = PresetRewards.load(section);

        preset.setRewards(rewards);

        PresetEvent event = PresetEvent.load(section.getConfigurationSection("event"), name);

        if (event != null)
            plugin.getEventManager().addEvent(event);

        presets.put(name, preset);
    }
}