package nl.Aurorion.BlockRegen;

import me.clip.placeholderapi.PlaceholderAPI;
import nl.Aurorion.BlockRegen.BlockFormat.BlockFormat;
import nl.Aurorion.BlockRegen.System.RegenProcess;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Bisected;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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

    public static void clearEvents() {
        for (String event : events.keySet())
            if (bars.containsKey(event))
                bars.get(event).removeAll();

        bars.clear();
        events.clear();
    }

    public static void clearProcess(Location loc) {
        RegenProcess regenProcess = getProcess(loc);

        if (regenProcess != null) {
            // Cancel particle task
            if (regenProcess.getBlockFormat().getParticleBR() != null)
                if (!regenProcess.getBlockFormat().getParticleBR().getTasks().isEmpty())
                    if (regenProcess.getBlockFormat().getParticleBR().getTasks().containsKey(loc))
                        regenProcess.getBlockFormat().getParticleBR().getTasks().get(loc).cancel();

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
            for (Object key : map.keySet())
                str.append(key.toString()).append(seperator).append(map.get(key).toString()).append(splitter);
        }

        return str == null ? null : str.toString();
    }

    public static void addProcess(RegenProcess regenProcess) {
        regenProcesses.add(regenProcess);
    }

    public static String locationToString(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ();
    }

    public static Location stringToLocation(String str) {
        try {
            String[] strar = str.split(";");

            Location newLoc = new Location(Bukkit.getWorld(strar[0]), Double.parseDouble(strar[1]), Double.parseDouble(strar[2]), Double.parseDouble(strar[3]));

            return newLoc.clone();
        } catch (NullPointerException e) {
            return null;
        }
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

    public static String parse(String str, Player p, BlockFormat blockFormat, String actualRegenTimes) {

        str = str.replace("%regenDelay%", String.valueOf(blockFormat.getRegenDelay()));
        str = str.replace("%regenTimesMax%", String.valueOf(blockFormat.getRegenTimes()));
        str = str.replace("%regenTimesActual%", String.valueOf(actualRegenTimes));

        return parse(str, p);
    }

    public static String parse(String str, BlockFormat blockFormat, String actualRegenTimes) {

        str = str.replace("%regenDelay%", String.valueOf(blockFormat.getRegenDelay()));
        str = str.replace("%regenTimesMax%", String.valueOf(blockFormat.getRegenTimes()));
        str = str.replace("%regenTimesActual%", String.valueOf(actualRegenTimes));

        return str;
    }

    public static String parse(String str, Player p) {
        str = str.replace("%player%", p.getName());

        if (Main.getInstance().usePlaceholderAPI())
            str = PlaceholderAPI.setPlaceholders(p, str);

        return str;
    }

    public static ItemStack copyMeta(ItemStack item, ItemMeta meta) {

        ItemMeta m = item.getItemMeta();

        m.setDisplayName(meta.getDisplayName());
        m.setLore(meta.getLore());
        m.setUnbreakable(meta.isUnbreakable());

        for (ItemFlag flag : meta.getItemFlags())
            m.addItemFlags(flag);

        for (Enchantment e : meta.getEnchants().keySet())
            m.addEnchant(e, meta.getEnchants().get(e), true);

        item.setItemMeta(m);

        return item;
    }

    public static String parseAndColor(String str, Player p) {
        return color(parse(str, p));
    }

    public static void setFlower(Block block, Material type) {

        // From top or bottom?

        Bisected b;

        try {
            b = (Bisected) block.getBlockData();
        } catch (ClassCastException e) {
            Main.getInstance().cO.err("Couldn't replace a flower.");
            return;
        }

        if (b.getHalf().equals(Bisected.Half.TOP)) {
            Block down = block.getRelative(BlockFace.DOWN);

            block.setType(type, false);
            down.setType(type, false);

            // Pickup halves
            Bisected dataLower = (Bisected) down.getBlockData();
            dataLower.setHalf(Bisected.Half.BOTTOM);

            Bisected dataHigher = (Bisected) block.getBlockData();
            dataHigher.setHalf(Bisected.Half.TOP);

            // Set block data
            block.setBlockData(dataHigher, false);
            down.setBlockData(dataLower, false);
        } else {
            // Relative, set types
            Block up = block.getRelative(BlockFace.UP);

            block.setType(type, false);
            up.setType(type, false);

            // Pickup halves
            Bisected dataLower = (Bisected) block.getBlockData();
            dataLower.setHalf(Bisected.Half.BOTTOM);

            Bisected dataHigher = (Bisected) up.getBlockData();
            dataHigher.setHalf(Bisected.Half.TOP);

            // Set block data
            block.setBlockData(dataLower, false);
            up.setBlockData(dataHigher, false);
        }
    }

    public static String parseAndColor(String str, Player p, BlockFormat blockFormat, String actualRegenTimes) {
        return color(parse(str, p, blockFormat, actualRegenTimes));
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

    /**
     * Creates a player skull based on a player's name.
     *
     * @param name The Player's name
     * @return The head of the Player
     * @deprecated names don't make for good identifiers
     */
    @Deprecated
    public static ItemStack itemFromName(String name) {
        ItemStack item = getPlayerSkullItem();

        return itemWithName(item, name);
    }

    /**
     * Creates a player skull based on a player's name.
     *
     * @param item The item to apply the name to
     * @param name The Player's name
     * @return The head of the Player
     * @deprecated names don't make for good identifiers
     */
    @Deprecated
    public static ItemStack itemWithName(ItemStack item, String name) {
        notNull(item, "item");
        notNull(name, "name");

        return Bukkit.getUnsafe().modifyItemStack(item,
                "{SkullOwner:\"" + name + "\"}"
        );
    }

    /**
     * Creates a player skull with a UUID. 1.13 only.
     *
     * @param id The Player's UUID
     * @return The head of the Player
     */
    public static ItemStack itemFromUuid(UUID id) {
        ItemStack item = getPlayerSkullItem();

        return itemWithUuid(item, id);
    }

    /**
     * Creates a player skull based on a UUID. 1.13 only.
     *
     * @param item The item to apply the name to
     * @param id   The Player's UUID
     * @return The head of the Player
     */
    public static ItemStack itemWithUuid(ItemStack item, UUID id) {
        notNull(item, "item");
        notNull(id, "id");

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates a player skull based on a Mojang server URL.
     *
     * @param url The URL of the Mojang skin
     * @return The head associated with the URL
     */
    public static ItemStack itemFromUrl(String url) {
        ItemStack item = getPlayerSkullItem();

        return itemWithUrl(item, url);
    }


    /**
     * Creates a player skull based on a Mojang server URL.
     *
     * @param item The item to apply the skin to
     * @param url  The URL of the Mojang skin
     * @return The head associated with the URL
     */
    public static ItemStack itemWithUrl(ItemStack item, String url) {
        notNull(item, "item");
        notNull(url, "url");

        return itemWithBase64(item, urlToBase64(url));
    }

    /**
     * Creates a player skull based on a base64 string containing the link to the skin.
     *
     * @param base64 The base64 string containing the texture
     * @return The head with a custom texture
     */
    public static ItemStack itemFromBase64(String base64) {
        ItemStack item = getPlayerSkullItem();
        return itemWithBase64(item, base64);
    }

    /**
     * Applies the base64 string to the ItemStack.
     *
     * @param item   The ItemStack to put the base64 onto
     * @param base64 The base64 string containing the texture
     * @return The head with a custom texture
     */
    public static ItemStack itemWithBase64(ItemStack item, String base64) {
        notNull(item, "item");
        notNull(base64, "base64");

        UUID hashAsId = new UUID(base64.hashCode(), base64.hashCode());
        return Bukkit.getUnsafe().modifyItemStack(item,
                "{SkullOwner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + base64 + "\"}]}}}"
        );
    }

    /**
     * Sets the block to a skull with the given name.
     *
     * @param block The block to set
     * @param name  The player to set it to
     * @deprecated names don't make for good identifiers
     */
    @Deprecated
    public static void blockWithName(Block block, String name) {
        notNull(block, "block");
        notNull(name, "name");

        setBlockType(block);
        ((Skull) block.getState()).setOwningPlayer(Bukkit.getOfflinePlayer(name));
    }

    /**
     * Sets the block to a skull with the given UUID.
     *
     * @param block The block to set
     * @param id    The player to set it to
     */
    public static void blockWithUuid(Block block, UUID id) {
        notNull(block, "block");
        notNull(id, "id");

        setBlockType(block);
        ((Skull) block.getState()).setOwningPlayer(Bukkit.getOfflinePlayer(id));
    }

    /**
     * Sets the block to a skull with the given UUID.
     *
     * @param block The block to set
     * @param url   The mojang URL to set it to use
     */
    public static void blockWithUrl(Block block, String url) {
        notNull(block, "block");
        notNull(url, "url");

        blockWithBase64(block, urlToBase64(url));
    }

    /**
     * Sets the block to a skull with the given UUID.
     *
     * @param block  The block to set
     * @param base64 The base64 to set it to use
     */
    public static void blockWithBase64(Block block, String base64) {
        notNull(block, "block");
        notNull(base64, "base64");

        UUID hashAsId = new UUID(base64.hashCode(), base64.hashCode());

        String args = String.format(
                "%d %d %d %s",
                block.getX(),
                block.getY(),
                block.getZ(),
                "{Owner:{Id:\"" + hashAsId + "\",Properties:{textures:[{Value:\"" + base64 + "\"}]}}}"
        );

        if (newerApi()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "data merge block " + args);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "blockdata " + args);
        }
    }

    private static boolean newerApi() {
        try {

            Material.valueOf("PLAYER_HEAD");
            return true;

        } catch (IllegalArgumentException e) { // If PLAYER_HEAD doesn't exist
            return false;
        }
    }

    private static ItemStack getPlayerSkullItem() {
        if (newerApi()) {
            return new ItemStack(Material.valueOf("PLAYER_HEAD"));
        } else {
            return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (byte) 3);
        }
    }

    private static void setBlockType(Block block) {
        try {
            block.setType(Material.valueOf("PLAYER_HEAD"), false);
        } catch (IllegalArgumentException e) {
            block.setType(Material.valueOf("SKULL"), false);
        }
    }

    private static void notNull(Object o, String name) {
        if (o == null) {
            throw new NullPointerException(name + " should not be null!");
        }
    }

    private static String urlToBase64(String url) {

        URI actualUrl;
        try {
            actualUrl = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl.toString() + "\"}}}";
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }
}
