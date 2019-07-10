package nl.Aurorion.BlockRegen.BlockFormat;

import me.clip.placeholderapi.PlaceholderAPI;
import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Drop {

    // Todo add more ItemStack data

    private Material material;

    private String displayName;
    private List<String> lore;

    private List<String> enchants;
    private List<String> flags;

    // NBT UTILS NEEDED
    private List<String> NBTData;

    // Muh.
    private boolean dropNaturally;

    private boolean dropExpNaturally;

    // Amounts
    private Amount expAmount;

    private Amount amount;

    private boolean applyEvents;

    // Used when loading the block
    private boolean valid;

    // Used only for debugging, not even for that, lol.
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public Drop(String material) {
        try {
            this.material = Material.valueOf(material.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            Main.getInstance().cO.warn(material + " is not a valid Material, skipping the drop item..");
            valid = false;
            return;
        }

        valid = true;
    }

    public ItemStack getItemStack(Player player) {
        ItemStack item = new ItemStack(material, amount.getAmount());

        ItemMeta meta = item.getItemMeta();

        if (Main.getInstance().isPlaceholderAPI()) {
            meta.setDisplayName(Utils.color(PlaceholderAPI.setPlaceholders(player, displayName)));
            for (String line : lore)
                lore.set(lore.indexOf(line), PlaceholderAPI.setPlaceholders(player, line));
        } else {
            meta.setDisplayName(Utils.color(displayName));
        }

        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName == null)
            this.displayName = "";
        else
            this.displayName = displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        if (lore == null)
            this.lore = new ArrayList<>();
        else
            this.lore = lore;
    }

    public boolean isDropNaturally() {
        return dropNaturally;
    }

    public void setDropNaturally(boolean dropNaturally) {
        this.dropNaturally = dropNaturally;
    }

    public boolean isDropExpNaturally() {
        return dropExpNaturally;
    }

    public void setDropExpNaturally(boolean dropExpNaturally) {
        this.dropExpNaturally = dropExpNaturally;
    }

    public Amount getExpAmount() {
        return expAmount;
    }

    public boolean isApplyEvents() {
        return applyEvents;
    }

    public void setApplyEvents(boolean applyEvents) {
        this.applyEvents = applyEvents;
    }

    public void setExpAmount(Amount expAmount) {
        this.expAmount = expAmount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public boolean isValid() {
        return valid;
    }

    public String toString() {
        return material.toString() + ", " + displayName + ", " + lore.toString() + ", " + dropExpNaturally + ", " + dropExpNaturally + ", " + amount.toString();
    }
}
