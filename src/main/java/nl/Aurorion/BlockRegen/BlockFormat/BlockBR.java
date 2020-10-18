package nl.Aurorion.BlockRegen.BlockFormat;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.JobProgression;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Messages;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockBR {

    /*
     * null list => empty/null
     * null String => ""
     * null int => 0
     * */

    private Material blockType;
    private byte blockData;

    private Material replaceBlock;
    private byte replaceBlockData;

    private boolean regenerate;

    private int regenDelay;
    private boolean naturalBreak;

    private boolean applyFortune;

    private List<String> consoleCommands;
    private List<String> playerCommands;
    private String particle;
    private Amount money;
    private int regenTimes;

    private List<String> toolsRequired;
    private List<String> enchantsRequired;
    private JobRequirement jobRequirement;
    private List<String> permissions;

    private List<Drop> drops;

    private EventBR event;

    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    public BlockBR(String blockType, String replaceBlock) {
        try {
            this.blockType = Material.valueOf(blockType.split(";")[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn(blockType + " is not a valid Material, skipping the whole block..");
            valid = false;
            return;
        }

        try {
            this.replaceBlock = Material.valueOf(replaceBlock.split(";")[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn(replaceBlock + " is not a valid Material, skipping the whole block..");
            valid = false;
            return;
        }

        this.toolsRequired = new ArrayList<>();
        valid = true;
    }

    public int getRegenTimes() {
        return regenTimes;
    }

    public void setRegenTimes(int regenTimes) {
        this.regenTimes = regenTimes;
    }

    public Amount getMoney() {
        return money;
    }

    public void setMoney(Amount money) {
        this.money = money;
    }

    public void reward(Player player, Block block, int expDrop, Location... loc) {
        Location blockLocation;

        ItemStack tool;

        if (Main.getInstance().isOver18())
            tool = player.getInventory().getItemInMainHand();
        else
            tool = player.getItemInHand();

        if (block != null)
            blockLocation = block.getLocation();
        else
            blockLocation = loc[0];

        // Commands
        if (!consoleCommands.isEmpty())
            for (String command : consoleCommands) {
                if (Main.getInstance().isPlaceholderAPI())
                    command = PlaceholderAPI.setPlaceholders(player, command);
                Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), command.replace("%player%", player.getName()));
            }

        if (!playerCommands.isEmpty())
            for (String command : playerCommands) {
                if (Main.getInstance().isPlaceholderAPI())
                    command = PlaceholderAPI.setPlaceholders(player, command);
                Main.getInstance().getServer().dispatchCommand(player, command.replace("%player%", player.getName()));
            }

        // Items

        // Custom drops
        // Drop Vanilla stuff?
        if (!naturalBreak) {
            // Custom drops
            if (!drops.isEmpty())
                for (Drop drop : drops) {

                    Main.getInstance().cO.debug("Drop: " + drop.getId());

                    ItemStack item = drop.getItemStack(player);

                    Main.getInstance().cO.debug("EXP: " + drop.getExpAmount().toString() + " AMOUNT:" + drop.getExpAmount().getAmount());
                    expDrop = drop.getExpAmount().getAmount();

                    // Apply event boosters
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

                    // Drop/Give stuff
                    if (item.getAmount() > 0)
                        if (drop.isDropNaturally())
                            blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                        else
                            player.getInventory().addItem(item);

                    if (expDrop > 0)
                        if (drop.isDropExpNaturally())
                            blockLocation.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);
                        else
                            player.giveExp(expDrop);
                }
        } else {
            // MC Drops, why does that sound like a cool name for a hip-hop rapper?

            if (block != null) {
                for (ItemStack item : block.getDrops()) {
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

                    if (expDrop > 0)
                        block.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);

                    if (item.getAmount() > 0)
                        blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                }
            }
        }

        // Event item
        if (event != null)
            if (Utils.events.get(Utils.removeColors(event.getName()))) {
                if (event.getDrop() != null) {
                    if (Main.getInstance().getRandom().nextInt(event.getDropRarity()) + 1 == 1) {
                        ItemStack eventItem = event.getDrop().getItemStack(player);

                        if (eventItem.getAmount() > 0)
                            if (event.getDrop().isDropNaturally())
                                blockLocation.getWorld().dropItemNaturally(blockLocation, eventItem);
                            else
                                player.getInventory().addItem(eventItem);
                    }
                }
            }

        // Money
        int moneyToGive = money.getAmount();

        if (moneyToGive != 0 && Main.getInstance().getEconomy() != null) {
                EconomyResponse response = Main.getInstance().getEconomy().depositPlayer(player, moneyToGive);
            if (response.transactionSuccess())
                Main.getInstance().cO.debug("Gave " + moneyToGive + " to " + player.getName());
            else
                Main.getInstance().cO.err("Could not deposit money to player's account.");
        }

        // Particles
        if (Main.getInstance().isOver18())
            showParticle(block);
    }

    private void showParticle(Block block) {
        Main.getInstance().getParticles().check(particle, block);
    }

    public boolean check(Player player) {

        // Permission check
        if (!permissions.isEmpty())
            if (!checkPermissions(player)) {
                player.sendMessage(Messages.get("Permission-Error").replace("%permission%", Utils.listToString(permissions, "&f, &7", "&cNo permissions configured.")));
                return false;
            }

        Main.getInstance().cO.debug("Passed permission check");

        // Tools

        ItemStack tool;

        if (Main.getInstance().isOver18())
            tool = player.getInventory().getItemInMainHand();
        else
            tool = player.getInventory().getItemInHand();

        if (toolsRequired != null)
            if (!toolsRequired.isEmpty()) {
                if (!toolsRequired.contains(tool.getType().toString().toUpperCase())) {
                    Main.getInstance().cO.debug("Tool check failed");

                    player.sendMessage(Messages.get("Tool-Required-Error").replace("%tool%", Utils.listToString(toolsRequired, "§f, §7", "§cNo tools set")));

                    return false;
                }
            } else
                Main.getInstance().cO.debug("Skipping tool check");

        Main.getInstance().cO.debug("Tool check passed");

        // Enchants

        boolean ep = false;

        if (!enchantsRequired.isEmpty()) {
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
        } else {
            Main.getInstance().cO.debug("Skipping enchant check");
            ep = true;
        }

        if (!ep) {
            Main.getInstance().cO.debug("Enchant check failed");
            player.sendMessage(Messages.get("Enchant-Required-Error").replace("%enchant%", Utils.listToString(enchantsRequired, "§f, §7", "§cNo enchants set")));
            return false;
        }

        // Jobs

        boolean jp = false;

        if (Main.getInstance().getJobs() && jobRequirement != null) {

            Main.getInstance().cO.debug("Job Req.: " + jobRequirement.getJob() + jobRequirement.getLevel());

            List<JobProgression> jobs = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

            for (JobProgression job : jobs) {
                if (job.getJob().getName().toLowerCase().equals(jobRequirement.getJob().toLowerCase())) {
                    Main.getInstance().cO.debug(job.getJob().getName().toLowerCase());
                    Main.getInstance().cO.debug(String.valueOf(job.getLevel()));
                    if (job.getLevel() > jobRequirement.getLevel())
                        jp = true;
                }
            }

            if (!jp) {
                player.sendMessage(Messages.get("Jobs-Error").replace("%level%", String.valueOf(jobRequirement.getLevel())).replace("%job%", jobRequirement.getJob()));
                return false;
            }
        }

        Main.getInstance().cO.debug("Nominal, pass");

        return true;
    }

    private boolean checkPermissions(Player p) {
        List<String> permissions = this.permissions;

        boolean pass = p.hasPermission(permissions.get(0));

        for (int i = 1; i < permissions.size(); i++) {
            String perm = permissions.get(i);

            if (perm.startsWith("AND ")) {
                pass = pass && p.hasPermission(perm.replace("AND ", ""));
            } else {
                // OR, perms with no prefix are taken as OR as well.
                if (p.hasPermission(perm.replace("OR ", ""))) {
                    return true;
                }
            }
        }

        return pass;
    }

    public Material getReplaceBlock() {
        return replaceBlock;
    }

    public int getRegenDelay() {
        return regenDelay;
    }

    public byte getBlockData() {
        return blockData;
    }

    public byte getReplaceBlockData() {
        return replaceBlockData;
    }

    public void setReplaceBlockData(byte replaceBlockData) {
        this.replaceBlockData = replaceBlockData;
    }

    public void setBlockData(byte blockData) {
        this.blockData = blockData;
    }

    public void setRegenDelay(int regenDelay) {
        this.regenDelay = regenDelay;
    }

    public List<String> getConsoleCommands() {
        return consoleCommands;
    }

    public void setConsoleCommands(List<String> consoleCommands) {
        this.consoleCommands = consoleCommands == null ? new ArrayList<>() : consoleCommands;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions == null ? new ArrayList<>() : permissions;
    }

    public void setPlayerCommands(List<String> playerCommands) {
        this.playerCommands = playerCommands == null ? new ArrayList<>() : playerCommands;
    }

    public List<String> getToolsRequired() {
        return toolsRequired;
    }

    public void setToolsRequired(List<String> toolsRequired) {
        for (String toolString : toolsRequired) {
            try {
                Material mat = Material.valueOf(toolString.toUpperCase());
            } catch (IllegalArgumentException e) {
                Main.getInstance().cO.warn("Tool material " + toolString + " is not valid, skipping.");
                continue;
            }

            this.toolsRequired.add(toolString);
        }
    }

    public List<String> getEnchantsRequired() {
        return enchantsRequired;
    }

    public void setEnchantsRequired(List<String> enchantsRequired) {
        List<String> finalEnchants = new ArrayList<>();
        for (String enchant : enchantsRequired) {
            try {
                if (enchant.contains(";"))
                    if (enchant.split(";")[1] == null)
                        enchant += "";
            } catch (ArrayIndexOutOfBoundsException e) {
                enchant = enchant.replace(";", "");
            }

            if (Main.getInstance().getEnchantUtil().get(enchant.split(";")[0]) != null)
                finalEnchants.add(enchant);
            else
                Main.getInstance().cO.warn("Enchant " + enchant.toUpperCase() + " not valid, skipping.");
        }

        this.enchantsRequired = finalEnchants;
    }

    public JobRequirement getJobRequirement() {
        return jobRequirement;
    }

    public void setJobRequirement(JobRequirement jobRequirement) {
        this.jobRequirement = jobRequirement;
    }

    public String getParticle() {
        return particle;
    }

    public void setParticle(String particle) {
        if (particle == null)
            particle = "";
        this.particle = particle.toLowerCase();
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

    public void setDrops(List<Drop> drops) {
        if (drops == null)
            this.drops = new ArrayList<>();
        else
            this.drops = drops;
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

    public boolean isApplyFortune() {
        return applyFortune;
    }

    public void setApplyFortune(boolean applyFortune) {
        this.applyFortune = applyFortune;
    }
}
