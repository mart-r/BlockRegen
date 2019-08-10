package nl.Aurorion.BlockRegen.BlockFormat;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Particles.ParticleBR;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockBR {

    /*
     * null list => empty/null
     * null String => ""
     * null int => 0
     * */

    // Misc

    private Material blockType;
    private Material replaceBlock;

    private boolean regenerate;

    private int regenTimes;

    private int regenDelay;
    private boolean naturalBreak;

    private boolean applyFortune;

    private EventBR event;

    // Rewards

    private List<String> consoleCommands;
    private List<String> playerCommands;

    private List<String> broadcastMessage;
    private List<String> informMessage;

    private String particle;
    private Amount money;

    private List<Drop> drops;

    // On-regen

    private List<String> onRegenConsoleCommands;

    private List<String> onRegenBroadcastMessage;
    private List<String> onRegenInformMessage;

    // Conditions

    private List<String> toolsRequired;
    private List<String> enchantsRequired;

    private List<JobRequirement> jobRequirements;
    private String permission;

    private ParticleBR particleBR;

    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    public BlockBR(String blockType, String replaceBlock) {
        try {
            this.blockType = Material.valueOf(blockType.toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn("Material " + blockType + " is not valid, skipping the whole block..");
            valid = false;
            return;
        }

        try {
            this.replaceBlock = Material.valueOf(replaceBlock.toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn("Material " + replaceBlock + "is not valid, skipping the whole block..");
            valid = false;
            return;
        }

        this.drops = new ArrayList<>();
        this.enchantsRequired = new ArrayList<>();
        this.toolsRequired = new ArrayList<>();
        this.jobRequirements = new ArrayList<>();
        this.playerCommands = new ArrayList<>();
        this.consoleCommands = new ArrayList<>();
        this.onRegenInformMessage = new ArrayList<>();
        this.onRegenBroadcastMessage = new ArrayList<>();
        this.onRegenConsoleCommands = new ArrayList<>();

        valid = true;
    }

    // ----------------- Checked setters ------------------------

    public void setToolsRequired(List<String> toolsRequired) {

        for (String toolString : toolsRequired) {
            try {
                Material mat = Material.valueOf(toolString.toUpperCase());
            } catch (IllegalArgumentException e) {
                Main.getInstance().cO.warn("Tool material " + toolString + " is not valid, skipping..");
                continue;
            }

            this.toolsRequired.add(toolString);
        }

        this.toolsRequired = toolsRequired;
    }

    public void setEnchantsRequired(List<String> enchantsRequired) {
        List<String> finalEnchants = new ArrayList<>();
        for (String enchant : enchantsRequired) {
            Main.getInstance().cO.debug("Input: " + enchant);

            try {
                if (enchant.contains(";"))
                    if (enchant.split(";")[1] == null)
                        enchant += "";
            } catch (ArrayIndexOutOfBoundsException e) {
                enchant = enchant.replace(";", "");
            }

            if (Main.getInstance().getEnchantUtil().get(enchant.split(";")[0]) == null) {
                Main.getInstance().cO.warn("Enchantment " + enchant.split(";")[0].toUpperCase() + " is not valid, skipping..");
                continue;
            }

            finalEnchants.add(enchant.toLowerCase());
        }

        this.enchantsRequired = finalEnchants;
    }

    public void setJobRequirements(List<String> jobRequirements) {
        for (String str : jobRequirements) {

            String levelStr = "";
            String jobName = "";

            if (str.contains(";")) {
                jobName = str.split(";")[0];

                if (str.split(";").length == 2)
                    levelStr = str.split(";")[1];
            }

            if (jobName == "" || Jobs.getJob(jobName) == null) {
                Main.getInstance().cO.warn("Job name in job requirement for block " + blockType.toString() + " is not valid, skipping..");
                continue;
            }

            int level;

            try {
                level = Integer.valueOf(levelStr);
            } catch (NumberFormatException e) {
                Main.getInstance().cO.warn("Job level in job requirement for block " + blockType.toString() + " is not valid, using 0..");
                level = 0;
            }

            this.jobRequirements.add(new JobRequirement(jobName, level));
        }
    }

    public void setParticle(String particle) {
        if (particle == null)
            particle = "";
        this.particle = particle.toLowerCase();
    }

    public void setDrops(List<Drop> drops) {
        if (drops == null)
            this.drops = new ArrayList<>();
        else
            this.drops = drops;
    }

    // ---------------------------------------- Block Break Actions -------------------------------------------

    /**
     * Returns whether or not conditions are met
     *
     * @param player Player to test conditions for
     * @return boolean
     */

    public boolean check(Player player) {
        // Permission

        if (!permission.equals(""))
            if (!player.hasPermission(permission)) {
                player.sendMessage(Messages.get("Permission-Error").replace("%permission%", permission));
                return false;
            }

        Main.getInstance().cO.debug("Passed permission check");

        // Tools

        ItemStack tool = player.getInventory().getItemInMainHand();

        if (toolsRequired != null)
            if (!toolsRequired.isEmpty()) {
                if (player.getInventory().getItemInMainHand() == null) {
                    player.sendMessage(Messages.get("Tool-Required-Error").replace("%tool%", Utils.listToString(toolsRequired, "§f, §7", "§cNo tools set")));
                    Main.getInstance().cO.debug("Tool check failed");
                    return false;
                }

                if (!toolsRequired.contains(tool.getType().toString().toUpperCase())) {
                    Main.getInstance().cO.debug("Tool check failed");

                    player.sendMessage(Messages.get("Tool-Required-Error").replace("%tool%", Utils.listToString(toolsRequired, "§f, §7", "§cNo tools set")));

                    return false;
                }
            } else
                Main.getInstance().cO.debug("Skipping tool check");

        Main.getInstance().cO.debug("Tool check passed");

        // Enchant check

        if (!enchantsRequired.isEmpty()) {

            boolean ep = false;

            for (String enchant : enchantsRequired) {
                Enchantment enchantment = Main.getInstance().getEnchantUtil().get(enchant.split(";")[0]);

                if (tool.getItemMeta().hasEnchant(enchantment)) {
                    if (enchant.contains(";"))
                        if (tool.getItemMeta().getEnchantLevel(enchantment) >= Integer.valueOf(enchant.split(";")[1])) {
                            ep = true;
                            break;
                        } else
                            continue;

                    ep = true;
                    Main.getInstance().cO.debug("Enchant check passed");
                    break;
                }
            }

            if (!ep) {
                Main.getInstance().cO.debug("Enchant check failed");
                player.sendMessage(Messages.get("Enchant-Required-Error").replace("%enchant%", Utils.listToString(enchantsRequired, "§f, §7", "§cNo enchants set")));
                return false;
            }
        } else
            Main.getInstance().cO.debug("Skipping enchant check");

        // Jobs

        if (Main.getInstance().getJobs() && jobRequirements != null) {
            if (!jobRequirements.isEmpty()) {
                boolean jp = true;

                List<JobProgression> jobs = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

                List<String> jobNames = new ArrayList<>();
                jobs.forEach(job -> jobNames.add(job.getJob().getName().toLowerCase()));

                int i = 0;

                for (JobRequirement jobReq : jobRequirements) {
                    if (jobNames.contains(jobReq.getJob().toLowerCase())) {
                        if (jobs.get(i).getLevel() < jobReq.getLevel()) {
                            Main.getInstance().cO.debug("Missing the level, " + jobs.get(i).getLevel() + " < " + jobReq.getLevel());
                            jp = false;
                        }
                    } else {
                        jp = false;
                        Main.getInstance().cO.debug("Missing the job..");
                    }
                    i++;
                }

                Main.getInstance().cO.debug(String.valueOf(jp));

                HashMap<String, String> jobReqs = new HashMap<>();
                for (JobRequirement jobReq : jobRequirements)
                    jobReqs.put(jobReq.getJob(), String.valueOf(jobReq.getLevel()));

                if (!jp) {
                    player.sendMessage(Messages.get("Jobs-Error").replace("%jobs%", Utils.mapToString(jobReqs, ", ", ";", "§cNo job requirements set.")));
                    return false;
                }
            }
        }

        Main.getInstance().cO.debug("Nominal, pass");

        return true;
    }

    /**
     * Actions after conditions are passed
     *
     * @param player  Player who mined
     * @param block   Mined block
     * @param expDrop Default experience drop
     */

    public void reward(Player player, Block block, int expDrop) {

        Location blockLocation = block.getLocation();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // How many times the block can be mined, unlimited if no limit
        // Used for placeholders
        String actualRegenTimes = "Unlimited";

        if (Utils.regenTimesBlocks.containsKey(blockLocation))
            actualRegenTimes = String.valueOf(Utils.regenTimesBlocks.get(blockLocation) - 1);
        else if (regenTimes != 0)
            actualRegenTimes = String.valueOf(regenTimes - 1);

        // Commands and messages ----------------------------------------------------------------

        // Parsing sand firing console commands
        if (!consoleCommands.isEmpty())
            for (String command : consoleCommands) {
                // Utils.parse uses PlaceholderAPI integration, if possible
                command = Utils.parse(command, player, this, actualRegenTimes);
                Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), command);
            }

        // Parsing and firing player commands
        if (!playerCommands.isEmpty())
            for (String command : playerCommands) {
                command = Utils.parse(command, player, this, actualRegenTimes);
                Main.getInstance().getServer().dispatchCommand(player, command);
            }

        // Parsing and broadcasting messages
        if (!broadcastMessage.isEmpty())
            for (String line : broadcastMessage) {
                line = Utils.parseAndColor(line, player, this, actualRegenTimes);
                for (Player p : Main.getInstance().getServer().getOnlinePlayers())
                    p.sendMessage(line);
            }

        // Parsing and sending messages
        if (!informMessage.isEmpty())
            for (String line : informMessage) {
                line = Utils.parseAndColor(line, player, this, actualRegenTimes);
                player.sendMessage(line);
            }

        // Item Drops ---------------------------------------------------------------------------

        // Go with vanilla drops?
        if (!naturalBreak)
            // Custom drops
            if (!drops.isEmpty())
                for (Drop drop : drops) {

                    // Creates and parses ItemStack as configured in Blocklist.yml
                    ItemStack item = drop.getItemStack(player);

                    // Retrieves amount random/fixed based on configuration
                    expDrop = drop.getExpAmount().getAmount();

                    // Apply event boosters
                    if (drop.isApplyEvents())
                        if (event != null)
                            if (Utils.events.get(Utils.removeColors(event.getName()))) {
                                if (event.isDoubleDrops())
                                    item.setAmount(item.getAmount() * 2);

                                if (event.isDoubleXp())
                                    expDrop *= 2;
                            }

                    // Modify item amount based on Fortune enchantment
                    // Adds fortune generated amount to the base amount picked by format
                    if (applyFortune)
                        if (tool != null)
                            item.setAmount(item.getAmount() + Utils.checkFortune(block.getType(), tool));

                    // Drop Item
                    if (item.getAmount() > 0)
                        if (drop.isDropNaturally())
                            blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                        else
                            player.getInventory().addItem(item);

                    // Drop Experience
                    if (expDrop > 0)
                        if (drop.isDropExpNaturally())
                            blockLocation.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);
                        else
                            player.giveExp(expDrop);
                }
            else {
                // Vanilla drops
                if (block != null) {
                    if (!block.getDrops().isEmpty())
                        for (ItemStack item : block.getDrops()) {

                            // Event modifier
                            if (event != null)
                                if (Utils.events.get(Utils.removeColors(event.getName()))) {
                                    if (event.isDoubleDrops())
                                        item.setAmount(item.getAmount() * 2);

                                    if (event.isDoubleXp())
                                        expDrop *= 2;
                                }

                            // Modify item amount based on Fortune enchantment
                            // Works like Vanilla fortune
                            if (applyFortune)
                                if (tool != null)
                                    item.setAmount(Utils.checkFortune(block.getType(), tool));

                            // Above can set to 0, we don't want that here.
                            if (item.getAmount() == 0)
                                item.setAmount(1);

                            // Drop Experience
                            if (expDrop > 0)
                                block.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);

                            // Drop Item
                            if (item.getAmount() > 0)
                                blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                        }
                }
            }

        // Special Event Item -------------------------------------------------------------------------------
        if (event != null)
            if (Utils.events.get(Utils.removeColors(event.getName()))) {
                if (event.getDrop() != null) {
                    if (event.isDropEnabled())
                        if (Main.getInstance().getRandom().nextInt(event.getDropRarity()) + 1 == 1) {
                            ItemStack eventItem = event.getDrop().getItemStack(player);

                            // Drop Item
                            if (eventItem.getAmount() > 0)
                                if (event.getDrop().isDropNaturally())
                                    blockLocation.getWorld().dropItemNaturally(blockLocation, eventItem);
                                else
                                    player.getInventory().addItem(eventItem);
                        }
                }
            }

        // Money ------------------------------------------------------------------------------------------
        if (money != null) {
            int moneyToGive = money.getAmount();
            if (moneyToGive != 0 && Main.getInstance().getEconomy() != null) {
                EconomyResponse response = Main.getInstance().getEconomy().depositPlayer(player, moneyToGive);
                if (response.transactionSuccess())
                    Main.getInstance().cO.debug("Gave " + moneyToGive + " to " + player.getName());
                else
                    Main.getInstance().cO.err("Could not deposit money to player's account.");
            }
        }

        // Particles --------------------------------------------------------------------------------------

        if (particle != null)
            showParticle(blockLocation);
        else if (particleBR != null)
            particleBR.castParticles(blockLocation, player);
        else Main.getInstance().cO.debug("No particles configured");
    }

    /**
     * Shows particle based on configuration on {@link @location}
     *
     * @param location Location of the mined block
     */

    @Deprecated
    private void showParticle(Location location) {
        Main.getInstance().getParticles().check(particle, location);
    }

    public void onRegen(Player player, Location blockLocation) {

        Main.getInstance().cO.debug("On-regen actions running..");

        String actualRegenTimes = "Unlimited";

        if (Utils.regenTimesBlocks.containsKey(blockLocation))
            actualRegenTimes = String.valueOf(Utils.regenTimesBlocks.get(blockLocation));
        else if (regenTimes != 0)
            actualRegenTimes = String.valueOf(regenTimes);

        // Commands
        if (!onRegenConsoleCommands.isEmpty())
            for (String command : onRegenConsoleCommands) {
                command = Utils.parse(command, player, this, actualRegenTimes);
                Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), command);
            }

        // Messages
        if (!onRegenBroadcastMessage.isEmpty())
            for (String line : onRegenBroadcastMessage) {
                line = Utils.parseAndColor(line, player, this, actualRegenTimes);

                for (Player p : Main.getInstance().getServer().getOnlinePlayers())
                    p.sendMessage(line);
            }

        if (!onRegenInformMessage.isEmpty())
            for (String line : onRegenInformMessage)
                player.sendMessage(Utils.parseAndColor(line, player, this, actualRegenTimes));
    }

    public Material getReplaceBlock() {
        return replaceBlock;
    }

    public int getRegenDelay() {
        return regenDelay;
    }

    public void setRegenDelay(int regenDelay) {
        this.regenDelay = regenDelay;
    }

    public List<String> getConsoleCommands() {
        return consoleCommands;
    }

    public void setConsoleCommands(List<String> consoleCommands) {
        this.consoleCommands = consoleCommands;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public void setPlayerCommands(List<String> playerCommands) {
        this.playerCommands = playerCommands;
    }

    public List<String> getToolsRequired() {
        return toolsRequired;
    }

    public List<String> getEnchantsRequired() {
        return enchantsRequired;
    }

    public Amount getMoney() {
        return money;
    }

    public void setMoney(Amount money) {
        this.money = money;
    }

    public int getRegenTimes() {
        return regenTimes;
    }

    public void setRegenTimes(int regenTimes) {
        this.regenTimes = regenTimes;
    }

    public String getParticle() {
        return particle;
    }

    public boolean isNaturalBreak() {
        return naturalBreak;
    }

    public void setNaturalBreak(boolean naturalBreak) {
        this.naturalBreak = naturalBreak;
    }

    public List<Drop> getDrops() {
        return drops;
    }

    public EventBR getEvent() {
        return event;
    }

    public void setEvent(EventBR event) {
        this.event = event;
    }

    public boolean isRegenerate() {
        return regenerate;
    }

    public void setRegenerate(boolean regenerate) {
        this.regenerate = regenerate;
    }

    public List<String> getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(List<String> broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public List<String> getInformMessage() {
        return informMessage;
    }

    public void setInformMessage(List<String> informMessage) {
        this.informMessage = informMessage;
    }

    public void setOnRegenConsoleCommands(List<String> onRegenConsoleCommands) {
        this.onRegenConsoleCommands = onRegenConsoleCommands;
    }

    public void setOnRegenBroadcastMessage(List<String> onRegenBroadcastMessage) {
        this.onRegenBroadcastMessage = onRegenBroadcastMessage;
    }

    public void setOnRegenInformMessage(List<String> onRegenInformMessage) {
        this.onRegenInformMessage = onRegenInformMessage;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public ParticleBR getParticleBR() {
        return particleBR;
    }

    public void setParticleBR(ParticleBR particleBR) {
        this.particleBR = particleBR;
    }

    public boolean isApplyFortune() {
        return applyFortune;
    }

    public void setApplyFortune(boolean applyFortune) {
        this.applyFortune = applyFortune;
    }
}
