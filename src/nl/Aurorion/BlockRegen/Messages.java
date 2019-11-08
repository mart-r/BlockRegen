package nl.Aurorion.BlockRegen;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;

public class Messages {

    //                 MESSAGE_ID, MESSAGE_CONTENT
    private static HashMap<String, String> messages;

    // Return cached message, prefix included
    public static String get(String name) {
        return messages.get("Prefix") + messages.get(name);
    }

    // Load messages to cache
    public static void load() {
        messages = new HashMap<>();

        // Add new to yaml before load
        updateMessages();

        for (String name : Main.getInstance().getFiles().getMessages().getConfigurationSection("Messages").getKeys(false))
            messages.put(name, color(Main.getInstance().getFiles().getMessages().getString("Messages." + name)));
    }

    private static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    private static void updateMessages() {
        addNew("Imported-Region", "&aImported region from WorldGuard.");
        addNew("WorldGuard-Required", "&cWorldGuard is required for this.");
        addNew("Invalid-Region-Id", "&cThere's no region by that ID.");
        addNew("Invalid-Region-Id-Tip", "&7(You have to be in the same world as the region)");

        Main.getInstance().getFiles().saveMessages();
    }

    private static void addNew(String key, String value) {
        ConfigurationSection messageSection = Main.getInstance().getFiles().getMessages().getConfigurationSection("Messages");

        if (!messageSection.contains(key))
            messageSection.set(key, value);
    }
}
