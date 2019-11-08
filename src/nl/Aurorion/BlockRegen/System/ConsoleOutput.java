package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ConsoleOutput {

    private Plugin plugin;

    private boolean debug;
    private String prefix;

    private CommandSender reloadSender;

    private List<CommandSender> debuggers = new ArrayList<>();

    public void switchDebug(CommandSender sender) {
        if (debuggers.contains(sender))
            debuggers.remove(sender);
        else
            debuggers.add(sender);
    }

    public void setReloadSender(CommandSender reloadSender) {
        this.reloadSender = reloadSender;
    }

    public boolean isDebug() {
        return debug;
    }

    public ConsoleOutput(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void debug(String msg) {
        if (debug) {
            plugin.getServer().getConsoleSender().sendMessage(color(prefix + "&7DEBUG: " + msg));

            if (reloadSender != null)
                reloadSender.sendMessage(color("&eDEBUG: " + msg));

            for (CommandSender s : debuggers)
                if (s != null)
                    s.sendMessage(color("&eDEBUG: " + msg));
        }
    }

    public void err(String msg) {
        plugin.getServer().getConsoleSender().sendMessage(color(prefix + "&4" + msg));

        if (reloadSender != null)
            reloadSender.sendMessage(color("&4" + msg));
    }

    public void info(String msg) {
        plugin.getServer().getConsoleSender().sendMessage(color(prefix + "&7" + msg));

        if (reloadSender != null)
            reloadSender.sendMessage(color("&7" + msg));
    }

    public void warn(String msg) {
        plugin.getServer().getConsoleSender().sendMessage(color(prefix + "&c" + msg));

        if (reloadSender != null)
            reloadSender.sendMessage(color("&c" + msg));
    }

    private String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public String getPrefix() {
        return prefix;
    }
}
