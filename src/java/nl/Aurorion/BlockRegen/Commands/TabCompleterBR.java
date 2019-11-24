package nl.Aurorion.BlockRegen.Commands;

import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class TabCompleterBR implements TabCompleter {

    // Subcommand lists
    private final String[] subcommands = {"reload", "bypass", "check", "region", "events", "world", "debug"};

    // Second layer
    private final String[] regionCommands = {"set", "remove", "list", "fromWG", "listBlocks", "add", "useall", "enabled", "clear"};
    private final String[] worldCommands = {"listBlocks", "remove", "add", "clear", "useall", "enabled"};
    private final String[] eventCommands = {"activate", "deactivate"};

    // Fourth layer boolean options
    private final String[] boolOptions = {"true", "false"};

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        // Final tab complete list
        List<String> completeList = new ArrayList<>();

        // First argument, basic subcommands
        if (args.length == 1) {
            for (String subCommand : subcommands) {
                if (!args[0].equals("")) {
                    if (subCommand.toLowerCase().startsWith(args[0].toLowerCase()))
                        completeList.add(subCommand);
                    continue;
                }
                completeList.add(subCommand);
            }
            // Second layer, region, world and even subcommands
        } else if (args.length == 2) {
            if (args[0].equals("region")) {
                for (String regionCommand : regionCommands) {
                    if (!args[1].equals("")) {
                        if (regionCommand.toLowerCase().startsWith(args[1].toLowerCase()))
                            completeList.add(regionCommand);
                        continue;
                    }
                    completeList.add(regionCommand);
                }

                // Add WorldGuard command if hooked
                if (Main.getInstance().worldGuard != null) {
                    if (!args[1].equals("")) {
                        if ("fromwg".startsWith(args[1].toLowerCase()))
                            completeList.add("fromWG");
                    } else completeList.add("fromWG");
                }
            } else if (args[0].equals("events")) {
                for (String eventCommand : eventCommands) {
                    if (!args[1].equals("")) {
                        if (eventCommand.toLowerCase().startsWith(args[1].toLowerCase()))
                            completeList.add(eventCommand);
                        continue;
                    }
                    completeList.add(eventCommand);
                }
            } else if (args[0].equalsIgnoreCase("worlds")) {
                for (String eventCommand : worldCommands) {
                    if (!args[1].equals("")) {
                        if (eventCommand.toLowerCase().startsWith(args[1].toLowerCase()))
                            completeList.add(eventCommand);
                        continue;
                    }

                    completeList.add(eventCommand);
                }
            }
            // Third layer, region names, world names, block formats, event names
        } else if (args.length == 3) {
            if (args[0].equals("region")) {
                // Fill region names
                if (!args[1].equalsIgnoreCase("fromWG"))
                    for (String regionName : Main.getInstance().getOptionHandler().getRegions()) {
                        if (!args[2].equals("")) {
                            if (regionName.toLowerCase().startsWith(args[2].toLowerCase()))
                                completeList.add(regionName);
                            continue;
                        }

                        completeList.add(regionName);
                    }
            } else if (args[0].equals("events")) {
                for (String eventName : Utils.events.keySet()) {
                    if (!args[2].equals("")) {
                        if (eventName.toLowerCase().startsWith(args[2].toLowerCase()))
                            completeList.add(eventName);
                        continue;
                    }
                    completeList.add(eventName);
                }
            } else if (args[0].equalsIgnoreCase("world")) {
                for (String worldName : Main.getInstance().getOptionHandler().getWorlds()) {
                    if (!args[2].equals("")) {
                        if (worldName.toLowerCase().startsWith(args[2].toLowerCase()))
                            completeList.add(worldName);
                        continue;
                    }

                    completeList.add(worldName);
                }
            }
            // Fourth layer true/false & format names
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("add")) {
                for (String formatName : Main.getInstance().getFormatHandler().getTypes()) {
                    if (!args[3].equals("")) {
                        if (formatName.toLowerCase().startsWith(args[3].toLowerCase()))
                            completeList.add(formatName);
                    }

                    completeList.add(formatName);
                }
            } else if (args[1].equalsIgnoreCase("enabled") || args[1].equalsIgnoreCase("useall")) {
                for (String boolOption : boolOptions) {
                    if (!args[3].equals("")) {
                        if (boolOption.toLowerCase().startsWith(args[3].toLowerCase()))
                            completeList.add(boolOption);
                    }

                    completeList.add(boolOption);
                }
            }
        }

        if (completeList.isEmpty())
            return null;

        return completeList;
    }
}
