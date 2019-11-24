package nl.Aurorion.BlockRegen;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import nl.Aurorion.BlockRegen.BlockFormat.Amount;
import nl.Aurorion.BlockRegen.BlockFormat.BlockFormat;
import nl.Aurorion.BlockRegen.Commands.Commands;
import nl.Aurorion.BlockRegen.Commands.TabCompleterBR;
import nl.Aurorion.BlockRegen.Configurations.Files;
import nl.Aurorion.BlockRegen.Events.BlockListener;
import nl.Aurorion.BlockRegen.Events.PlayerInteract;
import nl.Aurorion.BlockRegen.Events.PlayerJoin;
import nl.Aurorion.BlockRegen.Particles.ParticleUtil;
import nl.Aurorion.BlockRegen.System.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author (s) Aurorion & Wertik1206
 */

public class Main extends JavaPlugin {

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    // Soft-Dependencies, soft as a meatbag, you.
    public Economy econ;
    public WorldEditPlugin worldEdit;
    public GriefPrevention griefPrevention;

    // There's no way just yet.
    public WorldGuardPlugin worldGuard;

    // Util classes
    private ParticleUtil particleUtil;
    private EnchantUtil enchantUtil;

    // Has all the information from Blocklist.yml loaded on startup
    private FormatHandler formatHandler;

    // handles region creation etc..
    private OptionHandler optionHandler;

    // Handles every output going to console, easier, more centralized control.
    public ConsoleOutput cO;

    // File system
    private Files files;

    // New version, deactivated for dev builds
    public String newVersion = null;

    // Use PAPI?
    private boolean placeholderAPI = false;

    // There's no real random, or coincidence in the world. What do we say about it? The Universe is rarely so lazy.
    private Random random;

    // Boolean options from Settings.yml

    private boolean useJobs;

    public boolean useJobs() {
        return useJobs;
    }

    private boolean persist;

    private boolean useGP;
    private boolean updateChecker;
    private boolean useTowny;
    private boolean dataRecovery;
    private boolean reloadInfo;
    private boolean useRegions;

    public Material defaultReplaceBlock;
    public Amount defaultRegenDelay;

    private void loadOptions() {
        // Recover data on sudden shutdown?
        dataRecovery = files.getSettings().getBoolean("Data-Recovery", false);

        reloadInfo = files.getSettings().getBoolean("Reload-Info-To-Sender", false);

        // Check for updates?
        updateChecker = files.getSettings().getBoolean("Update-Checker", false);

        // Grief Prevention support
        useGP = files.getSettings().getBoolean("GriefPrevention-Support", false);

        // Towny support
        useTowny = files.getSettings().getBoolean("Towny-Support", false);

        persist = files.getSettings().getBoolean("Persist", false);

        useRegions = files.getSettings().getBoolean("Use-Regions", false);

        defaultRegenDelay = loadAmount("defaults.regen-delay", 1);
        defaultReplaceBlock = Material.valueOf(files.getSettings().getString("defaults.replace-block", "AIR").toUpperCase());
    }

    public boolean usePlaceholderAPI() {
        return placeholderAPI;
    }

    @Override
    public void onEnable() {
        instance = this; // Set instance

        // Generates files
        files = new Files(this);

        // Enables particles
        particleUtil = new ParticleUtil(this);

        // Random
        random = new Random();

        // Boolean options
        loadOptions();

        // Setup ConsoleOutput for the first time
        cO = new ConsoleOutput(this);
        cO.setDebug(files.getSettings().getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.getMessages().getString("Messages.Prefix")));

        // Setup enchantUtil
        enchantUtil = new EnchantUtil();

        // Jobs support
        useJobs = getJobs();
        cO.debug("Jobs.. " + useJobs + ", " + getJobs());

        // Loads BlockBR formats
        formatHandler = new FormatHandler(this);
        formatHandler.loadBlocks();

