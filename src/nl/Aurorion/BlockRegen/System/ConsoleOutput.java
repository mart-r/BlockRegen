package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ConsoleOutput {

    private Main main;

    private boolean debug;
    private String prefix;

    private CommandSender reloadSender;

    public void setReloadSender(CommandSender reloadSender) {
        this.reloadSender = reloadSender;
    }

    public boolean isDebug() {
        return debug;
    }

    public ConsoleOutput(Main main) {
        this.main = main;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void debug(String msg) {
        if (debug)
            main.getServer().getConsoleSender().sendMessage(color(prefix + "&7DEBUG: " + msg));
    }

    public void err(String msg) {
        main.getServer().getConsoleSender().sendMessage(color(prefix + "&4" + msg));

        if (reloadSender != null)
            reloadSender.sendMessage(color("&4" + msg));
    }

    public void info(String msg) {
        main.getServer().getConsoleSender().sendMessage(color(prefix + "&7" + msg));
    }

    public void warn(String msg) {
        main.getServer().getConsoleSender().sendMessage(color(prefix + "&c" + msg));

        if (reloadSender != null)
            reloadSender.sendMessage(color("&c" + msg));
    }

    private String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
