package nl.Aurorion.BlockRegen.Configurations;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Configuration {

    private String name;

    private File file;
    private FileConfiguration fileConfiguration;

    private Main plugin;

    public Configuration(Main plugin, String name) {
        this.plugin = plugin;
        this.name = name;

        file = new File(plugin.getDataFolder(), name + ".yml");

        if (!file.exists()) {
            try {
                plugin.saveResource(name + ".yml", false);
            } catch (Exception e) {
                try {
                    file.createNewFile();
                } catch (IOException e1) {
                    return;
                }
            }
        }

        fileConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
        }
    }

    public void clear() {
        file.delete();

        try {
            plugin.saveResource(name + ".yml", false);
        } catch (Exception e) {
            try {
                file.createNewFile();
            } catch (IOException e1) {
            }
        }
    }

    public void reload() {
        if (!file.exists()) {
            try {
                plugin.saveResource(name + ".yml", false);
            } catch (Exception e) {
                try {
                    file.createNewFile();
                } catch (IOException e1) {
                }
            }
        }

        fileConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getConfig() {
        return fileConfiguration;
    }
}
