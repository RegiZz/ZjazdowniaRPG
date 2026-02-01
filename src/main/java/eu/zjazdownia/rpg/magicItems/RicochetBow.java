package eu.zjazdownia.rpg.magicItems;

import eu.zjazdownia.rpg.ZjazdowniaRPG;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/**
 * Ricochet Bow: łuk ze strzałami odbijającymi się do kolejnego LivingEntity w zasięgu
 * po trafieniu (liczba odbić w PDC ricochet_bow/ricochet_left). Obrażenia maleją przy każdym odbiciu.
 */
public class RicochetBow implements Listener {

    /** Referencja pluginu do tworzenia NamespacedKey w getItem() (statyczna metoda). */
    public static ZjazdowniaRPG plugin;

    private int defaultJumps;
    private double searchRange;
    private double damageMultiplierPerBounce;
    private double arrowSpeedMultiplier;

    public RicochetBow(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        RicochetBow.plugin = plugin;
        reloadFromConfig();
    }

    /** Przeładowuje parametry z config.yml (ricochet-bow.*). */
    public void reloadFromConfig() {
        var cfg = plugin.getConfig();
        defaultJumps = Math.max(1, cfg.getInt("ricochet-bow.default-jumps", 4));
        searchRange = Math.max(1, cfg.getDouble("ricochet-bow.search-range", 10));
        damageMultiplierPerBounce = Math.max(0.1, Math.min(1.0, cfg.getDouble("ricochet-bow.damage-multiplier-per-bounce", 0.9)));
        arrowSpeedMultiplier = Math.max(0.5, cfg.getDouble("ricochet-bow.arrow-speed-multiplier", 2.5));
    }

    /** Tworzy łuk z PDC ricochet_bow (liczba odbić z configu). */
    public static ItemStack getItem() {
        ItemStack rBow = new ItemStack(Material.BOW);
        ItemMeta meta = rBow.getItemMeta();
        if (meta != null && plugin != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9RicoshetBow"));
            int jumps = plugin.getConfig().getInt("ricochet-bow.default-jumps", 4);
            jumps = Math.max(1, jumps);
            NamespacedKey key = new NamespacedKey(plugin, "ricochet_bow");
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, jumps);

            rBow.setItemMeta(meta);
        }

        return rBow;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        NamespacedKey key = new NamespacedKey(plugin, "ricochet_bow");
        if (!bow.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return;

        Arrow arrow = (Arrow) event.getProjectile();

        int jumps = bow.getItemMeta()
                .getPersistentDataContainer()
                .get(key, PersistentDataType.INTEGER);

        arrow.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "ricochet_left"),
                PersistentDataType.INTEGER,
                jumps
        );
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!(event.getHitEntity() instanceof LivingEntity hit)) return;

        NamespacedKey jumpsKey = new NamespacedKey(plugin, "ricochet_left");
        Integer jumpsLeft = arrow.getPersistentDataContainer()
                .get(jumpsKey, PersistentDataType.INTEGER);

        if (jumpsLeft == null) return;

        hit.damage(arrow.getDamage(), player);

        if (jumpsLeft <= 0) {
            arrow.remove();
            return;
        }

        LivingEntity nextTarget = null;

        for (Entity e : hit.getNearbyEntities(searchRange, searchRange, searchRange)) {
            if (e instanceof LivingEntity le && le != hit && le != player) {
                nextTarget = le;
                break;
            }
        }

        if (nextTarget == null) {
            arrow.remove();
            return;
        }

        event.setCancelled(true);

        Vector direction = nextTarget.getEyeLocation()
                .toVector()
                .subtract(hit.getEyeLocation().toVector())
                .normalize();

        arrow.teleport(hit.getEyeLocation());
        arrow.setVelocity(direction.multiply(arrowSpeedMultiplier));

        arrow.getPersistentDataContainer().set(
                jumpsKey,
                PersistentDataType.INTEGER,
                jumpsLeft - 1
        );

        arrow.setDamage(arrow.getDamage() * damageMultiplierPerBounce);
    }



}
