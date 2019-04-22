package me.skulldev.events;

import me.skulldev.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class InventoryInteractionEvents implements Listener {

    public Main main;

    public InventoryInteractionEvents(Main pl) {
        this.main = pl;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        if(e.getView().getTopInventory().getTitle().equals(main.translateColors(main.getStartInventoryTitle()))) {
            if(e.getCurrentItem().getType().equals(Material.BAKED_POTATO)) {
                e.setCancelled(true);
                if(player.hasPermission(main.getUseSumoPermission())) {
                    if (main.getSumoActive()) {
                        player.sendMessage(main.translateColors(main.getPrefix() + main.getEventAlreadyStartedMessage().replaceAll("<Event>", main.getSumoTitle())));
                        player.closeInventory();
                    } else {
                        main.startSumoEvent(player);
                    }
                } else {
                    player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                }
            } else if(e.getCurrentItem().getType().equals(Material.STAINED_GLASS_PANE)) {
                e.setCancelled(true);
            }
        }
    }
}
