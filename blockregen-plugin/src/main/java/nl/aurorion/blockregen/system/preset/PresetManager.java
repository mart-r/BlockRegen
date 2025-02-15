package nl.aurorion.blockregen.system.preset;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.google.common.base.Strings;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.ConsoleOutput;
import nl.aurorion.blockregen.hooks.MMOCoreHook;
import nl.aurorion.blockregen.system.event.struct.PresetEvent;
import nl.aurorion.blockregen.system.preset.struct.Amount;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import nl.aurorion.blockregen.system.preset.struct.PresetConditions;
import nl.aurorion.blockregen.system.preset.struct.PresetRewards;
import nl.aurorion.blockregen.system.preset.struct.material.DynamicMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.MMOTargetMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.RegularTargetMaterial;
import nl.aurorion.blockregen.system.preset.struct.material.TargetMaterial;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class PresetManager {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d*");
    private static final MMOCoreHook MMO_CORE_HOOK = MMOCoreHook.getInstance();

    private final BlockRegen plugin;

    private final Map<String, BlockPreset> presets = new HashMap<>();

    public PresetManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public Optional<BlockPreset> getPreset(String name) {
        return Optional.ofNullable(presets.getOrDefault(name, null));
    }

    public Optional<BlockPreset> getPresetByBlock(Block block) {
        return presets.values().stream()
                .filter(preset -> preset.getTargetMaterial().matchesMaterial(block))
                //plugin.getVersionManager().getMethods().compareType(block, preset.getTargetMaterial()))
                .findAny();
    }

    public Map<String, BlockPreset> getPresets() {
        return Collections.unmodifiableMap(presets);
    }

    public void loadAll() {
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

        TargetMaterial target;
        if (NUMBER_PATTERN.matcher(targetMaterial).matches()) {
            target = new MMOTargetMaterial(Integer.valueOf(targetMaterial));
        } else {
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(targetMaterial.toUpperCase());
    
            if (!xMaterial.isPresent()) {
                ConsoleOutput.getInstance().warn("Could not load preset " + name + ", invalid target material.");
                return;
            }
            target = new RegularTargetMaterial(xMaterial.get());
        }

        preset.setTargetMaterial(target);

        // Replace material
        String replaceMaterial = section.getString("replace-block");

        if (Strings.isNullOrEmpty(replaceMaterial))
            replaceMaterial = "AIR";

        try {
            preset.setReplaceMaterial(DynamicMaterial.fromString(replaceMaterial));
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
            preset.setRegenMaterial(DynamicMaterial.fromString(regenerateInto));
        } catch (IllegalArgumentException e) {
            plugin.getConsoleOutput().err("Dynamic material ( " + regenerateInto + " ) in regenerate-into material for " + name + " is invalid: " + e.getMessage());
            if (plugin.getConsoleOutput().isDebug())
                e.printStackTrace();
            return;
        }

        // Delay
        preset.setDelay(Amount.load(file, "Blocks." + name + ".regen-delay", 3));

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

        // MMOCore proefession requirement
        String professionRequired = section.getString("profession-level");
        if (professionRequired != null) {
            String[] split = professionRequired.split(" ");
            if (split.length != 2) {
                ConsoleOutput.getInstance().warn("Unable to parse profession-level " + professionRequired +
                            " because it does not contain the level requirement information");
            } else if (!NUMBER_PATTERN.matcher(split[1]).matches()) {
                ConsoleOutput.getInstance().warn("Unable to parse profession-level " + professionRequired +
                            " because the level is not a number");
            } else {
                String professionName = split[0];
                if (!MMO_CORE_HOOK.isProfession(professionName)) {
                    ConsoleOutput.getInstance().warn("Unable to parse profession-level " + professionRequired +
                                " because the profession was not found");
                } else {
                    int level = Integer.valueOf(split[1]);
                    conditions.setProfessionRequired(MMO_CORE_HOOK.getProfession(professionName), level);
                }
            }
        }

        preset.setConditions(conditions);

        // Rewards
        PresetRewards rewards = PresetRewards.load(section, preset);

        preset.setRewards(rewards);

        PresetEvent event = PresetEvent.load(section.getConfigurationSection("event"), name, preset);

        if (event != null)
            plugin.getEventManager().addEvent(event);

        presets.put(name, preset);
    }
}