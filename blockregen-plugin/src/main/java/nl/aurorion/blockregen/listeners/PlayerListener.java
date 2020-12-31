package nl.aurorion.blockregen.listeners;

import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.Message;
import nl.aurorion.blockregen.util.Utils;
import nl.aurorion.blockregen.version.api.INodeData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListener implements Listener {

    private final BlockRegen plugin;

    public PlayerListener(BlockRegen instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;

        if (event.getClickedBlock() != null && Utils.dataCheck.contains(player.getUniqueId())) {
            event.setCancelled(true);
            INodeData nodeData = plugin.getVersionManager().obtainNodeData(event.getClickedBlock());
            if (plugin.getVersionManager().isAbove("1.12", false))
                plugin.getJsonMessenger().sendCopyMessage(player, "Block data: " + nodeData.getAsString(true) + " ( click to copy)", nodeData.getAsString(true));
            else {
                player.sendMessage("Pasted " + nodeData.getAsString(true) + " to Data-Paste.yml with current time stamp.");
                plugin.pasteData(nodeData.getAsString(true));
            }
        }
    }

    // Inform about a new version
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // newVersion will be null when the checker is disabled, or there are no new available
        if (player.hasPermission("blockregen.admin") && plugin.newVersion != null)
            player.sendMessage(Message.UPDATE.get(player)
                    .replaceAll("(?i)%newVersion%", plugin.newVersion)
                    .replaceAll("(?i)%version%", plugin.getDescription().getVersion()));

        // Add to bars if needed
        plugin.getEventManager().addBars(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEventManager().removeBars(event.getPlayer());
    }
}