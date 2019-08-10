package nl.Aurorion.BlockRegen;

import me.clip.placeholderapi.PlaceholderAPI;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.System.RegenProcess;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    // from /br bypass command, prevents regeneration altogether
    public static List<String> bypass = new ArrayList<>();

    // Contains player names that are looking for block types, prevents regeneration altogether
    public static List<String> blockCheck = new ArrayList<>();

    // List of event names, enabled/disabled
    public static Map<String, Boolean> events = new HashMap<>();

    // Event bossbars
    public static Map<String, BossBar> bars = new HashMap<>();

    // Regen processes, added on blockBreak, removed on regeneration
    public static List<RegenProcess> regenProcesses = new ArrayList<>();

    // Regen times, how many times you can break the block, loaded on startup, saved onDisable
    public static HashMap<Location, Integer> regenTimesBlocks = new HashMap<>();

    // Firework colors
    public static List<Color> colors = new ArrayList<>();

    public static void clearProcess(Location loc) {
        RegenProcess regenProcess = getProcess(loc);
        if (regenProcess != null) {

            // Cancel particle task
            if (regenProcess.getBlockBR().getParticleBR() != null) {
                Main.getInstance().cO.debug("Tasks: " + regenProcess.getBlockBR().getParticleBR().getTasks().keySet().toString());
                Main.getInstance().cO.debug(loc.toString());

                if (!regenProcess.getBlockBR().getParticleBR().getTasks().isEmpty())
                    if (regenProcess.getBlockBR().getParticleBR().getTasks().containsKey(loc)) {
                        Main.getInstance().cO.debug("Contains task..");
                        regenProcess.getBlockBR().getParticleBR().getTasks().get(loc).cancel();
                    }
            }

            // Clear process
            regenProcesses.remove(regenProcess);
        }
    }

    public static RegenProcess getProcess(Location loc) {
        for (RegenProcess regenProcess : regenProcesses) {
            if (regenProcess.getLoc().equals(loc))
                return regenProcess;
        }

        return null;
    }

    public static String mapToString(HashMap<?, ?> map, String splitter, String seperator, String ifEmpty) {
        StringBuilder str = ifEmpty == null ? null : new StringBuilder(ifEmpty);

        if (!map.isEmpty()) {
            str = new StringBuilder();
            for (Object key : map.keySet()) {
                str.append(key.toString()).append(seperator).append(map.get(key).toString()).append(splitter);
            }
        }

        return str == null ? null : str.toString();
    }

    public static void addProcess(Location loc, BukkitTask task, Material material, BlockBR blockBR, long regenTime) {
        regenProcesses.add(new RegenProcess(loc, task, material, blockBR, regenTime));
    }

    public static String locationToString(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ();
    }

    public static Location stringToLocation(String str) {
        String[] strar = str.split(";");

        Location newLoc = new Location(Bukkit.getWorld(strar[0]), Double.parseDouble(strar[1]), Double.parseDouble(strar[2]), Double.parseDouble(strar[3]));

        return newLoc.clone();
    }

    public static String listToString(List<String> list, String splitter, String ifEmpty) {
        StringBuilder stringList = ifEmpty == null ? null : new StringBuilder(ifEmpty);
        if (list != null)
            if (!list.isEmpty()) {
                stringList = new StringBuilder(list.get(0).replace("_", " "));
                for (int i = 1; i < list.size(); i++) {
                    stringList.insert(0, list.get(i).replace("_", " ") + splitter);
                }
            }
        return stringList == null ? null : stringList.toString();
    }

    public static List<String> stringToList(String string) {
        List<String> list = new ArrayList<>();
        if (string != null)
            for (String str : string.split(","))
                list.add(str.trim());
        return list;
    }

    public static void fillFireworkColors() {
        colors.add(Color.AQUA);
        colors.add(Color.BLUE);
        colors.add(Color.FUCHSIA);
        colors.add(Color.GREEN);
        colors.add(Color.LIME);
        colors.add(Color.ORANGE);
        colors.add(Color.WHITE);
        colors.add(Color.YELLOW);
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String removeColors(String str) {
        return ChatColor.stripColor(color(str));
    }

    public static String parse(String str, Player p, BlockBR blockBR, String actualRegenTimes) {

        str = str.replace("%regenDelay%", String.valueOf(blockBR.getRegenDelay()));
        str = str.replace("%regenTimesMax%", String.valueOf(blockBR.getRegenTimes()));
        str = str.replace("%regenTimesActual%", String.valueOf(actualRegenTimes));

        return parse(str, p);
    }

    public static String parse(String str, Player p) {
        str = str.replace("%player%", p.getName());

        if (Main.getInstance().isPlaceholderAPI())
            str = PlaceholderAPI.setPlaceholders(p, str);

        return str;
    }

    public static String parseAndColor(String str, Player p) {
        return color(parse(str, p));
    }

    public static String parseAndColor(String str, Player p, BlockBR blockBR, String actualRegenTimes) {
        return color(parse(str, p, blockBR, actualRegenTimes));
    }

    // Both methods were taken directly from Spigot (Bukkit) source code & modified

    /**
     * Returns the quantity of items to drop on block destruction.
     */

    private static int quantityDropped(Material mat) {
        return mat == Material.LAPIS_ORE ? 4 + Main.getInstance().getRandom().nextInt(5) : 1;
    }

    /**
     * Get the quantity dropped based on the given fortune level
     */

    public static int checkFortune(Material mat, ItemStack tool) {
        if (tool.hasItemMeta())
            if (tool.getItemMeta().hasEnchants())
                if (tool.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS)) {
                    int fortune = tool.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS);

                    if (fortune > 0) {
                        int i = Main.getInstance().getRandom().nextInt(fortune + 2) - 1;

                        if (i < 0)
                            i = 0;

                        return quantityDropped(mat) * (i + 1);
                    } else return quantityDropped(mat);
                }
        return 0;
    }
}
