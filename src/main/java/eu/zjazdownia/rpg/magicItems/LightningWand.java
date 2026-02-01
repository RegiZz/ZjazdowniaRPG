package eu.zjazdownia.rpg.magicItems;

/**
 * Lightning Wand: przedmiot (Bamboo) — prawy klik w cel (raytrace bloków) zamiata
 * obszar błyskawicami, zadając obrażenia żywym entity. Cooldown 50s. Nie można stawiać jako blok.
 */
import eu.zjazdownia.rpg.ZjazdowniaRPG;
import eu.zjazdownia.rpg.classes.ClassAbilities;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LightningWand implements Listener {
    private final ZjazdowniaRPG plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private long cooldownMs;
    private int lightningCount;
    private double baseDamage;
    private double radius;
    private double spread;
    private int raytraceRange;

    public LightningWand(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    /** Przeładowuje parametry z config.yml (lightning-wand.*). */
    public void reloadFromConfig() {
        var cfg = plugin.getConfig();
        cooldownMs = Math.max(1000, cfg.getLong("lightning-wand.cooldown-ms", 50000));
        lightningCount = Math.max(1, cfg.getInt("lightning-wand.lightning-count", 4));
        baseDamage = Math.max(0, cfg.getDouble("lightning-wand.base-damage", 14.0));
        radius = Math.max(0.5, cfg.getDouble("lightning-wand.radius", 2.5));
        spread = Math.max(0, cfg.getDouble("lightning-wand.spread", 4.5));
        raytraceRange = Math.max(5, cfg.getInt("lightning-wand.raytrace-range", 100));
    }

    public static ItemStack createWand(){
        ItemStack lightningWand = new ItemStack(Material.BAMBOO);
        ItemMeta meta = lightningWand.getItemMeta();
        if(meta != null){
            meta.setDisplayName(ChatColor.AQUA + "Lightning Wand");
            NamespacedKey key = new NamespacedKey("zjazdownia", "undroppable");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            lightningWand.setItemMeta(meta);
        }

        return lightningWand;
    }



    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("zjazdownia", "undroppable");

        if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!e.getAction().name().contains("RIGHT_CLICK")) return;

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Lightning Wand")) return;


        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long end = cooldowns.getOrDefault(uuid, Long.valueOf(0L));

        if (now < end) {
            long secondsLeft = (end - now) / 1000;
            player.sendMessage(ChatColor.RED + "Musisz poczekać " + secondsLeft + " sekund(y), aby ponownie użyć Lightning Wand!");
            return;
        }

        RayTraceResult result = player.rayTraceBlocks(raytraceRange);
        if (result == null) return;

        Block hitBlock = result.getHitBlock();
        if (hitBlock == null) return;

        cooldowns.put(uuid, Long.valueOf(now + cooldownMs));

        int lvl = plugin.accounts().getLevel(player.getUniqueId());
        double flat = plugin.levels().getAttackPerLevelMage() * Math.max(0, lvl - 1);
        double damage = baseDamage + flat;

        Location base = hitBlock.getLocation().add(0.5, 1, 0.5);
        World world = base.getWorld();

        for (int i = 0; i < lightningCount; i++) {

            double offsetX = (Math.random() - 0.5) * spread;
            double offsetZ = (Math.random() - 0.5) * spread;

            Location strike = base.clone().add(offsetX, 0, offsetZ);

            world.strikeLightningEffect(strike);

            for (Entity entity : world.getNearbyEntities(
                    strike, radius, 3, radius)) {

                if (entity instanceof LivingEntity living) {
                    living.damage(damage, player);
                }
            }
            world.spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    base,
                    60,
                    1.2, 0.5, 1.2,
                    0.1
            );

        }

    }
}
