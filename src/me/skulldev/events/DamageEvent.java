package me.skulldev.events;

import me.skulldev.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageEvent implements Listener {

    public Main main;

    public DamageEvent(Main pl) {
        this.main = pl;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if((e.getEntity() instanceof Player) && (e.getDamager() instanceof Player)) {
            Player player = (Player) e.getEntity();
            if(main.inSumoFight.contains(player.getUniqueId())) {
                player.setHealth(20);
                player.setFoodLevel(20);
            }
        }
    }
}
