package eu.zjazdownia.rpg;

import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.cities.City;
import eu.zjazdownia.rpg.classes.ClassAbilities;
import eu.zjazdownia.rpg.commands.*;
import eu.zjazdownia.rpg.gui.AccountGUI;
import eu.zjazdownia.rpg.gui.ClassGUI;
import eu.zjazdownia.rpg.level.LevelManager;
import eu.zjazdownia.rpg.level.LevelingListener;
import eu.zjazdownia.rpg.listener.MobSpawnListener;
import eu.zjazdownia.rpg.listener.NLoginListener;
import eu.zjazdownia.rpg.listener.PlayerInCityListener;
import eu.zjazdownia.rpg.listener.PlayerJoinQuitListener;
import eu.zjazdownia.rpg.magicItems.Heartstone;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZjazdowniaRPG extends JavaPlugin implements Listener {
    private static ZjazdowniaRPG instance;

    private AccountManager accountManager;
    private BoardManager boardManager;
    private AccountGUI accountGUI;
    private ClassGUI classGUI;
    private LevelManager levelManager;
    private Heartstone  heartstone;

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
        this.accountGUI = new AccountGUI(this);
        this.classGUI = new ClassGUI(this);
        this.heartstone = new Heartstone(this, cityCommands);

// inicjalizuj levelManager przed rejestracją listenerów, żeby inne klasy mogły z niego korzystać
        this.levelManager = new LevelManager(this);

        cityCommands.loadCities();

// rejestracja listenerów
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
// jeśli chcesz trzymać referencję do ClassAbilities, możesz utworzyć instancję i zarejestrować ją:
        getServer().getPluginManager().registerEvents(new ClassAbilities(this), this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(accountGUI, this);
        getServer().getPluginManager().registerEvents(classGUI, this);
// rejestruj LevelingListener z instancją levelManager
        getServer().getPluginManager().registerEvents(new LevelingListener(this, this.levelManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInCityListener(cityCommands), this);
        Bukkit.getPluginManager().registerEvents(cityCommands, this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(cityCommands, this), this);
        Bukkit.getPluginManager().registerEvents(new NLoginListener(this, this.accountGUI),  this);
        Bukkit.getPluginManager().registerEvents(this.heartstone, this);

// komendy
        getCommand("konto").setExecutor(new KontoCommand(accountGUI));
        getCommand("klasa").setExecutor(new KlasaCommand(classGUI, accountManager));
        getCommand("resetkonto").setExecutor(new ResetKontoCommand(accountManager));
        getCommand("zradmin").setExecutor(new ZRAdminCommand(this));
        getCommand("city").setExecutor(cityCommands);

        getLogger().info("ZjazdowniaRPG wlaczone.");
    }

    @Override
    public void onDisable() {
// zapisz pozycje wszystkich online
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

    private Location citySpawn(Location lastPlayerLoacation){
        CityComands CityCommands = new CityComands(this);
        City city = CityCommands.findCityAt(lastPlayerLoacation);

        return city.getLocation();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        accountManager.saveLastLocation(p.getUniqueId(), p.getLocation());
        boardManager.hide(p);
        accountManager.saveInventory(p.getUniqueId(), accountManager.getCurrentAccount(p.getUniqueId()), p.getInventory().getContents());
        accountManager.flush(p.getUniqueId());
    }



    public AccountManager accounts() { return accountManager; }
    public BoardManager board() { return boardManager; }
    public ClassGUI classGUI() { return classGUI; }
    public LevelManager levels() { return levelManager; }
}