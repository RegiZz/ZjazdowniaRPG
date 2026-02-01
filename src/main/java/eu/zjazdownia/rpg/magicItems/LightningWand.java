package eu.zjazdownia.rpg.magicItems;

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

    private final long COOLDOWN_MS = 50_000;

    public LightningWand(ZjazdowniaRPG plugin) {
        this.plugin = plugin;
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

        RayTraceResult result = player.rayTraceBlocks(100);
        if (result == null) return;

        Block hitBlock = result.getHitBlock();
        if (hitBlock == null) return;

        Location strikeLocation = hitBlock.getLocation().add(0.5, 1, 0.5);


        cooldowns.put(uuid, Long.valueOf(now + COOLDOWN_MS));

        int lvl = plugin.accounts().getLevel(player.getUniqueId());
        double flat = plugin.levels().getAttackPerLevelMage() * Math.max(0, lvl - 1);

        int lightningCount = 4;
        double damage = 14.0 + flat;
        double radius = 2.5;
        double spread = 4.5;


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
