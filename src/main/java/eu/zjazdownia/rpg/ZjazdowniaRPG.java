package eu.zjazdownia.rpg;

/**
 * Główna klasa pluginu ZjazdowniaRPG dla serwera Minecraft.
 * Inicjalizuje konfigurację, menedżerów (konta, klasy, party, miasta, levelowanie),
 * rejestruje komendy i listenery oraz obsługuje teleportację graczy przy join/quit.
 */
import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.cities.City;
import eu.zjazdownia.rpg.classes.ClassAbilities;
import eu.zjazdownia.rpg.commands.*;
import eu.zjazdownia.rpg.friends.FriendsManager;
import eu.zjazdownia.rpg.gui.AccountGUI;
import eu.zjazdownia.rpg.gui.ClassGUI;
import eu.zjazdownia.rpg.gui.MenuGUI;
import eu.zjazdownia.rpg.level.LevelManager;
import eu.zjazdownia.rpg.level.LevelingListener;
import eu.zjazdownia.rpg.listener.MobSpawnListener;
import eu.zjazdownia.rpg.listener.NLoginListener;
import eu.zjazdownia.rpg.listener.PlayerInCityListener;
import eu.zjazdownia.rpg.listener.PlayerJoinQuitListener;
import eu.zjazdownia.rpg.magicItems.Heartstone;
import eu.zjazdownia.rpg.magicItems.LightningWand;
import eu.zjazdownia.rpg.magicItems.RicochetBow;
import eu.zjazdownia.rpg.party.PartyManager;
import eu.zjazdownia.rpg.scoreboard.BoardManager;
import eu.zjazdownia.rpg.util.ConfigUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZjazdowniaRPG extends JavaPlugin implements Listener {
    /** Singleton instancji pluginu. */
    private static ZjazdowniaRPG instance;

    /** Menedżer kont graczy (multi-konto, poziom, klasa, ekwipunek). */
    private AccountManager accountManager;
    private BoardManager boardManager;
    private AccountGUI accountGUI;
    private ClassGUI classGUI;
    private LevelManager levelManager;
    private Heartstone  heartstone;
    public PartyManager partyManager;
    public LightningWand lightningWand;
    public RicochetBow ricochetBow;
    /** Menedżer listy znajomych. */
    public FriendsManager friendsManager;
    /** GUI menu gracza (znajomi itd.). */
    public MenuGUI menuGUI;

    /** Komendy administracyjne miast (create, delete, showborder itd.). */
    CityComands cityCommands = new CityComands(this);

    @Override
    public void onEnable() {
        instance = this;

// config
        saveDefaultConfig();
        ConfigUtils.migrateAndFillDefaults(getConfig());
        saveConfig();

// menedżery / moduły
        this.accountManager = new AccountManager(this);
        this.boardManager = new BoardManager(this);
        this.friendsManager = new FriendsManager();
        this.menuGUI = new MenuGUI(this,friendsManager);
        this.accountGUI = new AccountGUI(this);
        this.classGUI = new ClassGUI(this);
        this.heartstone = new Heartstone(this, cityCommands);
        partyManager  = new PartyManager(this);
        this.lightningWand = new LightningWand(this);
        this.ricochetBow = new  RicochetBow(this);
        this.levelManager = new LevelManager(this);

        cityCommands.loadCities();

// rejestracja listenerów
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ClassAbilities(this), this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(accountGUI, this);
        getServer().getPluginManager().registerEvents(classGUI, this);
        getServer().getPluginManager().registerEvents(new LevelingListener(this, this.levelManager, partyManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInCityListener(this, cityCommands), this);
        Bukkit.getPluginManager().registerEvents(cityCommands, this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(cityCommands, this), this);
        Bukkit.getPluginManager().registerEvents(new NLoginListener(this, this.accountGUI),  this);
        Bukkit.getPluginManager().registerEvents(this.heartstone, this);
        Bukkit.getPluginManager().registerEvents(this.lightningWand, this);
        Bukkit.getPluginManager().registerEvents(this.ricochetBow, this);
        Bukkit.getPluginManager().registerEvents(menuGUI, this);


// komendy
        getCommand("konto").setExecutor(new KontoCommand(accountGUI));
        getCommand("klasa").setExecutor(new KlasaCommand(classGUI, accountManager));
        getCommand("resetkonto").setExecutor(new ResetKontoCommand(accountManager));
        getCommand("zradmin").setExecutor(new ZRAdminCommand(this, this.lightningWand));
        getCommand("city").setExecutor(cityCommands);
        getCommand("party").setExecutor(new PartyCommands(this, partyManager));
        getCommand("menu").setExecutor((sender, cmd, label, args)->{
            if (sender instanceof Player p) menuGUI.openFor(p);
            return true;
        });

        getLogger().info("ZjazdowniaRPG wlaczone.");
    }
    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            accountManager.saveLastLocation(p.getUniqueId(), p.getLocation());
            accountManager.flush(p.getUniqueId());
            cityCommands.saveCities();
        }
        if (accountManager != null) {
            accountManager.flushAll();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        FileConfiguration config = getConfig();

        accountManager.ensureLoaded(id);

        Location last = accountManager.getLastLocation(id);
        Location spawn = Bukkit.getWorld("world").getSpawnLocation();
        if(!p.hasPermission("zjazdownia.admin")){
            if (last != null && last.getWorld() != null) {
                Bukkit.getScheduler().runTask(this, () -> {
                    if(citySpawn(last) != null){
                        p.teleport(citySpawn(last));
                    }else{
                        p.teleport(spawn);
                    }
                });
            }
            else if(last == null){
                String worldname = config.getString("firstSpawn.world");
                double x = config.getDouble("firstSpawn.x");
                double y = config.getDouble("firstSpawn.y");
                double z = config.getDouble("firstSpawn.z");
                float yaw = (float)config.getDouble("firstSpawn.yaw");
                float pitch = (float)config.getDouble("firstSpawn.pitch");

                World world = Bukkit.getWorld(worldname);
                Location loc = new Location(world, x, y, z, yaw, pitch);

                Bukkit.getScheduler().runTask(this, () -> p.teleport(loc));
            }
        }
    }

    /**
     * Zwraca punkt spawnu miasta, w którym gracz był ostatnio (lub null).
     */
    private Location citySpawn(Location lastPlayerLoacation) {
        CityComands CityCommands = new CityComands(this);
        City city = CityCommands.findCityAt(lastPlayerLoacation);
        return city == null ? null : city.getLocation();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        accountManager.saveLastLocation(p.getUniqueId(), p.getLocation());
        boardManager.hide(p);
        accountManager.saveInventory(p.getUniqueId(), accountManager.getCurrentAccount(p.getUniqueId()), p.getInventory().getContents(), p.getInventory().getArmorContents());
        accountManager.flush(p.getUniqueId());
    }



    public AccountManager accounts() { return accountManager; }
    public BoardManager board() { return boardManager; }
    public ClassGUI classGUI() { return classGUI; }
    public LevelManager levels() { return levelManager; }
}