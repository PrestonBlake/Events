package me.skulldev.events;

import me.skulldev.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;

public class JoinLeaveEvents implements Listener {

    public Main main;

    public JoinLeaveEvents(Main pl) {
        this.main = pl;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if(main.inEvent.contains(player.getUniqueId())) {
            main.inEvent.remove(player.getUniqueId());
        }
    }

    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if(main.inEvent.contains(player.getUniqueId())) {
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
            main.inEvent.remove(player.getUniqueId());
        }
    }
}
