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

public class RicochetBow implements Listener {

    public static ZjazdowniaRPG plugin;

    public RicochetBow(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
    }

    public static ItemStack getItem() {
        ItemStack rBow = new ItemStack(Material.BOW);
        ItemMeta meta = rBow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9RicoshetBow"));
            NamespacedKey key = new NamespacedKey(plugin, "ricochet_bow");
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 4);

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

        // ZADAJ DAMAGE RĘCZNIE ZAWSZE
        hit.damage(arrow.getDamage(), player);

        if (jumpsLeft <= 0) {
            arrow.remove();
            return;
        }

        LivingEntity nextTarget = null;
        double range = 10;

        for (Entity e : hit.getNearbyEntities(range, range, range)) {
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
        arrow.setVelocity(direction.multiply(2.5));

        arrow.getPersistentDataContainer().set(
                jumpsKey,
                PersistentDataType.INTEGER,
                jumpsLeft - 1
        );

        arrow.setDamage(arrow.getDamage() * 0.9);
    }



}
