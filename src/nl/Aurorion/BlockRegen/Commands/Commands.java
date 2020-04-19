package nl.Aurorion.BlockRegen.Commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

import java.util.Set;

public class Commands implements CommandExecutor, Listener {

    private Main main;

    public Commands(Main main) {
        this.main = main;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage("§8§m        §r §3BlockRegen §7v.§f" + main.getDescription().getVersion() + " §8§m        "
                + "\n§3/" + label + " reload §8- §7Reload the Settings.yml, Messages.yml and Blocklist.yml."
                + "\n§3/" + label + " bypass §8- §7Bypass the events."
                + "\n§3/" + label + " check §8- §7Check the type of the block to put in the blocklist."
                + "\n§3/" + label + " region §8- §7All the info to set a region."
                + "\n§3/" + label + " events §8- §7Check all your events."
                + "\n§8§m                                                 §r");
    }

    private void regionHelp(CommandSender s, String label) {
        s.sendMessage("§8§m        §r §3BlockRegen §8§m        §r"
                + "\n§3/" + label + " region set <name> §8- §7set a region."
                + "\n§3/" + label + " region remove <name> §8- §7remove a region."
                + "\n§3/" + label + " region list §8- §7a list of all your regions.");
        if (main.worldGuard != null)
            s.sendMessage("§3/" + label + " region fromWG <name> <WorldGuard ID (name)> §8- §7create a region from WG region boundaries.");
        s.sendMessage("§8§m                                                 §r");
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

        switch (args[0].toLowerCase()) {
            case "reload":
                main.reload(sender);
                sender.sendMessage(Messages.get("Reload"));
                break;
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
            case "convert":
                convert();
                sender.sendMessage(Messages.get("Prefix") + "§aConverted your regions to BlockRegen 3.4.0+ compatibility!");
                break;
            case "region":
                if (isPlayer(sender))
                    return true;

                if (main.worldEdit == null) {
                    sender.sendMessage(Messages.get("Prefix") + "§cWorldEdit is needed for region support.");
                    return true;
                }

                player = (Player) sender;

                ConfigurationSection regions = main.getFiles().getRegions().getConfigurationSection("Regions");

                Set<String> regionSet = regions.getKeys(false);

                // blockregen region set/remove/list

                if (args.length < 2) {
                    regionHelp(sender, label);
                    return true;
                } else if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("list")) {

                        player.sendMessage("§8§m       §r §3BlockRegen Regions §8§m        §r");
                        player.sendMessage("§r ");
                        if (!regionSet.isEmpty())
                            regionSet.forEach(loopRegion -> player.sendMessage("§8 - §7" + loopRegion));
                        else
                            player.sendMessage("§cThere are no regions set.");
                        player.sendMessage("\n§8§m                                                 §r");
                        return true;
                    }
                } else if (args.length == 3) {
                    switch (args[1].toLowerCase()) {
                        case "set":
                            Region s;

                            try {
                                s = main.getWorldEdit().getSession(player).getSelection(main.getWorldEdit().getSession(player).getSelectionWorld());
                            } catch (IncompleteRegionException e) {
                                player.sendMessage(Messages.get("No-Region-Selected"));
                                return true;
                            }

                            if (regionSet.contains(args[2])) {
                                player.sendMessage(Messages.get("Duplicated-Region"));
                                return true;
                            } else {
                                main.getFiles().getRegions().set("Regions." + args[2] + ".Min", Utils.locationToString(new Location(player.getWorld(), s.getMinimumPoint().getX(), s.getMinimumPoint().getY(), s.getMinimumPoint().getZ())));
                                main.getFiles().getRegions().set("Regions." + args[2] + ".Max", Utils.locationToString(new Location(player.getWorld(), s.getMaximumPoint().getX(), s.getMaximumPoint().getY(), s.getMaximumPoint().getZ())));

                                main.getFiles().saveRegions();

                                player.sendMessage(Messages.get("Set-Region"));
                            }
                            break;
                        case "remove":
                            if (regionSet.contains(args[2])) {
                                main.getFiles().getRegions().set("Regions." + args[2], null);
                                main.getFiles().saveRegions();
                                player.sendMessage(Messages.get("Remove-Region"));
                            } else
                                player.sendMessage(Messages.get("Unknown-Region"));
                            break;
                        default:
                            regionHelp(sender, label);
                            break;
                    }
                } else if (args.length == 4) {
                    if (args[1].equalsIgnoreCase("fromWG")) {

                        if (main.worldGuard == null) {
                            player.sendMessage(Messages.get("WorldGuard-Required"));
                            return true;
                        }

                        // Duplication check
                        if (regionSet.contains(args[2])) {
                            player.sendMessage(Messages.get("Duplicated-Region"));
                            return true;
                        }

                        RegionManager regionManager = WorldGuardPlugin.inst().getRegionManager(player.getWorld());

                        // Check region
                        if (!regionManager.hasRegion(args[3])) {
                            player.sendMessage(Messages.get("Invalid-Region-Id"));
                            player.sendMessage(Messages.get("Invalid-Region-Id-Tip"));
                            return true;
                        }

                        ProtectedRegion region = regionManager.getRegion(args[3]);

                        // Save region
                        main.getFiles().getRegions().set("Regions." + args[2] + ".Min", Utils.locationToString(new Location(player.getWorld(), region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ())));
                        main.getFiles().getRegions().set("Regions." + args[2] + ".Max", Utils.locationToString(new Location(player.getWorld(), region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ())));

                        main.getFiles().saveRegions();

                        player.sendMessage(Messages.get("Imported-Region"));
                    } else regionHelp(sender, label);
                } else regionHelp(sender, label);
                break;
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
                            String state;
                            if (Utils.events.get(events) == false) {
                                state = ChatColor.RED + "(inactive)";
                            } else {
                                state = ChatColor.GREEN + "(active)";
                            }
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

                                BlockBR blockBR = main.getFormatHandler().getBlockBRByEvent(allArgs);

                                if (blockBR == null) {
                                    player.sendMessage(Messages.get("Event-Not-Found"));
                                    return false;
                                }

                                if (blockBR.getEvent().getBossbarTitle() == null && blockBR.getEvent().getBossbarColor() == null)
                                    return false;

                                BossBar bossbar = Bukkit.createBossBar(Utils.color("&7Event " + blockBR.getEvent().getName() + " &7is active."), BarColor.YELLOW, BarStyle.SOLID);

                                if (blockBR.getEvent().getBossbarTitle() != null)
                                    bossbar.setTitle(Utils.color(blockBR.getEvent().getBossbarTitle().replace("%event%", blockBR.getEvent().getName())));

                                if (blockBR.getEvent().getBossbarColor() != null)
                                    bossbar.setColor(BarColor.valueOf(blockBR.getEvent().getBossbarColor()));

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
        FileConfiguration regions = main.getFiles().getRegions();

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

        main.getFiles().saveRegions();
    }
}