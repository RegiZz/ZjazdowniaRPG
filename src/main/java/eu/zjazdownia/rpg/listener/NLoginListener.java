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

/**
 * Listener nLogin: po zalogowaniu/rejestracji/sesji otwiera AccountGUI (wybór konta)
 * z opóźnieniem 1 sekunda.
 */
public class NLoginListener implements Listener {

    private final ZjazdowniaRPG plugin;
    private AccountGUI accountGUI;

    public NLoginListener(ZjazdowniaRPG plugin, AccountGUI accountGUI) {
        this.plugin = plugin;
        this.accountGUI = accountGUI;
    }

    private long getGuiOpenDelayTicks() {
        return Math.max(0, plugin.getConfig().getLong("nlogin.gui-open-delay-ticks", 20));
    }

    @EventHandler
    public void onLogin(RegisterEvent e){
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> accountGUI.openFor(p), getGuiOpenDelayTicks());
    }

    @EventHandler
    public void onLogin(LoginEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> accountGUI.openFor(p), getGuiOpenDelayTicks());
    }

    @EventHandler
    public void onLogin(PremiumLoginEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> accountGUI.openFor(p), getGuiOpenDelayTicks());
    }

    @EventHandler
    public void onLogin(SessionLoginEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> accountGUI.openFor(p), getGuiOpenDelayTicks());
    }
}
