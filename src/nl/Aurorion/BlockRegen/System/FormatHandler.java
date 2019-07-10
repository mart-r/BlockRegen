package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.BlockFormat.*;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class FormatHandler {

    private Main main;

    // MATERIAL, BLOCK DATA
    private HashMap<String, BlockBR> blocks;

    private FileConfiguration blocklist;

    public FormatHandler(Main main) {
        this.main = main;

        blocklist = main.getFiles().getBlocklist();

        loadBlocks();
    }

    public Set<String> getBlockNames() {
        return blocks.keySet();
    }

    public void reload() {
        blocklist = main.getFiles().getBlocklist();

        loadBlocks();
    }

    public BlockBR getBlockBRByEvent(String eventName) {
        for (BlockBR blockBR : blocks.values()) {
            if (Utils.removeColors(blockBR.getEvent().getName()).equals(eventName))
                return blockBR;
        }
        return null;
    }

    public BlockBR getBlockBR(String blockName) {
        return blocks.get(blockName.toUpperCase());
    }

    private void loadBlocks() {
        blocks = new HashMap<>();

        main.cO.info("Starting to load block formats..");
        for (String name : blocklist.getConfigurationSection("Blocks").getKeys(false)) {
            BlockBR block = loadBlock(name);
            if (block != null) {
                blocks.put(name, block);
                main.cO.debug("Loaded " + name);
            }
        }

        main.cO.info("Loaded " + blocks.size() + " block format(s) and " + Utils.events.size() + " event(s).");

        for (BlockBR blockBR : blocks.values())
            if (blockBR.getRegenTimes() > 0) {
                main.useRegenTimes(true);
                break;
            }
    }

    private BlockBR loadBlock(String name) {
        ConfigurationSection section = blocklist.getConfigurationSection("Blocks." + name);

        BlockBR block = new BlockBR(name, section.getString("replace-block"));

        // Block data

        if (name.contains(";"))
            try {
                byte data = Byte.valueOf(name.split(";")[1]);
                block.setBlockData(data);
                main.cO.debug(name + ": Data: " + data);
            } catch (Exception e) {
                e.printStackTrace();
            }

        if (section.getString("replace-block").contains(";"))
            try {
                byte data = Byte.valueOf(section.getString("replace-block").split(";")[1]);
                block.setReplaceBlockData(data);
                main.cO.debug(name + ": Replace Block Data: " + data);
            } catch (Exception e) {
                e.printStackTrace();
            }


        if (block.isValid()) {
            main.cO.debug(name + ": Valid");

            // Misc info

            // Regenerate
            block.setRegenerate(section.getBoolean("regenerate", true));
            main.cO.debug(name + ": isRegenerate " + block.isRegenerate());

            // Not needed, 0 by default
            block.setRegenDelay(section.getInt("regen-delay", 3));
            main.cO.debug(name + ": regenDelay " + block.getRegenDelay());

            // false by default
            block.setNaturalBreak(section.getBoolean("natural-break", false));
            main.cO.debug(name + ": isNaturalBreak " + block.isNaturalBreak());

            // If null, simply won't work, no need to check
            block.setParticle(section.getString("particles"));
            main.cO.debug(name + ": particle " + block.getParticle());

            block.setRegenTimes(section.getInt("regen-times", 0));
            main.cO.debug(name + ": regenTimes " + block.getRegenTimes());

            // Rewards

            block.setConsoleCommands(getStringOrList(name, "console-commands", "console-command"));
            main.cO.debug(name + ": consoleCommands " + block.getConsoleCommands().toString());

            block.setPlayerCommands(getStringOrList(name, "player-commands", "player-command"));
            main.cO.debug(name + ": playerCommands " + block.getPlayerCommands().toString());

            block.setMoney(loadAmount(section.getCurrentPath() + ".money"));
            main.cO.debug(name + ": money " + block.getMoney().toString());

            // Decide if single or multiple format
            // Support both drop-item(s)
            String path;
            if (section.contains("drop-item"))
                path = "drop-item";
            else if (section.contains("drop-items"))
                path = "drop-items";
            else
                path = "nil";

            List<Drop> drops = new ArrayList<>();

            if (!path.equals("nil")) {
                // Legacy format?
                ConfigurationSection dropSection = section.getConfigurationSection(path);

                // Legacy
                if (dropSection.contains("material")) {
                    main.cO.debug("Loading a legacy drop..");

                    Drop drop = loadDrop("Blocks." + name + "." + path);
                    drop.setId("legacy");

                    if (drop != null) {
                        drops.add(drop);
                        block.setDrops(drops);
                    } else
                        main.cO.debug("Legacy drop not valid");

                } else {
                    // New format, look for multiples
                    main.cO.debug("Looking for multiple drops format..");
                    for (String id : dropSection.getKeys(false)) {

                        Drop drop = loadDrop("Blocks." + name + "." + path + "." + id);

                        if (drop != null) {
                            drop.setId(id);
                            drops.add(drop);
                        } else
                            main.cO.debug("Drop not valid, skipping");
                    }

                    block.setDrops(drops);
                }

                main.cO.debug("Checking drops..");
                main.cO.debug(block.getDrops().toString());
            } else
                block.setDrops(drops);

            // Conditions

            block.setToolsRequired(Utils.stringToList(section.getString("tool-required")));
            main.cO.debug(name + ": toolsRequired " + block.getToolsRequired().toString());

            block.setEnchantsRequired(Utils.stringToList(section.getString("enchant-required")));
            main.cO.debug(name + ": enchantsRequired " + block.getEnchantsRequired().toString());

            if (section.contains("jobs-check")) {
                block.setJobRequirement(new JobRequirement(section.getString("jobs-check").split(";")[0], Integer.valueOf(section.getString("jobs-check").split(";")[1])));
                main.cO.debug(name + ": jobsRequired " + block.getJobRequirement().getJob() + " - " + block.getJobRequirement().getLevel());
            }

            // Events

            if (section.contains("event"))
                block.setEvent(loadEvent(section.getCurrentPath() + ".event"));

            return block;
        }

        return null;
    }

    private Drop loadDrop(String path) {
        ConfigurationSection dropSection = blocklist.getConfigurationSection(path);

        main.cO.debug("Drop path: " + path);

        Drop drop = new Drop(dropSection.getString("material"));

        // Handled with null

        if (!drop.isValid())
            return null;

        main.cO.debug(drop.getMaterial().toString());

        if (dropSection.getString("material").contains(";"))
            try {
                byte data = Byte.valueOf(dropSection.getString("material").split(";")[1]);
                drop.setData(data);
                main.cO.debug("Drop Data: " + data);
            } catch (Exception e) {
                e.printStackTrace();
            }

        drop.setDisplayName(dropSection.getString("name"));
        main.cO.debug("displayName " + drop.getDisplayName());

        drop.setLore(dropSection.getStringList("lores"));
        main.cO.debug("Lore " + drop.getLore().toString());

        if (!dropSection.contains("amount"))
            drop.setAmount(new Amount(1));
        else
            drop.setAmount(loadAmount(dropSection.getCurrentPath() + ".amount"));

        drop.setDropNaturally(dropSection.getBoolean("drop-naturally", true));
        main.cO.debug("isDropNaturally " + drop.isDropNaturally());

        if (dropSection.contains("exp")) {
            drop.setDropExpNaturally(dropSection.getBoolean("exp.drop-naturally", false));

            if (!dropSection.contains("exp.amount"))
                drop.setExpAmount(new Amount(1));
            else
                drop.setExpAmount(loadAmount(dropSection.getCurrentPath() + ".exp.amount"));
        } else drop.setExpAmount(new Amount(0));
        main.cO.debug("Exp " + drop.getExpAmount().toString());

        return drop;
    }

    private Amount loadAmount(String path) {

        ConfigurationSection section = blocklist.getConfigurationSection(path);

        Amount amount = new Amount(1);

        // Fixed or not?
        try {
            if (section.contains("high") && section.contains("low")) {
                // Random amount

                try {
                    amount = new Amount(section.getInt("low"), section.getInt("high"));
                } catch (NullPointerException e) {
                    main.cO.err("Amount on path " + path + " is not valid, returning default.");
                    return new Amount(1);
                }
            }
        } catch (NullPointerException e) {
            // Fixed
            try {
                amount = new Amount(blocklist.getInt(path));
            } catch (NullPointerException e1) {
                main.cO.err("Amount on path " + path + " is not valid, returning default.");
                return new Amount(1);
            }
        }

        main.cO.debug("Amount " + amount.toString());
        return amount;
    }

    private EventBR loadEvent(String path) {

        ConfigurationSection section = blocklist.getConfigurationSection(path);

        if (!section.contains("event-name")) {
            main.cO.warn("Event needs to have a name, skipping at path " + path);
            return null;
        }

        EventBR eventBR = new EventBR(section.getString("event-name"));

        if (main.isOver18()) {
            if (section.contains("bossbar")) {
                try {
                    eventBR.setBossbarColor(section.getString("bossbar.color", "GREEN").toUpperCase());
                    eventBR.setBossbarTitle(section.getString("bossbar.name"));
                } catch (NullPointerException e) {
                    main.cO.err("Bossbar settings not valid on " + path);
                }
            }
        } else main.cO.warn("Boss bars are not supported on this version. Only 1.9+");

        eventBR.setDoubleDrops(section.getBoolean("double-drops", false));

        eventBR.setDoubleXp(section.getBoolean("double-exp", false));

        // Drop has to be loaded before enabled
        if (section.contains("custom-item")) {

            Drop drop = loadDrop(section.getCurrentPath() + ".custom-item");

            if (drop != null) {
                eventBR.setDrop(drop);
                eventBR.setDropEnabled(section.getBoolean("custom-item.enabled"));
                eventBR.setDropRarity(section.getInt("custom-item.rarity"));
            } else
                main.cO.warn("Drop on path " + path + " is not valid.");
        }

        // Adding event to the system
        Utils.events.put(Utils.removeColors(eventBR.getName()), false);
        main.cO.debug("Event added: " + Utils.removeColors(eventBR.getName()) + " - " + Utils.events.get(Utils.removeColors(eventBR.getName())));

        return eventBR;
    }

    private List<String> getStringOrList(String blockname, String parameter1, String parameter2) {
        List<String> list = new ArrayList<>();

        String path = "Blocks." + blockname;
        if (main.getFiles().getBlocklist().getConfigurationSection(path).contains(parameter1))
            path += "." + parameter1;
        else if (main.getFiles().getBlocklist().getConfigurationSection(path).contains(parameter2))
            path += "." + parameter2;
        else return null;

        if (!main.getFiles().getBlocklist().getStringList(path).isEmpty())
            list = main.getFiles().getBlocklist().getStringList(path);
        else
            list.add(main.getFiles().getBlocklist().getString(path));

        return list;
    }
}
