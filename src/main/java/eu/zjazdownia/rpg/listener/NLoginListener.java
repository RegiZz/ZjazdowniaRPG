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
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            accountGUI.openFor(p);
        }, 20L);
    }

    @EventHandler
    public void onLogin(PremiumLoginEvent e) {
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            accountGUI.openFor(p);
        }, 20L);
    }

    @EventHandler
    public void onLogin(SessionLoginEvent e) {
        Player p = e.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            accountGUI.openFor(p);
        }, 20L);
    }
}
