package nl.Aurorion.BlockRegen.Configurations;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.configuration.file.FileConfiguration;

public class Files {

    // Switched recovery options from Recovery.yml to Data.yml
    public Configuration settings, messages, blocklist, regions, data;

    public Files(Main plugin) {

        // Plugin configuration
        settings = new Configuration(plugin, "Settings");

        // Language file
        messages = new Configuration(plugin, "Messages");

        // Blocklist configuration
        blocklist = new Configuration(plugin, "Blocklist");

        // Region storage
        regions = new Configuration(plugin, "Regions");

        // Data.yml ==> storing regen times, Recovery data, Persist
        data = new Configuration(plugin, "Data");

        // Recovery section created if needed
        if (settings.getYaml().getBoolean("Data-Recovery"))
            if (!data.getYaml().contains("Recovery"))
                data.getYaml().createSection("Recovery");
    }

    public FileConfiguration getSettings() {
        return settings.getYaml();
    }

    public FileConfiguration getMessages() {
        return messages.getYaml();
    }

    public FileConfiguration getBlocklist() {
        return blocklist.getYaml();
    }

    public FileConfiguration getRegions() {
        return regions.getYaml();
    }

    public FileConfiguration getData() {
        return data.getYaml();
    }

    public void reload() {
        blocklist.reload();
        settings.reload();
        messages.reload();
    }
}