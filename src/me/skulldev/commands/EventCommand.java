package me.skulldev.commands;

import me.skulldev.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.IOException;

public class EventCommand implements CommandExecutor {

    public Main main;

    public EventCommand(Main pl) {
        this.main = pl;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("event")) {
            if (s instanceof ConsoleCommandSender) {
                s.sendMessage("Only Players can use this command!");
                return true;
            } else {
                Player player = (Player) s;
                    if (args.length >= 1) {
                        if (args[0].equalsIgnoreCase("start")) {
                            if (player.hasPermission(main.getUsePermission())) {
                                main.openStartInventory(player);
                            } else {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                                return true;
                            }
                        } else if (args[0].equalsIgnoreCase("help")) {
                            if(player.hasPermission(main.getHelpPermission())) {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getHelpMessages(1).replaceAll("<Command>", "/event join")));
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getHelpMessages(1).replaceAll("<Command>", "/event leave")));
                                if (player.hasPermission(main.getUsePermission())) {
                                    player.sendMessage(main.translateColors(main.getPrefix() + main.getHelpMessages(1).replaceAll("<Command>", "/event start")));
                                }
                                if (player.hasPermission(main.getSetSpawnPermission())) {
                                    player.sendMessage(main.translateColors(main.getPrefix() + main.getHelpMessages(1).replaceAll("<Command>", "/event setspawn [event] [id]")));
                                }
                            } else {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                                return true;
                            }
                            return true;
                        } else if (args[0].equalsIgnoreCase("setspawn")) {
                            if (player.hasPermission(main.getSetSpawnPermission())) {
                                if (args.length >= 2) {
                                    if (args[1].equalsIgnoreCase("sumo")) {
                                        if (args.length == 3) {
                                            if (args[2].equalsIgnoreCase("spectator")) {
                                                Location loc = player.getLocation();
                                                main.setSpawnLocation("sumo", "spectator", loc, player);
                                            } else if (args[2].equalsIgnoreCase("arenapos1")) {
                                                Location loc = player.getLocation();
                                                main.setSpawnLocation("sumo", "arenapos1", loc, player);
                                            } else if (args[2].equalsIgnoreCase("arenapos2")) {
                                                Location loc = player.getLocation();
                                                main.setSpawnLocation("sumo", "arenapos2", loc, player);
                                            } else {
                                                player.sendMessage(main.translateColors(main.getPrefix() + main.getInvalidArgs()).replaceAll("<Command>", "/event setspawn sumo [spectator/arenapos1/arenapos2]"));
                                                return true;
                                            }
                                        } else {
                                            player.sendMessage(main.translateColors(main.getPrefix() + main.getInvalidArgs()).replaceAll("<Command>", "/event setspawn [event] [id]"));
                                            return true;
                                        }
                                    } else {
                                        player.sendMessage(main.translateColors(main.getPrefix() + main.getInvalidArgs()).replaceAll("<Command>", "/event setspawn [event] [id]"));
                                        return true;
                                    }
                                    /**
                                     * Add on other events later
                                     **/
                                } else {
                                    player.sendMessage(main.translateColors(main.getPrefix() + main.getInvalidArgs()).replaceAll("<Command>", "/event help"));
                                    return true;
                                }
                            } else {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                                return true;
                            }
                        } else if(args[0].equalsIgnoreCase("join")) {
                            if(player.hasPermission(main.getJoinPermission())) {
                                if(main.inEvent.contains(player.getUniqueId())) {
                                    player.sendMessage(main.translateColors(main.getPrefix() + main.getAlreadyInEventMessage()));
                                } else {
                                    if(main.getAmountOfEventPlayers() >= main.getMaxSumoPlayers()) {
                                        if(main.getSumoActive()) {
                                            player.sendMessage(main.translateColors(main.getPrefix() + main.getEventFullMessage().replaceAll("<Event>", main.getSumoTitle())));
                                        }
                                    }
                                    if(main.getSumoActive()) {
                                        if(!main.getSumoInGame()) {
                                            player.sendMessage(main.translateColors(main.getPrefix() + main.getEventJoinMessage().replaceAll("<Event>", main.getSumoTitle())));
                                            Bukkit.getServer().broadcastMessage(main.translateColors(main.getPlayerJoinBroadcastMessage().replaceAll("<Player>", player.getName()).replaceAll("<Event>", main.getSumoTitle())));
                                            main.inEvent.add(player.getUniqueId());
                                            main.sumoTeleportToSpectator(player);
                                            main.getInventoryStorageConfig().set(player.getUniqueId().toString() + ".inventory", Main.toBase64(player.getInventory()));
                                            main.getInventoryStorageConfig().set(player.getUniqueId().toString() + ".armor", Main.itemStackArrayToBase64(player.getInventory().getArmorContents()));
                                            main.saveInventoryStorageConfig();
                                            player.getInventory().clear();
                                            player.getInventory().setArmorContents(null);
                                            player.setGameMode(GameMode.SURVIVAL);
                                        } else {
                                            player.sendMessage(main.translateColors(main.getPrefix() + main.getEventAlreadyStartedMessage().replaceAll("<Event>", main.getSumoTitle())));
                                        }
                                    } else {
                                        player.sendMessage(main.translateColors(main.getPrefix() + main.getNoActiveEventMessage()));
                                    }
                                }
                            } else {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                                return true;
                            }

                        } else if(args[0].equalsIgnoreCase("leave")) {
                            if(player.hasPermission(main.getLeavePermission())) {
                                if(main.inEvent.contains(player.getUniqueId())) {
                                    if(main.getSumoActive()) {
                                        player.sendMessage(main.translateColors(main.getPrefix() + main.getEventLeaveMessage().replaceAll("<Event>", main.getSumoTitle())));
                                    }
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
                                } else {
                                    player.sendMessage(main.translateColors(main.getPrefix() + main.getNotInEventMessage()));
                                }
                            } else {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                                return true;
                            }
                        } else if(args[0].equalsIgnoreCase("spectate")) {
                            if(player.hasPermission(main.getSpectatePermission())) {
                                if(main.getSumoActive()) {
                                    main.sumoTeleportToSpectator(player);
                                } else {
                                    player.sendMessage(main.translateColors(main.getPrefix() + main.getNoActiveEventMessage()));
                                }
                            } else {
                                player.sendMessage(main.translateColors(main.getPrefix() + main.getNoPermission()));
                                return true;
                            }
                        } else {
                            player.sendMessage(main.translateColors(main.getPrefix() + main.getInvalidArgs()).replaceAll("<Command>", "/event help"));
                            return true;
                        }
                    } else {
                        player.sendMessage(main.translateColors(main.getPrefix() + main.getInvalidArgs()).replaceAll("<Command>", "/event help"));
                        return true;
                    }
            }
        }
        return false;
    }
}