        // Load regions
        optionHandler = new OptionHandler();

        if (useRegions)
            optionHandler.loadRegions();
        else
            optionHandler.loadWorlds();

        // Load messages to cache
        Messages.load();

        // Soft dependencies
        setupWorldEdit();
        setupWorldGuard();
        setupEconomy();

        // PlaceholderAPI support
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            cO.info("Found PAPI! &aHooking!");
            placeholderAPI = true;
        }

        // What?
        Utils.fillFireworkColors();

        // Check for block recovers
        recoveryCheck();

        // Load regen times
        FileConfiguration data = getFiles().getData();

        if (data.contains("Regen-Times"))
            for (String dataString : data.getStringList("Regen-Times")) {
                Location loc = Utils.stringToLocation(dataString.split(":")[0]);
                int value = Integer.parseInt(dataString.split(":")[1]);
                Utils.regenTimesBlocks.put(loc, value);
            }
        else data.createSection("Regen-Times");

        // Persist
        if (persist)
            // Load stuff
            for (String dataString : files.getData().getStringList("Persist")) {
                String[] arr = dataString.split(":");

                // locationString:MATERIAL_ORIGIN:TIME_UNTIL

                Location loc = Utils.stringToLocation(arr[0]);

                if (loc == null) {
                    cO.err("Invalid location in persist, regen process has not been added.");
                    continue;
                }

                // Ignore if there's no way to fetch BlockBR data, means it stays the original
                if (!formatHandler.getTypes().contains(arr[1]))
                    continue;

                Material material = Material.valueOf(arr[1].toUpperCase());

                BlockFormat blockBR = formatHandler.getBlockBR(material.name());

                loc.getBlock().setType(blockBR.getReplaceBlock());

                long time = Long.parseLong(arr[2]);

                new RegenProcess(loc, material, time);
            }

        // Metrics stats
        enableMetrics();

        // Check for updates
        if (updateChecker) {
            if (!getDescription().getVersion().contains("-b"))
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                    UpdateCheck updater = new UpdateCheck(this, 9885);

                    try {
                        if (updater.checkForUpdates())
                            newVersion = updater.getLatestVersion();
                    } catch (Exception e) {
                        cO.err("Could not check for updates.");
                    }
                }, 20L);
            else
                cO.info("You're running a development build, update checks ignored.");
        }

        // Events and commands
        registerEvents();

        registerCommands();

        // And we're all done
        cO.info("§aDone loading.");

        cO.info("&fYou are using development version &7v.&6" + getDescription().getVersion());
        cO.info("Bug reports and suggestions are accepted on discord only.");
        cO.info("Last warning before using it, can contain errors.");
        cO.info("Doesn't matter, it's already too late, enjoy the ride. &c;)");
    }

    public void reload(CommandSender sender) {
        long start = System.currentTimeMillis();

        if (reloadInfo)
            cO.setReloadSender(sender);

        if (useRegions)
            optionHandler.saveRegions();
        else
            optionHandler.saveWorlds();

        // Reload configuration files
        files.reload();

        // Load boolean options from Settings.yml
        loadOptions();

        // Update ConsoleOutput
        cO.setDebug(files.getSettings().getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.getMessages().getString("Messages.Prefix")));

        cO.info("Reloaded files..");

        if (cO.isDebug())
            cO.info("§eDebug enabled..");

        // Clear bars
        Utils.clearEvents();

        // In case soft-dependencies are installed now
        setupWorldEdit();
        setupWorldGuard();
        setupEconomy();
        useJobs = getJobs();

        // PAPI integration on reload, can be useful
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (!placeholderAPI) {
                if (reloadInfo)
                    cO.info("§7Found PAPI! Hooking!");
                placeholderAPI = true;
            }
        } else if (placeholderAPI) {
            cO.info("§7PAPI not found, disabling support.");
            placeholderAPI = false;
        }

        cO.info("§7Searched for dependencies..");

        // Setup enchantUtil
        enchantUtil = new EnchantUtil();

        // Load all messages from Messages.yml to cache
        Messages.load();
        cO.info("Loaded messages..");

        // Load block formats from Blocklist.yml
        formatHandler.reload();

        cO.info("Loaded " + formatHandler.getBlocks().size() + " format(s) and " + Utils.events.size() + " event(s)..");

        optionHandler.loadWorlds();
        optionHandler.loadRegions();

        cO.setReloadSender(null);

        long stop = System.currentTimeMillis();

        sender.sendMessage(Messages.get("Reload").replace("%time%", String.valueOf(stop - start)));
    }

    @Override
    public void onDisable() {
        // Stop all regen tasks
        getServer().getScheduler().cancelTasks(this);

        // If there's no recovery, replace all the blocks with before-regen state
        if (!Utils.regenProcesses.isEmpty() && persist) {
            cO.info("Saving persist..");

            FileConfiguration data = files.getData();
            List<String> persist = new ArrayList<>();

            for (RegenProcess regenProcess : Utils.regenProcesses) {
                // Replace, in case we never come back.
                regenProcess.getLoc().getBlock().setType(regenProcess.getMaterial());

                // Save time until regen
                long time = regenProcess.getRegenTime() - System.currentTimeMillis();
                cO.debug("Regen Time: " + regenProcess.getRegenTime() + " Persist time: " + time);
                cO.debug("Until Regen: " + regenProcess.getUntilRegenFormatted());

                // Save to Data.yml
                // - locationString:MATERIAL_ORIGINAL:TIME_UNTIL
                persist.add(Utils.locationToString(regenProcess.getLoc()) + ":" + regenProcess.getMaterial().name() + ":" + time);
            }

            data.set("Persist", persist);
        }

        // Clear all boss bars
        Utils.clearEvents();

        // Save regenTimes
        FileConfiguration data = getFiles().getData();
        List<String> regenTimes = new ArrayList<>();

        Utils.regenTimesBlocks.keySet().forEach(loc -> regenTimes.add(Utils.locationToString(loc) + ":" + Utils.regenTimesBlocks.get(loc)));

        data.set("Regen-Times", regenTimes);

        // Save all data
        getFiles().data.save();
    }

    // Register commands and tab completer
    private void registerCommands() {
        getCommand("blockregen").setExecutor(new Commands(this));
        getCommand("blockregen").setTabCompleter(new TabCompleterBR());
    }

    private void registerEvents() {
        PluginManager pm = this.getServer().getPluginManager();

        pm.registerEvents(new Commands(this), this);
        pm.registerEvents(new BlockListener(), this);
        pm.registerEvents(new PlayerInteract(this), this);
        pm.registerEvents(new PlayerJoin(this), this);
    }

    // Metrics
    public void enableMetrics() {
        new MetricsLite(this);
        cO.info("MetricsLite enabled");
    }

    // Vault economy
    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            cO.info("Didn't found Vault. &cEconomy functions disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            cO.info("Vault found, but no economy plugin. &cEconomy functions disabled.");
            return false;
        }

        econ = rsp.getProvider();
        cO.info("Vault & economy plugin found! &aEnabling economy functions.");
        return econ != null;
    }

    private boolean setupWorldEdit() {
        Plugin worldeditplugin = this.getServer().getPluginManager().getPlugin("WorldEdit");

        if (worldeditplugin == null || !(worldeditplugin instanceof WorldEditPlugin)) {
            cO.info("Did not find WorldEdit. &cRegion functions disabled.");
            cO.info("&7Plugin functions are limited to World-Wide usage only without WorldEdit.");
            return false;
        }

        cO.info("WorldEdit found! &aEnabling region fuctions.");
        worldEdit = (WorldEditPlugin) worldeditplugin;
        return worldEdit != null;
    }

    private boolean setupWorldGuard() {
        Plugin worldGuardPlugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (worldGuardPlugin == null || !(worldGuardPlugin instanceof WorldGuardPlugin)) {
            cO.info("Did not find WorldGuard. &cNo need to support it.");
            return false;
        }

        cO.info("WorldGuard Found! &aEnabling WG to BR command.");
        worldGuard = (WorldGuardPlugin) worldGuardPlugin;
        return worldGuard != null;
    }

    // Moved to Data.yml "." Recovery section
    public void recoveryCheck() {

        if (dataRecovery) {

            // Section is created on file startup
            ConfigurationSection section = files.getData().contains("Recovery") ? files.getData().getConfigurationSection("Recovery") : files.getData().createSection("Recovery");

            Set<String> blockTypes = section.getKeys(false);

            if (!blockTypes.isEmpty()) {

                cO.info("Recovering blocks..");

                // Loop through them
                while (blockTypes.iterator().hasNext()) {

                    // String list path and block material to place
                    String blockType = blockTypes.iterator().next();

                    // List of locations in string
                    List<String> locations = files.getData().getStringList(blockType);

                    // Go through locations and recover
                    for (int i = 0; i < locations.size(); i++) {

                        Location location = Utils.stringToLocation(locations.get(i));

                        // Check for world
                        if (location.getWorld() == null) {
                            cO.err("Could not recover due to a missing world.");
                            continue;
                        }

                        // Set the block
                        location.getBlock().setType(Material.valueOf(blockType));

                        cO.debug("Recovered " + blockType + " on position " + Utils.locationToString(location));

                        locations.remove(locations.get(i));
                    }

                    // Remove from set
                    blockTypes.remove(blockType);

                    // Clear the location list from Data.yml
                    // If not empty, keep it in case our world returns
                    if (locations.isEmpty())
                        section.set(blockType, null);
                    else
                        section.set(blockType, locations);
                }

                // Recovered all
                cO.info("Done.");
            }

            // Save the file
            files.data.save();
        }
    }

    private Amount loadAmount(String path, int defaultValue) {
        ConfigurationSection section = files.getSettings().getConfigurationSection(path);

        Amount amount = new Amount(defaultValue);

        // Fixed or not?
        try {
            if (section.contains("high") && section.contains("low")) {
                // Random amount
                try {
                    amount = new Amount(section.getInt("low"), section.getInt("high"));
                } catch (NullPointerException e) {
                    cO.warn("Amount on path " + path + " is not valid, returning default.");
                    return new Amount(defaultValue);
                }

                cO.debug(amount.toString());
            }
        } catch (NullPointerException e) {
            // Fixed
            try {
                amount = new Amount(files.getSettings().getInt(path));
            } catch (NullPointerException e1) {
                cO.warn("Amount on path " + path + " is not valid, returning default.");
                return new Amount(defaultValue);
            }
        }

        return amount;
    }

    //-------------------- Getters --------------------------
    public Economy getEconomy() {
        return this.econ;
    }

    public WorldEditPlugin getWorldEdit() {
        return this.worldEdit;
    }

    private boolean getJobs() {
        return getServer().getPluginManager().getPlugin("Jobs") != null;
    }

    public EnchantUtil getEnchantUtil() {
        return enchantUtil;
    }

    public GriefPrevention getGriefPrevention() {
        return griefPrevention;
    }

    public Files getFiles() {
        return this.files;
    }

    public ParticleUtil getParticles() {
        return this.particleUtil;
    }

    public Random getRandom() {
        return this.random;
    }

    public FormatHandler getFormatHandler() {
        return formatHandler;
    }

    public boolean isUseGP() {
        return useGP;
    }

    public boolean isUseTowny() {
        return useTowny;
    }

    public boolean isDataRecovery() {
        return dataRecovery;
    }

    public OptionHandler getOptionHandler() {
        return optionHandler;
    }
}
