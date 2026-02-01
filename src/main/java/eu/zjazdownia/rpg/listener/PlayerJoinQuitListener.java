package eu.zjazdownia.rpg.listener;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener join/quit: przy joinie ładuje dane konta (ensureLoaded), przy quit zapisuje (flush).
 */
public class PlayerJoinQuitListener implements Listener {

    private final ZjazdowniaRPG plugin;

    public PlayerJoinQuitListener(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.accounts().ensureLoaded(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.accounts().flush(e.getPlayer().getUniqueId());

    }
}
