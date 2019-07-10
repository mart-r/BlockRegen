package nl.Aurorion.BlockRegen.Events;

import nl.Aurorion.BlockRegen.Main;
import nl.Aurorion.BlockRegen.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Crops;

public class PlayerInteract implements Listener {

    private Main main;

    public PlayerInteract(Main instance) {
        this.main = instance;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (main.getFiles().getSettings().getBoolean("Bone-Meal-Override")) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = event.getPlayer();

                if (player.getInventory().getItemInHand().getType().equals(Material.INK_SACK) && player.getInventory().getItemInHand().getDurability() == 15) {

                    if (event.getClickedBlock().getState().getData() instanceof Crops) {

                        Location loc = event.getClickedBlock().getLocation();

                        if (Utils.getProcess(loc) != null)
                            Utils.clearProcess(loc);
                    }
                }
            }
        }
    }
}
