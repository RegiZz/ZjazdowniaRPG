package eu.zjazdownia.rpg;

import eu.zjazdownia.rpg.account.AccountManager;
import eu.zjazdownia.rpg.classes.ClassAbilities;
import eu.zjazdownia.rpg.commands.KlasaCommand;
import eu.zjazdownia.rpg.commands.KontoCommand;
import eu.zjazdownia.rpg.commands.ResetKontoCommand;
import eu.zjazdownia.rpg.gui.AccountGUI;
import eu.zjazdownia.rpg.gui.ClassGUI;
import eu.zjazdownia.rpg.level.LevelManager;
import eu.zjazdownia.rpg.level.LevelingListener;
import eu.zjazdownia.rpg.listener.PlayerJoinQuitListener;
import eu.zjazdownia.rpg.scoreboard.BoardManager;
import eu.zjazdownia.rpg.util.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class ZjazdowniaRPG extends JavaPlugin implements Listener {
    private static ZjazdowniaRPG instance;

    private AccountManager accountManager;
    private BoardManager boardManager;
    private AccountGUI accountGUI;
    private ClassGUI classGUI;
    private LevelManager levelManager; // <--- dodane

    public static ZjazdowniaRPG get() { return instance; }

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

// inicjalizuj levelManager przed rejestracją listenerów, żeby inne klasy mogły z niego korzystać
        this.levelManager = new LevelManager(this);

// rejestracja listenerów
        Bukkit.getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
// jeśli chcesz trzymać referencję do ClassAbilities, możesz utworzyć instancję i zarejestrować ją:
        getServer().getPluginManager().registerEvents(new ClassAbilities(this), this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(accountGUI, this);
        getServer().getPluginManager().registerEvents(classGUI, this);
// rejestruj LevelingListener z instancją levelManager
        getServer().getPluginManager().registerEvents(new LevelingListener(this, this.levelManager), this);

// komendy
        getCommand("konto").setExecutor(new KontoCommand(accountGUI));
        getCommand("klasa").setExecutor(new KlasaCommand(classGUI, accountManager));
        getCommand("resetkonto").setExecutor(new ResetKontoCommand(accountManager));

        getLogger().info("ZjazdowniaRPG wlaczone.");
    }

    @Override
    public void onDisable() {
// zapisz pozycje wszystkich online
        for (Player p : Bukkit.getOnlinePlayers()) {
            accountManager.saveLastLocation(p.getUniqueId(), p.getLocation());
            accountManager.flush(p.getUniqueId());
        }
        if (accountManager != null) {
            accountManager.flushAll();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
// wczytaj dane + ustaw aktualne konto na 1 (domyślnie)
        accountManager.ensureLoaded(id);
// teleport na ostatnią znaną pozycję (jeśli jest)
        Location last = accountManager.getLastLocation(id);
        if (last != null && last.getWorld() != null) {
            Bukkit.getScheduler().runTask(this, () -> p.teleport(last));
        }
// jeśli konto puste – pokaż kreator, inaczej uruchom stały scoreboard
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (accountManager.getSelectedClass(id) == null) {
                accountGUI.openFor(p);
            } else {
                boardManager.show(p, accountManager);
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        accountManager.saveLastLocation(p.getUniqueId(), p.getLocation());
        boardManager.hide(p);
        accountManager.flush(p.getUniqueId());
    }

    public AccountManager accounts() { return accountManager; }
    public BoardManager board() { return boardManager; }
    public ClassGUI classGUI() { return classGUI; }

    // getter dla LevelManager
    public LevelManager levels() { return levelManager; }
}