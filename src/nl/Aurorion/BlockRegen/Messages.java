package nl.Aurorion.BlockRegen;

import org.bukkit.ChatColor;

import java.util.HashMap;

public class Messages {

    //                 MESSAGE_ID, MESSAGE_CONTENT
    private static HashMap<String, String> messages;

    public static String get(String name) {
        return messages.get("Prefix") + messages.get(name);
    }

    public static void load() {
        messages = new HashMap<>();
        for (String name : Main.getInstance().getFiles().getMessages().getConfigurationSection("Messages").getKeys(false))
            messages.put(name, color(Main.getInstance().getFiles().getMessages().getString("Messages." + name)));
    }

    private static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}
