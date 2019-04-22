package me.skulldev.events;

import me.skulldev.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;

public class DeathEvent implements Listener {

    public Main main;

    public DeathEvent(Main pl) {
        this.main = pl;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if(main.inEvent.contains(player.getUniqueId())) {
            main.inEvent.remove(player.getUniqueId());
            if(main.getSumoActive()) {
                if(main.getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".inventory") !=null ) {
                    try {
                        Inventory inv = Main.fromBase64(main.getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".inventory"));
                        player.getInventory().setContents(inv.getContents());
                    } catch(IOException ex) {
                        ex.printStackTrace();
                    }
                }

                if(main.getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".armor") != null) {
                    try {
                        player.getInventory().setArmorContents(Main.itemStackArrayFromBase64(main.getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".armor")));
                    } catch(IOException ex) {
                        ex.printStackTrace();
                    }
                }
                player.sendMessage(main.translateColors(main.getPrefix() + main.getEventLeaveMessage().replaceAll("<Event>", main.getSumoTitle())));
            }
        }
    }
}
