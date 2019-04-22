package me.skulldev.events;

import me.skulldev.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;
import java.util.UUID;

public class MoveEvent implements Listener {

    public Main main;

    public MoveEvent(Main pl) {
        this.main = pl;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if(main.inSumoFight.contains(player.getUniqueId())) {
            if(main.getFightCooldown()) {
                e.setCancelled(true);
            } else {
                double y = player.getLocation().getY();
                Location arenapos1 = main.getSpawnLocation("sumo", "arenapos1");
                double outOfBounds = arenapos1.getY() - 2;

                if(y <= outOfBounds) {
                    int i = 0;
                    while (i < main.inSumoFight.size()) {
                        UUID p = UUID.fromString(main.inSumoFight.get(i).toString());
                        System.out.println(p);
                        main.sumoTeleportToSpectator(Bukkit.getServer().getPlayer(p));
                        i++;
                    }
                    if(main.getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".inventory") != null ) {
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
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "spawn " + player.getName());
                    main.inSumoFight.remove(player.getUniqueId());
                    main.inEvent.add(main.inSumoFight.get(0));
                    UUID p = UUID.fromString(main.inSumoFight.get(0).toString());
                    Bukkit.getServer().broadcastMessage(main.translateColors(main.getWinFightMessage().replaceAll("<Player>", Bukkit.getServer().getPlayer(p).getName())));
                    main.inSumoFight.clear();
                    main.sumoArenaFight();
                }
            }
        }
    }
}
