package nl.Aurorion.BlockRegen.System;

import nl.Aurorion.BlockRegen.BlockFormat.Amount;
import nl.Aurorion.BlockRegen.BlockFormat.BlockBR;
import nl.Aurorion.BlockRegen.BlockFormat.Drop;
import nl.Aurorion.BlockRegen.BlockFormat.EventBR;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public void loadBlocks() {
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
    }

    private BlockBR loadBlock(String name) {
        ConfigurationSection section = blocklist.getConfigurationSection("Blocks." + name);

        BlockBR block = new BlockBR(name, section.getString("replace-block"));

        if (block.isValid()) {
            main.cO.debug(name + " Valid");

            // Misc info

            // Regenerate
            block.setRegenerate(section.getBoolean("regenerate", true));
            main.cO.debug(name + " isRegenerate " + block.isRegenerate());

            // Not needed, 0 by default
            block.setRegenDelay(section.getInt("regen-delay", 3));
            main.cO.debug(name + " regenDelay " + block.getRegenDelay());

            // false by default
            block.setNaturalBreak(section.getBoolean("natural-break", false));
            main.cO.debug(name + " isNaturalBreak " + block.isNaturalBreak());

            // If null, simply won't work, no need to check
            block.setParticle(section.getString("particles"));
            main.cO.debug(name + " particle " + block.getParticle());

            // Number of times the block will regenerate
            block.setRegenTimes(section.getInt("regen-times", 0));
            main.cO.debug(name + " regenTimes " + block.getRegenTimes());

            // Rewards

            block.setConsoleCommands(getStringOrList(name, "console-commands", "console-command"));
            main.cO.debug(name + " consoleCommands " + block.getConsoleCommands().toString());

            block.setPlayerCommands(getStringOrList(name, "player-commands", "player-command"));
            main.cO.debug(name + " playerCommands " + block.getPlayerCommands().toString());

            block.setBroadcastMessage(getStringOrList(name, "broadcast-message", "broadcast-message"));
            main.cO.debug(name + " broadcastMessage " + block.getBroadcastMessage());

            block.setInformMessage(getStringOrList(name, "inform-message", "inform-message"));
            main.cO.debug(name + " informMessage " + block.getInformMessage());

            // On-regen actions

            if (section.contains("on-regen")) {
                block.setOnRegenConsoleCommands(getStringOrList(name + ".on-regen", "console-commands", "console-command"));

                block.setOnRegenBroadcastMessage(getStringOrList(name + ".on-regen", "broadcast-message", "broadcast-message"));

                block.setOnRegenInformMessage(getStringOrList(name + ".on-regen", "inform-message", "inform-message"));
            }

            block.setMoney(loadAmount(section.getCurrentPath() + ".money", 0));
            main.cO.debug(name + " money " + block.getMoney().toString());

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

                    if (drop != null) {
                        drop.setId("legacy");
                        drops.add(drop);
                        block.setDrops(drops);
                    } else
                        main.cO.debug("Drop not valid");

                } else {
                    // New format, look for multiples
                    main.cO.debug("Looking for multiple drops format..");
                    for (String id : dropSection.getKeys(false)) {

                        Drop drop = loadDrop("Blocks." + name + "." + path + "." + id);
                        drop.setId(id);

                        if (drop != null)
                            drops.add(drop);
                        else
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
            main.cO.debug(name + " toolsRequired " + block.getToolsRequired().toString());

            block.setPermission(section.getString("permission", ""));
            main.cO.debug(name + " permission " + block.getPermission());

            // Enchants

            block.setEnchantsRequired(Utils.stringToList(section.getString("enchant-required")));
            main.cO.debug(name + " enchantsRequired " + block.getEnchantsRequired().toString());

            if (section.contains("jobs-check") && main.getJobs())
                block.setJobRequirements(Utils.stringToList(section.getString("jobs-check")));

            // Events
            if (section.contains("event"))
                block.setEvent(loadEvent(section.getCurrentPath() + ".event"));

            return block;
        }

        return null;
    }

    private Drop loadDrop(String path) {
        ConfigurationSection dropSection = blocklist.getConfigurationSection(path);

        Drop drop = new Drop(dropSection.getString("material"));

        if (!drop.isValid())
            return null;

        main.cO.debug("Loading drop on path '" + path + "'");

        main.cO.debug("material " + drop.getMaterial().toString());

        drop.setDisplayName(dropSection.getString("name"));
        main.cO.debug("displayName " + drop.getDisplayName());

        drop.setApplyEvents(dropSection.getBoolean("apply-events", true));
        main.cO.debug("applyEvents " + drop.isApplyEvents());

        drop.setLore(dropSection.getStringList("lores"));
        main.cO.debug("lore " + drop.getLore().toString());

        if (!dropSection.contains("amount"))
            drop.setAmount(new Amount(1));
        else
            drop.setAmount(loadAmount(dropSection.getCurrentPath() + ".amount", 1));

        drop.setDropNaturally(dropSection.getBoolean("drop-naturally", true));
        main.cO.debug("dropNaturally " + drop.isDropNaturally());

        if (dropSection.contains("exp")) {
            drop.setDropExpNaturally(dropSection.getBoolean("exp.drop-naturally", false));

            if (!dropSection.contains("exp.amount"))
                drop.setExpAmount(new Amount(1));
            else
                drop.setExpAmount(loadAmount(dropSection.getCurrentPath() + ".exp.amount", 1));
        } else drop.setExpAmount(new Amount(0));

        return drop;
    }

    private Amount loadAmount(String path, int defaultValue) {

        ConfigurationSection section = blocklist.getConfigurationSection(path);

        Amount amount = new Amount(defaultValue);

        // Fixed or not?
        try {
            if (section.contains("high") && section.contains("low")) {
                // Random amount
                try {
                    amount = new Amount(section.getInt("low"), section.getInt("high"));
                } catch (NullPointerException e) {
                    main.cO.warn("Amount on path " + path + " is not valid, returning default.");
                    return new Amount(defaultValue);
                }

                main.cO.debug(amount.toString());
            }
        } catch (NullPointerException e) {
            // Fixed
            try {
                amount = new Amount(blocklist.getInt(path));
            } catch (NullPointerException e1) {
                main.cO.warn("Amount on path " + path + " is not valid, returning default.");
                return new Amount(defaultValue);
            }
        }

        return amount;
    }

    public EventBR loadEvent(String path) {

        ConfigurationSection section = blocklist.getConfigurationSection(path);

        if (!section.contains("event-name")) {
            main.cO.warn("Event needs to have a name, skipping at path " + path);
            return null;
        }

        EventBR eventBR = new EventBR(section.getString("event-name"));

        if (section.contains("bossbar")) {
            try {
                eventBR.setBossbarColor(section.getString("bossbar.color", "GREEN").toUpperCase());
                eventBR.setBossbarTitle(section.getString("bossbar.name"));
            } catch (NullPointerException e) {
                main.cO.err("Bossbar settings not valid on " + path);
            }
        }

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
        main.cO.debug("Event added: " + eventBR.getName() + " - " + Utils.events.get(Utils.removeColors(eventBR.getName())));

        return eventBR;
    }

    public List<String> getStringOrList(String blockname, String parameter1, String parameter2) {
        List<String> list = new ArrayList<>();

        String path = "Blocks." + blockname;
        if (main.getFiles().getBlocklist().getConfigurationSection(path).contains(parameter1))
            path += "." + parameter1;
        else if (main.getFiles().getBlocklist().getConfigurationSection(path).contains(parameter2))
            path += "." + parameter2;
        else return list;

        if (!main.getFiles().getBlocklist().getStringList(path).isEmpty())
            list = main.getFiles().getBlocklist().getStringList(path);
        else
            list.add(main.getFiles().getBlocklist().getString(path));

        return list;
    }
}
