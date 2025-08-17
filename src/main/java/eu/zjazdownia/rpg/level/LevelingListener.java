package eu.zjazdownia.rpg.level;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LevelingListener implements Listener {
    private final ZjazdowniaRPG plugin;
    private final LevelManager levels;

    // map: target entity UUID -> last damager player UUID + time
    private final Map<UUID, UUID> lastDamagerPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDamagerTime = new ConcurrentHashMap<>();
    private final long DAMAGE_TIMEOUT_MS = 8_000L; // 8s okno

    public LevelingListener(ZjazdowniaRPG plugin, LevelManager levels) {
        this.plugin = plugin;
        this.levels = levels;
        plugin.getLogger().info("LevelingListener created");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Player player;

        if (damager instanceof Player) {
            player = (Player) damager;
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player) {
            player = (Player) proj.getShooter();
        } else {
            player = null;
        }

        if (player == null) return;

        UUID targetId = e.getEntity().getUniqueId();
        lastDamagerPlayer.put(targetId, player.getUniqueId());
        lastDamagerTime.put(targetId, System.currentTimeMillis());

        plugin.getLogger().finer(() -> "Recorded damager " + player.getName() + " for entity " + e.getEntity().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        plugin.getLogger().info("EntityDeathEvent fired for: " + e.getEntityType());
        Player killer = e.getEntity().getKiller();

        if (killer == null) {
            UUID entId = e.getEntity().getUniqueId();
            Long time = lastDamagerTime.get(entId);
            UUID pu = lastDamagerPlayer.get(entId);
            if (time != null && pu != null && (System.currentTimeMillis() - time) <= DAMAGE_TIMEOUT_MS) {
                killer = plugin.getServer().getPlayer(pu);
                plugin.getLogger().info("Fallback killer from damage map: " + (killer == null ? "player offline/null" : killer.getName()));
            } else {
                plugin.getLogger().info("No killer found and no recent damager recorded.");
            }
        } else {
            plugin.getLogger().info("getKiller() -> " + killer.getName());
        }

// cleanup
        lastDamagerPlayer.remove(e.getEntity().getUniqueId());
        lastDamagerTime.remove(e.getEntity().getUniqueId());

        if (killer == null) return;

        int xp = levels.xpForMob(e.getEntityType());
        plugin.getLogger().info("Configured xp for " + e.getEntityType() + " = " + xp);
        if (xp <= 0) return;

        levels.addExp(killer, xp);
        plugin.getLogger().info("Added " + xp + " xp to " + killer.getName());
    }
}