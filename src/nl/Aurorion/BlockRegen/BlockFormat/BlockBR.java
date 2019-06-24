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
    private Material replaceBlock;

    private boolean regenerate;

    private int regenDelay;
    private boolean naturalBreak;

    private List<String> consoleCommands;
    private List<String> playerCommands;
    private String particle;
    private Amount money;

    private List<String> toolsRequired;
    private List<String> enchantsRequired;
    private JobRequirement jobRequirement;
    private List<Drop> drops;

    private EventBR event;

    private boolean valid;

    public boolean isValid() {
        return valid;
    }

    public BlockBR(String blockType, String replaceBlock) {
        try {
            this.blockType = Material.valueOf(blockType.toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn(blockType + " is not a valid Material, skipping the whole block..");
            valid = false;
            return;
        }

        try {
            this.replaceBlock = Material.valueOf(replaceBlock.toUpperCase());
        } catch (IllegalArgumentException e) {
            Main.getInstance().cO.warn(replaceBlock + " is not a valid Material, skipping the whole block..");
            valid = false;
            return;
        }

        valid = true;
    }

    public Amount getMoney() {
        return money;
    }

    public void setMoney(Amount money) {
        this.money = money;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void reward(Player player, Block block, int expDrop, Location... loc) {
        Location blockLocation;

        if (block != null)
            blockLocation = block.getLocation();
        else
            blockLocation = loc[0];

        // Commands
        if (!consoleCommands.isEmpty())
            for (String command : consoleCommands) {
                if (Main.getInstance().isPlaceholderAPI())
                    command = PlaceholderAPI.setPlaceholders(player, command);
                Main.getInstance().getServer().dispatchCommand(Main.getInstance().getServer().getConsoleSender(), command);
            }

        if (!playerCommands.isEmpty())
            for (String command : playerCommands) {
                if (Main.getInstance().isPlaceholderAPI())
                    command = PlaceholderAPI.setPlaceholders(player, command);
                Main.getInstance().getServer().dispatchCommand(player, command);
            }

        // Items

        // Custom drops
        // Drop Vanilla stuff?
        if (!naturalBreak)
            // Custom drops
            for (Drop drop : drops) {

                Main.getInstance().cO.debug("Drop: " + drop.getId());

                ItemStack item = drop.getItemStack(player);

                Main.getInstance().cO.debug("EXP: " + drop.getExpAmount().toString() + " AMOUNT:" + drop.getExpAmount().getAmount());
                expDrop = drop.getExpAmount().getAmount();

                Main.getInstance().cO.debug("Loaded events: " + Utils.events.toString() + " Event: " + Utils.removeColors(event.getName()));

                // Apply event boosters
                if (Utils.events.get(Utils.removeColors(event.getName()))) {
                    if (event.isDoubleDrops())
                        item.setAmount(item.getAmount() * 2);

                    if (event.isDoubleXp())
                        expDrop *= 2;
                }

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
        else {
            // MC Drops, why does that sound like a cool name for a hip-hop rapper?

            if (block != null) {
                for (ItemStack item : block.getDrops()) {
                    if (Utils.events.get(Utils.removeColors(event.getName()))) {
                        if (event.isDoubleDrops())
                            item.setAmount(item.getAmount() * 2);

                        if (event.isDoubleXp())
                            expDrop *= 2;
                    }

                    if (expDrop > 0)
                        block.getWorld().spawn(blockLocation, ExperienceOrb.class).setExperience(expDrop);

                    if (item.getAmount() > 0)
                        blockLocation.getWorld().dropItemNaturally(blockLocation, item);
                }
            }
        }

        // Event item
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
                Enchantment enchantment = Main.getInstance().getEnchantUtil().get(enchant);

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

    public Material getBlockType() {
        return blockType;
    }

    public void setBlockType(Material blockType) {
        this.blockType = blockType;
    }

    public Material getReplaceBlock() {
        return replaceBlock;
    }

    public void setReplaceBlock(Material replaceBlock) {
        this.replaceBlock = replaceBlock;
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
        if (consoleCommands == null)
            this.consoleCommands = new ArrayList<>();
        else
            this.consoleCommands = consoleCommands;
    }

    public List<String> getPlayerCommands() {
        return playerCommands;
    }

    public void setPlayerCommands(List<String> playerCommands) {
        if (playerCommands == null)
            this.playerCommands = new ArrayList<>();
        else
            this.playerCommands = playerCommands;
    }

    public List<String> getToolsRequired() {
        return toolsRequired;
    }

    public void setToolsRequired(List<String> toolsRequired) {
        this.toolsRequired = toolsRequired;
    }

    public List<String> getEnchantsRequired() {
        return enchantsRequired;
    }

    public void setEnchantsRequired(List<String> enchantsRequired) {
        List<String> finalEnchants = new ArrayList<>();
        for (String enchant : enchantsRequired) {
            if (Main.getInstance().getEnchantUtil().get(enchant) != null)
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
}
