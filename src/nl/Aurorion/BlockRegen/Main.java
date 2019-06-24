package nl.Aurorion.BlockRegen;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;
import java.util.Set;

public class Main extends JavaPlugin {

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    public Economy econ;
    public WorldEditPlugin worldEdit;
    public GriefPrevention griefPrevention;

    private Files files;

    private ParticleUtil particleUtil;
    private Getters getters;

    private Random random;

    // Handles every output going to console, easier, more centralized control.
    public ConsoleOutput cO;

    // Has all the information from Blocklist.yml loaded on startup
    private FormatHandler formatHandler;

    public String newVersion = null;

    private boolean placeholderAPI = false;

    public boolean isPlaceholderAPI() {
        return placeholderAPI;
    }

    @Override
    public void onEnable() {
        instance = this;

        registerClasses(); // Also generates files

        // Setup ConsoleOutput
        cO = new ConsoleOutput(this);
        cO.setDebug(files.settings.getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.messages.getString("Messages.Prefix")));

        formatHandler = new FormatHandler(this);

        registerCommands();
        registerEvents();

        Messages.load();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            cO.info("&eFound PAPI! &aHooking!");
            placeholderAPI = true;
        }

        setupEconomy();
        setupWorldEdit();
        checkForPlugins();

        Utils.fillFireworkColors();
        recoveryCheck();

        enableMetrics();

        if (getGetters().updateChecker()) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                UpdateCheck updater = new UpdateCheck(this, 9885);

                try {
                    if (updater.checkForUpdates())
                        newVersion = updater.getLatestVersion();
                } catch (Exception e) {
                    cO.err("Could not check for updates.");
                }
            }, 20L);
        }

        cO.info("§aDone loading.");

        cO.info("You are using version " + getDescription().getVersion());
        cO.info("Report bugs or suggestions to discord only.");
        cO.info("Always backup if you are not sure about things.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        if (!getGetters().dataRecovery() && !Utils.regenProcesses.isEmpty()) {
            for (RegenProcess regenProcess : Utils.regenProcesses)
                regenProcess.getLoc().getBlock().setType(regenProcess.getMaterial());
        }

        for (BossBar bossBar : Utils.bars.values())
            bossBar.removeAll();

        instance = null;
    }

    private void registerClasses() {
        files = new Files(this);
        particleUtil = new ParticleUtil(this);
        getters = new Getters(this);
        random = new Random();
    }

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

    public void enableMetrics() {
        new MetricsLite(this);
        cO.info("MetricsLite enabled");
    }

    // ??
    private void checkForPlugins() {
        if (this.getJobs())
            cO.info("&eJobs found! &aEnabling Jobs fuctions.");
    }

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
        cO.info("&eVault & economy plugin found! &aEnabling economy functions.");
        return econ != null;
    }

    private boolean setupWorldEdit() {
        Plugin worldeditplugin = this.getServer().getPluginManager().getPlugin("WorldEdit");

        if (worldeditplugin == null || !(worldeditplugin instanceof WorldEditPlugin)) {
            cO.info("&eDidn't found WorldEdit. &cRegion functions disabled.");
            return false;
        }

        cO.info("&eWorldEdit found! &aEnabling region fuctions.");
        worldEdit = (WorldEditPlugin) worldeditplugin;
        return worldEdit != null;
    }

    //-------------------- Getters --------------------------
    public Economy getEconomy() {
        return this.econ;
    }

    public WorldEditPlugin getWorldEdit() {
        return this.worldEdit;
    }

    public boolean getJobs() {
        if (this.getServer().getPluginManager().getPlugin("Jobs") != null) {
            return true;
        }
        return false;
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

    public Getters getGetters() {
        return this.getters;
    }

    public Random getRandom() {
        return this.random;
    }

    public void recoveryCheck() {
        if (getGetters().dataRecovery()) {
            Set<String> set = files.getData().getKeys(false);
            if (!set.isEmpty()) {
                cO.info("Recovering blocks..");
                while (set.iterator().hasNext()) {
                    String name = set.iterator().next();
                    List<String> list = files.getData().getStringList(name);
                    for (int i = 0; i < list.size(); i++) {
                        Location loc = Utils.stringToLocation(list.get(i));
                        loc.getBlock().setType(Material.valueOf(name));
                        cO.debug("Recovered " + name + " on position " + Utils.locationToString(loc));
                    }
                    set.remove(name);
                }
                cO.info("Done.");
            }

            for (String key : files.getData().getKeys(false)) {
                files.getData().set(key, null);
            }

            files.saveData();
        }
    }

    public FormatHandler getFormatHandler() {
        return formatHandler;
    }
}
