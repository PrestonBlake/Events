package me.skulldev;

import me.skulldev.commands.EventCommand;
import me.skulldev.events.*;
import org.bukkit.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin {

    private File configFile;
    private FileConfiguration config;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private File sumoFile;
    private FileConfiguration sumoConfig;
    private File inventoryStorageFile;
    private FileConfiguration inventoryStorageConfig;

    private boolean sumoActive;
    private boolean sumoInGame;
    private boolean fightCooldown;

    public List inEvent = new ArrayList<UUID>();
    public List inSumoFight = new ArrayList<UUID>();
    public int eventSize = inEvent.size();

    private int counter;
    private int counter2;
    private int preFightCooldownTime;

    @Override
    public void onEnable() {
        registerCommands();
        registerEvents();

        //Configs
        createConfig();
        createMessagesConfig();
        createSumoConfig();
        createInventoryStorageConfig();
        saveConfig();
        saveMessagesConfig();
        saveSumoConfig();
        saveInventoryStorageConfig();
    }

    @Override
    public void onDisable() {
        inEvent.clear();
        inSumoFight.clear();
        Bukkit.getServer().getScheduler().cancelTasks(this);
    }

    public void registerCommands() {
        this.getCommand("event").setExecutor(new EventCommand(this));
    }

    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryInteractionEvents(this), this);
        pm.registerEvents(new JoinLeaveEvents(this), this);
        pm.registerEvents(new DeathEvent(this), this);
        pm.registerEvents(new MoveEvent(this), this);
        pm.registerEvents(new DamageEvent(this), this);
    }

    public String translateColors(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void openStartInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, translateColors(getStartInventoryTitle()));

        ItemStack sumoItem = new ItemStack(Material.BAKED_POTATO);
        ItemMeta sumItemM = sumoItem.getItemMeta();
        sumItemM.setDisplayName(translateColors(getSumoTitle()));

        List lore = new ArrayList<String>();
        lore.add(translateColors(getLoresline1()).replaceAll("<Event>", translateColors(getSumoTitle())));

        sumItemM.setLore(lore);
        sumoItem.setItemMeta(sumItemM);

        ItemStack spacer = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta spacerM = spacer.getItemMeta();
        spacerM.setDisplayName(" ");
        spacerM.setLore(Arrays.asList(" "));
        spacer.setItemMeta(spacerM);

        inv.setItem(0, spacer);
        inv.setItem(1, spacer);
        inv.setItem(2, spacer);
        inv.setItem(3, spacer);
        inv.setItem(4, spacer);
        inv.setItem(5, spacer);
        inv.setItem(6, spacer);
        inv.setItem(7, spacer);
        inv.setItem(8, spacer);
        inv.setItem(9, spacer);
        inv.setItem(17, spacer);
        inv.setItem(18, spacer);
        inv.setItem(19, spacer);
        inv.setItem(20, spacer);
        inv.setItem(21, spacer);
        inv.setItem(22, spacer);
        inv.setItem(23, spacer);
        inv.setItem(24, spacer);
        inv.setItem(25, spacer);
        inv.setItem(26, spacer);
        inv.setItem(13, sumoItem);
        player.openInventory(inv);
    }

    public void startSumoEvent(Player player) {
        if(!getSumoActive()) {
            if(areSpawnPointsSet()) {
                player.closeInventory();
                startSumoCountdown();
                setSumoActive(true);
            } else {
                player.sendMessage(translateColors(getPrefix() + getSetSpawnPointsError().replaceAll("<Event>", getSumoTitle())));
                player.closeInventory();
            }
        }
    }

    public void startSumoCountdown() {
        setCountDown(getSumoCountDownTime());
        int id = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                Bukkit.getServer().broadcastMessage(translateColors(getSumoBroadcastMessage(1).replaceAll("<Event>", getSumoTitle()).replaceAll("<Time>", String.valueOf(counter))));
                Bukkit.getServer().broadcastMessage(translateColors(getSumoBroadcastMessage(2).replaceAll("<Command>", "/event join")));
                Bukkit.getServer().broadcastMessage(translateColors(getSumoBroadcastMessage(3).replaceAll("<Players>", String.valueOf(getAmountOfEventPlayers())).replaceAll("<Max>", String.valueOf(getMaxSumoPlayers()))));
                setCountDown(getCountDown() - 10);
            }
        }, 0, 200);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                if(getAmountOfEventPlayers() < getMinSumoPlayers()) {
                    Bukkit.getServer().broadcastMessage(translateColors(getNotEnoughPlayersMessage().replaceAll("<Event>", getSumoTitle())));
                    Bukkit.getServer().getScheduler().cancelTask(id);
                    inEvent.clear();
                } else {
                    Bukkit.getServer().broadcastMessage(translateColors(getStartMessage().replaceAll("<Event>", getSumoTitle())));
                    Bukkit.getServer().getScheduler().cancelTask(id);
                    startSumo();
                }
            }
        }, getSumoCountDownTime()*20);
    }

    public void startSumo() {
        setSumoInGame(true);
        int i = 0;
        while (i < inEvent.size()) {
            if(isUUIDOnline((UUID) inEvent.get(i))) {
                UUID target = (UUID) inEvent.get(i);
                sumoTeleportToSpectator(getPlayerByUuid(target));
            } else {
                inEvent.remove(i);
            }
            i++;
        }
        sumoArenaFight();
    }

    public void endSumo(Player player) {
        Bukkit.getServer().broadcastMessage(translateColors(getPrefix() + getWinEventMessage().replaceAll("<Player>", player.getName()).replaceAll("<Event>", getSumoTitle())));
        if(getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".inventory") !=null ) {
            try {
                Inventory inv = Main.fromBase64(getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".inventory"));
                player.getInventory().setContents(inv.getContents());
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }

        if(getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".armor") != null) {
            try {
                player.getInventory().setArmorContents(Main.itemStackArrayFromBase64(getInventoryStorageConfig().getString(player.getUniqueId().toString() + ".armor")));
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        saveInventoryStorageConfig();
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "spawn " + player.getName());
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), getSumoEventReward().replaceAll("<Player>", player.getName()));
        setSumoInGame(false);
        setSumoActive(false);
        inEvent.clear();
        inSumoFight.clear();
    }

    public void sumoArenaFight() {
        setCountDown2(getCountDownTimeBetweenSumoFights());
        if(getAmountOfEventPlayers() >=2) {
            int id = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    Bukkit.getServer().broadcastMessage(translateColors(getBetweenFightsBroadcastMessage().replaceAll("<Time>", String.valueOf(counter2))));
                    setCountDown2(getCountDown2() - 1);
                }
            }, 0, 20);
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    List players = getRandomPlayers();
                    UUID p1 = UUID.fromString(players.get(0).toString());
                    UUID p2 = UUID.fromString(players.get(1).toString());
                    Player player1 = Bukkit.getServer().getPlayer(p1);
                    Player player2 = Bukkit.getServer().getPlayer(p2);
                    sumoTeleportToArenaPos1(player1);
                    sumoTeleportToArenaPos2(player2);
                    inSumoFight.add(player1.getUniqueId());
                    inSumoFight.add(player2.getUniqueId());
                    Bukkit.getServer().broadcastMessage(translateColors(getSumoFightStartMessage().replaceAll("<Player1>", player1.getName()).replaceAll("<Player2>", player2.getName())));
                    Bukkit.getServer().getScheduler().cancelTask(id);
                    preFightCooldown();
                }
            }, 20*getCountDownTimeBetweenSumoFights());
        } else {
            UUID p = UUID.fromString(inEvent.get(0).toString());
            endSumo(Bukkit.getServer().getPlayer(p));
        }
    }

    public void preFightCooldown() {
        setPreFightCooldown(getSumoPreFightCooldown());
        int id = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                setFightCooldown(true);
                Bukkit.getServer().broadcastMessage(translateColors("&c" + getPreFightCooldown()));
                setPreFightCooldown((getPreFightCooldown() - 1));
            }
        }, 0, 20);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                setFightCooldown(false);
                Bukkit.getServer().broadcastMessage(translateColors("&cFight!"));
                Bukkit.getServer().getScheduler().cancelTask(id);
            }
        }, 20*getSumoPreFightCooldown());
    }

    public void sumoTeleportToSpectator(Player player) {
        player.teleport(getSpawnLocation("sumo", "spectator"));
    }

    public void sumoTeleportToArenaPos1(Player player) {
        System.out.println(player);
        player.teleport(getSpawnLocation("sumo", "arenaPos1"));

    }

    public void sumoTeleportToArenaPos2(Player player) {
        player.teleport(getSpawnLocation("sumo", "arenaPos2"));
    }

    private boolean areSpawnPointsSet() {

        if(getSumoConfig().getString("spectatorSpawn.x")==null || getSumoConfig().getString("spectatorSpawn.y")==null || getSumoConfig().getString("spectatorSpawn.z")==null || getSumoConfig().getString("spectatorSpawn.yaw")==null || getSumoConfig().getString("spectatorSpawn.world")==null || getSumoConfig().getString("spectatorSpawn.pitch")==null) {
            return false;
        } else if(getSumoConfig().getString("arenaPos1Spawn.x")==null || getSumoConfig().getString("arenaPos1Spawn.y")==null || getSumoConfig().getString("arenaPos1Spawn.z")==null || getSumoConfig().getString("arenaPos1Spawn.world")==null || getSumoConfig().getString("arenaPos1Spawn.yaw")==null || getSumoConfig().getString("arenaPos1Spawn.pitch")==null) {
            return false;
        } else if(getSumoConfig().getString("arenaPos2Spawn.x")==null || getSumoConfig().getString("arenaPos2Spawn.y")==null || getSumoConfig().getString("arenaPos2Spawn.z")==null || getSumoConfig().getString("arenaPos2Spawn.world")==null || getSumoConfig().getString("arenaPos2Spawn.yaw")==null || getSumoConfig().getString("arenaPos2Spawn.pitch")==null) {
            return false;
        }

        return true;
    }

    /**
     * Configs
     */
    public FileConfiguration getConfig() {
        return this.config;
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    public FileConfiguration getSumoConfig() {
        return this.sumoConfig;
    }

    public FileConfiguration getInventoryStorageConfig() {
        return this.inventoryStorageConfig;
    }

    private void createConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        config= new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }

        messagesConfig= new YamlConfiguration();
        try {
            messagesConfig.load(messagesFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void createSumoConfig() {
        sumoFile = new File(getDataFolder(), "sumo.yml");
        if (!sumoFile.exists()) {
            sumoFile.getParentFile().mkdirs();
            saveResource("sumo.yml", false);
        }

        sumoConfig= new YamlConfiguration();
        try {
            sumoConfig.load(sumoFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void createInventoryStorageConfig() {
        inventoryStorageFile = new File(getDataFolder(), "storage.yml");
        if (!inventoryStorageFile.exists()) {
            inventoryStorageFile.getParentFile().mkdirs();
            saveResource("storage.yml", false);
        }

        inventoryStorageConfig= new YamlConfiguration();
        try {
            inventoryStorageConfig.load(inventoryStorageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSumoConfig() {
        try {
            sumoConfig.save(sumoFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void saveInventoryStorageConfig() {
        try {
            inventoryStorageConfig.save(inventoryStorageFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Strings
     */
    public String getUsePermission() {
        return "events.use";
    }
    public String getUseSumoPermission() {
        return "events.use.sumo";
    }
    public String getSetSpawnPermission() {
        return "events.set.spawn";
    }
    public String getJoinPermission() { return "events.join"; }
    public String getLeavePermission() { return "events.leave"; }
    public String getSpectatePermission() { return "events.spectate"; }
    public String getHelpPermission() { return "events.help"; }
    public String getPrefix() {
        return this.getConfig().getString("prefix");
    }
    public String getServerName() {
        return this.getConfig().getString("serverName");
    }
    public String getNoPermission() {
        return this.getMessagesConfig().getString("noPermission");
    }
    public String getInvalidArgs() {
        return this.getMessagesConfig().getString("invalidArgs");
    }
    public String getStartInventoryTitle() {
        return this.getConfig().getString("startInventoryTitle");
    }
    public String getLoresline1() {
        return this.getMessagesConfig().getString("lores.line1");
    }
    public String getSumoTitle() {
        return this.getSumoConfig().getString("title");
    }
    public String getSumoBroadcastMessage(int i) {
        if(i==1) {
            return this.getMessagesConfig().getString("broadcastMessage.line1");
        } else if(i==2) {
            return this.getMessagesConfig().getString("broadcastMessage.line2");
        } else if(i==3) {
            return this.getMessagesConfig().getString("broadcastMessage.line3");
        }
        return null;
    }
    public String getStartMessage() {
        return this.getMessagesConfig().getString("eventStarting");
    }
    public int getCountDown() {
        return counter;
    }
    public void setCountDown(int i) {
        this.counter = i;
    }
    public int getCountDown2() {
        return counter2;
    }
    public void setCountDown2(int i) {
        this.counter2 = i;
    }
    public void setPreFightCooldown(int i) {
        this.preFightCooldownTime = i;
    }
    public int getPreFightCooldown() {
        return preFightCooldownTime;
    }
    public int getSumoCountDownTime() {
        return this.getSumoConfig().getInt("countdown");
    }
    public int getCountDownTimeBetweenSumoFights() {
        return this.getSumoConfig().getInt("betweenFightsCountdown");
    }
    public int getSumoPreFightCooldown() { return this.getSumoConfig().getInt("preFightCooldown"); }
    public boolean getSumoActive() {
        return sumoActive;
    }
    public void setSumoActive(boolean bool) {
        this.sumoActive = bool;
    }
    public boolean getSumoInGame() {
        return sumoInGame;
    }
    public void setSumoInGame(boolean bool) {
        this.sumoInGame = bool;
    }
    public boolean getFightCooldown() {
        return fightCooldown;
    }
    public void setFightCooldown(boolean bool) {
        this.fightCooldown = bool;
    }
    public String getSetSpawnPointsError() {
        return this.getMessagesConfig().getString("setSpawnPointsError");
    }
    public String getHelpMessages(int i) {
        if(i == 1) {
            return this.getMessagesConfig().getString("helpMessages.line1");
        } else if(i == 2) {
            return this.getMessagesConfig().getString("helpMessages.line2");
        } else if(i == 3) {
            return this.getMessagesConfig().getString("helpMessages.line3");
        }
        return null;
    }
    public String getSetSpawnMessages(int i) {
        if(i == 1) {
            return this.getMessagesConfig().getString("setSpawnMessages.line1");
        } else if(i == 2) {
            return this.getMessagesConfig().getString("setSpawnMessages.line2");
        } else if(i == 3) {
            return this.getMessagesConfig().getString("setSpawnMessages.line3");
        } else if(i == 4) {
            return this.getMessagesConfig().getString("setSpawnMessages.line4");
        } else if(i == 5) {
            return this.getMessagesConfig().getString("setSpawnMessages.line5");
        }
        return null;
    }

    public Player getPlayerByUuid(UUID uuid) {
        for(Player p : getServer().getOnlinePlayers()) {
            if(p.getUniqueId().equals(uuid)) {
                return p;
            }
        }
        return null;
    }
    public boolean isUUIDOnline(UUID uuid) {
        for(Player p : getServer().getOnlinePlayers()) {
            if(p.getUniqueId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void setSpawnLocation(String event, String id, Location loc, Player player) {
        if(event.equalsIgnoreCase("sumo")) {
            if(id.equalsIgnoreCase("spectator")) {
                this.getSumoConfig().set("spectatorSpawn.x", loc.getX());
                this.getSumoConfig().set("spectatorSpawn.y", loc.getY());
                this.getSumoConfig().set("spectatorSpawn.z", loc.getZ());
                this.getSumoConfig().set("spectatorSpawn.world", loc.getWorld().getName());
                this.getSumoConfig().set("spectatorSpawn.pitch", loc.getPitch());
                this.getSumoConfig().set("spectatorSpawn.yaw", loc.getYaw());
                player.sendMessage(translateColors(getSetSpawnMessages(1).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "spectator").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(2).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "spectator").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(3).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "spectator").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(4).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "spectator").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(5).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "spectator").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                this.saveSumoConfig();
            } else if(id.equalsIgnoreCase("arenapos1")) {
                this.getSumoConfig().set("arenaPos1Spawn.x", loc.getX());
                this.getSumoConfig().set("arenaPos1Spawn.y", loc.getY());
                this.getSumoConfig().set("arenaPos1Spawn.z", loc.getZ());
                this.getSumoConfig().set("arenaPos1Spawn.world", loc.getWorld().getName());
                this.getSumoConfig().set("arenaPos1Spawn.pitch", loc.getPitch());
                this.getSumoConfig().set("arenaPos1Spawn.yaw", loc.getYaw());
                player.sendMessage(translateColors(getSetSpawnMessages(1).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos1").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(2).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos1").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(3).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos1").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(4).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos1").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(5).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos1").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                this.saveSumoConfig();
            } else if(id.equalsIgnoreCase("arenapos2")) {
                this.getSumoConfig().set("arenaPos2Spawn.x", loc.getX());
                this.getSumoConfig().set("arenaPos2Spawn.y", loc.getY());
                this.getSumoConfig().set("arenaPos2Spawn.z", loc.getZ());
                this.getSumoConfig().set("arenaPos2Spawn.world", loc.getWorld().getName());
                this.getSumoConfig().set("arenaPos2Spawn.pitch", loc.getPitch());
                this.getSumoConfig().set("arenaPos2Spawn.yaw", loc.getYaw());
                player.sendMessage(translateColors(getSetSpawnMessages(1).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos2").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(2).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos2").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(3).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos2").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(4).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos2").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                player.sendMessage(translateColors(getSetSpawnMessages(5).replaceAll("<Event>", getSumoTitle()).replaceAll("<ID>", "arenapos2").replaceAll("<X>", String.valueOf(loc.getX())).replaceAll("<Y>", String.valueOf(loc.getY())).replaceAll("<Z>", String.valueOf(loc.getZ())).replaceAll("<WORLD>", String.valueOf(loc.getWorld()))));
                this.saveSumoConfig();
            }
        }
    }

    public Location getSpawnLocation(String event, String id) {
        if(event.equalsIgnoreCase("sumo")) {
            if(id.equalsIgnoreCase("spectator")) {
                double x = Double.valueOf(this.getSumoConfig().getString("spectatorSpawn.x"));
                double y = Double.valueOf(this.getSumoConfig().getString("spectatorSpawn.y"));
                double z = Double.valueOf(this.getSumoConfig().getString("spectatorSpawn.z"));
                String world = this.getSumoConfig().getString("spectatorSpawn.world");
                World w = Bukkit.getServer().getWorld(world);
                float yaw = Float.valueOf(this.getSumoConfig().getString("spectatorSpawn.yaw"));
                float pitch = Float.valueOf(this.getSumoConfig().getString("spectatorSpawn.pitch"));
                Location loc = new Location(w,x,y,z,yaw,pitch);
                return loc;
            } else if(id.equalsIgnoreCase("arenapos1")) {
                double x = Double.valueOf(this.getSumoConfig().getString("arenaPos1Spawn.x"));
                double y = Double.valueOf(this.getSumoConfig().getString("arenaPos1Spawn.y"));
                double z = Double.valueOf(this.getSumoConfig().getString("arenaPos1Spawn.z"));
                String world = this.getSumoConfig().getString("arenaPos1Spawn.world");
                World w = Bukkit.getServer().getWorld(world);
                float yaw = Float.valueOf(this.getSumoConfig().getString("arenaPos1Spawn.yaw"));
                float pitch = Float.valueOf(this.getSumoConfig().getString("arenaPos1Spawn.pitch"));
                Location loc = new Location(w,x,y,z,yaw,pitch);
                return loc;
            } else if(id.equalsIgnoreCase("arenapos2")) {
                double x = Double.valueOf(this.getSumoConfig().getString("arenaPos2Spawn.x"));
                double y = Double.valueOf(this.getSumoConfig().getString("arenaPos2Spawn.y"));
                double z = Double.valueOf(this.getSumoConfig().getString("arenaPos2Spawn.z"));
                String world = this.getSumoConfig().getString("arenaPos2Spawn.world");
                World w = Bukkit.getServer().getWorld(world);
                float yaw = Float.valueOf(this.getSumoConfig().getString("arenaPos2Spawn.yaw"));
                float pitch = Float.valueOf(this.getSumoConfig().getString("arenaPos2Spawn.pitch"));
                Location loc = new Location(w,x,y,z,yaw,pitch);
                return loc;
            }
            return null;
        }
        return null;
    }
    public Integer getMinSumoPlayers() {
        return this.getSumoConfig().getInt("minPlayers");
    }
    public Integer getMaxSumoPlayers() {
        return this.getSumoConfig().getInt("maxPlayers");
    }
    public String getNotEnoughPlayersMessage() {
        return this.getMessagesConfig().getString("notEnoughPlayersMessage");
    }
    public String getEventFullMessage() {
        return this.getMessagesConfig().getString("eventFullMessage");
    }
    public String getEventLeaveMessage() {
        return this.getMessagesConfig().getString("leaveEventMessage");
    }
    public String getEventJoinMessage() {
        return this.getMessagesConfig().getString("joinEventMessage");
    }
    public String getAlreadyInEventMessage() {
        return this.getMessagesConfig().getString("alreadyInEventMessage");
    }
    public String getNotInEventMessage() {
        return this.getMessagesConfig().getString("notInEventMessage");
    }
    public String getNoActiveEventMessage() {
        return this.getMessagesConfig().getString("noActiveEventMessage");
    }
    public String getEventAlreadyStartedMessage() {
        return this.getMessagesConfig().getString("eventAlreadyStartedMessage");
    }
    public Integer getAmountOfEventPlayers() {
        return inEvent.size();
    }

    public List<UUID> getRandomPlayers() {
        /**Random random = new Random();
        Player randomPlayer = Bukkit.getServer().getPlayer(inEvent.get(random.nextInt(getAmountOfEventPlayers())).toString());

        Player randomPlayer2 = Bukkit.getServer().getPlayer(inEvent.get(random.nextInt(getAmountOfEventPlayers())).toString());

        while ((randomPlayer2 == randomPlayer) && (getAmountOfEventPlayers() > 1)) {
            randomPlayer2 = Bukkit.getPlayer(inEvent.get(random.nextInt(getAmountOfEventPlayers())).toString());
        }**/
        Collections.shuffle(inEvent);
        UUID p1 = UUID.fromString(inEvent.remove(0).toString());
        UUID p2 = UUID.fromString(inEvent.remove(0).toString());
        List list = new ArrayList<UUID>();
        list.add(p1);
        list.add(p2);
        return list;
    }

    public String getBetweenFightsBroadcastMessage() {
        return this.getSumoConfig().getString("betweenFightsBroadcastMessage");
    }
    public String getSumoFightStartMessage() {
        return this.getSumoConfig().getString("startFightMessage");
    }
    public String getWinFightMessage() {
        return this.getSumoConfig().getString("winFightMessage");
    }
    public String getWinEventMessage() {
        return this.getMessagesConfig().getString("winEventMessage");
    }
    public String getSumoEventReward() { return this.getSumoConfig().getString("eventReward"); }

    public static String toBase64(Inventory inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(inventory.getSize());

            // Save every element in the list
            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.length);

            // Save every element in the list
            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static Inventory fromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Inventory inventory = Bukkit.getServer().createInventory(null, dataInput.readInt());

            // Read the serialized inventory
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();
            return inventory;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public String getPlayerJoinBroadcastMessage() {
        return this.getMessagesConfig().getString("playerJoinBroadcastMessage");
    }
}
