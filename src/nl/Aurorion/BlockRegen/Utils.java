package nl.Aurorion.BlockRegen;

import me.clip.placeholderapi.PlaceholderAPI;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.System.RegenProcess;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
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

    public static HashMap<Location, Integer> regenTimesBlocks = new HashMap<>();

    // Firework colors
    public static List<Color> colors = new ArrayList<>();

    public static void clearProcess(Location loc) {
        if (getProcess(loc) != null)
            regenProcesses.remove(getProcess(loc));
    }

    public static RegenProcess getProcess(Location loc) {
        for (RegenProcess regenProcess : regenProcesses) {
            if (regenProcess.getLoc().equals(loc))
                return regenProcess;
        }

        return null;
    }

    public static String mapToString(HashMap<?, ?> map, String splitter, String seperator, String ifEmpty) {
        String str = ifEmpty;

        if (!map.isEmpty()) {
            str = "";
            for (Object key : map.keySet()) {
                str += key.toString() + seperator + map.get(key).toString() + splitter;
            }
        }

        return str;
    }

    public static void addProcess(Location loc, BukkitTask task, Material material) {
        regenProcesses.add(new RegenProcess(loc, task, material));
    }

    public static String locationToString(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ();
    }

    public static Location stringToLocation(String str) {
        String[] strar = str.split(";");
        Location newLoc = new Location(Bukkit.getWorld(strar[0]), Double.valueOf(strar[1]).doubleValue(), Double.valueOf(strar[2]).doubleValue(), Double.valueOf(strar[3]).doubleValue());
        return newLoc.clone();
    }

    public static String listToString(List<String> list, String splitter, String ifEmpty) {
        String stringList = ifEmpty;
        if (list != null)
            if (!list.isEmpty()) {
                stringList = list.get(0).replace("_", " ");
                for (int i = 1; i < list.size(); i++) {
                    stringList = list.get(i).replace("_", " ") + splitter + stringList;
                }
            }
        return stringList;
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
}
