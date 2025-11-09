package eu.zjazdownia.rpg.listener;

import com.nickuc.login.api.event.bukkit.auth.LoginEvent;
import com.nickuc.login.api.event.bukkit.auth.PremiumLoginEvent;
import com.nickuc.login.api.event.bukkit.auth.RegisterEvent;
import com.nickuc.login.api.event.bukkit.auth.SessionLoginEvent;
import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.gui.AccountGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NLoginListener implements Listener {

    private final ZjazdowniaRPG plugin;
    private AccountGUI accountGUI;

    public NLoginListener(ZjazdowniaRPG plugin, AccountGUI accountGUI) {
        this.plugin = plugin;
        this.accountGUI = accountGUI;
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        plugin.getLogger().info(e.getPlayer().getName() + "zalogowal sie");
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            accountGUI.openFor(p);
        });
    }

    @EventHandler
    public void onLogin(PremiumLoginEvent e) {
        plugin.getLogger().info(e.getPlayer().getName() + "zalogowal sie");
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            accountGUI.openFor(p);
        });
    }

    @EventHandler
    public void onLogin(SessionLoginEvent e) {
        plugin.getLogger().info(e.getPlayer().getName() + "zalogowal sie");
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> {
            accountGUI.openFor(p);
        });
    }
}
