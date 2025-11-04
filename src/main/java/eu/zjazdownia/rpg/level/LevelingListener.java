package eu.zjazdownia.rpg.level;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
        Player killer = e.getEntity().getKiller();

        if (killer == null) {
            UUID entId = e.getEntity().getUniqueId();
            Long time = lastDamagerTime.get(entId);
            UUID pu = lastDamagerPlayer.get(entId);
            if (time != null && pu != null && (System.currentTimeMillis() - time) <= DAMAGE_TIMEOUT_MS) {
                killer = plugin.getServer().getPlayer(pu);
            }
        } else {
            plugin.getLogger().info("getKiller() -> " + killer.getName());
        }

// cleanup
        lastDamagerPlayer.remove(e.getEntity().getUniqueId());
        lastDamagerTime.remove(e.getEntity().getUniqueId());

        if (killer == null) return;

        int basexp = levels.xpForMob(e.getEntityType());
        if (basexp <= 0) return;

        LivingEntity mob = e.getEntity();
        NamespacedKey key = new NamespacedKey(plugin, "mob_level");
        PersistentDataContainer data = mob.getPersistentDataContainer();

        int mobLevel = data.getOrDefault(key, PersistentDataType.INTEGER, 1);

        // Skalowanie expa zależne od levelu moba
        int finalXp = (int) Math.round(basexp * (1.0 + (mobLevel - 1) * 0.25));

        levels.addExp(killer, finalXp);
        plugin.getLogger().info("Added " + finalXp + " xp to " + killer.getName());
    }
}