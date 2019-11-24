package nl.Aurorion.BlockRegen.Commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionType;
import nl.Aurorion.BlockRegen.BlockFormat.BlockFormat;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.System.RegionBR;
import nl.Aurorion.BlockRegen.System.WorldBR;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor, Listener {

    // Todo add regen region command
    // Todo add ItemStack to Blocklist.yml command

    private Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage("§8§m        §r §3BlockRegen §7v.§f" + plugin.getDescription().getVersion() + " §8§m        "
                + "\n§3/" + label + " reload §8- §7Reload the Settings.yml, Messages.yml and Blocklist.yml."
                + "\n§3/" + label + " bypass §8- §7Bypass the events."
                + "\n§3/" + label + " check §8- §7Check the type of the block to put in the blocklist."
                + "\n§3/" + label + " regions §8- §7All the info about regions."
                + "\n§3/" + label + " events §8- §7Check all your events."
                + "\n§3/" + label + " worlds §8- §7All the info about worlds."
                + "\n§3/" + label + " debug §8- §7Enabled player debug mode. \n§c[§4!§c] §7A slightly less experimental feature\n"
                + "\n§8§m                                                 §r");
    }

    private void regionHelp(CommandSender s, String label) {
        s.sendMessage("§8§m        §r §3BlockRegen §8§m        §r\n");

        if (!plugin.getFiles().getSettings().getBoolean("Use-Regions"))
            s.sendMessage("§cEnable §7Use-Regions §cin your config in order to use per-region.");

        s.sendMessage("\n§3/" + label + " region set <name> §8- §7Set a region." +
                "\n§3/" + label + " region remove <name> §8- §7Remove a region." +
                "\n§3/" + label + " region list §8- §7List all your regions." +
                "\n§3/" + label + " region info <name> §8- §7List configured blocks for a region." +
                "\n§3/" + label + " region add <regionName> <formatName> §8- §7Add block type to a region." +
                "\n§3/" + label + " region remove <regionName> <formatName> §8- §7Remove block type from a region." +
                "\n§3/" + label + " region clear <regionName> §8- §7Clear block types from a region." +
                "\n§3/" + label + " region enabled <name> <true/false> §8- §7Enable/Disable a region." +
                "\n§3/" + label + " region useAll <regionName> <true/false> §8- §7Override configured blocks.");

        if (plugin.worldGuard != null)
            s.sendMessage("§3/" + label + " region fromWG <name> <WorldGuard ID (name)> §8- §7create a region from WG region boundaries.");

        s.sendMessage("\n§8§m                                                 §r");
    }

    private void worldHelp(CommandSender s, String label) {
        s.sendMessage("§8§m        §r §3BlockRegen §8§m        §r\n");

        if (plugin.getFiles().getSettings().getBoolean("Use-Regions"))
            s.sendMessage("§cDisable §7Use-Regions §cin your config in order to use per-world.");

        s.sendMessage("\n§3/" + label + " world add <worldName> <formatName> §8- §7Add block type to a world." +
                "\n§3/" + label + " world remove <worldName> <formatName> §8- §7Remove block type from a world." +
                "\n§3/" + label + " world info <worldName> §8- §7List configured blocks for a world." +
                "\n§3/" + label + " world clear <worldName> §8- §7Clear all block settings for a world." +
                "\n§3/" + label + " world useAll <worldName> <true/false> §8- §7Override block settings." +
                "\n§3/" + label + " world enabled <worldName> <true/false> §8- §7Enable/Disable BlockRegen in a world.");

        s.sendMessage("\n§8§m                                                 §r");
    }

    private boolean checkFormat(CommandSender s, String name) {
        if (plugin.getFormatHandler().getBlockBR(name) == null) {
            s.sendMessage("§cThere is no loaded block format with that name.");
            return false;
        }

        return true;
    }

    private boolean checkRegion(CommandSender s, String name) {
        if (!plugin.getOptionHandler().isValid(name)) {
            s.sendMessage(Messages.get("Unknown-Region"));
            return false;
        }

        return true;
    }

    private boolean checkWorld(CommandSender s, String name) {
        if (!plugin.getFiles().getSettings().getStringList("Worlds-Enabled").contains(name)) {
            s.sendMessage("§cThat world is not enabled.");
            return false;
        }

        return true;
    }

    private boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("Console-Sender-Error"));
            return true;
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            help(sender, label);
            return false;
        }

        Player player;

        if (!sender.hasPermission("blockregen.admin")) {
            sender.sendMessage(Messages.get("Insufficient-Permission"));
            return true;
        }

        // First argument switch
        switch (args[0].toLowerCase()) {
            // --------------------------------------------------------------- Reload
            case "reload":
                plugin.reload(sender);
                break;
            // --------------------------------------------------------------- Bypass
            case "bypass":
                if (isPlayer(sender))
                    return true;

                player = (Player) sender;

                if (!Utils.bypass.contains(player.getName())) {
                    Utils.bypass.add(player.getName());
                    player.sendMessage(Messages.get("Bypass-On"));
                } else {
                    Utils.bypass.remove(player.getName());
                    player.sendMessage(Messages.get("Bypass-Off"));
                }
                break;
            // --------------------------------------------------------------- Check
            case "check":
                if (isPlayer(sender))
                    return true;

                player = (Player) sender;

                if (!player.hasPermission("blockregen.datacheck")) {
                    player.sendMessage(Messages.get("Insufficient-Permission"));
                    return true;
                }

                if (!Utils.blockCheck.contains(player.getName())) {
                    Utils.blockCheck.add(player.getName());
                    player.sendMessage(Messages.get("Data-Check-On"));
                } else {
                    Utils.blockCheck.remove(player.getName());
                    player.sendMessage(Messages.get("Data-Check-Off"));
                }
                break;
            // --------------------------------------------------------------- Convert
            case "convert":
                convert();
                sender.sendMessage(Messages.get("Prefix") + "§aConverted your regions to BlockRegen 3.4.0+ compatibility!");
                break;
            // --------------------------------------------------------------- Region
            case "regions":
            case "region":
                if (isPlayer(sender))
                    return true;

                if (plugin.worldEdit == null) {
                    sender.sendMessage(Messages.get("Prefix") + "§cWorldEdit is needed for region support.");
                    return true;
                }

                player = (Player) sender;
                RegionBR regionBR;

                List<String> regions = new ArrayList<>(plugin.getOptionHandler().getRegionCache().keySet());

                if (args.length < 2) {
                    regionHelp(sender, label);
                    return true;
                }

                // Second argument switch
                switch (args[1].toLowerCase()) {
                    // --------------------------------------------------------------- List
                    case "l":
                    case "list":
                        player.sendMessage("§8§m       §r §3BlockRegen Regions §8§m        §r");
                        player.sendMessage("§r ");

                        if (!regions.isEmpty())
                            regions.forEach(loopRegion -> player.sendMessage("§8 - §7" + loopRegion));
                        else
                            player.sendMessage("§cThere are no regions set yet. §7/" + label + " region set <name>");

                        player.sendMessage("\n§8§m                                                 §r");
                        break;
                    // --------------------------------------------------------------- Set/Create
                    case "set":
                        Region s;

                        try {
                            s = plugin.getWorldEdit().getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
                        } catch (IncompleteRegionException e) {
                            player.sendMessage(Messages.get("No-Region-Selected"));
                            return true;
                        }

                        if (regions.contains(args[2])) {
                            player.sendMessage(Messages.get("Duplicated-Region"));
                            return true;
                        }

                        plugin.getOptionHandler().createRegion(args[2], BukkitAdapter.adapt(player.getWorld(), s.getMinimumPoint()), BukkitAdapter.adapt(player.getWorld(), s.getMaximumPoint()));
                        player.sendMessage(Messages.get("Set-Region"));
                        break;
                    // --------------------------------------------------------------- From WG
                    case "fromwg":
                        // WorldGuard not enabled
                        if (plugin.worldGuard == null) {
                            player.sendMessage(Messages.get("WorldGuard-Required"));
                            return true;
                        }

                        // Duplication check
                        if (regions.contains(args[2])) {
                            player.sendMessage(Messages.get("Duplicated-Region"));
                            return true;
                        }

                        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

                        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

                        RegionManager regionManager = container.get(localPlayer.getWorld());

                        // Check region
                        if (!regionManager.hasRegion(args[3])) {
                            player.sendMessage(Messages.get("Invalid-Region-Id"));
                            player.sendMessage(Messages.get("Invalid-Region-Id-Tip"));
                            return true;
                        }

                        ProtectedRegion region = regionManager.getRegion(args[3]);

                        // Polygonal region
                        if (region.getType().equals(RegionType.POLYGON)) {

                            player.sendMessage("§7");

                        } else {

                            // Create regionBR option instance
                            plugin.getOptionHandler().createRegion(args[2], BukkitAdapter.adapt(player.getWorld(), region.getMinimumPoint()), BukkitAdapter.adapt(player.getWorld(), region.getMaximumPoint()));
                        }

                        player.sendMessage(Messages.get("Imported-Region"));
                        break;
                    // --------------------------------------------------------------- Remove Region / Remove Block Type
                    // /blockregen region remove <regionName> (formatName)
                    case "r":
                    case "remove":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        // Block type action
                        if (args.length > 3) {
                            regionBR = plugin.getOptionHandler().getRegion(args[2]);

                            if (!regionBR.getBlockTypes().contains(args[3])) {
                                sender.sendMessage("§cRegion does not have that format configured. §7/" + label + " region info " + regionBR.getName());
                                return true;
                            }

                            regionBR.getBlockTypes().remove(args[3]);
                            sender.sendMessage("§aBlock type added to region.");
                            break;
                        }

                        plugin.getOptionHandler().removeRegion(args[2]);

                        player.sendMessage(Messages.get("Remove-Region"));
                        break;
                    // --------------------------------------------------------------- List Block Types
                    case "i":
                    case "info":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        regionBR = plugin.getOptionHandler().getRegion(args[2]);

                        List<String> blockTypes = regionBR.getBlockTypes();

                        if (regionBR.isUseAll())
                            blockTypes = plugin.getFormatHandler().getTypes();

                        sender.sendMessage("§3Block Types: §7" + Utils.listToString(blockTypes, "§f, §7", "§cNo block types configured."));
                        sender.sendMessage("§3Use all: " + (regionBR.isUseAll() ? "§ayes" : "§cno"));
                        sender.sendMessage("§3Enabled: " + (regionBR.isEnabled() ? "§ayes" : "§cno"));
                        sender.sendMessage("§3All blocks: " + (regionBR.isAllBlocks() ? "§ayes" : "§cno"));
                        break;
                    // --------------------------------------------------------------- Add Block Type
                    case "a":
                    case "add":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        regionBR = plugin.getOptionHandler().getRegion(args[2]);

                        if (!checkFormat(sender, args[3]))
                            return true;

                        if (regionBR.getBlockTypes().contains(args[3].toUpperCase())) {
                            sender.sendMessage("§cThat block type is already configured. §7/" + label + " region info " + regionBR.getName());
                            return true;
                        }

                        regionBR.addType(args[3].toUpperCase());
                        sender.sendMessage("§aBlock format successfully added.");
                        break;
                    // --------------------------------------------------------------- Clear Block Types
                    case "c":
                    case "clear":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        regionBR = plugin.getOptionHandler().getRegion(args[2]);

                        regionBR.setBlockTypes(new ArrayList<>());
                        sender.sendMessage("§aBlock types successfully cleared.");
                        break;
                    // --------------------------------------------------------------- Enable/Disable Region
                    case "enabled":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        regionBR = plugin.getOptionHandler().getRegion(args[2]);

                        String b = args[3];

                        if (b.equalsIgnoreCase("t"))
                            b = "true";
                        else if (b.equalsIgnoreCase("f"))
                            b = "false";

                        boolean bol;

                        try {
                            bol = Boolean.parseBoolean(b.toLowerCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cThird argument has to be a boolean. §7true/false (t/f)");
                            return true;
                        }

                        regionBR.setEnabled(bol);
                        sender.sendMessage("§aEnabled property set to " + regionBR.isEnabled());
                        break;
                    // --------------------------------------------------------------- Override Block Type Settings
                    case "ua":
                    case "useall":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        regionBR = plugin.getOptionHandler().getRegion(args[2]);

                        b = args[3];

                        if (b.equalsIgnoreCase("t"))
                            b = "true";
                        else if (b.equalsIgnoreCase("f"))
                            b = "false";

                        try {
                            bol = Boolean.parseBoolean(b.toLowerCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cThird argument has to be a boolean. §7true/false (t/f)");
                            return true;
                        }

                        regionBR.setUseAll(bol);
                        sender.sendMessage("§aUse all property set to §7" + regionBR.isUseAll());
                        break;
                    case "allb":
                    case "ablocks":
                    case "ab":
                    case "allblocks":
                        if (!checkRegion(sender, args[2]))
                            return true;

                        regionBR = plugin.getOptionHandler().getRegion(args[2]);

                        b = args[3];

                        if (b.equalsIgnoreCase("t"))
                            b = "true";
                        else if (b.equalsIgnoreCase("f"))
                            b = "false";

                        try {
                            bol = Boolean.parseBoolean(b.toLowerCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cThird argument has to be a boolean. §7true/false (t/f)");
                            return true;
                        }

                        regionBR.setAllBlocks(bol);
                        sender.sendMessage("§aAll blocks property set to §7" + regionBR.isAllBlocks());
                        break;
                    case "h":
                    case "help":
                    default:
                        regionHelp(sender, label);
                        break;
                }

                break;
            // --------------------------------------------------------------- Worlds
            case "worlds":
            case "world":
                if (isPlayer(sender))
                    return true;

                player = (Player) sender;
                WorldBR worldBR;

                List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
                List<String> enabledWorlds = plugin.getFiles().getSettings().getStringList("Worlds-Enabled");

                for (String name : enabledWorlds)
                    worlds.remove(name);

                if (args.length < 2) {
                    worldHelp(sender, label);
                    return true;
                }

                // Second argument switch
                switch (args[1].toLowerCase()) {
                    // --------------------------------------------------------------- List
                    case "l":
                    case "list":
                        player.sendMessage("§8§m       §r §3BlockRegen Worlds §8§m        §r");
                        player.sendMessage("§r ");

                        if (!enabledWorlds.isEmpty())
                            enabledWorlds.forEach(loopWorld -> player.sendMessage("§8- §7" + loopWorld));
                        else
                            player.sendMessage("§cThere are no worlds configured. §7Settings.yml --> Worlds-Enabled");

                        player.sendMessage("");

                        if (!worlds.isEmpty())
                            worlds.forEach(loopWorld -> player.sendMessage("§8 - §c" + loopWorld));

                        player.sendMessage("\n§8§m                                                 §r");
                        break;
                    // --------------------------------------------------------------- List Block Types
                    case "i":
                    case "info":
                        if (!checkWorld(sender, args[2]))
                            return true;

                        worldBR = plugin.getOptionHandler().getWorld(args[2]);

                        List<String> blockTypes = worldBR.getBlockTypes();

                        if (worldBR.isUseAll())
                            blockTypes = plugin.getFormatHandler().getTypes();

                        sender.sendMessage("§3Block Types: §7" + Utils.listToString(blockTypes, "§f, §7", "§cNo block types configured."));
                        sender.sendMessage("§3Use all: " + (worldBR.isUseAll() ? "§ayes" : "§cno"));
                        sender.sendMessage("§3Enabled: " + (worldBR.isEnabled() ? "§ayes" : "§cno"));
                        sender.sendMessage("§3All blocks: " + (worldBR.isEnabled() ? "§ayes" : "§cno"));
                        break;
                    // --------------------------------------------------------------- Add Block Type
                    case "a":
                    case "add":
                        if (!checkWorld(sender, args[2]))
                            return true;

                        worldBR = plugin.getOptionHandler().getWorld(args[2]);

                        if (!checkFormat(sender, args[3]))
                            return true;

                        if (worldBR.getBlockTypes().contains(args[3].toUpperCase())) {
                            sender.sendMessage("§cThat block type is already configured. §7/" + label + " worlds info " + worldBR.getName());
                            return true;
                        }

                        worldBR.addType(args[3].toUpperCase());
                        sender.sendMessage("§aBlock format successfully added.");
                        break;
                    // --------------------------------------------------------------- Clear Block Types
                    case "c":
                    case "clear":
                        if (!checkWorld(sender, args[2]))
                            return true;

                        worldBR = plugin.getOptionHandler().getWorld(args[2]);

                        worldBR.setBlockTypes(new ArrayList<>());
                        sender.sendMessage("§aBlock types successfully cleared.");
                        break;
                    // --------------------------------------------------------------- Enable/Disable Region
                    case "enabled":
                        if (!checkWorld(sender, args[2]))
                            return true;

                        worldBR = plugin.getOptionHandler().getWorld(args[2]);

                        String b = args[3];

                        if (b.equalsIgnoreCase("t"))
                            b = "true";
                        else if (b.equalsIgnoreCase("f"))
                            b = "false";

                        boolean bol;

                        try {
                            bol = Boolean.parseBoolean(b.toLowerCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cThird argument has to be a boolean. §7true/false (t/f)");
                            return true;
                        }

                        worldBR.setEnabled(bol);
                        sender.sendMessage("§aEnabled property set to " + worldBR.isEnabled());
                        break;
                    // --------------------------------------------------------------- Override Block Type Settings
                    case "ua":
                    case "useall":
                        if (!checkWorld(sender, args[2]))
                            return true;

                        worldBR = plugin.getOptionHandler().getWorld(args[2]);

                        b = args[3];

                        if (b.equalsIgnoreCase("t"))
                            b = "true";
                        else if (b.equalsIgnoreCase("f"))
                            b = "false";

                        try {
                            bol = Boolean.parseBoolean(b.toLowerCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cThird argument has to be a boolean. §7true/false (t/f)");
                            return true;
                        }

                        worldBR.setUseAll(bol);
                        sender.sendMessage("§aUse all property set to §7" + worldBR.isUseAll());
                        break;
                    case "allb":
                    case "ablocks":
                    case "ab":
                    case "allblocks":
                        if (!checkWorld(sender, args[2]))
                            return true;

                        worldBR = plugin.getOptionHandler().getWorld(args[2]);

                        b = args[3];

                        if (b.equalsIgnoreCase("t"))
                            b = "true";
                        else if (b.equalsIgnoreCase("f"))
                            b = "false";

                        try {
                            bol = Boolean.parseBoolean(b.toLowerCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("§cThird argument has to be a boolean. §7true/false (t/f)");
                            return true;
                        }

                        worldBR.setAllBlocks(bol);
                        sender.sendMessage("§aAll blocks property set to §7" + worldBR.isAllBlocks());
                        break;
                    default:
                        worldHelp(sender, label);
                        break;
                }
                break;
            // --------------------------------------------------------------- Events
            case "events":
                if (isPlayer(sender))
                    return true;

                player = (Player) sender;

                if (!player.hasPermission("blockregen.admin")) {
                    player.sendMessage(Messages.get("Insufficient-Permission"));
                    return true;
                }

                if (args.length < 3) {
                    if (Utils.events.isEmpty()) {
                        player.sendMessage("§8§m        §r §3BlockRegen §8§m        "
                                + "\n§cYou haven't yet made any events. Make some to up your servers game!"
                                + "\n§8§m                                                 §r");
                    } else {
                        player.sendMessage("§8§m        §r §3BlockRegen §8§m        " +
                                "\n§7You have the following events ready to be activated:");
                        player.sendMessage(" ");

                        for (String events : Utils.events.keySet()) {
                            String state = !Utils.events.get(events) ? ChatColor.RED + "(inactive)" : ChatColor.GREEN + "(active)";
                            player.sendMessage(ChatColor.AQUA + "§8- §e" + events + " " + state);
                        }

                        player.sendMessage("\n§3/" + label + " events activate <event name> §8- §7activate event" +
                                "\n§3/" + label + " events deactivate <event name> §8- §7de-activate event"
                                + "\n§8§m                                                 §r");
                    }
                } else {

                    String allArgs = args[2];

                    if (args.length > 3) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < args.length; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        allArgs = sb.toString().trim();
                    }

                    if (!Utils.events.containsKey(allArgs)) {
                        player.sendMessage(Messages.get("Event-Not-Found"));
                        return true;
                    }

                    switch (args[1].toLowerCase()) {
                        case "activate":
                            if (!Utils.events.get(allArgs)) {
                                // Activate
                                Utils.events.put(allArgs, true);
                                player.sendMessage(Messages.get("Activate-Event").replace("%event%", allArgs));

                                // Bossbar

                                BlockFormat blockFormat = plugin.getFormatHandler().getBlockBRByEvent(allArgs);

                                if (blockFormat == null) {
                                    player.sendMessage(Messages.get("Event-Not-Found"));
                                    return false;
                                }

                                if (blockFormat.getEvent().getBossbarTitle() == null && blockFormat.getEvent().getBossbarColor() == null)
                                    return false;

                                BossBar bossbar = Bukkit.createBossBar(Utils.color("&7Event " + blockFormat.getEvent().getName() + " &7is active."), BarColor.YELLOW, BarStyle.SOLID);

                                if (blockFormat.getEvent().getBossbarTitle() != null)
                                    bossbar.setTitle(Utils.color(blockFormat.getEvent().getBossbarTitle().replace("%event%", blockFormat.getEvent().getName())));

                                if (blockFormat.getEvent().getBossbarColor() != null)
                                    bossbar.setColor(BarColor.valueOf(blockFormat.getEvent().getBossbarColor()));

                                Utils.bars.put(allArgs, bossbar);

                                for (Player online : Bukkit.getOnlinePlayers())
                                    bossbar.addPlayer(online);
                            } else player.sendMessage(Messages.get("Event-Already-Active"));
                            break;
                        case "deactivate":
                            if (Utils.events.get(allArgs)) {
                                Utils.events.put(allArgs, false);

                                player.sendMessage(Messages.get("De-Activate-Event").replace("%event%", allArgs));

                                if (Utils.bars.containsKey(allArgs)) {
                                    BossBar bossbar = Utils.bars.get(allArgs);
                                    bossbar.removeAll();
                                    Utils.bars.remove(allArgs);
                                }
                            } else player.sendMessage(Messages.get("Event-Not-Active"));
                            break;
                        default:
                            break;
                    }
                }
                break;
            // --------------------------------------------------------------- Debug
            case "debug":
                Main.getInstance().cO.switchDebug(sender);
                sender.sendMessage("§eSwitched player debug mode. §c[§4!§c] §7A slightly less experimental feature");
                break;
            // --------------------------------------------------------------- Help
            case "help":
            default:
                help(sender, label);
                break;
        }

        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!Utils.bars.isEmpty()) {
            for (String bars : Utils.bars.keySet()) {
                BossBar bar = Utils.bars.get(bars);
                bar.addPlayer(player);
            }
        }
    }

    private void convert() {
        FileConfiguration regions = plugin.getFiles().getRegions();

        String[] locA;
        String[] locB;
        String world;

        ConfigurationSection regionsection = regions.getConfigurationSection("Regions");
        Set<String> regionset = regionsection.getKeys(false);

        for (String regionloop : regionset) {
            if (regions.get("Regions." + regionloop + ".World") != null) {
                locA = regions.getString("Regions." + regionloop + ".Max").split(";");
                locB = regions.getString("Regions." + regionloop + ".Min").split(";");

                world = regions.getString("Regions." + regionloop + ".World");

                regions.set("Regions." + regionloop + ".Max", world + ";" + locA[0] + ";" + locA[1] + ";" + locA[2]);
                regions.set("Regions." + regionloop + ".Min", world + ";" + locB[0] + ";" + locB[1] + ";" + locB[2]);

                regions.set("Regions." + regionloop + ".World", null);
            }
        }

        plugin.getFiles().regions.save();
    }
}
