package nl.Aurorion.BlockRegen.Commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        s.sendMessage("§8§m----§r §3BlockRegen §7v.§f" + main.getDescription().getVersion() + " §8§m----"
                + "\n§3/" + label + " reload §7- §8Reload the Settings.yml, Messages.yml and Blocklist.yml, also generates Recovery.yml if needed."
                + "\n§3/" + label + " bypass §7- §8Bypass the events."
                + "\n§3/" + label + " check &7- §8Check the name + data of the block to put in the blocklist."
                + "\n§3/" + label + " region &7- §8All the info to set a region."
                + "\n§3/" + label + " events &7- §8Check all your events."
                + "\n§8§m--------------------------");
    }

    private boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("Console-Sender-Error"));
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            help(sender, label);
            return false;
        }

        Player player;

        switch (args[0].toLowerCase()) {
            case "reload":
                sender.sendMessage("§7Reloading..");
                main.reload(sender);
                sender.sendMessage(Messages.get("Reload"));
                break;
            case "bypass":
                if (!isPlayer(sender))
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
                if (!isPlayer(sender))
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
                sender.sendMessage(Messages.get("Prefix") + "§a§lConverted your regions to BlockRegen 3.4.0 compatibility!");
                break;
            case "region":
                if (!isPlayer(sender))
                    return true;

                player = (Player) sender;

                if (!player.hasPermission("blockregen.admin")) {
                    player.sendMessage(Messages.get("Console-Sender-Error"));
                    return true;
                }


                ConfigurationSection regions = main.getFiles().getRegions().getConfigurationSection("Regions");

                Set<String> regionSet = regions.getKeys(false);

                // blockregen region set/remove/list

                if (args.length < 2) {
                    player.sendMessage("§cNot enough arguments. §7/blockregen region");
                    return true;
                } else if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("list")) {

                        player.sendMessage("§8§m----&r &3BlockRegen Regions §8§m----\n");
                        if (!regionSet.isEmpty())
                            regionSet.forEach(loopRegion -> player.sendMessage("§8 - §7" + loopRegion));
                        else
                            player.sendMessage("§cThere are no regions set.");
                        player.sendMessage("\n§8§m-----------------------");
                        return true;
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&m-----&r &3&lBlockRegen &6&m-----"
                                + "\n&3/" + label + "  region set <name> &7: set a region."
                                + "\n&3/" + label + "  region remove <name> &7: remove a region."
                                + "\n&3/" + label + "  region list &7: a list of all your regions."
                                + "\n&6&m-----------------------"));
                        return true;
                    }
                } else if (args.length == 3) {
                    switch (args[1].toLowerCase()) {
                        case "set":
                            Region s;

                            try {
                                s = main.getWorldEdit().getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
                            } catch (IncompleteRegionException e) {
                                player.sendMessage(Messages.get("No-Region-Selected"));
                                return true;
                            }

                            if (regionSet.contains(args[2])) {
                                player.sendMessage(Messages.get("Duplicated-Region"));
                                return true;
                            } else {
                                main.getFiles().getRegions().set("Regions." + args[2] + ".Min", Utils.locationToString(BukkitAdapter.adapt(player.getWorld(), s.getMinimumPoint())));
                                main.getFiles().getRegions().set("Regions." + args[2] + ".Max", Utils.locationToString(BukkitAdapter.adapt(player.getWorld(), s.getMaximumPoint())));

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
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&m-----&r &3&lBlockRegen &6&m-----"
                                    + "\n&3/" + label + "  region set <name> &7: set a region."
                                    + "\n&3/" + label + "  region remove <name> &7: remove a region."
                                    + "\n&3/" + label + "  region list &7: a list of all your regions."
                                    + "\n&6&m-----------------------"));
                            break;
                    }
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&m-----&r &3&lBlockRegen &6&m-----"
                            + "\n&3/" + label + "  region set <name> &7: set a region."
                            + "\n&3/" + label + "  region remove <name> &7: remove a region."
                            + "\n&3/" + label + "  region list &7: a list of all your regions."
                            + "\n&6&m-----------------------"));
                }
                break;
            case "events":
                if (!isPlayer(sender))
                    return true;

                player = (Player) sender;
                if (!player.hasPermission("blockregen.admin")) {
                    player.sendMessage(Messages.get("Insufficient-Permission"));
                    return true;
                }
                if (args.length < 3) {
                    if (Utils.events.isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&m-----&r &3&lBlockRegen &6&m-----"
                                + "\n&eYou haven't yet made any events. Make some to up your servers game!"
                                + "\n&6&m-----------------------"));
                    } else {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&m-----&r &3&lBlockRegen &6&m-----"));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eYou have the following events ready to be activated."));
                        player.sendMessage(" ");
                        for (String events : Utils.events.keySet()) {
                            String state;
                            if (Utils.events.get(events) == false) {
                                state = ChatColor.RED + "(inactive)";
                            } else {
                                state = ChatColor.GREEN + "(active)";
                            }
                            player.sendMessage(ChatColor.AQUA + "- " + events + " " + state);
                        }
                        player.sendMessage(" ");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eUse &3/" + label + "  events activate <event name> &eto activate it."));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eUse &3/" + label + "  events deactivate <event name> &eto de-activate it."));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&m-----------------------"));
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
