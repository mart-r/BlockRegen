package nl.Aurorion.BlockRegen;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import nl.Aurorion.BlockRegen.Commands.Commands;
import nl.Aurorion.BlockRegen.Commands.TabCompleterBR;
import nl.Aurorion.BlockRegen.Configurations.Files;
import nl.Aurorion.BlockRegen.Events.BlockBreak;
import nl.Aurorion.BlockRegen.Events.PlayerInteract;
import nl.Aurorion.BlockRegen.Events.PlayerJoin;
import nl.Aurorion.BlockRegen.Particles.ParticleUtil;
import nl.Aurorion.BlockRegen.System.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
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
    // Todo figure out a way to bypass WorldGuard protection
    public WorldGuardPlugin worldGuard;

    // Util classes
    private ParticleUtil particleUtil;
    private EnchantUtil enchantUtil;

    // Has all the information from Blocklist.yml loaded on startup
    private FormatHandler formatHandler;

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

    private boolean useGP;
    private boolean updateChecker;
    private boolean useTowny;
    private boolean dataRecovery;
    private boolean reloadInfo;

    private void loadOptions() {
        // Recover data on sudden shutdown?
        dataRecovery = files.getSettings().getBoolean("Data-Recovery");

        reloadInfo = files.getSettings().getBoolean("Reload-Info-To-Sender");

        // Check for updates?
        if (files.getSettings().get("Update-Checker") != null)
            updateChecker = files.getSettings().getBoolean("Update-Checker");
        else updateChecker = false;

        // Grief Prevention support
        useGP = files.getSettings().getBoolean("GriefPrevention-Support");

        // Towny support
        useTowny = files.getSettings().getBoolean("Towny-Support");
    }

    public boolean isPlaceholderAPI() {
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
        cO.setDebug(files.settings.getConfig().getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.messages.getConfig().getString("Messages.Prefix")));

        // Setup enchantUtil
        enchantUtil = new EnchantUtil();

        // Loads BlockBR formats on instantiation
        formatHandler = new FormatHandler(this);

        // Commands and events
        registerCommands();
        registerEvents();

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

        Utils.fillFireworkColors();

        recoveryCheck();

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

        // Load regen times
        FileConfiguration data = getFiles().getData();

        if (data.contains("Regen-Times"))
            for (String dataString : data.getStringList("Regen-Times")) {
                Location loc = Utils.stringToLocation(dataString.split(":")[0]);
                int value = Integer.parseInt(dataString.split(":")[1]);
                Utils.regenTimesBlocks.put(loc, value);
            }
        else data.createSection("Regen-Times");

        // And we're all done
        cO.info("§aDone loading.");

        cO.info("&fYou are using development version &7v.&6" + getDescription().getVersion());
        cO.info("Bug reports and suggestions are accepted on discord only.");
        cO.info("Last warning before using it, can contain errors.");
        cO.info("Doesn't matter, it's already too late, enjoy the ride. &c;)");
    }

    public void reload(CommandSender sender) {
        if (reloadInfo)
            cO.setReloadSender(sender);

        // Reload all files
        files.reload();

        // Load boolean options from Settings.yml
        loadOptions();

        if (reloadInfo)
            sender.sendMessage("§7Reloaded files..");

        // Update ConsoleOutput
        cO.setDebug(files.settings.getConfig().getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.messages.getConfig().getString("Messages.Prefix")));

        if (cO.isDebug())
            if (reloadInfo)
                sender.sendMessage("§eDebug enabled..");

        // Clear bars
        Utils.bars.clear();

        // In case soft-dependencies are installed now
        setupWorldEdit();
        setupWorldGuard();
        setupEconomy();

        // PAPI integration on reload, can be useful
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (!placeholderAPI) {
                if (reloadInfo)
                    sender.sendMessage("§7Found PAPI! Hooking!");
                placeholderAPI = true;
            }
        } else if (placeholderAPI) {
            if (reloadInfo)
                sender.sendMessage("§7PAPI not found, disabling support.");
            placeholderAPI = false;
        }

        if (reloadInfo)
            sender.sendMessage("§7Searched for dependencies..");

        // Setup enchantUtil
        enchantUtil = new EnchantUtil();

        // Load all messages from Messages.yml to cache
        Messages.load();
        if (reloadInfo)
            sender.sendMessage("§7Loaded messages..");

        // Load block formats from Blocklist.yml
        formatHandler.reload();
        if (reloadInfo)
            sender.sendMessage("§7Loaded " + formatHandler.getBlocks().size() + " format(s) and " + Utils.events.size() + " event(s)..");

        cO.setReloadSender(null);
    }

    @Override
    public void onDisable() {
        // Stop all regen tasks
        getServer().getScheduler().cancelTasks(this);

        // If there's no recovery, replace all the blocks with before-regen state
        if (!dataRecovery && !Utils.regenProcesses.isEmpty())
            for (RegenProcess regenProcess : Utils.regenProcesses)
                regenProcess.getLoc().getBlock().setType(regenProcess.getMaterial());

        // Clear all boss bars
        for (BossBar bossBar : Utils.bars.values())
            bossBar.removeAll();

        // Save regenTimes
        FileConfiguration data = getFiles().getData();
        List<String> regenTimes = new ArrayList<>();

        for (Location loc : Utils.regenTimesBlocks.keySet())
            regenTimes.add(Utils.locationToString(loc) + ":" + Utils.regenTimesBlocks.get(loc));

        data.set("Regen-Times", regenTimes);

        // Save all data
        getFiles().saveData();
    }

    // Register commands and tab completers
    private void registerCommands() {
        getCommand("blockregen").setExecutor(new Commands(this));
        getCommand("blockregen").setTabCompleter(new TabCompleterBR());
    }

    private void registerEvents() {
        PluginManager pm = this.getServer().getPluginManager();

        pm.registerEvents(new Commands(this), this);
        pm.registerEvents(new BlockBreak(this), this);
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
            ConfigurationSection section = files.getData().getConfigurationSection("Recovery");

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
            files.saveData();
        }
    }

    //-------------------- Getters --------------------------
    public Economy getEconomy() {
        return this.econ;
    }

    public WorldEditPlugin getWorldEdit() {
        return this.worldEdit;
    }

    public boolean getJobs() {
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
}
