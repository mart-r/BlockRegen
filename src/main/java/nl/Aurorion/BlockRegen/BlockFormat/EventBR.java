package nl.Aurorion.BlockRegen.BlockFormat;

import nl.Aurorion.BlockRegen.Main;
import org.bukkit.boss.BarColor;

public class EventBR {

    private String name;

    private String bossbarTitle;
    private String bossbarColor;

    private boolean doubleDrops;
    private boolean doubleXp;

    private Drop drop;

    private boolean dropEnabled;
    private int dropRarity;

    public EventBR(String name) {
        this.name = name;

        this.dropEnabled = false;
    }

    public boolean isDropEnabled() {
        return dropEnabled;
    }

    public void setDropEnabled(boolean dropEnabled) {
        if (dropEnabled && drop == null)
            this.dropEnabled = false;
        this.dropEnabled = dropEnabled;
    }

    public int getDropRarity() {
        return dropRarity;
    }

    public void setDropRarity(int dropRarity) {
        this.dropRarity = dropRarity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBossbarTitle() {
        return bossbarTitle;
    }

    public void setBossbarTitle(String bossbarTitle) {
        this.bossbarTitle = bossbarTitle;
    }

    public String getBossbarColor() {
        return bossbarColor;
    }

    public void setBossbarColor(String bossbarColor) {
        try {
            BarColor.valueOf(bossbarColor.toUpperCase());
        } catch (Exception e) {
            Main.getInstance().cO.warn("Bossbar color for event " + name + " is not valid.");
            this.bossbarColor = null;
            return;
        }

        this.bossbarColor = bossbarColor.toUpperCase();
    }

    public boolean isDoubleDrops() {
        return doubleDrops;
    }

    public void setDoubleDrops(boolean doubleDrops) {
        this.doubleDrops = doubleDrops;
    }

    public boolean isDoubleXp() {
        return doubleXp;
    }

    public void setDoubleXp(boolean doubleXp) {
        this.doubleXp = doubleXp;
    }

    public Drop getDrop() {
        return drop;
    }

    public void setDrop(Drop drop) {
        this.drop = drop;
    }
}
