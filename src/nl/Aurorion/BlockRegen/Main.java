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
import org.bukkit.Server;
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

public class Main extends JavaPlugin {

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    private Economy econ;
    public WorldEditPlugin worldEdit;
    public WorldGuardPlugin worldGuard;

    private GriefPrevention griefPrevention;

    private Files files;

    private ParticleUtil particleUtil;
    private EnchantUtil enchantUtil;

    private Random random;

    // Handles every output going to console, easier, more centralized control.
    public ConsoleOutput cO;

    // Has all the information from Blocklist.yml loaded on startup
    private FormatHandler formatHandler;

    public String newVersion = null;

    private boolean placeholderAPI = false;
    private boolean over18 = false;
    private boolean useRegenTimes = false;

    public boolean useRegenTimes() {
        return useRegenTimes;
    }

    public void useRegenTimes(boolean useRegenTimes) {
        this.useRegenTimes = useRegenTimes;
    }

    public boolean isOver18() {
        return over18;
    }

    public boolean isPlaceholderAPI() {
        return placeholderAPI;
    }

    private boolean useGP;
    private boolean updateChecker;
    private boolean useTowny;
    private boolean dataRecovery;

    private void loadOptions() {
        useGP = files.getSettings().getBoolean("GriefPrevention-Support");
        if (files.getSettings().get("Update-Checker") != null)
            updateChecker = files.getSettings().getBoolean("Update-Checker");
        else updateChecker = false;
        dataRecovery = files.getSettings().getBoolean("Data-Recovery");
        useTowny = files.getSettings().getBoolean("Towny-Support");
    }

    @Override
    public void onEnable() {
        instance = this;

        files = new Files(this);

        // Setup ConsoleOutput
        cO = new ConsoleOutput(this);
        cO.setDebug(files.settings.getConfig().getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.messages.getConfig().getString("Messages.Prefix")));

        loadOptions();

        // Versions, just to remove the need to check every, single, time.

        if (getVersion().contains("1_12") || getVersion().contains("1_9") || getVersion().contains("1_10") || getVersion().contains("1_11"))
            over18 = true;

        cO.debug("Version: " + getVersion() + " Over 1.8: " + over18);

        registerClasses(); // Also generates files

        formatHandler = new FormatHandler(this);

        registerEvents();

        Messages.load();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            cO.info("Found PAPI! &aHooking!");
            placeholderAPI = true;
        }

        setupEconomy();
        setupWorldEdit();
        setupWorldGuard();
        checkForPlugins();

        Utils.fillFireworkColors();
        recoveryCheck();

        enableMetrics();

        if (updateChecker) {
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

        cO.info("Loading breaks..");

        // Load regen times
        FileConfiguration data = getFiles().getData();

        if (data.contains("Regen-Times"))
            for (String dataString : data.getStringList("Regen-Times")) {
                Location loc = Utils.stringToLocation(dataString.split(":")[0]);
                int value = Integer.parseInt(dataString.split(":")[1]);
                Utils.regenTimesBlocks.put(loc, value);
            }
        else data.createSection("Regen-Times");

        // Commands moved here, don't want them registered without the plugin loading fine, thanks mibby.
        registerCommands();

        // And we're all done
        cO.info("§aDone loading.");

        cO.info("&fYou are using development version &7v.&6" + getDescription().getVersion());
        cO.info("Bug reports and suggestions are accepted on discord only.");
        cO.info("Last warning before using it, can contain errors.");
        cO.info("Doesn't matter, it's already too late, enjoy the ride. &c;)");
    }

    public void reload(CommandSender sender) {
        instance = this;

        files.reload();
        loadOptions();
        sender.sendMessage("§7Reloaded files..");

        // Setup ConsoleOutput
        cO = new ConsoleOutput(this);
        cO.setDebug(files.settings.getConfig().getBoolean("Debug-Enabled", false));
        cO.setPrefix(Utils.color(files.messages.getConfig().getString("Messages.Prefix")));

        Utils.bars.clear();
        Utils.events.clear();

        setupEconomy();
        setupWorldEdit();
        setupWorldGuard();
        checkForPlugins();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            sender.sendMessage("§eFound PAPI! §aHooking!");
            placeholderAPI = true;
        }

        sender.sendMessage("§7Searched for dependencies..");

        enchantUtil = new EnchantUtil();

        Messages.load();

        formatHandler.reload();
        sender.sendMessage("§7Loaded formats..");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        if (!dataRecovery && !Utils.regenProcesses.isEmpty())
            for (RegenProcess regenProcess : Utils.regenProcesses)
                regenProcess.getLoc().getBlock().setType(regenProcess.getMaterial());

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

        instance = null;
    }

    private void registerClasses() {
        particleUtil = new ParticleUtil(this);
        random = new Random();

        enchantUtil = new EnchantUtil();
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

    private void enableMetrics() {
        new MetricsLite(this);
        cO.info("MetricsLite enabled");
    }

    // ??
    private void checkForPlugins() {
        if (this.getJobs())
            cO.info("Jobs found! &aEnabling Jobs fuctions.");
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
        cO.info("Vault & economy plugin found! &aEnabling economy functions.");
        return econ != null;
    }

    private boolean setupWorldEdit() {
        Plugin worldeditplugin = this.getServer().getPluginManager().getPlugin("WorldEdit");

        if (worldeditplugin == null || !(worldeditplugin instanceof WorldEditPlugin)) {
            cO.info("Didn't found WorldEdit. &cRegion functions disabled.");
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

    //-------------------- Getters --------------------------
    public Economy getEconomy() {
        return this.econ;
    }

    public WorldEditPlugin getWorldEdit() {
        return this.worldEdit;
    }

    public boolean getJobs() {
        return this.getServer().getPluginManager().getPlugin("Jobs") != null;
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

    private void recoveryCheck() {
        if (dataRecovery) {
            ConfigurationSection recovery = files.getData().getConfigurationSection("Recovery");

            Set<String> set = recovery.getKeys(false);

            if (!set.isEmpty()) {

                cO.info("Recovering blocks..");

                while (set.iterator().hasNext()) {

                    String blockString = set.iterator().next();

                    List<String> list = recovery.getStringList(blockString);

                    if (list.isEmpty()) {
                        set.remove(blockString);
                        recovery.set(blockString, null);
                        continue;
                    }

                    while (list.iterator().hasNext()) {
                        String locString = list.iterator().next();
                        Location loc = Utils.stringToLocation(locString);

                        if (loc.getWorld() == null) {
                            cO.err("Could not recover due to a missing world.");
                            continue;
                        }

                        loc.getBlock().setType(Material.valueOf(blockString.split(";")[0].toUpperCase()));
                        loc.getBlock().setData(Byte.valueOf(blockString.split(";")[1]));

                        cO.debug("Recovered " + blockString + " on position " + Utils.locationToString(loc));
                        list.remove(locString);
                    }

                    set.remove(blockString);
                    recovery.set(blockString, list);
                }
                cO.info("Done.");
            }

            files.saveData();
        }
    }

    public static String getVersion() {
        Server server = instance.getServer();
        final String packageName = server.getClass().getPackage().getName();

        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    public EnchantUtil getEnchantUtil() {
        return enchantUtil;
    }

    public FormatHandler getFormatHandler() {
        return formatHandler;
    }

    public boolean isUseRegenTimes() {
        return useRegenTimes;
    }

    public boolean isUseGP() {
        return useGP;
    }

    public boolean isUpdateChecker() {
        return updateChecker;
    }

    public boolean isUseTowny() {
        return useTowny;
    }

    public boolean isDataRecovery() {
        return dataRecovery;
    }
}
