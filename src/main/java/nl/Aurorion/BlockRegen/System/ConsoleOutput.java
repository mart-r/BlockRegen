package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.ChatColor;

public class ConsoleOutput {

    private Main main;

    private boolean debug;
    private String prefix;

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
            main.getServer().getConsoleSender().sendMessage(color(prefix + "&7DEBUG: " + Utils.color(msg)));
    }

    public void err(String msg) {
        main.getServer().getConsoleSender().sendMessage(color(prefix + "&4" + Utils.color(msg)));
    }

    public void info(String msg) {
        main.getServer().getConsoleSender().sendMessage(color(prefix + "&7" + Utils.color(msg)));
    }

    public void warn(String msg) {
        main.getServer().getConsoleSender().sendMessage(color(prefix + "&c" + Utils.color(msg)));
    }

    private String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
